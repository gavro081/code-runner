import { useState } from "react";

const Tooltip: React.FC<{
	children: React.ReactNode;
	content: string;
	className?: string;
	isDark?: boolean;
}> = ({ children, content, className = "", isDark = true }) => {
	const [isVisible, setIsVisible] = useState(false);

	return (
		<div
			className={`relative inline-block ${className}`}
			onMouseEnter={() => setIsVisible(true)}
			onMouseLeave={() => setIsVisible(false)}
		>
			{children}
			{isVisible && (
				<div className="absolute bottom-full mb-2 left-1/2 transform -translate-x-1/2 z-50">
					<div
						className={`${
							isDark
								? "bg-gray-800 text-white border-gray-600"
								: "bg-gray-200 text-gray-900 border-gray-300"
						} text-sm px-3 py-2 rounded-lg shadow-lg border whitespace-nowrap`}
					>
						{content}
						<div
							className={`absolute top-full left-1/2 transform -translate-x-1/2 w-0 h-0 border-l-4 border-r-4 border-t-4 border-transparent ${
								isDark ? "border-t-gray-800" : "border-t-gray-200"
							}`}
						></div>
					</div>
				</div>
			)}
		</div>
	);
};

export default Tooltip;
