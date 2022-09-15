#include "main.h"
#include <jni.h>
#include <string>

extern "C" JNIEXPORT jstring JNICALL
Java_com_alexsun_nativedemo_MainActivity_stringFromJNI(
        JNIEnv* env,
        jobject /* this */,
        jint argc,
        jobjectArray argv) {
    std::string res;
    char **argv_for_main = new char*[argc];
    for (int i =0; i < argc; ++i) {
        auto value = (jstring) env->GetObjectArrayElement(argv, i);
        res += env->GetStringUTFChars(value, nullptr);
        res += "#";
        argv_for_main[i] = (char *)env->GetStringUTFChars(value, nullptr);
    }
    int proc_ret;
    if (strcmp(argv_for_main[0], "esrgan") == 0) {
        proc_ret = esrgan((int)argc, argv_for_main);
    } else if (strcmp(argv_for_main[0], "waifu2x") == 0) {
        proc_ret = waifu2x((int)argc, argv_for_main);
    } else {
        proc_ret = -1;
    }
    res += std::to_string(proc_ret);
    delete[] argv_for_main;
    return env->NewStringUTF(res.c_str());
}