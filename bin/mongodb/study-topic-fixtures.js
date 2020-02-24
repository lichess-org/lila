const topics = [
  "Advanced Pawn",
  "Attraction",
  "Avoiding Perpetual",
  "Avoiding Stalemate",
  "Blocking",
  "Capturing Defender",
  "Clearance",
  "Coercion",
  "Counting",
  "Defensive Move",
  "Desperado",
  "Discovered Attack",
  "Distraction",
  "Double Check",
  "Exposed King",
  "Fork/Double Attack",
  "Hanging Piece",
  "Interference",
  "Mate - Anastasia's",
  "Mate - Arabian",
  "Mate - Back Rank Mate",
  "Mate - Balestra",
  "Mate - Blackburne's",
  "Mate - Boden's",
  "Mate - Damiano's",
  "Mate - Damiano's Bishop",
  "Mate - Double Bishop",
  "Mate - Dovetail",
  "Mate - Dovetail - Bishop",
  "Mate - Epaulette",
  "Mate - Escalator",
  "Mate - Greco's",
  "Mate - Hook",
  "Mate - Kill Box",
  "Mate - Lawnmower",
  "Mate - Lolli's",
  "Mate - Morphy's",
  "Mate - Opera",
  "Mate - Pawn",
  "Mate - Pillsbury's",
  "Mate - Railroad",
  "Mate - Smother",
  "Mate - Suffocation",
  "Mate - Swallow's Tail",
  "Mate - Triangle",
  "Mate - Vukovic",
  "Mate Threat",
  "Needs Different Opponent Move...",
  "Needs More Moves...",
  "Overloading",
  "Pin",
  "Quiet Move",
  "Sacrifice",
  "Simplification",
  "Skewer",
  "Trapped Piece",
  "Unpinning",
  "Unsound Sacrifice",
  "Weak Back Rank",
  "X-Ray Attack",
  "Zugzwang",
  "Zwischenzug"
];

db.study.find().forEach(s => {
  shuffleArray(topics);
  const t = topics.slice(0, Math.round(Math.random() * 20));
  db.study.update({_id:s._id},{$set:{topics: t}});
});

/* Randomize array element order in-place. Using Durstenfeld shuffle algorithm. */
function shuffleArray(array) {
  for (let i = array.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1));
    [array[i], array[j]] = [array[j], array[i]];
  }
  return array;
}
