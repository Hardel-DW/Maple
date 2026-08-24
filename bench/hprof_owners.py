import struct
import sys
import numpy as np

CONTAINER_PREFIXES = ('java/', 'jdk/', 'sun/', 'it/unimi/', 'com/google/', 'org/apache/', '[')
PRIMITIVE_SIZES = {4: 1, 5: 2, 6: 4, 7: 8, 8: 1, 9: 2, 10: 4, 11: 8}
PRIMITIVE_FORMATS = {4: 'b', 5: 'H', 6: 'f', 7: 'd', 8: 'b', 9: 'h', 10: 'i', 11: 'q'}
BATCH = 4_000_000

path = sys.argv[1]
top = int(sys.argv[2]) if len(sys.argv) > 2 else 40


def read_header(file):
    while file.read(1) != b'\0':
        pass
    id_size = struct.unpack('>I', file.read(4))[0]
    file.read(8)
    return id_size


def records(file):
    while True:
        head = file.read(9)
        if len(head) < 9:
            return
        tag, _, length = struct.unpack('>BII', head)
        yield tag, file.read(length)


class Classes:
    def __init__(self, id_size):
        self.id_size = id_size
        self.id_format = 'Q' if id_size == 8 else 'I'
        self.strings = {}
        self.names = {}
        self.supers = {}
        self.fields = {}
        self.instance_sizes = {}
        self.layouts = {}

    def load_class(self, body):
        class_id = struct.unpack_from('>' + self.id_format, body, 4)[0]
        name_id = struct.unpack_from('>' + self.id_format, body, 4 + self.id_size + 4)[0]
        self.names[class_id] = self.strings.get(name_id, '?').replace('.', '/')

    def class_dump(self, body, off):
        class_id = struct.unpack_from('>' + self.id_format, body, off)[0]
        super_id = struct.unpack_from('>' + self.id_format, body, off + self.id_size + 4)[0]
        off += self.id_size + 4 + self.id_size * 6
        instance_size = struct.unpack_from('>I', body, off)[0]
        off += 4
        constants = struct.unpack_from('>H', body, off)[0]
        off += 2
        for _ in range(constants):
            kind = body[off + 2]
            off += 3 + (self.id_size if kind == 2 else PRIMITIVE_SIZES[kind])
        statics = struct.unpack_from('>H', body, off)[0]
        off += 2
        for _ in range(statics):
            kind = body[off + self.id_size]
            off += self.id_size + 1 + (self.id_size if kind == 2 else PRIMITIVE_SIZES[kind])
        count = struct.unpack_from('>H', body, off)[0]
        off += 2
        kinds = []
        for _ in range(count):
            kinds.append(body[off + self.id_size])
            off += self.id_size + 1
        self.supers[class_id] = super_id
        self.fields[class_id] = kinds
        self.instance_sizes[class_id] = instance_size
        return off

    def layout(self, class_id):
        cached = self.layouts.get(class_id)
        if cached is not None:
            return cached
        fmt = '>'
        refs = []
        index = 0
        current = class_id
        while current in self.fields:
            for kind in self.fields[current]:
                if kind == 2:
                    fmt += self.id_format
                    refs.append(index)
                else:
                    fmt += PRIMITIVE_FORMATS[kind]
                index += 1
            current = self.supers[current]
        cached = (struct.Struct(fmt), refs)
        self.layouts[class_id] = cached
        return cached


def walk(file, classes, on_object, on_refs):
    id_size = classes.id_size
    id_format = '>' + classes.id_format
    for tag, body in records(file):
        if tag == 0x01:
            string_id = struct.unpack_from(id_format, body, 0)[0]
            classes.strings[string_id] = body[id_size:].decode('utf-8', 'replace')
        elif tag == 0x02:
            classes.load_class(body)
        elif tag in (0x0C, 0x1C):
            off = 0
            end = len(body)
            while off < end:
                sub = body[off]
                off += 1
                if sub in ROOTS:
                    off += id_size + ROOTS[sub]
                elif sub == 0x20:
                    off = classes.class_dump(body, off)
                elif sub == 0x21:
                    object_id, _, class_id, length = struct.unpack_from(id_format + 'I' + classes.id_format + 'I', body, off)
                    data_off = off + id_size + 4 + id_size + 4
                    on_object(object_id, class_id, 16 + classes.instance_sizes.get(class_id, 0))
                    if on_refs is not None:
                        layout, refs = classes.layout(class_id)
                        if refs and layout.size <= length:
                            values = layout.unpack_from(body, data_off)
                            on_refs(object_id, [values[index] for index in refs])
                    off = data_off + length
                elif sub == 0x22:
                    object_id, _, count, class_id = struct.unpack_from(id_format + 'II' + classes.id_format, body, off)
                    data_off = off + id_size + 8 + id_size
                    on_object(object_id, class_id, 16 + count * 4)
                    if on_refs is not None and count:
                        on_refs(object_id, struct.unpack_from('>' + str(count) + classes.id_format, body, data_off))
                    off = data_off + count * id_size
                elif sub == 0x23:
                    object_id, _, count, kind = struct.unpack_from(id_format + 'IIB', body, off)
                    on_object(object_id, PRIMITIVE_ARRAY_CLASS[kind], 16 + count * PRIMITIVE_SIZES[kind])
                    off += id_size + 9 + count * PRIMITIVE_SIZES[kind]
                else:
                    raise SystemExit(f'unknown sub-record {sub:#x}')


with open(path, 'rb', buffering=1 << 22) as file:
    id_size = read_header(file)
    ROOTS = {0xFF: 0, 0x01: id_size, 0x02: 8, 0x03: 8, 0x04: 4, 0x05: 0, 0x06: 4, 0x07: 0, 0x08: 8}
    PRIMITIVE_ARRAY_CLASS = {kind: -kind for kind in PRIMITIVE_SIZES}
    classes = Classes(id_size)
    ids, class_ids, sizes = [], [], []

    def collect(object_id, class_id, size):
        ids.append(object_id)
        class_ids.append(class_id)
        sizes.append(size)

    walk(file, classes, collect, None)

ids = np.array(ids, dtype=np.uint64)
order = np.argsort(ids)
ids = ids[order]
class_ids = np.array(class_ids, dtype=np.int64)[order]
sizes = np.array(sizes, dtype=np.int64)[order]
count = len(ids)
print(f'{count} objects, {sizes.sum() / 2**20:.0f} MB', file=sys.stderr)

class_index = {}
class_names = []
for class_id in np.unique(class_ids):
    class_index[class_id] = len(class_names)
    name = classes.names.get(class_id) if class_id >= 0 else {4: 'boolean[]', 5: 'char[]', 6: 'float[]', 7: 'double[]', 8: 'byte[]', 9: 'short[]', 10: 'int[]', 11: 'long[]'}[-class_id]
    class_names.append(name or '?')
class_of = np.array([class_index[c] for c in class_ids], dtype=np.int32)
is_container = np.array([name.startswith(CONTAINER_PREFIXES) or name.endswith('[]') for name in class_names])
owner = np.full(count, -1, dtype=np.int64)

pending_sources, pending_targets = [], []


def flush():
    global pending_sources, pending_targets
    if not pending_targets:
        return
    targets = np.array(pending_targets, dtype=np.uint64)
    sources = np.array(pending_sources, dtype=np.uint64)
    positions = np.searchsorted(ids, targets)
    positions[positions >= count] = 0
    found = ids[positions] == targets
    positions = positions[found]
    sources = sources[found]
    container = is_container[class_of[positions]] & (owner[positions] == -1)
    positions = positions[container]
    sources = sources[container]
    source_positions = np.searchsorted(ids, sources)
    owner[positions] = source_positions
    pending_sources, pending_targets = [], []


def note_refs(object_id, refs):
    for ref in refs:
        if ref:
            pending_sources.append(object_id)
            pending_targets.append(ref)
    if len(pending_targets) >= BATCH:
        flush()


with open(path, 'rb', buffering=1 << 22) as file:
    read_header(file)
    walk(file, classes, lambda *_: None, note_refs)
flush()

resolved = np.where(is_container[class_of] & (owner != -1), owner, np.arange(count))
for _ in range(40):
    jumped = resolved[resolved]
    if np.array_equal(jumped, resolved):
        break
    resolved = jumped

owned = np.bincount(class_of[resolved], weights=sizes, minlength=len(class_names))
own = np.bincount(class_of, weights=sizes, minlength=len(class_names))
orphan = is_container[class_of[resolved]]
print(f'unattributed container bytes: {sizes[orphan].sum() / 2**20:.0f} MB', file=sys.stderr)
print(f'{"owned MB":>9} {"own MB":>8}  class')
for index in np.argsort(owned)[::-1][:top]:
    print(f'{owned[index] / 2**20:9.1f} {own[index] / 2**20:8.1f}  {class_names[index]}')
