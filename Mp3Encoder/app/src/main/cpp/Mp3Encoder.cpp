#include <jni.h>
#include <string>
#include <android/log.h>

using namespace std;

#define LOG_TAG "Mp3Encoder"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)

extern "C"
JNIEXPORT void JNICALL com.cmder.mp3encoder