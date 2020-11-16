var util = require("../util");
var assert = require("../assert");
var arrow = util.arrow;
var circle = util.circle;

module.exports = {
  key: "pawn",
  title: "thePawn",
  subtitle: "itMovesForwardOnly",
  image: util.assetUrl + "images/learn/pieces/P.svg",
  intro: "pawnIntro",
  illustration: util.pieceImg("pawn"),
  levels: [
    {
      goal: "pawnsMoveOneSquareOnly",
      fen: "9/9/9/9/9/9/2P6/9/9 w -",
      apples: "c6",
      nbMoves: 3,
      shapes: [arrow("c3c4"), arrow("c4c5"), arrow("c5c6")],
    },
    {
      goal: "pawnPromotion",
      fen: "9/9/9/9/9/9/6P2/9/9 w -",
      apples: "h7",
      nbMoves: 5,
    },
    {
      goal: "pawnSummary",
      fen: "9/9/9/9/9/4P4/9/9/9 w -",
      apples: "e6",
      nbMoves: 2,
      shapes: [circle("e5")],
    },
    {
      goal: "tokinSummary",
      fen: "9/9/9/9/9/4T4/9/9/9 w -",
      apples: "e6",
      nbMoves: 2,
      shapes: [
        circle("d4"),
        circle("d5"),
        circle("e5"),
        circle("f5"),
        circle("f4"),
        circle("e3"),
      ],
    },
  ].map(util.toLevel),
  complete: "pawnComplete",
};
