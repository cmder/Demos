#include <jni.h>
#include <string>
#include <android/log.h>

using namespace std;

#define LOG_TAG "NativeCode"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)

extern "C"
JNIEXPORT void JNICALL Java_com_cmder_ndkdemo_MainActivity_callJavaFromCpp(JNIEnv* env, jobject thiz){
    jclass mainActivityClass = env->GetObjectClass(thiz);

    jmethodID getMessageMethod = env->GetMethodID(
            mainActivityClass, "getMessageFromJava", "()Ljava/lang/String;");

    if (getMessageMethod == nullptr){
        LOGD("Method getMessageFromJava not found");
        return;
    }

    jstring javaMessage = (jstring) env->CallObjectMethod(thiz, getMessageMethod);

    const char* mesaage = env->GetStringUTFChars(javaMessage, nullptr);
    LOGD("Message from Java: %s", mesaage);
    env->ReleaseStringUTFChars(javaMessage, mesaage);

    jmethodID logMessageMethod = env->GetMethodID(
            mainActivityClass, "logMessageFromCpp", "(Ljava/lang/String;)V");

    if (logMessageMethod == nullptr){
        LOGD("Method logMessageFromCpp not found");
        return;
    }

    jstring cppMessage = env->NewStringUTF("Hello From C++!");

    env->CallVoidMethod(thiz, logMessageMethod, cppMessage);

    env->DeleteLocalRef(cppMessage);
}