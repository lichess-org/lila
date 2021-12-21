var util = require('../util');
var assert = require('../assert');
var arrow = util.arrow,
  circle = util.circle;

var imgUrl = util.assetUrl + 'images/learn/guillotine.svg';

module.exports = {
  key: 'checkmate1',
  title: 'mateInOne',
  subtitle: 'defeatTheOpponentsKing',
  image: imgUrl,
  intro: 'mateInOneIntro',
  illustration: util.roundSvg(imgUrl),
  levels: [
    {
      goal: 'toWinInShogi',
      fen: '6k2/9/6P2/9/9/9/9/9/9 b G2r2b3g4s4n4l17p 1',
    },
    {
      goal: 'dropsCommonDeliverMate',
      fen: '7k1/9/7S1/9/9/9/9/9/9 b S2r2b4g2s4n4l18p 1',
    },
    {
      goal: 'attackYourOpponentsKing',
      fen: '7nk/7bl/9/9/6N2/9/9/9/9 b 2rb4g4s2n3l18p 1',
    },
    {
      goal: 'attackYourOpponentsKing',
      fen: '4R3G/7k1/6ppp/9/9/9/9/9/9 b r2b3g4s4n4l15p 1',
    },
    {
      goal: 'attackYourOpponentsKing',
      fen: '7kl/7s1/6PSp/9/9/9/9/9/9 b 2r2b4g2s4n3l16p 1',
    },
    {
      goal: 'attackYourOpponentsKing',
      fen: '9/5n2+B/5pk2/9/7+R1/9/9/9/9 b rb4g4s3n4l17p 1',
    },
    {
      goal: 'attackYourOpponentsKing',
      fen: '5k3/6s2/5NB2/9/9/9/9/9/9 b 2rb4g3s3n4l18p 2',
    },
    {
      goal: 'attackYourOpponentsKing',
      fen: '4R3l/6g1k/5gsp1/6p1P/9/9/9/9/8L b r2b2g3s4n2l15p 1',
    },
    {
      goal: 'chooseYourPieceCarefully',
      fen: '8l/6S1k/9/9/8g/9/9/9/9 b GS2r2b2g2s4n3l18p 1',
    },
    {
      goal: 'chooseYourPieceCarefully',
      fen: '8l/7k1/7pb/7N1/9/9/9/9/9 b RGrb3g4s3n3l17p 1',
    },
    {
      goal: 'chooseYourPieceCarefully',
      fen: '8l/6+Rgk/7pp/9/9/9/9/9/9 b GSr2b2g3s4n3l16p 1',
    },
    {
      // pawn
      goal: 'mateWithADroppedPawnIs',
      fen: '7+P1/8g/7pk/9/7Sp/9/9/9/9 b LP2r2b3g3s4n3l14p 1',
      scenario: [
        {
          levelFail: 'fail',
          move: 'p*i6',
          shapes: [circle('i6', 'red')],
        },
      ],
    },
    {
      // pawn 2
      goal: 'mateWithAPushedPawnIs',
      fen: '7+P1/8g/7pk/9/7SP/9/9/9/9 b 2r2b3g3s4n4l15p 1',
    },
    {
      goal: 'attackYourOpponentsKing',
      fen: '8l/5BSk1/6p1P/9/9/9/9/9/9 b 2rb4g3s4n3l16p 1',
      scenario: [
        {
          move: 'f8g9+',
          shapes: [arrow('g9i7', 'yellow')],
        },
      ],
    },
    {
      goal: 'attackYourOpponentsKing',
      fen: '5lk2/8R/5Ps2/6N2/6L2/9/9/9/9 b r2b4g3s3n2l17p 1',
    },
    {
      goal: 'attackYourOpponentsKing',
      fen: '2R3gkl/3R3s1/7p1/7Np/9/9/9/9/9 b 2b3g3s3n3l16p 1',
    },
  ].map(function (l, i) {
    l.nbMoves = 1;
    l.failure = assert.not(assert.mate);
    l.success = assert.mate;
    l.showFailureFollowUp = true;
    return util.toLevel(l, i);
  }),
  complete: 'mateInOneComplete',
};
