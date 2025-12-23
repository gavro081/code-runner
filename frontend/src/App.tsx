import { BrowserRouter, Route, Routes } from "react-router-dom";
import { CodeIde } from "./components/CodeIde";
import { ProblemsList } from "./components/ProblemsList";

function App() {
	return (
		<BrowserRouter>
			<Routes>
				<Route path="/" element={<ProblemsList />} />
				<Route path="/problem/:problemId" element={<CodeIde />} />
			</Routes>
		</BrowserRouter>
	);
}

export default App;
