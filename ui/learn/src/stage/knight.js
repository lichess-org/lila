var util = require("../util");
var arrow = util.arrow;

module.exports = {
  key: "knight",
  title: "theKnight",
  subtitle: "itMovesInAnLShape",
  image: util.assetUrl + "images/learn/pieces/N.svg",
  intro: "knightIntro",
  illustration: util.pieceImg("knight"),
  levels: [
    {
      goal: "knightsHaveAFancyWay",
      fen: "9/9/9/9/9/9/3N5/9/9 w -",
      apples: "c5 d7",
      nbMoves: 2,
      shapes: [arrow("d3c5"), arrow("c5d7")],
    },
    {
      goal: "grabAllTheStars",
      fen: "9/9/9/9/9/9/9/1N7/9 w -",
      apples: "c3 d4 e2 f3 f7 g5 h8",
      nbMoves: 8,
    },
    {
      goal: "grabAllTheStars",
      fen: "9/2N6/9/9/9/9/9/9/9 w -",
      apples: "b6 d5 d7 e6 f4",
      nbMoves: 5,
    },
  ].map(util.toLevel),
  complete: "knightComplete",
};
