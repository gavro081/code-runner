const fs = require("fs");

// INJECTED USER CODE
// prettier-ignore
{{USER_CODE}}

// INJECTED TEST CASES
const testCasesJson = `
{{TEST_CASES_JSON}}
`;

const METHOD_NAME = "{{METHOD_NAME}}";

// TEST RUNNER LOGIC
function runTests() {
	let testCases;
	try {
		testCases = JSON.parse(testCasesJson);
	} catch (e) {
		console.log(`SYSTEM ERROR: Invalid test case JSON. ${e.message}`);
		return;
	}

	let userMethod;
	try {
		userMethod = eval(METHOD_NAME);
	} catch (e) {
		// ignore
	}

	if (typeof userMethod !== "function") {
		console.log(`FAILED: Function '${METHOD_NAME}' not found.`);
		return;
	}

	for (let i = 0; i < testCases.length; i++) {
		const test = testCases[i];

		try {
			let args;
			try {
				args = JSON.parse(test.input);
			} catch (e) {
				args = [test.input];
			}

			if (!Array.isArray(args)) {
				args = [args];
			}

			const expected = JSON.parse(test.expectedOutput);

			const result = userMethod(...args);

			let match = false;
			// 1. direct equality check
			if (JSON.stringify(result) === JSON.stringify(expected)) {
				match = true;
			} else if (Array.isArray(result) && Array.isArray(expected)) {
				try {
					// 2. sorted list check (assume order doesn't matter)
					const resultSorted = JSON.stringify([...result].sort());
					const expectedSorted = JSON.stringify([...expected].sort());
					if (resultSorted === expectedSorted) {
						match = true;
					}
				} catch (e) {
					// ignore
				}
			}

			if (!match) {
				// print the raw args to help the user debug
				console.log(`${"FAILED:".padEnd(10)} Test Case ${i + 1}`);
				console.log(`${"Input:".padEnd(10)} ${JSON.stringify(args)}`);
				console.log(`${"Expected:".padEnd(10)} ${JSON.stringify(expected)}`);
				console.log(`${"Got:".padEnd(10)} ${JSON.stringify(result)}`);
				return;
			}
		} catch (e) {
			console.log(`FAILED: Test Case ${i + 1} (Runtime Error)`);
			console.log(`Error: ${e.message}`);
			return;
		}
	}

	console.log("PASSED ALL TEST CASES");
}

runTests();
