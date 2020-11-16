var util = require("../util");
var arrow = util.arrow;
var circle = util.circle;

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
    },
    {
      goal: "rookPromotion",
      fen: "9/7R1/9/9/9/9/9/9/9 w -",
      apples: "h2 g1 g8 f7",
      nbMoves: 4,
    },
    {
      goal: "rookSummary",
      fen: "9/9/9/9/4R4/9/9/9/9 w -",
      apples: "e1",
      nbMoves: 1,
      shapes: [
        arrow("e5e9", "green"),
        arrow("e5i5", "green"),
        arrow("e5e1", "green"),
        arrow("e5a5", "green"),
      ],
    },
    {
      goal: "dragonSummary",
      fen: "9/9/9/9/4D4/9/9/9/9 w -",
      apples: "e8 d7",
      nbMoves: 2,
      shapes: [
        arrow("e5e9", "green"),
        arrow("e5i5", "green"),
        arrow("e5e1", "green"),
        arrow("e5a5", "green"),
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
  complete: "rookComplete",
};
