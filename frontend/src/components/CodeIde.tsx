import { tokyoNight } from "@uiw/codemirror-theme-tokyo-night";
import { tokyoNightDay } from "@uiw/codemirror-theme-tokyo-night-day";
import CodeMirror from "@uiw/react-codemirror";
import { Moon, Sun } from "lucide-react";
import React, { useCallback, useEffect, useState } from "react";
import { Panel, PanelGroup, PanelResizeHandle } from "react-resizable-panels";
import { languages, type ProblemView } from "../consts/consts";
import Tooltip from "./Tooltip";

type LanguageName = keyof typeof languages;
type ThemeMode = "light" | "dark";
interface JobResult {
	jobStatus: "RUNNING" | "COMPLETED" | "FAILED";
	stdout: string;
	stderr: string;
}

const getKeyboardShortcut = () => {
	const userAgent = navigator.userAgent.toLowerCase();
	const isMac = userAgent.includes("mac") || userAgent.includes("darwin");
	return isMac ? "⌘+↵" : "Ctrl+↵";
};

export const CodeIde = () => {
	const [language, setLanguage] = useState<LanguageName>("JAVASCRIPT");
	const [code, setCode] = useState(languages[language].boilerplate);
	const [problemId, setProblemId] = useState("two-sum");
	const [problem, setProblem] = useState<ProblemView>();
	const [themeMode, setThemeMode] = useState<ThemeMode>("dark");
	const [effectiveTheme, setEffectiveTheme] = useState(tokyoNightDay);
	const isDark = effectiveTheme === tokyoNight;
	const [isLoading, setIsLoading] = useState(false);
	const [jobResult, setJobResult] = useState<JobResult | null>(null);

	useEffect(() => {
		setEffectiveTheme(themeMode === "dark" ? tokyoNight : tokyoNightDay);
	}, [themeMode]);

	// useEffect(() => {
	// 	let index = 0;
	// 	const interval = setInterval(() => {
	// 		if (index < problemsIds.length) {
	// 			setProblemId(problemsIds[index]);
	// 			index++;
	// 		} else {
	// 			clearInterval(interval);
	// 		}
	// 	}, 1000);

	// 	return () => clearInterval(interval);
	// }, []);

	useEffect(() => {
		const fetchData = async () => {
			const url = `http://localhost:8080/api/problems/${problemId}`;
			const response = await fetch(url);
			const data: ProblemView = await response.json();
			setProblem(data);
			setCode(data.starterTemplates[language]);
		};
		fetchData();
	}, [problemId]);

	const onChange = useCallback((value: string) => {
		setCode(value);
	}, []);

	const handleLanguageChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
		if (!problem) return;
		const newLang = e.target.value as LanguageName;
		setLanguage(newLang);
		const template = problem.starterTemplates[newLang];
		if (template) {
			setCode(template);
		}
	};

	const pollJobStatus = useCallback((jobId: string) => {
		const poll = setInterval(async () => {
			console.log("polling status...");
			try {
				const res = await fetch(`http://localhost:8080/api/status/${jobId}`);
				const jobResult: JobResult = await res.json();
				if (
					jobResult.jobStatus === "COMPLETED" ||
					jobResult.jobStatus === "FAILED"
				) {
					clearInterval(poll);
					console.log(jobResult);
					setJobResult(jobResult);
					setIsLoading(false);
				}
			} catch (error) {
				clearInterval(poll);
				setIsLoading(false);
			}
		}, 1000);

		setTimeout(() => {
			clearInterval(poll);
			setIsLoading((loading) => {
				if (loading) return false;
				return loading;
			});
		}, 10500);
	}, []);

	const handleSubmit = useCallback(async () => {
		setIsLoading(true);
		setJobResult(null);
		try {
			const response = await fetch("http://localhost:8080/api/submit", {
				method: "POST",
				headers: {
					"Content-Type": "application/json",
				},
				body: JSON.stringify({ code, language, problemId }),
			});
			const result = await response.json();
			console.log("Response:", result);
			pollJobStatus(result.job_id);
		} catch (error) {
			setIsLoading(false);
		}
	}, [code, language, problemId, pollJobStatus]);

	useEffect(() => {
		const handleKeyDown = (e: KeyboardEvent) => {
			if ((e.metaKey || e.ctrlKey) && e.key === "Enter") {
				e.preventDefault();
				handleSubmit();
			}
		};

		window.addEventListener("keydown", handleKeyDown);
		return () => window.removeEventListener("keydown", handleKeyDown);
	}, [handleSubmit]);

	const toggleTheme = () => {
		setThemeMode(themeMode === "dark" ? "light" : "dark");
	};

	if (!problem) return <p>Loading...</p>;

	return (
		<div
			className={`h-full w-full ${
				isDark ? "bg-gray-900 text-gray-100" : "bg-white text-gray-900"
			}`}
		>
			<PanelGroup direction="horizontal">
				<Panel defaultSize={50} minSize={20}>
					<div className="flex flex-col h-full">
						<div className="flex-1 overflow-auto p-6">
							<div className="flex items-center gap-3 mb-4">
								<h1
									className={`text-2xl font-bold ${
										isDark ? "text-white" : "text-gray-900"
									}`}
								>
									{problem.title}
								</h1>
								<span className="px-2 py-1 bg-green-700 text-green-300 text-xs rounded">
									{/* change color for difficulties */}
									{problem.difficulty}
								</span>
							</div>

							<p
								className={`text-gray-300 mb-6 leading-relaxed ${
									isDark ? "text-gray-300" : "text-gray-700"
								}`}
							>
								{problem.description}
							</p>

							<div className="mb-6">
								<ul
									className={`space-y-1 ${
										isDark ? "text-gray-400" : "text-gray-600"
									}`}
								>
									{problem.assumptions.map((assumption, i) => (
										<li key={i} className="text-sm">
											{i + 1}. {assumption}
										</li>
									))}
								</ul>
							</div>

							<div className="space-y-4 mb-6">
								{problem.exampleTestCases?.map((example, i) => (
									<div
										key={i}
										className={`rounded p-4 
											${isDark ? "bg-gray-800" : "bg-gray-200"}`}
									>
										<div
											className={`font-semibold mb-2 ${
												isDark ? "text-white" : "text-gray-800"
											}`}
										>
											Example {i + 1}:
										</div>
										<div className="space-y-1 text-sm">
											<div>
												<span
													className={`${
														isDark ? "text-gray-400" : "text-gray-800"
													}`}
												>
													Input:
												</span>{" "}
												<code
													className={`${
														isDark ? "text-green-400" : "text-green-700"
													}`}
												>
													{example.input}
												</code>
											</div>
											<div>
												<span
													className={`${
														isDark ? "text-gray-400" : "text-gray-800"
													}`}
												>
													Output:
												</span>{" "}
												<code
													className={`${
														isDark ? "text-green-400" : "text-green-700"
													}`}
												>
													{example.expectedOutput}
												</code>
											</div>
										</div>
									</div>
								))}
							</div>

							{problem.constraints.length != 0 && (
								<div>
									<h3
										className={`font-semibold ${
											isDark ? "text-white" : "text-gray-900"
										} mb-2`}
									>
										Constraints:
									</h3>
									<ul
										className={`space-y-1 ${
											isDark ? "text-gray-400" : "text-gray-600"
										}`}
									>
										{problem.constraints.map((constraint, i) => (
											<li key={i} className="text-sm">
												• {constraint}
											</li>
										))}
									</ul>
								</div>
							)}
						</div>
						{jobResult && (
							<div
								className={`h-1/3 shrink-0 border-t ${
									isDark
										? "border-gray-700 bg-gray-900"
										: "border-gray-300 bg-gray-50"
								} overflow-auto p-4`}
							>
								<h3
									className={`font-bold mb-2 ${
										isDark ? "text-white" : "text-gray-900"
									}`}
								>
									Result:{" "}
									<span
										className={
											jobResult.jobStatus === "COMPLETED"
												? "text-green-500"
												: "text-red-500"
										}
									>
										{jobResult.jobStatus}
									</span>
								</h3>
								{jobResult.jobStatus === "COMPLETED" ? (
									<div className="text-white font-bold text-lg">
										PASSED ALL TEST CASES
									</div>
								) : (
									<>
										{jobResult.stdout && (
											<div className="mb-2">
												<div
													className={`text-xs font-semibold mb-1 ${
														isDark ? "text-gray-400" : "text-gray-600"
													}`}
												>
													Output:
												</div>
												<pre
													className={`p-2 rounded text-sm font-mono whitespace-pre-wrap ${
														isDark
															? "bg-gray-800 text-red-400"
															: "bg-gray-200 text-red-700"
													}`}
												>
													{jobResult.stdout}
												</pre>
											</div>
										)}
										{jobResult.stderr && (
											<div>
												<div
													className={`text-xs font-semibold mb-1 ${
														isDark ? "text-gray-400" : "text-gray-600"
													}`}
												>
													Error:
												</div>
												<pre
													className={`p-2 rounded text-sm font-mono whitespace-pre-wrap ${
														isDark
															? "bg-red-900/20 text-red-300"
															: "bg-red-100 text-red-800"
													}`}
												>
													{jobResult.stderr}
												</pre>
											</div>
										)}
									</>
								)}
							</div>
						)}
					</div>
				</Panel>

				<PanelResizeHandle
					className={`w-2 ${
						isDark
							? "bg-gray-700 hover:bg-blue-600"
							: "bg-gray-300 hover:bg-blue-400"
					} transition-colors`}
				/>

				<Panel defaultSize={50} minSize={30}>
					<div className="flex flex-col h-full">
						<div
							className={`shrink-0 p-2 ${
								isDark ? "bg-gray-800" : "bg-gray-100"
							} flex justify-between items-center`}
						>
							<select
								value={language}
								onChange={handleLanguageChange}
								className={`${
									isDark
										? "bg-gray-700 text-white border-gray-600"
										: "bg-gray-200 text-gray-900 border-gray-300"
								} rounded px-3 py-1`}
							>
								<option value="JAVASCRIPT">JavaScript</option>
								<option value="PYTHON">Python</option>
							</select>
							<button
								onClick={toggleTheme}
								className={`${
									isDark
										? "bg-gray-700 hover:bg-gray-600 text-white"
										: "bg-gray-200 hover:bg-gray-300 text-gray-900"
								} p-2 rounded-lg transition-colors`}
								title={`Switch to ${
									themeMode === "dark" ? "light" : "dark"
								} mode`}
							>
								{themeMode === "dark" ? <Sun size={20} /> : <Moon size={20} />}
							</button>
						</div>

						<div className="grow overflow-auto">
							{/* code editor */}
							<CodeMirror
								value={code}
								onChange={onChange}
								extensions={languages[language].extension}
								theme={effectiveTheme}
								height="100%"
								style={{ fontSize: "16px", height: "100%" }}
							/>
						</div>

						<div
							className={`shrink-0 flex justify-center p-3 ${
								isDark
									? "bg-gray-800 border-gray-700"
									: "bg-gray-100 border-gray-300"
							} border-t`}
						>
							<Tooltip
								content={`Run code (${getKeyboardShortcut()})`}
								isDark={isDark}
							>
								<button
									onClick={handleSubmit}
									disabled={isLoading}
									className={`bg-green-600 hover:bg-green-700 text-white font-medium py-2 px-6 rounded-lg transition-colors ${
										isLoading
											? "opacity-50 cursor-not-allowed"
											: "hover:cursor-pointer"
									}`}
								>
									{isLoading ? "Running..." : "Submit"}
								</button>
							</Tooltip>
						</div>
					</div>
				</Panel>
			</PanelGroup>
		</div>
	);
};
