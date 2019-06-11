# Postshirt
Postshirt is a realtime wireless posture detection system that transmits and classifies accelerometer data from an Adafruit Feather to an Android application via Bluetooth. A demo video of the whole system can be viewed [here](https://www.youtube.com/watch?v=Y39d0TkPlog)

### Putting it Together
Postshirt has 3 distinct components: an arduino based sensor data recorder, a jupyter notebook SVM trainer, and an android application for runtime classification and user feedback

## Arduino 
This initial version of this project uses the [Adafruit Feather 32u4](https://learn.adafruit.com/adafruit-feather-32u4-bluefruit-le/overview) but should work with any version that supports the [Bluefruit LE Connect Android Application](https://github.com/adafruit/Bluefruit_LE_Connect_Android_v2)

## Jupyter Notebook
The initial version trained the classification models using Jupyter Notebook but can be run using standard Python.
To run the Python code you must have [scikit-learn](https://scikit-learn.org/stable/install.html) and [sklearn-porter](https://github.com/nok/sklearn-porter) installed.

## Android
The end device for this system is an Android smart phone with Bluetooth enabled. This section of the project was written using [Android Studio](https://developer.android.com/studio). The relevant files in this part of the project are DataCollectorFragment.java and PostureDetectorFragment.java
