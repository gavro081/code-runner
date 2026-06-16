import { ArrowLeft, Moon, Plus, Sun, Trash2 } from "lucide-react";
import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { API_BASE_URL } from "../utils/config";

type Difficulty = "EASY" | "MEDIUM" | "HARD";
type ThemeMode = "light" | "dark";

interface TestCase {
	input: string;
	expectedOutput: string;
}

interface ProblemPayload {
	title: string;
	difficulty: Difficulty;
	methodName: string;
	description: string;
	assumptions: string[];
	constraints: string[];
	exampleTestCases: TestCase[];
	testCases: TestCase[];
	starterTemplates: {
		PYTHON: string;
		JAVASCRIPT: string;
	};
}

const slugify = (title: string) =>
	title
		.toLowerCase()
		.trim()
		.replace(/[^a-z0-9\s-]/g, "")
		.replace(/\s+/g, "-");

const emptyTestCase = (): TestCase => ({ input: "", expectedOutput: "" });

export const AddProblem = () => {
	const navigate = useNavigate();
	const [themeMode, setThemeMode] = useState<ThemeMode>("dark");
	const isDark = themeMode === "dark";

	const [title, setTitle] = useState("");
	const [difficulty, setDifficulty] = useState<Difficulty>("EASY");
	const [methodName, setMethodName] = useState("");
	const [description, setDescription] = useState("");
	const [assumptions, setAssumptions] = useState<string[]>([""]);
	const [constraints, setConstraints] = useState<string[]>([""]);
	const [exampleTestCases, setExampleTestCases] = useState<TestCase[]>([
		emptyTestCase(),
	]);
	const [testCases, setTestCases] = useState<TestCase[]>([emptyTestCase()]);
	const [pythonTemplate, setPythonTemplate] = useState("");
	const [jsTemplate, setJsTemplate] = useState("");
	const [showPreview, setShowPreview] = useState(false);

	const generatedId = slugify(title);

	//  helpers for dynamic string lists
	const updateListItem = (
		list: string[],
		setList: (v: string[]) => void,
		index: number,
		value: string,
	) => {
		const updated = [...list];
		updated[index] = value;
		setList(updated);
	};

	const addListItem = (list: string[], setList: (v: string[]) => void) =>
		setList([...list, ""]);

	const removeListItem = (
		list: string[],
		setList: (v: string[]) => void,
		index: number,
	) => setList(list.filter((_, i) => i !== index));

	//  helpers for test case lists
	const updateTestCase = (
		list: TestCase[],
		setList: (v: TestCase[]) => void,
		index: number,
		field: keyof TestCase,
		value: string,
	) => {
		const updated = [...list];
		updated[index] = { ...updated[index], [field]: value };
		setList(updated);
	};

	const addTestCase = (list: TestCase[], setList: (v: TestCase[]) => void) =>
		setList([...list, emptyTestCase()]);

	const removeTestCase = (
		list: TestCase[],
		setList: (v: TestCase[]) => void,
		index: number,
	) => setList(list.filter((_, i) => i !== index));

	// build payload
	const buildPayload = (): ProblemPayload => ({
		title,
		difficulty,
		methodName,
		description,
		assumptions: assumptions.filter((a) => a.trim() !== ""),
		constraints: constraints.filter((c) => c.trim() !== ""),
		exampleTestCases: exampleTestCases.filter(
			(tc) => tc.input.trim() !== "" || tc.expectedOutput.trim() !== "",
		),
		testCases: testCases.filter(
			(tc) => tc.input.trim() !== "" || tc.expectedOutput.trim() !== "",
		),
		starterTemplates: {
			PYTHON: pythonTemplate,
			JAVASCRIPT: jsTemplate,
		},
	});

	const submitProblem = async () => {
		const payload = buildPayload();
		try {
			const response = await fetch(`${API_BASE_URL}/api/problems`, {
				method: "POST",
				headers: { "Content-Type": "application/json" },
				body: JSON.stringify(payload),
			});
			if (response.ok) {
				alert("Problem submitted successfully!");
				navigate("/");
			}
		} catch (error) {
			console.error("Error submitting problem:", error);
			alert("An error occurred while submitting the problem.");
		}
	};

	// styles

	return (
		<div
			className={`min-h-screen ${
				isDark ? "bg-gray-900 text-gray-100" : "bg-white text-gray-900"
			}`}
		>
			<div className="max-w-3xl mx-auto p-6">
				{/* Header */}
				<div className="flex justify-between items-center mb-8">
					<div className="flex items-center gap-3">
						<button
							onClick={() => navigate("/")}
							className={`flex items-center gap-1 text-sm transition-colors ${
								isDark
									? "text-gray-400 hover:text-white"
									: "text-gray-500 hover:text-gray-900"
							}`}
						>
							<ArrowLeft size={18} />
							Back
						</button>
						<h1
							className={`text-3xl font-bold ${
								isDark ? "text-white" : "text-gray-900"
							}`}
						>
							Add Problem
						</h1>
					</div>
					<button
						onClick={() => setThemeMode(isDark ? "light" : "dark")}
						className={`p-2 rounded-lg transition-colors cursor-pointer ${
							isDark
								? "bg-gray-700 hover:bg-gray-600 text-white"
								: "bg-gray-200 hover:bg-gray-300 text-gray-900"
						}`}
						title={`Switch to ${isDark ? "light" : "dark"} mode`}
					>
						{isDark ? <Sun size={20} /> : <Moon size={20} />}
					</button>
				</div>

				{/* Basic Info */}
				<div
					className={`rounded-xl p-5 mb-6 ${
						isDark ? "bg-gray-800" : "bg-gray-50 border border-gray-200"
					}`}
				>
					<h2
						className={`text-base font-bold mb-4 ${
							isDark ? "text-white" : "text-gray-900"
						}`}
					>
						Basic Info
					</h2>

					<div className="grid grid-cols-2 gap-4 mb-4">
						<div className="col-span-2">
							<label
								className={`block text-sm font-semibold mb-1 ${
									isDark ? "text-gray-300" : "text-gray-700"
								}`}
							>
								Title
							</label>
							<input
								className={`w-full rounded-lg px-3 py-2 text-sm border focus:outline-none focus:ring-2 focus:ring-blue-500 ${
									isDark
										? "bg-gray-800 border-gray-600 text-gray-100 placeholder-gray-500"
										: "bg-white border-gray-300 text-gray-900 placeholder-gray-400"
								}`}
								placeholder="e.g. Two Sum"
								value={title}
								onChange={(e) => setTitle(e.target.value)}
							/>
							{generatedId && (
								<p
									className={`text-xs mt-1 ${
										isDark ? "text-gray-500" : "text-gray-400"
									}`}
								>
									ID: <span className="font-mono">{generatedId}</span>
								</p>
							)}
						</div>

						<div>
							<label
								className={`block text-sm font-semibold mb-1 ${
									isDark ? "text-gray-300" : "text-gray-700"
								}`}
							>
								Difficulty
							</label>
							<select
								className={`w-full rounded-lg px-3 py-2 text-sm border focus:outline-none focus:ring-2 focus:ring-blue-500 ${
									isDark
										? "bg-gray-800 border-gray-600 text-gray-100 placeholder-gray-500"
										: "bg-white border-gray-300 text-gray-900 placeholder-gray-400"
								}`}
								value={difficulty}
								onChange={(e) => setDifficulty(e.target.value as Difficulty)}
							>
								<option value="EASY">Easy</option>
								<option value="MEDIUM">Medium</option>
								<option value="HARD">Hard</option>
							</select>
						</div>

						<div>
							<label
								className={`block text-sm font-semibold mb-1 ${
									isDark ? "text-gray-300" : "text-gray-700"
								}`}
							>
								Method Name
							</label>
							<input
								className={`w-full rounded-lg px-3 py-2 text-sm border focus:outline-none focus:ring-2 focus:ring-blue-500 ${
									isDark
										? "bg-gray-800 border-gray-600 text-gray-100 placeholder-gray-500"
										: "bg-white border-gray-300 text-gray-900 placeholder-gray-400"
								}`}
								placeholder="e.g. twoSum"
								value={methodName}
								onChange={(e) => setMethodName(e.target.value)}
							/>
						</div>
					</div>

					<div>
						<label
							className={`block text-sm font-semibold mb-1 ${
								isDark ? "text-gray-300" : "text-gray-700"
							}`}
						>
							Description
						</label>
						<textarea
							className={`w-full rounded-lg px-3 py-2 text-sm border focus:outline-none focus:ring-2 focus:ring-blue-500 resize-none ${
								isDark
									? "bg-gray-800 border-gray-600 text-gray-100 placeholder-gray-500"
									: "bg-white border-gray-300 text-gray-900 placeholder-gray-400"
							}`}
							rows={3}
							placeholder="Describe the problem..."
							value={description}
							onChange={(e) => setDescription(e.target.value)}
						/>
					</div>
				</div>

				{/* Assumptions */}
				<div
					className={`rounded-xl p-5 mb-6 ${
						isDark ? "bg-gray-800" : "bg-gray-50 border border-gray-200"
					}`}
				>
					<h2
						className={`text-base font-bold mb-4 ${
							isDark ? "text-white" : "text-gray-900"
						}`}
					>
						Assumptions
					</h2>
					{assumptions.map((assumption, i) => (
						<div key={i} className="flex items-center gap-2 mb-2">
							<input
								className={`w-full rounded-lg px-3 py-2 text-sm border focus:outline-none focus:ring-2 focus:ring-blue-500 ${
									isDark
										? "bg-gray-800 border-gray-600 text-gray-100 placeholder-gray-500"
										: "bg-white border-gray-300 text-gray-900 placeholder-gray-400"
								}`}
								placeholder={`Assumption ${i + 1}`}
								value={assumption}
								onChange={(e) =>
									updateListItem(assumptions, setAssumptions, i, e.target.value)
								}
							/>
							{assumptions.length > 1 && (
								<button
									className={`p-1.5 rounded transition-colors ${
										isDark
											? "text-gray-400 hover:text-red-400 hover:bg-gray-700"
											: "text-gray-400 hover:text-red-500 hover:bg-gray-200"
									}`}
									onClick={() => removeListItem(assumptions, setAssumptions, i)}
								>
									<Trash2 size={16} />
								</button>
							)}
						</div>
					))}
					<button
						className={`flex items-center gap-1 text-sm mt-2 px-3 py-1.5 rounded-lg transition-colors ${
							isDark
								? "text-blue-400 hover:bg-gray-700"
								: "text-blue-600 hover:bg-blue-50"
						}`}
						onClick={() => addListItem(assumptions, setAssumptions)}
					>
						<Plus size={16} /> Add assumption
					</button>
				</div>

				{/* Constraints */}
				<div
					className={`rounded-xl p-5 mb-6 ${
						isDark ? "bg-gray-800" : "bg-gray-50 border border-gray-200"
					}`}
				>
					<h2
						className={`text-base font-bold mb-4 ${
							isDark ? "text-white" : "text-gray-900"
						}`}
					>
						Constraints
					</h2>
					{constraints.map((constraint, i) => (
						<div key={i} className="flex items-center gap-2 mb-2">
							<input
								className={`w-full rounded-lg px-3 py-2 text-sm border focus:outline-none focus:ring-2 focus:ring-blue-500 ${
									isDark
										? "bg-gray-800 border-gray-600 text-gray-100 placeholder-gray-500"
										: "bg-white border-gray-300 text-gray-900 placeholder-gray-400"
								}`}
								placeholder={`e.g. 2 <= nums.length <= 10⁴`}
								value={constraint}
								onChange={(e) =>
									updateListItem(constraints, setConstraints, i, e.target.value)
								}
							/>
							{constraints.length > 1 && (
								<button
									className={`p-1.5 rounded transition-colors ${
										isDark
											? "text-gray-400 hover:text-red-400 hover:bg-gray-700"
											: "text-gray-400 hover:text-red-500 hover:bg-gray-200"
									}`}
									onClick={() => removeListItem(constraints, setConstraints, i)}
								>
									<Trash2 size={16} />
								</button>
							)}
						</div>
					))}
					<button
						className={`flex items-center gap-1 text-sm mt-2 px-3 py-1.5 rounded-lg transition-colors ${
							isDark
								? "text-blue-400 hover:bg-gray-700"
								: "text-blue-600 hover:bg-blue-50"
						}`}
						onClick={() => addListItem(constraints, setConstraints)}
					>
						<Plus size={16} /> Add constraint
					</button>
				</div>

				{/* Example Test Cases */}
				<div
					className={`rounded-xl p-5 mb-6 ${
						isDark ? "bg-gray-800" : "bg-gray-50 border border-gray-200"
					}`}
				>
					<h2
						className={`text-base font-bold mb-4 ${
							isDark ? "text-white" : "text-gray-900"
						}`}
					>
						Example Test Cases
					</h2>
					<p
						className={`text-xs mb-3 ${
							isDark ? "text-gray-500" : "text-gray-400"
						}`}
					>
						These are shown to the user in the problem description.
					</p>
					{exampleTestCases.map((tc, i) => (
						<div
							key={i}
							className={`rounded-lg p-3 mb-3 ${
								isDark ? "bg-gray-700" : "bg-gray-100"
							}`}
						>
							<div className="flex justify-between items-center mb-2">
								<span
									className={`text-xs font-semibold ${
										isDark ? "text-gray-400" : "text-gray-600"
									}`}
								>
									Example {i + 1}
								</span>
								{exampleTestCases.length > 1 && (
									<button
										className={`p-1.5 rounded transition-colors ${
											isDark
												? "text-gray-400 hover:text-red-400 hover:bg-gray-700"
												: "text-gray-400 hover:text-red-500 hover:bg-gray-200"
										}`}
										onClick={() =>
											removeTestCase(exampleTestCases, setExampleTestCases, i)
										}
									>
										<Trash2 size={15} />
									</button>
								)}
							</div>
							<div className="grid grid-cols-2 gap-2">
								<div>
									<label
										className={`${`block text-sm font-semibold mb-1 ${
											isDark ? "text-gray-300" : "text-gray-700"
										}`} text-xs`}
									>
										Input
									</label>
									<input
										className={`w-full rounded-lg px-3 py-2 text-sm border focus:outline-none focus:ring-2 focus:ring-blue-500 ${
											isDark
												? "bg-gray-800 border-gray-600 text-gray-100 placeholder-gray-500"
												: "bg-white border-gray-300 text-gray-900 placeholder-gray-400"
										}`}
										placeholder="e.g. [[2,7,11,15], 9]"
										value={tc.input}
										onChange={(e) =>
											updateTestCase(
												exampleTestCases,
												setExampleTestCases,
												i,
												"input",
												e.target.value,
											)
										}
									/>
								</div>
								<div>
									<label
										className={`${`block text-sm font-semibold mb-1 ${
											isDark ? "text-gray-300" : "text-gray-700"
										}`} text-xs`}
									>
										Expected Output
									</label>
									<input
										className={`w-full rounded-lg px-3 py-2 text-sm border focus:outline-none focus:ring-2 focus:ring-blue-500 ${
											isDark
												? "bg-gray-800 border-gray-600 text-gray-100 placeholder-gray-500"
												: "bg-white border-gray-300 text-gray-900 placeholder-gray-400"
										}`}
										placeholder="e.g. [0, 1]"
										value={tc.expectedOutput}
										onChange={(e) =>
											updateTestCase(
												exampleTestCases,
												setExampleTestCases,
												i,
												"expectedOutput",
												e.target.value,
											)
										}
									/>
								</div>
							</div>
						</div>
					))}
					<button
						className={`flex items-center gap-1 text-sm mt-2 px-3 py-1.5 rounded-lg transition-colors ${
							isDark
								? "text-blue-400 hover:bg-gray-700"
								: "text-blue-600 hover:bg-blue-50"
						}`}
						onClick={() => addTestCase(exampleTestCases, setExampleTestCases)}
					>
						<Plus size={16} /> Add example
					</button>
				</div>

				{/* Test Cases */}
				<div
					className={`rounded-xl p-5 mb-6 ${
						isDark ? "bg-gray-800" : "bg-gray-50 border border-gray-200"
					}`}
				>
					<h2
						className={`text-base font-bold mb-4 ${
							isDark ? "text-white" : "text-gray-900"
						}`}
					>
						Test Cases
					</h2>
					<p
						className={`text-xs mb-3 ${
							isDark ? "text-gray-500" : "text-gray-400"
						}`}
					>
						Hidden test cases used to evaluate submissions.
					</p>
					{testCases.map((tc, i) => (
						<div
							key={i}
							className={`rounded-lg p-3 mb-3 ${
								isDark ? "bg-gray-700" : "bg-gray-100"
							}`}
						>
							<div className="flex justify-between items-center mb-2">
								<span
									className={`text-xs font-semibold ${
										isDark ? "text-gray-400" : "text-gray-600"
									}`}
								>
									Test Case {i + 1}
								</span>
								{testCases.length > 1 && (
									<button
										className={`p-1.5 rounded transition-colors ${
											isDark
												? "text-gray-400 hover:text-red-400 hover:bg-gray-700"
												: "text-gray-400 hover:text-red-500 hover:bg-gray-200"
										}`}
										onClick={() => removeTestCase(testCases, setTestCases, i)}
									>
										<Trash2 size={15} />
									</button>
								)}
							</div>
							<div className="grid grid-cols-2 gap-2">
								<div>
									<label
										className={`${`block text-sm font-semibold mb-1 ${
											isDark ? "text-gray-300" : "text-gray-700"
										}`} text-xs`}
									>
										Input
									</label>
									<input
										className={`w-full rounded-lg px-3 py-2 text-sm border focus:outline-none focus:ring-2 focus:ring-blue-500 ${
											isDark
												? "bg-gray-800 border-gray-600 text-gray-100 placeholder-gray-500"
												: "bg-white border-gray-300 text-gray-900 placeholder-gray-400"
										}`}
										placeholder="e.g. [[3,2,4], 6]"
										value={tc.input}
										onChange={(e) =>
											updateTestCase(
												testCases,
												setTestCases,
												i,
												"input",
												e.target.value,
											)
										}
									/>
								</div>
								<div>
									<label
										className={`${`block text-sm font-semibold mb-1 ${
											isDark ? "text-gray-300" : "text-gray-700"
										}`} text-xs`}
									>
										Expected Output
									</label>
									<input
										className={`w-full rounded-lg px-3 py-2 text-sm border focus:outline-none focus:ring-2 focus:ring-blue-500 ${
											isDark
												? "bg-gray-800 border-gray-600 text-gray-100 placeholder-gray-500"
												: "bg-white border-gray-300 text-gray-900 placeholder-gray-400"
										}`}
										placeholder="e.g. [1, 2]"
										value={tc.expectedOutput}
										onChange={(e) =>
											updateTestCase(
												testCases,
												setTestCases,
												i,
												"expectedOutput",
												e.target.value,
											)
										}
									/>
								</div>
							</div>
						</div>
					))}
					<button
						className={`flex items-center gap-1 text-sm mt-2 px-3 py-1.5 rounded-lg transition-colors ${
							isDark
								? "text-blue-400 hover:bg-gray-700"
								: "text-blue-600 hover:bg-blue-50"
						}`}
						onClick={() => addTestCase(testCases, setTestCases)}
					>
						<Plus size={16} /> Add test case
					</button>
				</div>

				{/* Starter Templates */}
				<div
					className={`rounded-xl p-5 mb-6 ${
						isDark ? "bg-gray-800" : "bg-gray-50 border border-gray-200"
					}`}
				>
					<h2
						className={`text-base font-bold mb-4 ${
							isDark ? "text-white" : "text-gray-900"
						}`}
					>
						Starter Templates
					</h2>

					<div className="mb-4">
						<label
							className={`block text-sm font-semibold mb-1 ${
								isDark ? "text-gray-300" : "text-gray-700"
							}`}
						>
							JavaScript
						</label>
						<textarea
							className={`${`w-full rounded-lg px-3 py-2 text-sm border focus:outline-none focus:ring-2 focus:ring-blue-500 ${
								isDark
									? "bg-gray-800 border-gray-600 text-gray-100 placeholder-gray-500"
									: "bg-white border-gray-300 text-gray-900 placeholder-gray-400"
							}`} font-mono resize-none`}
							rows={6}
							placeholder={`/**\n * @param {number[]} nums\n * @param {number} target\n * @return {number[]}\n */\nvar ${methodName.length != 0 ? methodName : "twoSum"} = function(nums, target) {\n  \n};`}
							value={jsTemplate}
							onChange={(e) => setJsTemplate(e.target.value)}
						/>
					</div>

					<div>
						<label
							className={`block text-sm font-semibold mb-1 ${
								isDark ? "text-gray-300" : "text-gray-700"
							}`}
						>
							Python
						</label>
						<textarea
							className={`${`w-full rounded-lg px-3 py-2 text-sm border focus:outline-none focus:ring-2 focus:ring-blue-500 ${
								isDark
									? "bg-gray-800 border-gray-600 text-gray-100 placeholder-gray-500"
									: "bg-white border-gray-300 text-gray-900 placeholder-gray-400"
							}`} font-mono resize-none`}
							rows={5}
							placeholder={`class Solution:\n    def ${methodName.length != 0 ? methodName : "twoSum"}(self, nums: List[int], target: int) -> List[int]:\n        pass`}
							value={pythonTemplate}
							onChange={(e) => setPythonTemplate(e.target.value)}
						/>
					</div>
				</div>

				{/* Actions */}
				<div className="flex gap-3 mb-6">
					<button
						onClick={() => setShowPreview(!showPreview)}
						className={`flex-1 py-2.5 rounded-lg font-medium transition-colors border ${
							isDark
								? "border-gray-600 text-gray-300 hover:bg-gray-800"
								: "border-gray-300 text-gray-700 hover:bg-gray-100"
						}`}
					>
						{showPreview ? "Hide" : "Show"} JSON Preview
					</button>
					<button
						className="flex-1 py-2.5 rounded-lg font-medium bg-green-600 hover:bg-green-700 text-white transition-colors"
						onClick={submitProblem}
					>
						Submit Problem
					</button>
				</div>

				{/* JSON Preview */}
				{showPreview && (
					<div
						className={`rounded-xl p-5 mb-6 ${
							isDark ? "bg-gray-800" : "bg-gray-50 border border-gray-200"
						}`}
					>
						<h2
							className={`text-base font-bold mb-4 ${
								isDark ? "text-white" : "text-gray-900"
							}`}
						>
							JSON Preview
						</h2>
						<pre
							className={`text-xs font-mono rounded-lg p-4 overflow-auto max-h-96 ${
								isDark
									? "bg-gray-900 text-green-400"
									: "bg-gray-100 text-gray-800"
							}`}
						>
							{JSON.stringify(buildPayload(), null, 2)}
						</pre>
					</div>
				)}
			</div>
		</div>
	);
};
