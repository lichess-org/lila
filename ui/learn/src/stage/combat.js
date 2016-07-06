var m = require('mithril');
var util = require('../util');
var assert = require('../assert');
var arrow = util.arrow;

var imgUrl = util.assetUrl + 'images/learn/battle-gear.svg';

module.exports = {
  key: 'combat',
  title: 'Combat',
  subtitle: 'Attack and defend pieces',
  image: imgUrl,
  intro: 'TODO',
  illustration: util.roundSvg(imgUrl),
  levels: [{ // rook
    goal: 'Take the black pieces!<br>And don\'t lose yours.',
    fen: '8/2p2p2/8/8/8/2R5/8/8 w - -',
    nbMoves: 2,
    captures: 2,
    shapes: [arrow('c3c7'), arrow('c7f7')],
    success: assert.extinct('black')
  }].map(function(l, i) {
    l.pointsForCapture = true;
    return util.toLevel(l, i);
  }),
  complete: 'Congratulations! You know how to fight with chess pieces!'
};
