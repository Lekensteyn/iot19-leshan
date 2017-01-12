#!/usr/bin/env python
# Sets the light state based on commands from input.
# See RPiLightDevice.java

import sys
try:
    from sense_hat import SenseHat
    use_pi = True
except ImportError as e:
    sys.stderr.write("Failed to import packages, will not control light.\n");
    sys.stderr.write("Error was: %s\n" % e)
    sys.stderr.flush()
    use_pi = False

if not use_pi:
    # SenseHat not available, mock something.
    class SenseHat(object):
        def clear(self, r, g, b):
            self._log("clear(%r, %r, %r)" % (r, g, b))

        def __setattr__(self, name, value):
            self._log("%s = %r" % (name, value))

        def _log(self, msg):
            # For testing purposes, will write a line to console on Linux
            sys.stderr.write(msg + "\n")
            sys.stderr.flush()

sense = SenseHat()

for line in sys.stdin:
    command, args = line.strip().split(" ", 1)
    if command == "color":
        r, g, b = map(int, args.split())
        sense.clear(r, g, b)
    elif command == "lowlight":
        lowLight = args == "true"
        sense.low_light = lowLight
