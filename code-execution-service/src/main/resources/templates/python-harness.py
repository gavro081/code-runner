import sys
import json
from typing import List


# INJECTED USER CODE
{{USER_CODE}}

# INJECTED TEST CASES
test_cases_json = """
{{TEST_CASES_JSON}}
"""

ENTRY_POINT = "{{METHOD_NAME}}"

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

    # Ensure the method exists on the solution object
    if not hasattr(solution, ENTRY_POINT):
        print(f"FAILED: Method '{ENTRY_POINT}' not found in Solution class.")
        return

    # Get the function reference
    user_method = getattr(solution, ENTRY_POINT)

    for i, test in enumerate(test_cases):
        try:
            # the input in thr db must be a json list of arguments.
            try:
                args = json.loads(test['input'])
            except json.JSONDecodeError:
                # fallback: if input isn't json, treat it as a single string argument
                args = [test['input']]

            # Basic validation to ensure args is a list
            if not isinstance(args, list):
                args = [args]

            # parse expected output
            expected = json.loads(test['expectedOutput'])

            # unpack args into list
            result = user_method(*args)

            match = False

            # 1. direct equality check
            if result == expected:
                match = True
            # 2. sorted list check (assume order doesn't matter)
            elif isinstance(result, list) and isinstance(expected, list):
                try:
                    if sorted(str(x) for x in result) == sorted(str(x) for x in expected):
                        match = True
                except:
                    pass # sort failed, rely on previous check

            if not match:
                print(f"{'FAILED:':<10} Test Case {i+1}")
                # print the raw args to help the user debug
                print(f"{'Input:':<10} {json.dumps(args)}")
                print(f"{'Expected:':<10} {json.dumps(expected)}")
                print(f"{'Got:':<10} {json.dumps(result)}")
                return

        except Exception as e:
            print(f"FAILED: Test Case {i+1} (Runtime Error)")
            print(f"Error: {e}")
            return

    print("PASSED ALL TEST CASES")

if __name__ == "__main__":
    run_tests()
