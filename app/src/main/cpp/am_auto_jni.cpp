#include <thread>
#include <chrono>
#include "am_auto_jni.h"

using namespace cv;


extern "C" JNIEXPORT void JNICALL
Java_com_autogame_amdancer_MainActivity_setConfigPath(JNIEnv *env, jobject thiz,
													  jstring jconfig_path)
{
	const char *_config_path;
	_config_path = env->GetStringUTFChars(jconfig_path, nullptr);
	config_path = _config_path;
	env->ReleaseStringUTFChars(jconfig_path, _config_path);
	buble_config_path = config_path + "/" + "buble_config.ini";
	k4_config_path = config_path + "/" + "4k_config.ini";
	LOGD("Config dir: %s", config_path.c_str());
	LOGD("Buble config path: %s", buble_config_path.c_str());
	LOGD("4K config path: %s", k4_config_path.c_str());
}

extern "C" JNIEXPORT jboolean
JNICALL
Java_com_autogame_amdancer_MainActivity_initConfig(JNIEnv *env, jobject obj)
{
	buble_config.is_init = buble_config.parse_config(buble_config_path);
	if (buble_config.is_init)
	{
		LOGD("Parse buble config success!");
		buble_config.print_config();
		buble_config.adjust_bbox(buble_config.DELTA);
		return JNI_TRUE;
	} else
	{
		LOGD("Parse buble config failed!");
		return JNI_FALSE;
	}
}

extern "C" JNIEXPORT jboolean
JNICALL
Java_com_autogame_amdancer_MainActivity_init4kConfig(JNIEnv *env, jobject obj)
{
	k4_config.is_init = k4_config.parse_config(k4_config_path);
	if (k4_config.is_init)
	{
		LOGD("Parse 4K config success!");
		k4_config.print_config();
		return JNI_TRUE;
	} else
	{
		LOGD("Parse 4K config failed!");
		return JNI_FALSE;
	}
}

extern "C" JNIEXPORT void JNICALL
Java_com_autogame_amdancer_ScreenCaptureService_setupCB(JNIEnv *env, jobject obj)
{
	jclass thiz = env->GetObjectClass(obj);
	click_id = env->GetMethodID(thiz, "click", "(II)V");
	hold_id = env->GetMethodID(thiz, "hold", "(II)V");
	drag_id = env->GetMethodID(thiz, "drag", "(IIII)V");
}

extern "C" JNIEXPORT void JNICALL
Java_com_autogame_amdancer_ScreenCaptureService_processBB(
		JNIEnv
		*env,
		jobject obj,
		jint
		width,
		jint height,
		jobject
		buffer)
{
	auto *pixels_data = (jbyte *) env->GetDirectBufferAddress(buffer);
	Mat ori_image = Mat(Size(width, height), CV_8UC4, (void *) pixels_data);
	Mat bgr_image;
	cvtColor(ori_image, bgr_image, COLOR_RGBA2BGR
	);
	if (!buble_config.is_init)
	{
		bool has_config = buble_detect_config(bgr_image, buble_config);
		if (has_config)
		{
			Mat image = bgr_image.clone();
			buble_config.is_init = true;
			LOGD("Detect buble config:");
			buble_config.DELTA = DELTA;
			buble_config.print_config();
			buble_config.save_config(buble_config_path);
			buble_config.adjust_bbox(DELTA);
			rectangle(image, buble_config.SPACE_BUBLE_BBOX, Scalar(0, 255, 0), 2);
			rectangle(image, buble_config.SPACE_BUBLE_BBOX_IN, Scalar(0, 0, 255), 2);
			rectangle(image, buble_config.SPACE_BUBLE_BBOX_EXT, Scalar(255, 0, 255), 2);
			imwrite(config_path + "/" + "buble_config.jpg", image);
		} else
		{
			std::this_thread::sleep_for(std::chrono::milliseconds(500));
			return;
		}
	}
	bool has_active;
	static int pressing = false;
	Mat hls, l_thresholded;
	Mat hls_mats[3];
	vector<Rect> buble_bboxes;
	Rect buble_in, buble_out;
	cvtColor(bgr_image, hls, COLOR_BGR2HLS
	);
	split(hls, hls_mats
	);
	threshold(hls_mats[1], l_thresholded,
			  200, 255, THRESH_BINARY);
	find_bubles(l_thresholded, buble_bboxes, buble_config.MAX_DELTA_BUBLE_WH,
				buble_config.MIN_BUBLE_SIZE, buble_config.NOISE_SIZE);
	has_active = get_active_buble(buble_bboxes, buble_in, buble_out);
//	static int i = 0; //todo

	if (has_active)
	{
		pressing = true;
		BUBLE_TYPE buble_type = get_buble_type(hls_mats[0], buble_in);
		if (buble_type == BUBLE_TYPE::PURPLE)
		{
			env->CallVoidMethod(obj, click_id, buble_in.x + buble_in.width / 2,
								buble_in.y + buble_in.height / 2);

		} else if (buble_type == BUBLE_TYPE::HOLD)
		{
			env->CallVoidMethod(obj, hold_id, buble_in.x + buble_in.width / 2,
								buble_in.y + buble_in.height / 2);
			std::this_thread::sleep_for(std::chrono::milliseconds(300)
			);
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
			env->CallVoidMethod(obj, drag_id, x_start, y_start, x_end, y_end
			);
//			rectangle(bgr_image, buble_in, Scalar(0, 255, 0), 2);
//			rectangle(bgr_image, buble_out, Scalar(0, 0, 255), 2);
//			putText(bgr_image, std::to_string(arrow_type), buble_out.tl(), FONT_HERSHEY_SIMPLEX, 1, Scalar(0, 255, 0));
//			imwrite(config_path + "/img_" + std::to_string(i) + ".jpg", bgr_image);
//			i += 1;
			std::this_thread::sleep_for(std::chrono::milliseconds(100)
			);
		}
//		imwrite(config_path + "/img_" + std::to_string(i) + ".jpg", bgr_image);
//		i += 1;
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
				pressing = false;
			}
		}
	}
}

extern "C" JNIEXPORT void JNICALL
Java_com_autogame_amdancer_ScreenCaptureService_process4k(
		JNIEnv
		*env,
		jobject obj,
		jint
		width,
		jint height,
		jobject
		buffer)
{
	auto *pixels_data = (jbyte *) env->GetDirectBufferAddress(buffer);
	Mat ori_image = Mat(Size(width, height), CV_8UC4, (void *) pixels_data);
	Mat bgr_image;
	cvtColor(ori_image, bgr_image, COLOR_RGBA2BGR
	);
	if (bgr_image.cols != k4_config.WIDTH || bgr_image.rows != k4_config.HEIGHT)
	{
		std::this_thread::sleep_for(std::chrono::milliseconds(500));
		return;
	}
	static int pressing = false;
	static int last_x = 0;

	vector<Rect> left_boxes;
	vector<ARROW_TYPE_4K> arrows;
	Rect current_box, key_xy;

	bool is_active = process_arrows_key_image(bgr_image(k4_config.arrows_key), current_box,
											  left_boxes, arrows);
	if (is_active && !left_boxes.

			empty()

			)
	{
		if (left_boxes[0].x < last_x + 10)
		{
			std::this_thread::sleep_for(std::chrono::milliseconds(5)
			);
			return;
		}
		last_x = left_boxes[0].x;

		pressing = true;
		auto arrow = arrows[0];
		if (arrow == ARROW_UP)
			key_xy = k4_config.up_key;
		else if (arrow == ARROW_DOWN)
			key_xy = k4_config.down_key;
		else if (arrow == ARROW_LEFT)
			key_xy = k4_config.left_key;
		else
			key_xy = k4_config.right_key;

		env->
				CallVoidMethod(obj, click_id, key_xy
													  .x + key_xy.width / 2,
							   key_xy.y + key_xy.height / 2);
//		LOGD("Click: (%d, %d)", key_xy.x + key_xy.width / 2,
//			 key_xy.y + key_xy.height / 2);
		std::this_thread::sleep_for(std::chrono::milliseconds(20)
		);
	} else if (pressing)
	{
		Mat grey, thresholded, kernel, dilated_image;
		Mat pointer_img = bgr_image(k4_config.pointer_box);
		cvtColor(pointer_img, grey, COLOR_BGR2GRAY
		);
		threshold(grey, thresholded,
				  180, 255, THRESH_BINARY);
		kernel = Mat(8, 8, CV_8U, Scalar(1));
		erode(thresholded, dilated_image, kernel
		);
		vector<vector<Point>> contours;
		vector<Vec4i> hierarchy;
		vector<Rect> bboxes;
		findContours(dilated_image, contours, hierarchy, RETR_EXTERNAL, CHAIN_APPROX_SIMPLE
		);
		for (
			auto &ct
				: contours)
		{
			Rect r = boundingRect(ct);
			if (r.height > 20)
				bboxes.
						push_back(boundingRect(ct)
				);
		}
		std::sort(bboxes
						  .

								  begin(), bboxes

						  .

								  end(),

				  [](
						  const Rect &r1,
						  const Rect &r2
				  ) -> bool
				  {
					  return abs(1 -
								 float(r1
											   .width) / r1.height) < abs(1 -
																		  float(r2
																						.width) /
																		  r2.height);
				  });
		if (!bboxes.

				empty()

				)
		{
			Rect r = bboxes[0];
			if (r.x + r.width / 2 >=
				k4_config.per_box.x - k4_config.pointer_box.x)
			{
				env->
						CallVoidMethod(obj, click_id,
									   k4_config
											   .space_key.x + k4_config.space_key.width / 2,
									   k4_config.space_key.y + k4_config.space_key.height / 2);
//				LOGD("Click: (%d, %d)", k4_config.space_key.x + k4_config.space_key.width / 2,
//					 k4_config.space_key.y + k4_config.space_key.height / 2);
				pressing = false;
				last_x = 0;
			}
		}
		std::this_thread::sleep_for(std::chrono::milliseconds(10)
		);
	} else
	{
		last_x = 0;
		std::this_thread::sleep_for(std::chrono::milliseconds(50)
		);
	}
}


extern "C" JNIEXPORT void JNICALL
Java_com_autogame_amdancer_Settings_reload4kConfig(JNIEnv *env, jobject obj)
{
	k4_config.is_init = k4_config.parse_config(k4_config_path);
	if (k4_config.is_init)
	{
		LOGD("Reload 4K config success!");
		k4_config.print_config();
	} else
	{
		LOGD("Reload 4K config failed!");
	}
}
