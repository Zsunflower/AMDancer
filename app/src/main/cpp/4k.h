//
// Created by cuong on 10/3/2023.
//

#ifndef AMDANCER_4K_H
#define AMDANCER_4K_H

#include <jni.h>
#include <android/log.h>
#include <iostream>
#include <vector>
#include <string>
#include "opencv2/opencv.hpp"

#define  LOG_TAG    "AM_AUTO_JNI"
#define  LOGD(...)  __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)

using std::vector;
using std::string;
using namespace cv;


enum ARROW_TYPE_4K
{
	ARROW_UP,
	ARROW_DOWN,
	ARROW_LEFT,
	ARROW_RIGHT,
};


class K4Config
{
public:
	int WIDTH{}, HEIGHT{};
	Rect left_key, right_key, up_key, down_key;
	Rect space_key, arrows_key, pointer_box, per_box;
	bool is_init = false;
public:
	K4Config() = default;

	bool parse_config(const string &config_path);

	void print_config(void) const;
};

bool process_arrows_key_image(const Mat &arrows_img, Rect &current_box, vector<Rect> &left_boxes,
							  vector<ARROW_TYPE_4K> &arrows);

void get_arrow(const Mat &thresholed, const vector<Rect> &arrow_boxes, const Mat &image,
			   vector<ARROW_TYPE_4K> &arrows);

#endif //AMDANCER_4K_H
