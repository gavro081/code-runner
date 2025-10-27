import { tokyoNight } from "@uiw/codemirror-theme-tokyo-night";
import { tokyoNightDay } from "@uiw/codemirror-theme-tokyo-night-day";
import CodeMirror from "@uiw/react-codemirror";
import { Moon, Sun } from "lucide-react";
import React, { useCallback, useEffect, useState } from "react";
import { Panel, PanelGroup, PanelResizeHandle } from "react-resizable-panels";
import { languages, problem } from "../consts/consts";
import Tooltip from "./Tooltip";

type LanguageName = keyof typeof languages;
type ThemeMode = "light" | "dark";

const getKeyboardShortcut = () => {
	const userAgent = navigator.userAgent.toLowerCase();
	const isMac = userAgent.includes("mac") || userAgent.includes("darwin");
	return isMac ? "⌘+↵" : "Ctrl+↵";
};

export const CodeIde = () => {
	const [lang, setLang] = useState<LanguageName>("javascript");
	const [code, setCode] = useState(languages[lang].boilerplate);
	const [themeMode, setThemeMode] = useState<ThemeMode>("dark");
	const [effectiveTheme, setEffectiveTheme] = useState(tokyoNightDay);

	useEffect(() => {
		setEffectiveTheme(themeMode === "dark" ? tokyoNight : tokyoNightDay);
	}, [themeMode]);

	const isDark = effectiveTheme === tokyoNight;

	useEffect(() => {
		const handleKeyDown = (e: KeyboardEvent) => {
			if ((e.metaKey || e.ctrlKey) && e.key === "Enter") {
				e.preventDefault();
				handleSubmit();
			}
		};

		window.addEventListener("keydown", handleKeyDown);
		return () => window.removeEventListener("keydown", handleKeyDown);
	}, []);

	const onChange = useCallback((value: string) => {
		setCode(value);
	}, []);

	const handleLanguageChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
		const newLang = e.target.value as LanguageName;
		setLang(newLang);
		setCode(languages[newLang].boilerplate);
	};

	const handleSubmit = () => {
		console.log("Submitting code:", code);
		alert("Code submitted! (Check console)");
	};

	const toggleTheme = () => {
		setThemeMode(themeMode === "dark" ? "light" : "dark");
	};

	return (
		<div
			className={`h-full w-full ${
				isDark ? "bg-gray-900 text-gray-100" : "bg-white text-gray-900"
			}`}
		>
			<PanelGroup direction="horizontal">
				<Panel defaultSize={50} minSize={20} className="overflow-auto">
					<div className="p-6">
						<div className="flex items-center gap-3 mb-4">
							<h1
								className={`text-2xl font-bold ${
									isDark ? "text-white" : "text-gray-900"
								}`}
							>
								{problem.title}
							</h1>
							{/* <span className="px-2 py-1 bg-green-900 text-green-300 text-xs rounded">
								{problem.difficulty}
							</span> */}
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
							{problem.examples.map((example, i) => (
								<div
									key={i}
									className={`bg-gray-800 rounded p-4 ${
										isDark ? "bg-gray-800" : "bg-gray-100"
									}`}
								>
									<div
										className={`font-semibold text-white mb-2 ${
											isDark ? "text-white" : "text-gray-900"
										}`}
									>
										Example {i + 1}:
									</div>
									<div className="space-y-1 text-sm">
										<div>
											<span
												className={`${
													isDark ? "text-gray-400" : "text-gray-600"
												}`}
											>
												Input:
											</span>{" "}
											<code className="text-green-400">{example.input}</code>
										</div>
										<div>
											<span
												className={`${
													isDark ? "text-gray-400" : "text-gray-600"
												}`}
											>
												Output:
											</span>{" "}
											<code className="text-green-400">{example.output}</code>
										</div>
										{example.explanation && (
											<div
												className={`${
													isDark ? "text-gray-400" : "text-gray-600"
												} mt-2`}
											>
												<span
													className={`font-medium ${
														isDark ? "text-gray-300" : "text-gray-700"
													}`}
												>
													Explanation:
												</span>{" "}
												{example.explanation}
											</div>
										)}
									</div>
								</div>
							))}
						</div>

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
								value={lang}
								onChange={handleLanguageChange}
								className={`${
									isDark
										? "bg-gray-700 text-white border-gray-600"
										: "bg-gray-200 text-gray-900 border-gray-300"
								} rounded px-3 py-1`}
							>
								<option value="javascript">JavaScript</option>
								<option value="python">Python</option>
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
								extensions={languages[lang].extension}
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
									className="bg-green-600 hover:bg-green-700 text-white font-medium py-2 px-6 rounded-lg transition-colors hover:cursor-pointer"
								>
									Submit
								</button>
							</Tooltip>
						</div>
					</div>
				</Panel>
			</PanelGroup>
		</div>
	);
};
