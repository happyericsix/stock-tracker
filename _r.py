#!/usr/bin/env python3
import os

R = [
    ('com.abhinav.stocktracker', 'com.happyericsix.stocktracker'),
    ('com.abhinav', 'com.happyericsix'),
    ('com/abhinav/stocktracker', 'com/happyericsix/stocktracker'),
]
EXTS = {'.java', '.xml', '.properties', '.md', '.yml', '.yaml', '.gradle',
        '.kt', '.kts', '.json', '.txt', '.cfg'}
SKIP = {'.git', 'node_modules', 'target', 'build', 'dist',
        'venv', '.venv', '__pycache__', '.idea', '.vscode'}
n = 0
for dp, dns, fns in os.walk('.'):
    dns[:] = [d for d in dns if d not in SKIP]
    for f in fns:
        if os.path.splitext(f)[1].lower() not in EXTS:
            continue
        p = os.path.join(dp, f)
        try:
            with open(p, 'r', encoding='utf-8-sig') as fp:
                c = fp.read()
        except (UnicodeDecodeError, OSError):
            continue
        o = c
        for a, b in R:
            c = c.replace(a, b)
        if c != o:
            with open(p, 'w', encoding='utf-8') as fp:
                fp.write(c)
            n += 1
print(f'Updated {n}')
