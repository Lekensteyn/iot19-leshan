#!/usr/bin/env python2
# Read sensor and write the detected state to standard output.
# It must output one character, "F" (Free) or "U" (Used).

import random
import sys
import time

try:
    sys.path.insert(0, "/home/pi/reference/pi_code/webcam_face_detection")
    from pyimagesearch.facedetector import FaceDetector
    from pyimagesearch import imutils
    from picamera.array import PiRGBArray
    from picamera import PiCamera
    import cv2
    use_pi = True
except ImportError as e:
    sys.stderr.write("Failed to import packages, will use fake sensor.\n");
    sys.stderr.write("Error was: %s\n" % e)
    use_pi = False

def fakeProgram():
    sys.stdout.write('F')
    sys.stdout.flush()

    while True:
        val = random.choice("FU")
        sys.stdout.write(val)
        sys.stdout.flush()
        time.sleep(1)

def piProgram():
    # initialize the camera and grab a reference to the raw camera capture
    camera = PiCamera()
    camera.resolution = (640, 480)
    camera.framerate = 32
    rawCapture = PiRGBArray(camera, size=(640, 480))

    face_cascade_file = "/home/pi/reference/pi_code/webcam_face_detection/cascades/haarcascade_frontalface_default.xml"

    # construct the face detector and allow the camera to warm up
    fd = FaceDetector(face_cascade_file)
    time.sleep(0.1)

    # capture frames from the camera
    for f in camera.capture_continuous(rawCapture, format="bgr", use_video_port=True):
        # grab the raw NumPy array representing the image
        frame = f.array

        # resize the frame and convert it to grayscale
        frame = imutils.resize(frame, width = 300)
        gray = cv2.cvtColor(frame, cv2.COLOR_BGR2GRAY)

        # detect faces in the image and clear frame for next attempt
        faceRects = fd.detect(gray, scaleFactor = 1.1, minNeighbors = 5,
                minSize = (30, 30))
        face_detected = len(faceRects) > 0
        rawCapture.truncate(0)

        # Write sensor result to stdout
        sys.stdout.write("U" if face_detected else "F")
        sys.stdout.flush()

if __name__ == '__main__':
    # Dummy implementation, please wire up the real stuff!
    if use_pi:
        piProgram()
    else:
        fakeProgram()
