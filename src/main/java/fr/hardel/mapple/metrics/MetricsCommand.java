package fr.hardel.mapple.metrics;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import java.nio.file.Path;
import java.util.Map;
import java.util.TreeMap;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public final class MetricsCommand {
    private static final SimpleCommandExceptionType RUN_OPEN = new SimpleCommandExceptionType(Component.literal("A measurement run is already open"));
    private static final SimpleCommandExceptionType NO_RUN = new SimpleCommandExceptionType(Component.literal("No measurement run is open"));

    private MetricsCommand() {
    }

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registries, environment) -> dispatcher.register(Commands.literal("mapple")
            .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
            .then(Commands.literal("metrics")
                .then(Commands.literal("snapshot").executes(context -> snapshot(context.getSource())))
                .then(runNode())
                .then(Commands.literal("heapdump").executes(context -> heapDump(context.getSource()))))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> runNode() {
        return Commands.literal("run")
            .then(Commands.literal("start")
                .then(Commands.argument("name", StringArgumentType.word())
                    .then(Commands.argument("periodTicks", IntegerArgumentType.integer(1))
                        .executes(context -> startRun(context, false))
                        .then(Commands.argument("record", BoolArgumentType.bool()).executes(context -> startRun(context, BoolArgumentType.getBool(context, "record")))))))
            .then(Commands.literal("stop").executes(context -> stopRun(context.getSource())));
    }

    private static int snapshot(CommandSourceStack source) {
        Map<String, Map<String, Long>> values = new TreeMap<>();
        MetricsService.active().collect((scope, metric, value) -> values.computeIfAbsent(scope, key -> new TreeMap<>()).put(metric, value));

        StringBuilder message = new StringBuilder("Mapple snapshot");
        values.forEach((scope, metrics) -> {
            message.append('\n').append(scope);
            metrics.forEach((metric, value) -> message.append("\n  ").append(metric).append(" = ").append(value));
        });

        source.sendSuccess(() -> Component.literal(message.toString()), false);
        return values.size();
    }

    private static int startRun(CommandContext<CommandSourceStack> context, boolean record) throws CommandSyntaxException {
        String name = StringArgumentType.getString(context, "name");
        int periodTicks = IntegerArgumentType.getInteger(context, "periodTicks");
        if (!MetricsService.active().startRun(name, periodTicks, record)) {
            throw RUN_OPEN.create();
        }

        context.getSource().sendSuccess(() -> Component.literal("Run '" + name + "' opened, one sample every " + periodTicks + " ticks" + (record ? ", flight recording on" : "")), true);
        return Command.SINGLE_SUCCESS;
    }

    private static int stopRun(CommandSourceStack source) throws CommandSyntaxException {
        Path directory = MetricsService.active().stopRun();
        if (directory == null) {
            throw NO_RUN.create();
        }

        source.sendSuccess(() -> Component.literal("Run closed at " + directory), true);
        return Command.SINGLE_SUCCESS;
    }

    private static int heapDump(CommandSourceStack source) {
        Path file = MetricsService.active().heapDump();
        source.sendSuccess(() -> Component.literal("Heap dump written to " + file), true);
        return Command.SINGLE_SUCCESS;
    }
}
