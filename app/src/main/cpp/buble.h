//
// Created by cuong on 9/26/2023.
//

#ifndef AMDANCER_BUBLE_H
#define AMDANCER_BUBLE_H

#include <jni.h>
#include <android/log.h>
#include <vector>
#include <string>
#include <opencv2/opencv.hpp>

#define  LOG_TAG    "AM_AUTO_JNI"
#define  LOGD(...)  __android_log_print(ANDROID_LOG_DEBUG,LOG_TAG,__VA_ARGS__)

using std::vector;
using std::string;
using namespace cv;


enum ARROW_TYPE_BB
{
	LEFT,
	UP,
	RIGHT,
	DOWN,
	UP_LEFT,
	UP_RIGHT,
	DOWN_LEFT,
	DOWN_RIGHT,
	ARROW_UNK,
};

enum BUBLE_TYPE
{
	PURPLE,
	YELLOW,
	BLUE,
	CYAN,
	HOLD,
	BUBLE_UNK,
};


const int PURPLE_RANGE[2] = {130, 165};
const int YELLOW_RANGE[2] = {20, 50};
const int BLUE_RANGE[2] = {95, 110};
const int CYAN_RANGE[2] = {65, 95};
const int HOLD_RANGE[2] = {0, 15};


class BubleConfig
{
public:
	Rect SPACE_BUBLE_BBOX, SPACE_BUBLE_BBOX_EXT;
	Rect SPACE_BUBLE_BBOX_IN;
	int WIDTH, HEIGHT;
	int DELTA, MIN_BUBLE_SIZE, MAX_DELTA_BUBLE_WH, NOISE_SIZE;
	bool is_init = false;
public:
	BubleConfig() = default;

	bool parse_config(const string &config_path);

	bool save_config(const string &config_path) const;

	void print_config(void) const;

	void adjust_bbox(int delta);
};


ARROW_TYPE_BB get_arrow_type(const Mat &thresholded, const Rect &rect);

BUBLE_TYPE get_buble_type(const Mat &h_channel, const Rect &rect);

void find_bubles(const Mat &l_thresholded, vector<Rect> &buble_bboxes, int delta_wh_threshold,
				 int size_threshold, int wh_noise);

bool get_active_buble(vector<Rect> &buble_bboxes, Rect &buble_in, Rect &buble_out);

string get_buble_name(BUBLE_TYPE bb_type);

bool has_border(const Mat &h_img, const Mat &l_img, BubleConfig &buble_config);

bool buble_detect_config(const Mat &image, BubleConfig &buble_config);

#endif //AMDANCER_BUBLE_H
