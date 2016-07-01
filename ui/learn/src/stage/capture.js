var m = require('mithril');
var util = require('../util');
var assert = require('../assert');
var arrow = util.arrow;

var imgUrl = util.assetUrl + 'images/learn/bowman.svg';

module.exports = {
  key: 'capture',
  title: 'Capturing',
  subtitle: 'Take your enemy pieces.',
  image: imgUrl,
  intro: 'You are ready for combat! In this level, we will be capturing enemy pieces.',
  illustration: m('img.bg', {src: imgUrl}),
  levels: [{
    goal: 'Take black pieces!<br>And don\'t lose yours.',
    fen: '8/2p2p2/8/8/8/2R5/8/8 w - - 0 1',
    nbMoves: 2,
    shapes: [arrow('c3c7'), arrow('c7f7')],
    success: [assert.extinct('black')]
  }, {
    goal: 'Take black pieces!<br>And don\'t lose yours.',
    fen: '8/5r2/8/1r3p2/8/3B4/8/8 w - - 0 1',
    nbMoves: 5,
    success: [assert.extinct('black')]
  }, {
    goal: 'Grab all the stars!',
    fen: '8/8/8/8/3B4/8/8/8 w - - 0 1',
    apples: 'a1 b6 c1 e3 g7 h6',
    nbMoves: 6
  }, {
    goal: 'Grab all the stars!',
    fen: '8/8/8/8/2b5/8/8/8 b - - 0 1',
    apples: 'a4 a6 a8 b3 c2 d3 e2 f3',
    nbMoves: 8
  }, {
    goal: 'One light squares bishop,<br>one dark squares bishop.<br>You need both!',
    fen: '8/8/8/8/8/8/8/2b2b2 b - - 0 1',
    apples: 'c4 d3 d4 d5 e3 e4 e5 f4',
    nbMoves: 8
  }, {
    goal: 'One light squares bishop,<br>one dark squares bishop.<br>You need both!',
    fen: '8/3B4/8/8/8/2B5/8/8 w - - 0 1',
    apples: 'a5 b4 c2 c4 c7 e7 f5 f6 g8 h4 h7',
    nbMoves: 11
  }].map(util.toLevel),
  complete: 'Congratulations! You can command a bishop.'
};

