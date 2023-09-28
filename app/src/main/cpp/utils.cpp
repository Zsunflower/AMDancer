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