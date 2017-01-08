#!/usr/bin/env python
# Sets the light state based on commands from input.
# See RPiLightDevice.java

import sys

for line in sys.stdin:
    command, args = line.strip().split(" ", 1)
    if command == "color":
        r, g, b = map(int, args.split())
        pass  # TODO
    elif command == "lowlight":
        lowLight = args == "true"
        pass  # TODO
