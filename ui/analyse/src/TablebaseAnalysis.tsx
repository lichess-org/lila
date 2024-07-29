import React, { useState, useEffect } from "react";

interface AnalysisProps {
  fen: string;
}

const TablebaseAnalysis: React.FC<AnalysisProps> = ({ fen }) => {
  const [analysis, setAnalysis] = useState<AnalysisResult | null>(null);

  useEffect(() => {
    fetch(`/api/analyze/tablebase?fen=${encodeURIComponent(fen)}`)
      .then((response) => response.json())
      .then((data) => setAnalysis(data));
  }, [fen]);

  if (!analysis) {
    return <div>Loading...</div>;
  }

  return (
    <div>
      <h3>Tablebase Analysis</h3>
      <p>Best Move: {analysis.bestMove}</p>
      <p>Evaluation: {analysis.evaluation}</p>
      {analysis.dtz !== undefined && <p>Distance to Zeroing: {analysis.dtz}</p>}
    </div>
  );
};

export default TablebaseAnalysis;

interface AnalysisResult {
  bestMove: string;
  evaluation: number;
  dtz?: number;
}
