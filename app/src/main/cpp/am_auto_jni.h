//
// Created by cuong on 9/25/2023.
//

#ifndef AMDANCER_AM_AUTO_JNI_H
#define AMDANCER_AM_AUTO_JNI_H

#include <jni.h>
#include <android/log.h>
#include <opencv2/opencv.hpp>
#include "buble.h"
#include "4k.h"


#define  LOG_TAG    "AM_AUTO_JNI"
#define  LOGD(...)  __android_log_print(ANDROID_LOG_DEBUG,LOG_TAG,__VA_ARGS__)

static string config_path, buble_config_path, k4_config_path;
static int DELTA = 3;
static BubleConfig buble_config;
static K4Config k4_config;
static jmethodID click_id, hold_id, drag_id;

# ifdef __cplusplus
extern "C"
{
# endif

JNIEXPORT void JNICALL
Java_com_autogame_amdancer_MainActivity_setConfigPath(JNIEnv *env, jobject thiz,
													  jstring config_path);

JNIEXPORT jboolean JNICALL
Java_com_autogame_amdancer_MainActivity_initConfig(JNIEnv *env, jobject obj);

JNIEXPORT void JNICALL
Java_com_autogame_amdancer_ScreenCaptureService_setupCB(JNIEnv
														*env,
														jobject obj
);

JNIEXPORT void JNICALL
Java_com_autogame_amdancer_Settings_reload4kConfig(JNIEnv
														*env,
														jobject obj
);

JNIEXPORT jboolean JNICALL
Java_com_autogame_amdancer_MainActivity_init4kConfig(JNIEnv *env, jobject obj);

JNIEXPORT void JNICALL
Java_com_autogame_amdancer_ScreenCaptureService_processBB(JNIEnv
														  *env,
														  jobject obj, jint
														  width,
														  jint height, jobject
														  buffer);

JNIEXPORT void JNICALL
Java_com_autogame_amdancer_ScreenCaptureService_process4k(JNIEnv
														  *env,
														  jobject obj, jint
														  width,
														  jint height, jobject
														  buffer);

# ifdef __cplusplus
}
# endif


#endif //AMDANCER_AM_AUTO_JNI_H