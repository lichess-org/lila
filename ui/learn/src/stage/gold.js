var util = require("../util");
var arrow = util.arrow;
var circle = util.circle;

module.exports = {
  key: "gold",
  title: "theGold",
  subtitle: "itMovesInAnyDirectionExceptDiagonallyBack",
  image: util.assetUrl + "images/learn/pieces/G.svg",
  intro: "goldIntro",
  illustration: util.pieceImg("gold"),
  levels: [
    {
      goal: "grabAllTheStars",
      fen: "9/9/9/9/9/4G4/9/9/9 w -",
      apples: "f6",
      nbMoves: 4,
      shapes: [arrow("e4e5"), arrow("e5f6")],
    },
    {
      goal: "theFewerMoves",
      fen: "9/9/9/9/1G7/9/9/9/9 w -",
      apples: "b3 b4 c3 d4",
      nbMoves: 5,
    },
    {
      goal: "theFewerMoves",
      fen: "9/9/9/9/9/9/9/9/3G5 w -",
      apples: "d2 c3 b4 b5 c6 d7 d6 e6",
      nbMoves: 8,
    },
    {
      goal: "goldDoesntPromote",
      fen: "9/9/9/6G2/9/9/9/9/9 w -",
      apples: "h7 g8",
      nbMoves: 2,
    },
    {
      goal: "goldSummary",
      fen: "9/9/9/9/9/4G4/9/9/9 w -",
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
  complete: "goldComplete",
};
