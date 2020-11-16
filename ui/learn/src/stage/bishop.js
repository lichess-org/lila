var util = require("../util");
var arrow = util.arrow;
var circle = util.circle;

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
      goal: "bishopPromotion",
      fen: "9/9/9/9/9/9/2B6/9/9 w -",
      apples: "h8 g8 b3",
      nbMoves: 3,
    },
    {
      goal: "bishopSummary",
      fen: "9/9/9/9/4B4/9/9/9/9 w -",
      apples: "a1",
      nbMoves: 1,
      shapes: [
        arrow("e5i9", "green"),
        arrow("e5i1", "green"),
        arrow("e5a1", "green"),
        arrow("e5a9", "green"),
      ],
    },
    {
      goal: "horseSummary",
      fen: "9/9/9/9/4H4/9/9/9/9 w -",
      apples: "b8 b7",
      nbMoves: 2,
      shapes: [
        arrow("e5i9"),
        arrow("e5i1"),
        arrow("e5a1"),
        arrow("e5a9"),
        circle("e6"),
        circle("f6"),
        circle("f5"),
        circle("f4"),
        circle("e4"),
        circle("d4"),
        circle("d5"),
        circle("d6"),
      ],
    },
  ].map(util.toLevel),
  complete: "bishopComplete",
};
