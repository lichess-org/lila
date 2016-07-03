var m = require('mithril');
var util = require('../util');
var assert = require('../assert');
var arrow = util.arrow;

var imgUrl = util.assetUrl + 'images/learn/rally-the-troops.svg';

module.exports = {
  key: 'setup',
  title: 'Board setup',
  subtitle: 'How the game starts',
  image: imgUrl,
  intro: 'The two armies face each other, ready for the battle.',
  illustration: m('img.bg', {
    src: imgUrl
  }),
  levels: [{ // rook
    goal: 'TODO',
    fen: 'rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w - - 0 1'
  }].map(function(l, i) {
    return util.toLevel(l, i);
  }),
  complete: 'Congratulations! You checked your opponent, forcing them to defend their king!'
};
;
