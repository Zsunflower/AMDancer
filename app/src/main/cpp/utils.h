//
// Created by cuong on 9/26/2023.
//

#ifndef AMDANCER_UTILS_H
#define AMDANCER_UTILS_H

#include <string>
#include "opencv2/opencv.hpp"

std::string strip(const std::string &s);

bool parse_str_rect(const std::string &s, cv::Rect &rect);

#endif //AMDANCER_UTILS_H
