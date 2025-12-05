const fs = require('fs');

// INJECTED USER CODE
{{USER_CODE}}

// INJECTED TEST CASES
const testCasesJson = `
{{TEST_CASES_JSON}}
`;

// TEST RUNNER LOGIC
function runTests() {
    let testCases;
    try {
        testCases = JSON.parse(testCasesJson);
    } catch (e) {
        console.log(`SYSTEM ERROR: Invalid test case JSON. ${e.message}`);
        return;
    }

    for (let i = 0; i < testCases.length; i++) {
        const test = testCases[i];

        try {
            // Example input string: "[2,7,11,15]\\n9"
            const inputParts = test.input.trim().split("\n");
            const nums = JSON.parse(inputParts[0]);
            const target = parseInt(inputParts[1]);

            const expected = JSON.parse(test.expectedOutput);

            if (typeof twoSum !== "function") {
                console.log("FAILED: Function 'twoSum' not found.");
                return;
            }

            const result = twoSum(nums, target);

            // sort both to ensure order doesn't matter (e.g. [0,1] == [1,0])
            const resultSorted = JSON.stringify(result.sort((a, b) => a - b));
            const expectedSorted = JSON.stringify(expected.sort((a, b) => a - b));

            if (resultSorted !== expectedSorted) {
                console.log(`FAILED: Test Case ${i + 1}`);
                console.log(`Input:    nums=${JSON.stringify(nums)}, target=${target}`);
                console.log(`Expected: ${JSON.stringify(expected)}`);
                console.log(`Got:      ${JSON.stringify(result)}`);
                return;
            }
        } catch (e) {
            console.log(`FAILED: Test Case ${i + 1} (Runtime Error)`);
            console.log(`Error: ${e.message}`);
            return;
        }
    }

    console.log("PASSED ALL CASES");
}

runTests();
