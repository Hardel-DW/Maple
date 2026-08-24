import collections
import re
import subprocess
import sys

JFR = r'C:\Program Files\Eclipse Adoptium\jdk-25.0.1.8-hotspot\bin\jfr.exe'
UNITS = {'bytes': 1, 'kB': 1e3, 'MB': 1e6, 'GB': 1e9}

path = sys.argv[1]
loading_seconds = int(sys.argv[2]) if len(sys.argv) > 2 else 90
top = int(sys.argv[3]) if len(sys.argv) > 3 else 10

text = subprocess.run([JFR, 'print', '--events', 'jdk.ObjectAllocationSample', '--stack-depth', '5', path], capture_output=True, text=True, encoding='utf-8', errors='replace').stdout
samples = []
for event in text.split('jdk.ObjectAllocationSample {')[1:]:
    time = re.search(r'startTime = (\d\d):(\d\d):(\d\d)', event)
    weight = re.search(r'weight = ([\d.,]+) (\w+)', event)
    if not (time and weight):
        continue
    hours, minutes, seconds = map(int, time.groups())
    frames = re.findall(r'^\s+([\w$.<>]+\([^)]*\))\s+line:', event, re.M)
    samples.append((hours * 3600 + minutes * 60 + seconds, float(weight.group(1).replace(',', '.')) * UNITS[weight.group(2)], frames))

start = min(sample[0] for sample in samples)
end = max(sample[0] for sample in samples)


def report(label, selected):
    total = sum(sample[1] for sample in selected)
    span = selected[-1][0] - selected[0][0] + 1
    print(f'== {label}: {len(selected)} samples, {total / 1e9:.1f} GB over {span} s = {total / 1e6 / span:.0f} MB/s')
    sites = collections.Counter()
    for _, weight, frames in selected:
        sites[' <- '.join(frames[:3])] += weight
    for site, weight in sites.most_common(top):
        print(f'{100 * weight / total:5.1f}%  {site}')
    print()


report(f'loading, first {loading_seconds} s', [sample for sample in samples if sample[0] < start + loading_seconds])
report('plateau, last 60 s', [sample for sample in samples if sample[0] >= end - 60])
