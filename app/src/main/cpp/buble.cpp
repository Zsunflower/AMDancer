//
// Created by cuong on 9/26/2023.
//


#include <fstream>
#include "buble.h"
#include "utils.h"

template<typename T>
inline T square(const T &x)
{
	return x * x;
}

template<typename T>
inline T euclidean_distance(const T &x, const T &y)
{
	return sqrt(x * x + y * y);
}

string get_buble_name(BUBLE_TYPE bb_type)
{
	if (bb_type == BUBLE_TYPE::PURPLE)
		return "Purple";
	if (bb_type == BUBLE_TYPE::YELLOW)
		return "Yellow";
	if (bb_type == BUBLE_TYPE::BLUE)
		return "Blue";
	if (bb_type == BUBLE_TYPE::CYAN)
		return "Cyan";
	if (bb_type == BUBLE_TYPE::HOLD)
		return "Hold";
	return "Unk";
}

ARROW_TYPE_BB get_arrow_type(const Mat &thresholded, const Rect &rect)
{
	int delta = rect.width / 4;
	Rect rect_ext = rect;
	rect_ext.x -= delta;
	rect_ext.y -= delta;
	rect_ext.width += 2 * delta;
	rect_ext.height += 2 * delta;
	Mat buble_arrow_img = thresholded(rect_ext);
	delta /= 2;
	Mat eroded, kernel;
	vector<vector<Point>> contours;
	vector<Vec4i> hierarchy;

	// Check up/down type
	kernel = getStructuringElement(MORPH_RECT, Size(1, delta));
	erode(buble_arrow_img, eroded, kernel);

	findContours(eroded, contours, hierarchy, RETR_EXTERNAL, CHAIN_APPROX_SIMPLE);
	for (auto &ct: contours)
	{
		Rect bbox = boundingRect(ct);
		if ((bbox.height >= delta / 2) && abs(bbox.x + bbox.width / 2 - rect_ext.width / 2) < 5)
		{
			if (bbox.y > rect_ext.height / 2)
				return ARROW_TYPE_BB::DOWN;
			else
				return ARROW_TYPE_BB::UP;
		}
	}
	// Check left/right type
	kernel = getStructuringElement(MORPH_RECT, Size(delta, 1));
	erode(buble_arrow_img, eroded, kernel);
	findContours(eroded, contours, hierarchy, RETR_EXTERNAL, CHAIN_APPROX_SIMPLE);
	for (auto &ct: contours)
	{
		Rect bbox = boundingRect(ct);
		if ((bbox.width >= delta / 2) && abs(bbox.y + bbox.height / 2 - rect_ext.height / 2) < 5)
		{
			if (bbox.x > rect_ext.width / 2)
				return ARROW_TYPE_BB::RIGHT;
			else
				return ARROW_TYPE_BB::LEFT;
		}
	}
	// Check up_left/down_right type
	kernel = Mat::eye(delta, delta, CV_8U);
	erode(buble_arrow_img, eroded, kernel);
	findContours(eroded, contours, hierarchy, RETR_EXTERNAL, CHAIN_APPROX_SIMPLE);
	for (auto &ct: contours)
	{
		Rect bbox = boundingRect(ct);
		if ((bbox.width >= delta / 2) && (bbox.height >= delta / 2) && abs(bbox.x - bbox.y) < 5)
		{
			if (bbox.x > rect_ext.width / 2)
				return ARROW_TYPE_BB::DOWN_RIGHT;
			else
				return ARROW_TYPE_BB::UP_LEFT;
		}
	}
	// Check up_right/down_left type
	flip(kernel, kernel, 0);
	erode(buble_arrow_img, eroded, kernel);
	findContours(eroded, contours, hierarchy, RETR_EXTERNAL, CHAIN_APPROX_SIMPLE);
	for (auto &ct: contours)
	{
		Rect bbox = boundingRect(ct);
		if ((bbox.width >= delta / 2) && (bbox.height >= delta / 2) &&
			abs(bbox.x + bbox.width + bbox.y - rect_ext.width) < 5)
		{
			if (bbox.x > rect_ext.width / 2)
				return ARROW_TYPE_BB::UP_RIGHT;
			else
				return ARROW_TYPE_BB::DOWN_LEFT;
		}
	}
	return ARROW_TYPE_BB::ARROW_UNK;
}

BUBLE_TYPE get_buble_type(const Mat &h_channel, const Rect &rect)
{
	Mat buble = h_channel(rect);
	Rect up(rect.width / 2 - 5, rect.height / 4 - 5, 10, 10),
			right(rect.width * 3 / 4 - 5, rect.height / 2 - 5, 10, 10),
			down(rect.width / 2 - 5, rect.height * 3 / 4 - 5, 10, 10),
			left(rect.width / 4 - 5, rect.height / 2 - 5, 10, 10),
			mid(rect.width / 2 - 5, rect.height / 2 - 5, 10, 10);
	Scalar up_mean = mean(buble(up)), right_mean = mean(buble(right)),
			down_mean = mean(buble(down)), left_mean = mean(buble(left)),
			mid_mean = mean(buble(mid));

	// check for purple buble
	int c = 0;
	if (PURPLE_RANGE[0] <= up_mean[0] && up_mean[0] <= PURPLE_RANGE[1])
		c += 1;
	if (PURPLE_RANGE[0] <= right_mean[0] && right_mean[0] <= PURPLE_RANGE[1])
		c += 1;
	if (PURPLE_RANGE[0] <= down_mean[0] && down_mean[0] <= PURPLE_RANGE[1])
		c += 1;
	if (PURPLE_RANGE[0] <= left_mean[0] && left_mean[0] <= PURPLE_RANGE[1])
		c += 1;
	if (PURPLE_RANGE[0] <= mid_mean[0] && mid_mean[0] <= PURPLE_RANGE[1])
		c += 1;
	if (c > 1)
		return BUBLE_TYPE::PURPLE;
	// check for yellow buble
	c = 0;
	if ((YELLOW_RANGE[0] <= up_mean[0] && up_mean[0] <= YELLOW_RANGE[1]) ||
		(YELLOW_RANGE[0] <= down_mean[0] && down_mean[0] <= YELLOW_RANGE[1]))
	{
		if (YELLOW_RANGE[0] <= left_mean[0] && left_mean[0] <= YELLOW_RANGE[1])
			c += 1;
		if (YELLOW_RANGE[0] <= mid_mean[0] && mid_mean[0] <= YELLOW_RANGE[1])
			c += 1;
		if (YELLOW_RANGE[0] <= right_mean[0] && right_mean[0] <= YELLOW_RANGE[1])
			c += 1;
		if (c > 2)
			return BUBLE_TYPE::YELLOW;
	}
	// check for blue buble
	c = 0;
	if (BLUE_RANGE[0] <= up_mean[0] && up_mean[0] <= BLUE_RANGE[1])
		c += 1;
	if (BLUE_RANGE[0] <= right_mean[0] && right_mean[0] <= BLUE_RANGE[1])
		c += 1;
	if (BLUE_RANGE[0] <= down_mean[0] && down_mean[0] <= BLUE_RANGE[1])
		c += 1;
	if (BLUE_RANGE[0] <= left_mean[0] && left_mean[0] <= BLUE_RANGE[1])
		c += 1;
	if (BLUE_RANGE[0] <= mid_mean[0] && mid_mean[0] <= BLUE_RANGE[1])
		c += 1;
	if (c > 1)
		return BUBLE_TYPE::BLUE;
	// check for cyan buble
	c = 0;
	if (CYAN_RANGE[0] <= up_mean[0] && up_mean[0] <= CYAN_RANGE[1])
		c += 1;
	if (CYAN_RANGE[0] <= right_mean[0] && right_mean[0] <= CYAN_RANGE[1])
		c += 1;
	if (CYAN_RANGE[0] <= down_mean[0] && down_mean[0] <= CYAN_RANGE[1])
		c += 1;
	if (CYAN_RANGE[0] <= left_mean[0] && left_mean[0] <= CYAN_RANGE[1])
		c += 1;
	if (CYAN_RANGE[0] <= mid_mean[0] && mid_mean[0] <= CYAN_RANGE[1])
		c += 1;
	if (c > 1)
		return BUBLE_TYPE::CYAN;
	// check for hold buble
	if (YELLOW_RANGE[0] <= mid_mean[0] && mid_mean[0] <= YELLOW_RANGE[1])
	{
		if ((YELLOW_RANGE[0] <= left_mean[0] && left_mean[0] <= YELLOW_RANGE[1]) ||
			(YELLOW_RANGE[0] <= right_mean[0] && right_mean[0] <= YELLOW_RANGE[1]))
			if ((HOLD_RANGE[0] <= up_mean[0] && up_mean[0] <= HOLD_RANGE[1]) ||
				(HOLD_RANGE[0] <= down_mean[0] && down_mean[0] <= HOLD_RANGE[1]))
				return BUBLE_TYPE::HOLD;
	}
	return BUBLE_TYPE::BUBLE_UNK;
}

void find_bubles(const Mat &l_thresholded, vector<Rect> &buble_bboxes, int delta_wh_threshold,
				 int size_threshold, int wh_noise)
{
	vector<vector<Point>> contours;
	vector<Vec4i> hierarchy;
	findContours(l_thresholded, contours, hierarchy, RETR_CCOMP, CHAIN_APPROX_SIMPLE);
	buble_bboxes.clear();
	for (int i = 0; i < contours.size(); ++i)
	{
		Rect r = boundingRect(contours[i]);
		if ((abs(r.width - r.height) < delta_wh_threshold) && r.width > size_threshold)
		{
			if (hierarchy[i][2] < 0)
			{
				buble_bboxes.push_back(r);
			} else
			{
				Rect rc = boundingRect(contours[hierarchy[i][2]]);
				if (rc.width < wh_noise || rc.height < wh_noise)
				{
					buble_bboxes.push_back(r);
				}
			}
		}
	}
}

bool get_active_buble(vector<Rect> &buble_bboxes, Rect &buble_in, Rect &buble_out)
{
	if (buble_bboxes.size() < 2)
		return false;
	sort(buble_bboxes.begin(), buble_bboxes.end(),
		 [](const Rect &rect_1, const Rect &rect_2) -> bool
		 {
			 return euclidean_distance(rect_1.x + rect_1.width / 2, rect_1.y + rect_1.height / 2) <
					euclidean_distance(rect_2.x + rect_2.width / 2, rect_2.y + rect_2.height / 2);
		 });

	for (int i = 0; i < buble_bboxes.size() - 1; ++i)
	{
		if (buble_bboxes[i].contains(buble_bboxes[i + 1].tl()) &&
			buble_bboxes[i].contains(buble_bboxes[i + 1].br()))
		{
			buble_in = buble_bboxes[i + 1];
			buble_out = buble_bboxes[i];
			return true;
		}
		if (buble_bboxes[i + 1].contains(buble_bboxes[i].tl()) &&
			buble_bboxes[i + 1].contains(buble_bboxes[i].br()))
		{
			buble_in = buble_bboxes[i];
			buble_out = buble_bboxes[i + 1];
			return true;
		}
	}
	return false;
}

bool has_border(const Mat &h_img, const Mat &l_img, BubleConfig &buble_config)
{
	Mat l_mask, h_mask, mask;
	inRange(l_img(buble_config.SPACE_BUBLE_BBOX_EXT), 140, 250, l_mask);
	inRange(h_img(buble_config.SPACE_BUBLE_BBOX_EXT), 70, 100, h_mask);
	bitwise_and(l_mask, h_mask, mask);
	vector<vector<Point>> contours;
	vector<Vec4i> hierarchy;
	findContours(mask, contours, hierarchy, RETR_EXTERNAL, CHAIN_APPROX_SIMPLE);

	for (auto &ct: contours)
	{
		Rect r = boundingRect(ct);
		r.x += buble_config.SPACE_BUBLE_BBOX_EXT.x;
		r.y += buble_config.SPACE_BUBLE_BBOX_EXT.y;
		if (r.x < buble_config.SPACE_BUBLE_BBOX.x && r.y < buble_config.SPACE_BUBLE_BBOX.y &&
			r.x + r.width > buble_config.SPACE_BUBLE_BBOX.x + buble_config.SPACE_BUBLE_BBOX.width)
		{
			return true;
		}
	}
	return false;
}

bool BubleConfig::parse_config(const string &config_path)
{
	std::ifstream file(config_path);
	if (!file.is_open())
		return false;
	string line;
	while (std::getline(file, line))
	{
		size_t equalsPos = line.find('=');
		if (equalsPos != string::npos)
		{
			string key = line.substr(0, equalsPos);
			string value = line.substr(equalsPos + 1);
			key = strip(key);
			value = strip(value);
			if (key == "SPACE_BUBLE_BBOX")
			{
				std::istringstream ss(value);
				string token;
				vector<string> tokens;

				while (std::getline(ss, token, ','))
				{
					tokens.push_back(strip(token));
				}
				if (tokens.size() != 4)
					return false;
				SPACE_BUBLE_BBOX.x = std::stoi(tokens[0]);
				SPACE_BUBLE_BBOX.y = std::stoi(tokens[1]);
				SPACE_BUBLE_BBOX.width = std::stoi(tokens[2]);
				SPACE_BUBLE_BBOX.height = std::stoi(tokens[3]);
			} else if (key == "DELTA")
			{
				DELTA = std::stoi(value);
			} else if (key == "MIN_BUBLE_SIZE")
			{
				MIN_BUBLE_SIZE = std::stoi(value);
			} else if (key == "MAX_DELTA_BUBLE_WH")
			{
				MAX_DELTA_BUBLE_WH = std::stoi(value);
			} else if (key == "NOISE_SIZE")
			{
				NOISE_SIZE = std::stoi(value);
			} else if (key == "WIDTH")
			{
				WIDTH = std::stoi(value);
			} else if (key == "HEIGHT")
			{
				HEIGHT = std::stoi(value);
			}
		}
	}
	SPACE_BUBLE_BBOX.x -= DELTA;
	SPACE_BUBLE_BBOX.y -= DELTA;
	SPACE_BUBLE_BBOX.width += 2 * DELTA;
	SPACE_BUBLE_BBOX.height += 2 * DELTA;
	SPACE_BUBLE_BBOX.height = min(SPACE_BUBLE_BBOX.height, HEIGHT - SPACE_BUBLE_BBOX.y);

	SPACE_BUBLE_BBOX_EXT = SPACE_BUBLE_BBOX;
	SPACE_BUBLE_BBOX_EXT.x -= SPACE_BUBLE_BBOX.width;
	SPACE_BUBLE_BBOX_EXT.y -= SPACE_BUBLE_BBOX.width;
	SPACE_BUBLE_BBOX_EXT.width += 2 * SPACE_BUBLE_BBOX.width;
	SPACE_BUBLE_BBOX_EXT.height += 2 * SPACE_BUBLE_BBOX.width;
	SPACE_BUBLE_BBOX_EXT.height = min(SPACE_BUBLE_BBOX_EXT.height, HEIGHT - SPACE_BUBLE_BBOX_EXT.y);

	return true;
}

void BubleConfig::print_config() const
{
	LOGD("WIDTH x HEIGHT: (%d x %d)", WIDTH, HEIGHT);
	LOGD("SPACE_BUBLE_BBOX: (%d, %d, %d, %d)", SPACE_BUBLE_BBOX.x, SPACE_BUBLE_BBOX.y,
		 SPACE_BUBLE_BBOX.width, SPACE_BUBLE_BBOX.height);
	LOGD("DELTA: %d", DELTA);
	LOGD("SPACE_BUBLE_BBOX_EXT: (%d, %d, %d, %d)", SPACE_BUBLE_BBOX_EXT.x, SPACE_BUBLE_BBOX_EXT.y,
		 SPACE_BUBLE_BBOX_EXT.width, SPACE_BUBLE_BBOX_EXT.height);
	LOGD("MIN_BUBLE_SIZE: %d", MIN_BUBLE_SIZE);
	LOGD("MAX_DELTA_BUBLE_WH: %d", MAX_DELTA_BUBLE_WH);
	LOGD("NOISE_SIZE: %d", NOISE_SIZE);
}
