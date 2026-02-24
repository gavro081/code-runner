import { BrowserRouter, Route, Routes } from "react-router-dom";
import { AddProblem } from "./components/AddProblem";
import { CodeIde } from "./components/CodeIde";
import { ProblemsList } from "./components/ProblemsList";

function App() {
	return (
		<BrowserRouter>
			<Routes>
				<Route path="/" element={<ProblemsList />} />
				<Route path="/problem/:problemId" element={<CodeIde />} />
				<Route path="/add-problem" element={<AddProblem />} />
			</Routes>
		</BrowserRouter>
	);
}

export default App;
