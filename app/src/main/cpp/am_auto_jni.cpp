#include <thread>
#include <chrono>
#include "am_auto_jni.h"

using namespace cv;


extern "C" JNIEXPORT jboolean JNICALL
Java_com_autogame_amdancer_ScreenCaptureActivity_initConfig(JNIEnv *env, jobject obj,
															jstring config_path)
{
	const char *_config_path;
	_config_path = env->GetStringUTFChars(config_path, nullptr);
	LOGD("Parse buble config file: %s", _config_path);
	is_init = buble_config.parse_config(_config_path);
	env->ReleaseStringUTFChars(config_path, _config_path);

	if (is_init)
	{
		LOGD("Parse buble config success!");
		buble_config.print_config();
		return JNI_TRUE;
	} else
	{
		LOGD("Parse buble config failed!");
		return JNI_FALSE;
	}
}



extern "C" JNIEXPORT void JNICALL
Java_com_autogame_amdancer_ScreenCaptureService_process(
		JNIEnv *env,
		jobject obj,
		jint width, jint height,
		jobject buffer)
{
	auto *pixels_data = (jbyte *) env->GetDirectBufferAddress(buffer);
	Mat ori_image = Mat(Size(width, height), CV_8UC4, (void *) pixels_data);
	Mat bgr_image;
	cvtColor(ori_image, bgr_image, COLOR_RGBA2BGR);

	jclass thiz = env->GetObjectClass(obj);
	jmethodID click_id = env->GetMethodID(thiz, "click", "(II)V");
	jmethodID hold_id = env->GetMethodID(thiz, "hold", "(II)V");
	jmethodID drag_id = env->GetMethodID(thiz, "drag", "(IIII)V");

	bool has_active;
	static int pressing = false;
	Mat hls, l_thresholded;
	Mat hls_mats[3];
	vector<Rect> buble_bboxes;
	Rect buble_in, buble_out;
	cvtColor(bgr_image, hls, COLOR_BGR2HLS);
	split(hls, hls_mats);
	threshold(hls_mats[1], l_thresholded, 200, 255, THRESH_BINARY);
	find_bubles(l_thresholded, buble_bboxes, buble_config.MAX_DELTA_BUBLE_WH,
				buble_config.MIN_BUBLE_SIZE, buble_config.NOISE_SIZE);
	has_active = get_active_buble(buble_bboxes, buble_in, buble_out);
	if (has_active)
	{
		pressing = true;
		BUBLE_TYPE buble_type = get_buble_type(hls_mats[0], buble_in);
		if (buble_type == BUBLE_TYPE::PURPLE)
		{
			env->CallVoidMethod(obj, click_id, buble_in.x + buble_in.width / 2,
								buble_in.y + buble_in.height / 2);
			LOGD("Click: (%d, %d)", buble_in.x + buble_in.width / 2,
				 buble_in.y + buble_in.height / 2);
		} else if (buble_type == BUBLE_TYPE::HOLD)
		{
			env->CallVoidMethod(obj, hold_id, buble_in.x + buble_in.width / 2,
								buble_in.y + buble_in.height / 2);
			LOGD("Hold: (%d, %d)", buble_in.x + buble_in.width / 2,
				 buble_in.y + buble_in.height / 2);
			std::this_thread::sleep_for(std::chrono::milliseconds(500));
		} else if (buble_type == BUBLE_TYPE::YELLOW || buble_type == BUBLE_TYPE::BLUE)
		{
			ARROW_TYPE_BB arrow_type = get_arrow_type(l_thresholded, buble_in);
			int x_start, y_start, x_end, y_end;
			x_start = buble_in.x + buble_in.width / 2;
			y_start = buble_in.y + buble_in.height / 2;
			if (arrow_type == ARROW_TYPE_BB::UP)
			{
				x_end = buble_out.x + buble_out.width / 2;
				y_end = buble_out.y;
			} else if (arrow_type == ARROW_TYPE_BB::LEFT)
			{
				x_end = buble_out.x;
				y_end = buble_out.y + buble_out.height / 2;
			} else if (arrow_type == ARROW_TYPE_BB::DOWN)
			{
				x_end = buble_out.x + buble_out.width / 2;
				y_end = buble_out.y + buble_out.height;
			} else if (arrow_type == ARROW_TYPE_BB::RIGHT)
			{
				x_end = buble_out.x + buble_out.width;
				y_end = buble_out.y + buble_out.height / 2;
			} else if (arrow_type == ARROW_TYPE_BB::UP_LEFT)
			{
				x_end = buble_out.x;
				y_end = buble_out.y;
			} else if (arrow_type == ARROW_TYPE_BB::UP_RIGHT)
			{
				x_end = buble_out.x + buble_out.width;
				y_end = buble_out.y;
			} else if (arrow_type == ARROW_TYPE_BB::DOWN_LEFT)
			{
				x_end = buble_out.x;
				y_end = buble_out.y + buble_out.height;
			} else if (arrow_type == ARROW_TYPE_BB::DOWN_RIGHT)
			{
				x_end = buble_out.x + buble_out.width;
				y_end = buble_out.y + buble_out.height;
			} else
			{
				x_end = x_start;
				y_end = y_start;
			}
			env->CallVoidMethod(obj, drag_id, x_start, y_start, x_end, y_end);
			LOGD("Drag from (%d, %d) to (%d, %d)", x_start, y_start, x_end, y_end);
			std::this_thread::sleep_for(std::chrono::milliseconds(400));
		}
	}
	if (buble_bboxes.size() <= 1 && pressing)
	{
		if ((buble_bboxes.size() == 1 &&
			 (buble_config.SPACE_BUBLE_BBOX.contains(buble_bboxes[0].tl()) &&
			  buble_config.SPACE_BUBLE_BBOX.contains(buble_bboxes[0].br()))) ||
			buble_bboxes.empty())
		{
			if (!has_border(hls_mats[0], hls_mats[1], buble_config))
			{
				env->CallVoidMethod(obj, click_id, buble_config.SPACE_BUBLE_BBOX.x +
												   buble_config.SPACE_BUBLE_BBOX.width / 2,
									buble_config.SPACE_BUBLE_BBOX.y +
									buble_config.SPACE_BUBLE_BBOX.height / 2);
				LOGD("Click space: (%d, %d)",
					 buble_config.SPACE_BUBLE_BBOX.x + buble_config.SPACE_BUBLE_BBOX.width / 2,
					 buble_config.SPACE_BUBLE_BBOX.y + buble_config.SPACE_BUBLE_BBOX.height / 2);
				pressing = false;
			}
		}
	}
}