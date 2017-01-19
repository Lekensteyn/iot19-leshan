#!/usr/bin/env python3
"""
Changes the color to a random value every second.
"""

import random
import select
import sys
import time

print("Random light, ignoring all input!", file=sys.stderr, flush=True)

while True:
    # Be sure to empty the input buffer to avoid stalls.
    # Note that stdin is line-buffered by default. For our purposes, that is OK.
    while select.select([sys.stdin], [], [], 0)[0]:
        sys.stdin.buffer.read1(4096)

    # Change color to a random value.
    color = "(%d, %d, %d)" % tuple(random.randint(0, 255) for x in range(3))
    print("set color %s" % color, flush=True)

    # Wait for one second
    time.sleep(1)
#MEQCICYlnGTzaAUSrsz/mUa6K7AE4YJ6ZRZHNu3m6E7ZX8KHAiAkL0xX+Hjm7f0pnsctofB2vGfUv8fJ1kLTrIakltko7Q==
