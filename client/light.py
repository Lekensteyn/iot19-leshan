#!/usr/bin/env python
# Sets the light state based on commands from input.
# See RPiLightDevice.java

from sense_hat import SenseHat
import sys

sense = SenseHat()

for line in sys.stdin:
    command, args = line.strip().split(" ", 1)
    if command == "color":
        r, g, b = map(int, args.split())
        load_color = [(r, g, b)] * 64
        sense.set_pixels(load_color)
    elif command == "lowlight":
        lowLight = args == "true"
        sense.low_light = lowLight
