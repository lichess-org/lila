var util = require("../util");
var arrow = util.arrow;

module.exports = {
  key: "silver",
  title: "theSilver",
  subtitle: "itMovesEitherForwardOrDiagonallyBack",
  image: util.assetUrl + "images/learn/pieces/S.svg",
  intro: "queenIntro",
  illustration: util.pieceImg("silver"),
  levels: [
    {
      goal: "grabAllTheStars",
      fen: "9/9/9/9/9/9/4S4/9/9 w -",
      apples: "e5 b8",
      nbMoves: 2,
      shapes: [arrow("e2e5"), arrow("e5b8")],
    },
    {
      goal: "grabAllTheStars",
      fen: "9/9/9/9/9/4Q4/9/9/9 w -",
      apples: "a3 f2 f8 h3",
      nbMoves: 4,
    },
  ].map(util.toLevel),
  complete: "silverComplete",
};
