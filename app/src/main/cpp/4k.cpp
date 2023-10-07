//
// Created by cuong on 10/3/2023.
//

#include "4k.h"
#include <fstream>
#include "utils.h"


bool K4Config::parse_config(const string &config_path)
{
	std::ifstream file(config_path);
	if (!file.is_open())
		return false;
	string line;
	Rect rect;
	while (std::getline(file, line))
	{
		size_t equalsPos = line.find('=');
		if (equalsPos != string::npos)
		{
			string key = line.substr(0, equalsPos);
			string value = line.substr(equalsPos + 1);
			key = strip(key);
			value = strip(value);
			if (key == "left_key")
			{
				if (parse_str_rect(value, rect))
					left_key = rect;
				else
					return false;
			} else if (key == "right_key")
			{
				if (parse_str_rect(value, rect))
					right_key = rect;
				else
					return false;
			} else if (key == "up_key")
			{
				if (parse_str_rect(value, rect))
					up_key = rect;
				else
					return false;
			} else if (key == "down_key")
			{
				if (parse_str_rect(value, rect))
					down_key = rect;
				else
					return false;
			} else if (key == "space_key")
			{
				if (parse_str_rect(value, rect))
					space_key = rect;
				else
					return false;
			} else if (key == "arrows_key")
			{
				if (parse_str_rect(value, rect))
					arrows_key = rect;
				else
					return false;
			} else if (key == "pointer_box")
			{
				if (parse_str_rect(value, rect))
					pointer_box = rect;
				else
					return false;
			} else if (key == "per_box")
			{
				if (parse_str_rect(value, rect))
					per_box = rect;
				else
					return false;
			} else if (key == "WIDTH")
			{
				WIDTH = std::stoi(value);
			} else if (key == "HEIGHT")
			{
				HEIGHT = std::stoi(value);
			}
		}
	}
	return true;
}


void K4Config::print_config() const
{
	LOGD("WIDTH x HEIGHT: (%d x %d)", WIDTH, HEIGHT);
	LOGD("left_key: (%d, %d, %d, %d)", left_key.x, left_key.y, left_key.width, left_key.height);
	LOGD("right_key: (%d, %d, %d, %d)", right_key.x, right_key.y, right_key.width,
		 right_key.height);
	LOGD("up_key: (%d, %d, %d, %d)", up_key.x, up_key.y, up_key.width, up_key.height);
	LOGD("down_key: (%d, %d, %d, %d)", down_key.x, down_key.y, down_key.width, down_key.height);
	LOGD("space_key: (%d, %d, %d, %d)", space_key.x, space_key.y, space_key.width,
		 space_key.height);
	LOGD("arrows_key: (%d, %d, %d, %d)", arrows_key.x, arrows_key.y, arrows_key.width,
		 arrows_key.height);
	LOGD("pointer_box: (%d, %d, %d, %d)", pointer_box.x, pointer_box.y, pointer_box.width,
		 pointer_box.height);
	LOGD("per_box: (%d, %d, %d, %d)", per_box.x, per_box.y, per_box.width, per_box.height);
}

bool process_arrows_key_image(const Mat &arrows_img, Rect &current_box, vector<Rect> &left_boxes,
							  vector<ARROW_TYPE_4K> &arrows)
{
	left_boxes.clear();
	arrows.clear();
	Mat mask, hls;
	Mat hls_mats[3];
	cvtColor(arrows_img, hls, COLOR_BGR2HLS);
	split(hls, hls_mats);
	inRange(hls_mats[1], 200, 255, mask);

	vector<vector<Point> > contours;
	vector<Vec4i> hierarchicals;
	findContours(mask, contours, hierarchicals, RETR_CCOMP, CHAIN_APPROX_SIMPLE);
	vector<Rect> arrow_boxes;

	for (int i = 0; i < contours.size(); ++i)
	{
		Rect r = boundingRect(contours[i]);
		float wh = float(r.width) / r.height;
		float hw = float(r.height) / r.width;
		if (r.width > 20 && r.height > 20 && 0.87 < wh && wh < 1.15 && 0.87 < hw && hw < 1.15)
			if (hierarchicals[i][3] < 0)
				arrow_boxes.push_back(r);
	}
	if (arrow_boxes.empty())
		return false;
	std::sort(arrow_boxes.begin(), arrow_boxes.end(),
			  [](const Rect &rect_1, const Rect &rect_2) -> bool
			  {
				  return rect_1.x < rect_2.x;
			  });
	for (int i = 0; i < arrow_boxes.size() - 1; ++i)
		if (arrow_boxes[i].contains(arrow_boxes[i + 1].tl()) &&
			arrow_boxes[i].contains(arrow_boxes[i + 1].br()))
		{
			current_box = arrow_boxes[i];
			left_boxes.push_back(arrow_boxes[i + 1]);
			break;
		}
	get_arrow(mask, left_boxes, hls_mats[0], arrows);
	return true;
}


void get_arrow(const Mat &thresholed, const vector<Rect> &arrow_boxes, const Mat &h_channel,
			   vector<ARROW_TYPE_4K> &arrows)
{
	for (auto &arrow_box: arrow_boxes)
	{
		Mat arrow_img = thresholed(arrow_box);
		Mat red_pixels;
		inRange(h_channel(arrow_box), 166, 255, red_pixels);

		bool is_red = sum(red_pixels)[0] > 0.2 * 255 * arrow_box.width * arrow_box.height;
		int up = sum(arrow_img(Rect(0, 0, arrow_box.width, arrow_box.height / 2)))[0];
		int down = sum(
				arrow_img(Rect(0, arrow_box.height / 2, arrow_box.width, arrow_box.height / 2)))[0];
		int left = sum(arrow_img(Rect(0, 0, arrow_box.width / 2, arrow_box.height)))[0];
		int right = sum(
				arrow_img(Rect(arrow_box.width / 2, 0, arrow_box.width / 2, arrow_box.height)))[0];
		int max_w = max(max(up, down), max(left, right));
		if (up == max_w)
			arrows.push_back(is_red ? ARROW_DOWN : ARROW_UP);
		else if (down == max_w)
			arrows.push_back(is_red ? ARROW_UP : ARROW_DOWN);
		else if (left == max_w)
			arrows.push_back(is_red ? ARROW_RIGHT : ARROW_LEFT);
		else
			arrows.push_back(is_red ? ARROW_LEFT : ARROW_RIGHT);
	}
}
