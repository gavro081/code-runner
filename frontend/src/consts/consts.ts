import { javascript } from "@codemirror/lang-javascript";
import { python } from "@codemirror/lang-python";

export const problem = {
	title: "1. Two Sum",
	difficulty: "Easy",
	description:
		"Given an array of integers nums and an integer target, return indices of the two numbers such that they add up to target.",
	assumptions: [
		"You may assume that each input would have exactly one solution",
		"You may not use the same element twice",
		"You can return the answer in any order",
	],
	examples: [
		{
			input: "nums = [2,7,11,15], target = 9",
			output: "[0,1]",
			explanation: "Because nums[0] + nums[1] == 9, we return [0, 1].",
		},
		{
			input: "nums = [3,2,4], target = 6",
			output: "[1,2]",
		},
		{
			input: "nums = [3,3], target = 6",
			output: "[0,1]",
		},
	],
	constraints: [
		"2 <= nums.length <= 10⁴",
		"-10⁹ <= nums[i] <= 10⁹",
		"-10⁹ <= target <= 10⁹",
		"Only one valid answer exists",
	],
};

export const languages = {
	javascript: {
		extension: [javascript({ jsx: true, typescript: true })],
		boilerplate: `/**
 * @param {number[]} nums
 * @param {number} target
 * @return {number[]}
 */
var twoSum = function(nums, target) {
    
};`,
	},
	python: {
		extension: [python()],
		boilerplate: `from typing import List

class Solution:
    def twoSum(self, nums: List[int], target: int) -> List[int]:
        `,
	},
};
