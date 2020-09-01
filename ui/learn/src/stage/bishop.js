var util = require("../util");
var arrow = util.arrow;

module.exports = {
  key: "bishop",
  title: "theBishop",
  subtitle: "itMovesDiagonally",
  image: util.assetUrl + "images/learn/pieces/B.svg",
  intro: "bishopIntro",
  illustration: util.pieceImg("bishop"),
  levels: [
    {
      goal: "grabAllTheStars",
      fen: "9/9/9/9/9/9/6B2/9/9 w -",
      apples: "d6 b4",
      nbMoves: 2,
      shapes: [arrow("g3d6"), arrow("d6b4")],
    },
    {
      goal: "theFewerMoves",
      fen: "9/9/9/9/9/9/2B5/9/9 w -",
      apples: "b2 c1 c5 e1 e3 f2",
      nbMoves: 6,
    },
    {
      goal: "grabAllTheStars",
      fen: "9/9/9/9/9/3B4/9/9/9 w -",
      apples: "a1 b6 c1 e3 f6 g5",
      nbMoves: 6,
    },
    {
      goal: "grabAllTheStars",
      fen: "9/9/9/9/9/2B5/9/9/9 w -",
      apples: "a4 b1 b3 c2 d3 e2",
      nbMoves: 6,
    },
    {
      goal: "youNeedBothBishops",
      fen: "9/9/9/9/9/9/9/9/2B2B2 w - -",
      apples: "d3 d4 d5 e3 e4 e5",
      nbMoves: 6,
    },
    {
      goal: "grabAllTheStars",
      fen: "9/4B4/9/9/9/9/2B6/9/9 w -",
      apples: "a3 c2 f8 g6 g7 h9 i5 i8",
      nbMoves: 11,
    },
  ].map(util.toLevel),
  complete: "bishopComplete",
};
