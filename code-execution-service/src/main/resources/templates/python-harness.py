import sys
import json
from typing import List


# INJECTED USER CODE
{{USER_CODE}}

# INJECTED TEST CASES
test_cases_json = """
{{TEST_CASES_JSON}}
"""

# TEST RUNNER LOGIC
def run_tests():
    try:
        solution = Solution()
    except Exception as e:
        print(f"FAILED: Could not initialize Solution class. Error: {e}")
        return

    try:
        test_cases = json.loads(test_cases_json)
    except json.JSONDecodeError as e:
        print(f"SYSTEM ERROR: Invalid test case JSON format. Error: {e}")
        return

    for i, test in enumerate(test_cases):
        try:
            # Example input string: "[2,7,11,15]\\n9"

            # split the raw input string by newline
            input_parts = test['input'].strip().split('\n')
            nums = json.loads(input_parts[0])
            target = int(input_parts[1])

            expected = json.loads(test['expectedOutput'])

            # call user's function
            result = solution.twoSum(nums, target)

            # sort both to ensure order doesn't matter (e.g. [0,1] == [1,0])
            if sorted(result) != sorted(expected):
                print(f"FAILED: Test Case {i+1}")
                print(f"Input:    nums={nums}, target={target}")
                print(f"Expected: {expected}")
                print(f"Got:      {result}")
                return

        except Exception as e:
            print(f"FAILED: Test Case {i+1} (Runtime Error)")
            print(f"Error: {e}")
            return

    # If we get here, no tests failed
    print("PASSED ALL CASES")

if __name__ == "__main__":
    run_tests()