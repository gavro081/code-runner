export const getDifficultyColor = (difficulty: string, isDark: boolean) => {
	switch (difficulty) {
		case "EASY":
			return isDark
				? "bg-green-700 text-green-300"
				: "bg-green-200 text-green-800";
		case "MEDIUM":
			return isDark
				? "bg-yellow-700 text-yellow-300"
				: "bg-yellow-200 text-yellow-800";
		case "HARD":
			return isDark ? "bg-red-700 text-red-300" : "bg-red-200 text-red-800";
		default:
			return isDark ? "bg-gray-700 text-gray-300" : "bg-gray-200 text-gray-800";
	}
};
