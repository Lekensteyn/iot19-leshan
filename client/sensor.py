#!/usr/bin/env python
# Read sensor and write the detected state to standard output.
# It must output one character, "F" (Free) or "U" (Used).

import random
import sys
import time

def fakeProgram():
    sys.stdout.write('F')
    sys.stdout.flush()

    while True:
        val = random.choice("FU")
        sys.stdout.write(val)
        sys.stdout.flush()
        time.sleep(1)

if __name__ == '__main__':
    # Dummy implementation, please wire up the real stuff!
    fakeProgram()
