#!/usr/bin/env python3
# Sets the light state based on commands from input.
# See RPiLightDevice.java

import logging
import sys
try:
    from sense_hat import SenseHat
    use_pi = True
except ImportError as e:
    sys.stderr.write("Failed to import packages, will not control light.\n");
    sys.stderr.write("Error was: %s\n" % e)
    sys.stderr.flush()
    use_pi = False

_logger = logging.getLogger(__name__)
logging.basicConfig(level=logging.INFO, format="%(message)s")

if not use_pi:
    # SenseHat not available, mock something.
    class SenseHat(object):
        def clear(self, r, g, b):
            _logger.info("clear(%r, %r, %r)", r, g, b)

        def __setattr__(self, name, value):
            _logger.info("%s = %r", name, value)

sense = SenseHat()
_logger.info("SenseHat ready, lights off!")
sense.clear(0, 0, 0)

for line in sys.stdin:
    command, args = line.strip().split(" ", 1)
    if command == "color":
        r, g, b = map(int, args.split())
        _logger.info("Setting color to (%r, %r, %r)", r, g, b)
        sense.clear(r, g, b)
    elif command == "lowlight":
        lowLight = args == "true"
        _logger.info("Setting low light mode to %r", lowLight)
        sense.low_light = lowLight
    else:
        _logger.warn("Unrecognized command %r %r", command, args)
