var m = require('mithril');
var util = require('../util');
var assert = require('../assert');
var arrow = util.arrow;

var imgUrl = util.assetUrl + 'images/learn/guillotine.svg';

module.exports = {
  key: 'checkmate',
  title: 'Checkmate',
  subtitle: 'Defeat the opponent king.',
  image: imgUrl,
  intro: 'You win when your opponent cannot defend a check.',
  illustration: m('img.bg', {
    src: imgUrl
  }),
  levels: [{ // rook
    goal: 'Take black pieces!<br>And don\'t lose yours.',
    fen: '8/2p2p2/8/8/8/2R5/8/8 w - - 0 1',
    nbMoves: 2,
    captures: 2,
    shapes: [arrow('c3c7'), arrow('c7f7')],
    success: [assert.extinct('black')]
  }].map(util.toLevel),
  complete: 'Congratulations! TODO'
};
