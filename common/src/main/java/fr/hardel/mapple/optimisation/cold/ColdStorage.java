package fr.hardel.mapple.optimisation.cold;

import fr.hardel.mapple.optimisation.section.AirSectionCacheHolder;
import fr.hardel.mapple.optimisation.section.SharedAirSection;
import io.netty.buffer.Unpooled;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.LongStream;
import net.minecraft.core.Holder;
import net.minecraft.core.IdMap;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.GenerationChunkHolder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.CarvingMask;
import net.minecraft.world.level.chunk.ImposterProtoChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.chunk.PalettedContainerFactory;
import net.minecraft.world.level.chunk.PalettedContainerRO;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.Strategy;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.jpountz.lz4.LZ4Compressor;
import net.jpountz.lz4.LZ4Factory;
import org.jspecify.annotations.Nullable;

/**
 * A proto chunk at rest keeps its sections and carving mask as one LZ4 block.
 * A shared air section is stored as its biome, an owned section in the disk layout of its palettes with registry ids.
 */
public final class ColdStorage {
    private static final LZ4Factory LZ4 = LZ4Factory.fastestInstance();
    private static final long[] NO_STORAGE = new long[0];
    private final PalettedContainerFactory factory;
    private final AirSectionCacheHolder level;
    private final LevelChunkSection empty;

    public ColdStorage(PalettedContainerFactory factory, AirSectionCacheHolder level) {
        this.factory = factory;
        this.level = level;
        this.empty = new LevelChunkSection(factory);
    }

    /** A released claim leaves the holder to its tickets: a proto chunk with terrain that no claim and no ticket will move goes cold. */
    public void rest(GenerationChunkHolder holder) {
        if (holder.generationRefCount.get() != 0 || !(holder.getLatestChunk() instanceof ProtoChunk proto) || proto instanceof ImposterProtoChunk) {
            return;
        }

        ChunkStatus status = proto.getPersistedStatus();
        ChunkStatus allowed = holder.highestAllowedStatus;
        if (status.isOrAfter(ChunkStatus.NOISE) && (allowed == null || !allowed.isAfter(status))) {
            ((ColdChunk) proto).mapple$freeze();
        }
    }

    public LevelChunkSection sentinel(ColdChunk owner, int index) {
        return new ColdSection(owner, index, this.empty);
    }

    public byte[] freeze(LevelChunkSection[] sections, @Nullable CarvingMask mask) {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        IdMap<Holder<Biome>> biomes = this.factory.biomeStrategy().globalMap();
        for (LevelChunkSection section : sections) {
            buffer.writeBoolean(section instanceof SharedAirSection);
            if (section instanceof SharedAirSection) {
                buffer.writeVarInt(biomes.getId(section.getNoiseBiome(0, 0, 0)));
            } else {
                write(buffer, section.getStates(), this.factory.blockStatesStrategy());
                write(buffer, section.getBiomes(), this.factory.biomeStrategy());
            }
        }

        buffer.writeBoolean(mask != null);
        if (mask != null) {
            buffer.writeLongArray(mask.toArray());
        }

        return compress(buffer);
    }

    /** Fills the slots of the chunk in place and returns its carving mask. */
    public @Nullable CarvingMask thaw(byte[] cold, LevelChunkSection[] sections, int minY) {
        FriendlyByteBuf buffer = decompress(cold);
        IdMap<Holder<Biome>> biomes = this.factory.biomeStrategy().globalMap();
        for (int index = 0; index < sections.length; index++) {
            if (buffer.readBoolean()) {
                sections[index] = this.level.mapple$airSections().shared(biomes.byId(buffer.readVarInt()));
            } else {
                PalettedContainer<BlockState> states = read(buffer, this.factory.blockStatesStrategy());
                sections[index] = new LevelChunkSection(states, read(buffer, this.factory.biomeStrategy()));
            }
        }

        return buffer.readBoolean() ? new CarvingMask(buffer.readLongArray(), minY) : null;
    }

    private static <T> void write(FriendlyByteBuf buffer, PalettedContainerRO<T> container, Strategy<T> strategy) {
        PalettedContainerRO.PackedData<T> packed = container.pack(strategy);
        IdMap<T> ids = strategy.globalMap();
        buffer.writeVarInt(packed.paletteEntries().size());
        for (T entry : packed.paletteEntries()) {
            buffer.writeVarInt(ids.getId(entry));
        }

        buffer.writeLongArray(packed.storage().map(LongStream::toArray).orElse(NO_STORAGE));
    }

    private static <T> PalettedContainer<T> read(FriendlyByteBuf buffer, Strategy<T> strategy) {
        IdMap<T> ids = strategy.globalMap();
        int size = buffer.readVarInt();
        List<T> palette = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            palette.add(ids.byId(buffer.readVarInt()));
        }

        long[] storage = buffer.readLongArray();
        Optional<LongStream> values = storage.length == 0 ? Optional.empty() : Optional.of(LongStream.of(storage));
        return PalettedContainer.unpack(strategy, new PalettedContainerRO.PackedData<>(palette, values)).getOrThrow();
    }

    private static byte[] compress(FriendlyByteBuf buffer) {
        int length = buffer.readableBytes();
        LZ4Compressor compressor = LZ4.fastCompressor();
        byte[] cold = new byte[Integer.BYTES + compressor.maxCompressedLength(length)];
        int compressed = compressor.compress(buffer.array(), buffer.arrayOffset() + buffer.readerIndex(), length, cold, Integer.BYTES, cold.length - Integer.BYTES);
        ByteBuffer.wrap(cold).putInt(length);
        return Arrays.copyOf(cold, Integer.BYTES + compressed);
    }

    private static FriendlyByteBuf decompress(byte[] cold) {
        byte[] raw = new byte[ByteBuffer.wrap(cold).getInt()];
        LZ4.fastDecompressor().decompress(cold, Integer.BYTES, raw, 0, raw.length);
        return new FriendlyByteBuf(Unpooled.wrappedBuffer(raw));
    }
}
