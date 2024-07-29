import React, { useState, useEffect } from "react";
import { Chessboard } from "react-chessboard";

interface PracticeProps {
  theme: string;
}

const PracticeDrill: React.FC<PracticeProps> = ({ theme }) => {
  const [fen, setFen] = useState<string>("");

  useEffect(() => {
    fetch(`/api/practice/${theme}`)
      .then((response) => response.json())
      .then((data) => setFen(data.fen));
  }, [theme]);

  return (
    <div>
      <h3>Practice Drill: {theme}</h3>
      <Chessboard position={fen} />
    </div>
  );
};

export default PracticeDrill;
