import struct, sys, collections

path = sys.argv[1]
f = open(path, 'rb', buffering=1 << 22)
header = b''
while True:
    c = f.read(1)
    if c == b'\0':
        break
    header += c
id_size = struct.unpack('>I', f.read(4))[0]
f.read(8)
ID = '>Q' if id_size == 8 else '>I'

strings = {}
class_names = {}
prim_sizes = {4: 1, 5: 2, 6: 4, 7: 8, 8: 1, 9: 2, 10: 4, 11: 8}
prim_names = {4: 'boolean', 5: 'char', 6: 'float', 7: 'double', 8: 'byte', 9: 'short', 10: 'int', 11: 'long'}
prim_buckets = collections.defaultdict(lambda: collections.Counter())
prim_bytes = collections.defaultdict(lambda: collections.Counter())
ROOTS = {0xFF: 0, 0x01: id_size, 0x02: 8, 0x03: 8, 0x04: 4, 0x05: 0, 0x06: 4, 0x07: 0, 0x08: 8}
obj_buckets = collections.Counter()
obj_bytes = collections.Counter()


def read_id(buf, off):
    return struct.unpack_from(ID, buf, off)[0], off + id_size


while True:
    head = f.read(9)
    if len(head) < 9:
        break
    tag, _, length = struct.unpack('>BII', head)
    body = f.read(length)
    if tag == 0x01:
        sid, off = read_id(body, 0)
        strings[sid] = body[off:].decode('utf-8', 'replace')
    elif tag == 0x02:
        _, cid = struct.unpack_from('>I' + ID[1:], body, 0)
        nid = struct.unpack_from(ID, body, 4 + id_size + 4)[0]
        class_names[cid] = strings.get(nid, '?')
    elif tag in (0x0C, 0x1C):
        off = 0
        n = len(body)
        while off < n:
            sub = body[off]
            off += 1
            if sub in ROOTS:
                off += id_size + ROOTS[sub]
            elif sub == 0x20:
                off += id_size + 4 + id_size * 6 + 4
                cp = struct.unpack_from('>H', body, off)[0]
                off += 2
                for _ in range(cp):
                    t = body[off + 2]
                    off += 3 + (id_size if t == 2 else prim_sizes[t])
                sc = struct.unpack_from('>H', body, off)[0]
                off += 2
                for _ in range(sc):
                    t = body[off + id_size]
                    off += id_size + 1 + (id_size if t == 2 else prim_sizes[t])
                ic = struct.unpack_from('>H', body, off)[0]
                off += 2 + ic * (id_size + 1)
            elif sub == 0x21:
                size = struct.unpack_from('>I', body, off + id_size + 4 + id_size)[0]
                off += id_size + 4 + id_size + 4 + size
            elif sub == 0x22:
                count, = struct.unpack_from('>I', body, off + id_size + 4)
                cid = struct.unpack_from(ID, body, off + id_size + 8)[0]
                off += id_size + 8 + id_size + count * id_size
                name = class_names.get(cid, '?')
                if name in ('java.lang.Object[]', '[Ljava.lang.Object;', '[Ljava/lang/Object;'):
                    obj_buckets[count] += 1
                    obj_bytes[count] += 16 + count * 4
            elif sub == 0x23:
                count, = struct.unpack_from('>I', body, off + id_size + 4)
                t = body[off + id_size + 8]
                off += id_size + 9 + count * prim_sizes[t]
                prim_buckets[t][count] += 1
                prim_bytes[t][count] += 16 + count * prim_sizes[t]
            else:
                raise SystemExit(f'unknown sub-record {sub:#x} at {off}')

for t in (11, 8):
    print(f'== {prim_names[t]}[] by length, top 12 by bytes')
    for count, b in prim_bytes[t].most_common(12):
        print(f'  len={count:>7}  n={prim_buckets[t][count]:>9}  {b / 2**20:8.1f} MB')
print('== Object[] by length, top 12 by bytes')
for count, b in obj_bytes.most_common(12):
    print(f'  len={count:>7}  n={obj_buckets[count]:>9}  {b / 2**20:8.1f} MB')
