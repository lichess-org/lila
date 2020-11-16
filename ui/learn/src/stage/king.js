var util = require("../util");
var arrow = util.arrow;
var circle = util.circle;

module.exports = {
  key: "king",
  title: "theKing",
  subtitle: "theMostImportantPiece",
  image: util.assetUrl + "images/learn/pieces/K.svg",
  intro: "kingIntro",
  illustration: util.pieceImg("king"),
  levels: [
    {
      goal: "theKingIsSlow",
      fen: "9/9/9/9/9/9/3K5/9/9 w -",
      apples: "e6",
      nbMoves: 3,
      shapes: [arrow("d3d4"), arrow("d4d5"), arrow("d5e6")],
    },
    {
      goal: "grabAllTheStars",
      fen: "9/9/9/5K3/9/9/9/9/9 w -",
      apples: "d7 e8 f7 f8",
      nbMoves: 4,
    },
    {
      goal: "kingSummary",
      fen: "9/9/9/9/4K4/9/9/9/9 w -",
      apples: "e7",
      nbMoves: 2,
      shapes: [
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
  ].map(function (s, i) {
    s = util.toLevel(s, i);
    s.emptyApples = true;
    return s;
  }),
  complete: "kingComplete",
};
