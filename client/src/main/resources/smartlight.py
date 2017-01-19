#!/usr/bin/env python3
"""
Implementation of adaptive lighting behavior.
Author: Peter Wu <peter@lekensteyn.nl> (Group 19)

Interface is line-based. The first word is the command, the next argument after
the space is its argument. Multiple spaces are used below for readability.

Input:
    location            lightLocationX lightLocationY (required at startup!)
    ownership           ownership_json_contents
    sensor_occupied     sensorID true/false
    user3               user_id

The ownership_json_contents must have been validated by the supplier before, it
is assumed to be well-formed and consistent with the specified scheme. It must
be an array providing:
    - user_type, user_id, light_color, low_light: for output only
    - user_type, sensor_id: for matching sensor_occupied notifications
    - user_location_x, user_location_y: for priority logic
    - user_id: to find out who is eligible as user3

Output:
    set color           (r, g, b)
    set lowlight        true/false
    set userid          userid
    set state           USED/FREE
    set usertype        USER1/USER2/USER3

Expected transformations:
 - If sensor with higher user type is occupied, immediately apply prefs.
 - If effective sensor (user) leaves, wait for three seconds before next state.
 - If user1/user2 are not present, allow user3 to set user (but only if there is
   no existing user, or if the existing user3 has lower pref).
 - If some user is present in room, set color to (250, 200, 100), lowlight.
 - If no user is present in room, turn lights off.
"""

import logging
import json
from threading import Timer
import sys

_logger = logging.getLogger(__name__)

light_x, light_y = None, None

# Map from sensor ID or user ID to the same sensor/user info
sensors_db = {}
users_db = {}

# Map from sensor ID to True/False
sensors_occupied = {}
# Current active user (info)
current_setting = None
# Whether any user is present, used for deciding whether to dim/turn off lights.
any_user_present = False

# Timer object for delayed application of settings.
delayed_timer = None


def parse_ownership_json(data):
    """
    Loads information about sensors and users from a JSON string.
    """
    global current_setting

    # If current user is USER3, remember it for priority reasons.
    if current_setting and current_setting["user_type"] == "USER3":
        old_user3 = current_setting["user_id"]
    else:
        old_user3 = None

    # Clear all information from the previous DB and load new stuff.
    sensors_db.clear()
    users_db.clear()
    current_setting = None
    for info in json.loads(data):
        users_db[info["user_id"]] = info
        sensors_db[info["sensor_id"]] = info
    _logger.info("Loaded new userdb: %r", users_db)

    # Set user information with current information object.
    user = find_next_user(user3_id=old_user3)
    apply_light_setting(user)


def find_next_user(user3_id=None):
    """
    Find the user with the highest priority according to the currently known
    sensor occupance information.
    """
    user2, user3 = None, None
    for user_id, info in users_db.items():
        sensor_id = info["sensor_id"]
        user_id = info["user_id"]
        user_type = info["user_type"]
        if sensors_occupied.get(sensor_id):
            if user_type == "USER1":
                return info
            elif user_type == "USER2":
                user2 = info
            elif user_type == "USER3":
                if user3_id is not None and user3_id == user_id:
                    user3 = info
                if current_setting and current_setting["user_id"] == user_id:
                    user3 = info
    for user in (user2, user3):
        if user:
            return user


def set_sensor_occupied(sensor_id, is_occupied):
    """
    Save the sensor state. If the update results in a ownership change (due to
    priority), apply the change immediately (if occupied) or delay change (if
    not occupied).
    """
    # Note: even if sensor is known in db, remember in case we get updates
    # later.
    sensors_occupied[sensor_id] = is_occupied
    new_user = find_next_user()
    if is_occupied:
        apply_light_setting(new_user)
    else:
        apply_light_setting(new_user, delay=True)


def priority(info):
    """
    Calculate priority of the user. Lower values have higher priority.
    """
    user_x, user_y = info["user_location_x"], info["user_location_y"]
    # Use Euclidean distance (actually, omit sqrt because the exact value does
    # not matter, only the relative distance between two users).
    return (user_x - light_x)**2 + (user_y - light_y)**2


def set_user3(user_id):
    """
    User3 is allowed to take ownership if the light is free. If it is used by
    another user3 with lower priority, ownership can also be taken.
    """
    if user_id not in users_db:
        _logger.warn("Unknown user: %s", user_id)
        return  # Unknown user, go away please!

    if users_db[user_id]["user_type"] != "USER3":
        _logger.warn("Not a user3: %s", user_id)
        return

    if current_setting:
        if current_setting["user_type"] != "USER3":
            _logger.warn("Other user type is still active")
            return
        if priority(current_setting) < priority(users_db[user_id]):
            _logger.warn("Other user3 has higher piority than you")
            return

    # Yes, user3 is allowed to be set and take ownership. Assume that the
    # user is present (in case the light was added to network after sensor).
    sensors_occupied[user_id] = True
    apply_light_setting(users_db[user_id])


def apply_light_setting(info, delay=False):
    """
    Writes the appropriate commands to make the settings from "info" effective.
    If "info" is None, take into account whether people are in the room.
    """
    global current_setting, any_user_present, delayed_timer

    _logger.info("Trying to apply info=%r delay=%r", info, delay)

    if delayed_timer:
        delayed_timer.cancel()
        delayed_timer = None

    # Assume that all advertised sensors belong to this room.
    any_user_present_now = any(is_occupied
            for is_occupied in sensors_occupied.values())

    _logger.info("present_now=%r, sensors_occupied=%r", any_user_present_now,
            sensors_occupied);

    if info == current_setting and any_user_present == any_user_present_now:
        _logger.info("Nothing to do, situation is unchanged")
        return

    if delay:
        delayed_timer = Timer(3, apply_light_setting, args=[info])
        delayed_timer.start()
        return

    if info:
        lines = [
            ("color", info["light_color"]),
            ("lowlight", info["low_light"]),
            ("userid", info["user_id"]),
            ("state", "USED"),
            ("usertype", info["user_type"]),
        ]
    else:
        lines = [
            ("state", "FREE")
        ]
        if any_user_present_now:
            lines += [
                ("color", "(250, 200, 100)"),
                ("lowlight", True),
            ]
        else:
            # Assume that black is the same as "off" (valid according to
            # https://pythonhosted.org/sense-hat/api/).
            lines += [
                ("color", "(0, 0, 0)"),
            ]
    for key, value in lines:
        if type(value) == bool:
            value = "true" if value else "false"
        print("set %s %s" % (key, value))
    current_setting = info
    any_user_present = any_user_present_now
    sys.stdout.flush()
    _logger.info("New state: present=%r setting=%r", any_user_present,
            current_setting)


def main():
    global light_x, light_y

    logging.basicConfig(level=logging.INFO, format="%(message)s")

    for line in sys.stdin:
        line = line.strip()
        if " " in line:
            cmd, args = line.split(" ", 1)
        else:
            cmd, args = line, ""

        try:
            if cmd == "location":
                light_x, light_y = map(float, args.split())
            elif cmd == "ownership":
                parse_ownership_json(args)
            elif cmd == "sensor_occupied":
                sensor_id, occupied = args.split()
                set_sensor_occupied(sensor_id, occupied == "true")
            elif cmd == "user3":
                set_user3(args)
            else:
                _logger.warn("Unrecognized command %r %r", cmd, args)
        except Exception as e:
            _logger.exception("Error while processing %s", line)

if __name__ == '__main__':
    main()
