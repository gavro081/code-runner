import { javascript } from "@codemirror/lang-javascript";
import { python } from "@codemirror/lang-python";

interface TestCase {
	input: string;
	expectedOutput: string;
}

type LanguageName = keyof typeof languages;

export interface ProblemView {
	title: string;
	difficulty: "EASY" | "MEDIUM" | "HARD";
	description: string;
	assumptions: string[];
	exampleTestCases: TestCase[];
	constraints: string[];
	starterTemplates: Record<LanguageName, string>;
}

export const problem: ProblemView = {
	title: "1. Two Sum",
	difficulty: "EASY",
	description:
		"Given an array of integers nums and an integer target, return indices of the two numbers such that they add up to target.",
	assumptions: [
		"You may assume that each input would have exactly one solution",
		"You may not use the same element twice",
		"You can return the answer in any order",
	],
	exampleTestCases: [
		{
			input: "nums = [2,7,11,15], target = 9",
			expectedOutput: "[0,1]",
		},
		{
			input: "nums = [3,2,4], target = 6",
			expectedOutput: "[1,2]",
		},
		{
			input: "nums = [3,3], target = 6",
			expectedOutput: "[0,1]",
		},
	],
	constraints: [
		"2 <= nums.length <= 10⁴",
		"-10⁹ <= nums[i] <= 10⁹",
		"-10⁹ <= target <= 10⁹",
		"Only one valid answer exists",
	],
	starterTemplates: {
		PYTHON:
			"class Solution:\n def twoSum(self, nums: List[int], target: int) -> List[int]:\n pass",
		JAVASCRIPT:
			"/**\n * @param {number[]} nums\n * @param {number} target\n * @return {number[]}\n */\nvar twoSum = function(nums, target) {\n \n};",
	},
};

export const languages = {
	JAVASCRIPT: {
		extension: [javascript({ jsx: true, typescript: true })],
		boilerplate: `console.log('hello world')`,
	},
	PYTHON: {
		extension: [python()],
		boilerplate: `print('hello world')`,
	},
};

export const problemsIds = [
	"two-sum",
	"a-phone-code",
	"acronyms",
	"caesar-cipher",
	"cakes",
	"consecutive-numbers",
	"fibonacci",
	"first-non-repeating-character",
	"fizzbuzz",
	"frequency-deviation",
	"heap-algorithm",
	"look-and-say-sequence-conway",
	"rotate-2-dimensional-array-90-degrees",
	"signal-path-through-the-underground-network",
	"sudoku",
	"valid-parentheses",
	"print-pyramid",
	"maximum-subarray-sum",
];
