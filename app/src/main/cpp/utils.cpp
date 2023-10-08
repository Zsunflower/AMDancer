//
// Created by cuong on 9/26/2023.
//

#include "utils.h"

std::string strip(const std::string &s)
{
	size_t start = 0;
	size_t end = s.length() - 1;

	// Find the first non-whitespace character from the beginning
	while (start <= end && std::isspace(s[start]))
	{
		start++;
	}

	// Find the first non-whitespace character from the end
	while (end >= start && std::isspace(s[end]))
	{
		end--;
	}

	// Extract the substring without leading and trailing whitespace
	return s.substr(start, end - start + 1);
}

bool parse_str_rect(const std::string &s, cv::Rect &rect)
{
	std::istringstream ss(s);
	std::string token;
	std::vector<std::string> tokens;

	while (std::getline(ss, token, ','))
	{
		tokens.push_back(strip(token));
	}
	if (tokens.size() != 4)
		return false;
	rect.x = std::stoi(tokens[0]);
	rect.y = std::stoi(tokens[1]);
	rect.width = std::stoi(tokens[2]);
	rect.height = std::stoi(tokens[3]);
	return true;
}