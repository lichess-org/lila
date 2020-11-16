var util = require("../util");
var arrow = util.arrow;
var circle = util.circle;

module.exports = {
  key: "silver",
  title: "theSilver",
  subtitle: "itMovesEitherForwardOrDiagonallyBack",
  image: util.assetUrl + "images/learn/pieces/S.svg",
  intro: "silverIntro",
  illustration: util.pieceImg("silver"),
  levels: [
    {
      goal: "grabAllTheStars",
      fen: "9/9/9/9/9/4S4/9/9/9 w -",
      apples: "e5 d6 b4",
      nbMoves: 4,
      shapes: [arrow("e4e5"), arrow("e5d6"), arrow("d6c5"), arrow("c5b4")],
    },
    {
      goal: "grabAllTheStars",
      fen: "9/9/9/4S4/9/9/9/9/9 w -",
      apples: "f5 e4 d3 e2 f1",
      nbMoves: 5,
    },
    {
      goal: "theFewerMoves",
      fen: "9/9/9/9/3S5/6S2/9/9/9 w -",
      apples: "d4 d3 e4 g5 h4",
      nbMoves: 5,
    },
    {
      goal: "silverPromotion",
      fen: "9/9/9/6S2/9/9/9/9/9 w -",
      apples: "e6 f6 f7",
      nbMoves: 3,
    },
    {
      goal: "silverSummary",
      fen: "9/9/9/9/9/4S4/9/9/9 w -",
      apples: "e6",
      nbMoves: 2,
      shapes: [
        circle("e5"),
        circle("f5"),
        circle("d5"),
        circle("d3"),
        circle("f3"),
      ],
    },
    {
      goal: "psilverSummary",
      fen: "9/9/9/9/9/4A4/9/9/9 w -",
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
  complete: "silverComplete",
};
