var util = require("../util");
var arrow = util.arrow;

module.exports = {
  key: "rook",
  title: "theRook",
  subtitle: "itMovesInStraightLines",
  image: util.assetUrl + "images/learn/pieces/R.svg",
  intro: "rookIntro",
  illustration: util.pieceImg("rook"),
  levels: [
    {
      goal: "rookGoal",
      fen: "9/9/9/9/9/9/9/4R4/9 w -",
      apples: "e6",
      nbMoves: 1,
      shapes: [arrow("e2e6")],
    },
    {
      goal: "grabAllTheStars",
      fen: "9/9/9/2R5/9/9/9/9/9 w -",
      apples: "c3 g3",
      nbMoves: 2,
      shapes: [arrow("c6c3"), arrow("c3g3")],
    },
    {
      goal: "useTwoRooks",
      fen: "9/9/9/1R7/9/3R5/9/9/9 w -",
      apples: "a4 g3 g6 h4",
      nbMoves: 4,
    },
    {
      goal: "theFewerMoves",
      fen: "9/9/9/9/9/9/9/9/8R w -",
      apples: "g9 h1 h8 h9 i8",
      nbMoves: 5,
      explainPromotion: true,
    },
    {
      goal: "theFewerMoves",
      fen: "9/7R1/9/9/9/9/9/9/9 w -",
      apples: "h2 g1 g8 f7",
      nbMoves: 4,
    },
  ].map(util.toLevel),
  complete: "rookComplete",
};
