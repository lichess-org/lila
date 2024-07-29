import React from "react";
import TablebaseAnalysis from "./TablebaseAnalysis";
import PracticeDrill from "./PracticeDrill";

const AnalysisPage: React.FC = () => {
  const [fen, setFen] = useState<string>("start_fen_here");
  const [theme, setTheme] = useState<string>("basicKingAndPawn");

  return (
    <div>
      <h2>Game Analysis</h2>
      <TablebaseAnalysis fen={fen} />
      <h2>Practice Drill</h2>
      <PracticeDrill theme={theme} />
    </div>
  );
};

export default AnalysisPage;
