import { Moon, Sun } from "lucide-react";
import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import type { Problem } from "../utils/consts";
import { getDifficultyColor } from "../utils/helpers";

type ThemeMode = "light" | "dark";

export const ProblemsList = () => {
	const [problems, setProblems] = useState<Problem[]>([]);
	const [isLoading, setIsLoading] = useState(true);
	const [themeMode, setThemeMode] = useState<ThemeMode>("dark");
	const isDark = themeMode === "dark";
	const navigate = useNavigate();

	useEffect(() => {
		const fetchProblems = async () => {
			try {
				const response = await fetch("http://localhost:8080/api/problems");
				const data: Problem[] = await response.json();
				setProblems(data);
			} catch (error) {
				console.error("Failed to fetch problems:", error);
			} finally {
				setIsLoading(false);
			}
		};

		fetchProblems();
	}, []);

	const handleProblemClick = (problemId: string) => {
		navigate(`/problem/${problemId}`);
	};

	const toggleTheme = () => {
		setThemeMode(themeMode === "dark" ? "light" : "dark");
	};

	if (isLoading) {
		return (
			<div
				className={`min-h-screen flex items-center justify-center ${
					isDark ? "bg-gray-900 text-white" : "bg-white text-gray-900"
				}`}
			>
				<p className="text-xl">Loading problems...</p>
			</div>
		);
	}

	return (
		<div
			className={`min-h-screen ${
				isDark ? "bg-gray-900 text-gray-100" : "bg-white text-gray-900"
			}`}
		>
			<div className="max-w-5xl mx-auto p-6">
				<div className="flex justify-between items-center mb-8">
					<div>
						<h1
							className={`text-4xl font-bold ${
								isDark ? "text-white" : "text-gray-900"
							}`}
						>
							Code Runner
						</h1>
						<p className={`mt-2 ${isDark ? "text-gray-400" : "text-gray-600"}`}>
							Select a problem to start coding
						</p>
					</div>
					<button
						onClick={toggleTheme}
						className={`${
							isDark
								? "bg-gray-700 hover:bg-gray-600 text-white"
								: "bg-gray-200 hover:bg-gray-300 text-gray-900"
						} p-2 rounded-lg transition-colors cursor-pointer`}
						title={`Switch to ${themeMode === "dark" ? "light" : "dark"} mode`}
					>
						{themeMode === "dark" ? <Sun size={20} /> : <Moon size={20} />}
					</button>
				</div>

				{problems.length === 0 ? (
					<p
						className={`text-center ${
							isDark ? "text-gray-400" : "text-gray-600"
						}`}
					>
						No problems available
					</p>
				) : (
					<div className="space-y-3">
						{problems.map((problem) => (
							<button
								key={problem.id}
								onClick={() => handleProblemClick(problem.id)}
								className={`w-full text-left p-4 rounded-lg transition-all cursor-pointer ${
									isDark
										? "bg-gray-800 hover:bg-gray-700 border border-gray-700 hover:border-blue-600"
										: "bg-gray-50 hover:bg-gray-100 border border-gray-200 hover:border-blue-500"
								}`}
							>
								<div className="flex items-center justify-between">
									<h2
										className={`text-lg font-semibold ${
											isDark ? "text-white" : "text-gray-900"
										}`}
									>
										{problem.title}
									</h2>
									<span
										className={`px-3 py-1 text-xs rounded font-medium ${getDifficultyColor(
											problem.difficulty,
											isDark
										)}`}
									>
										{problem.difficulty}
									</span>
								</div>
							</button>
						))}
					</div>
				)}
			</div>
		</div>
	);
};
