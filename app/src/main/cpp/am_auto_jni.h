//
// Created by cuong on 9/25/2023.
//

#ifndef AMDANCER_AM_AUTO_JNI_H
#define AMDANCER_AM_AUTO_JNI_H

#include <jni.h>
#include <android/log.h>
#include <opencv2/opencv.hpp>
#include "buble.h"


#define  LOG_TAG    "AM_AUTO_JNI"
#define  LOGD(...)  __android_log_print(ANDROID_LOG_DEBUG,LOG_TAG,__VA_ARGS__)

static bool is_init = false;
static BubleConfig buble_config;


# ifdef __cplusplus
extern "C"
{
# endif

JNIEXPORT jboolean JNICALL
Java_com_autogame_amdancer_ScreenCaptureActivity_initConfig(JNIEnv *env, jobject obj,
															jstring config_path);

JNIEXPORT void JNICALL
Java_com_autogame_amdancer_ScreenCaptureService_process(JNIEnv *env, jobject obj, jint width,
														jint height, jobject buffer);


# ifdef __cplusplus
}
# endif


#endif //AMDANCER_AM_AUTO_JNI_H
