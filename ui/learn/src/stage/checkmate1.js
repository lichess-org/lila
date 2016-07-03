var m = require('mithril');
var util = require('../util');
var assert = require('../assert');
var arrow = util.arrow;

var imgUrl = util.assetUrl + 'images/learn/guillotine.svg';

var attack = 'Attack your opponent king<br>in a way that cannot be defended!';

module.exports = {
  key: 'checkmate1',
  title: 'Mate in one',
  subtitle: 'Defeat the opponent king',
  image: imgUrl,
  intro: 'You win when your opponent cannot defend a check.',
  illustration: m('img.bg', {
    src: imgUrl
  }),
  levels: [{ // rook
    goal: attack,
    fen: '3qk3/3ppp2/8/8/2B5/5Q2/8/8 w - - 0 1',
    shapes: [arrow('f3f7')]
  }, { // smoothered
    goal: attack,
    fen: '6rk/6pp/7P/6N1/8/8/8/8 w - - 0 1',
  }, { // rook
    goal: attack,
    fen: 'R7/8/7k/2r5/5n2/8/6Q1/8 w - - 0 1',
  }, { // Q+N
    goal: attack,
    fen: '2rb4/2k5/5N2/1Q6/8/8/8/8 w - - 0 1',
  }, { // discovered
    goal: attack,
    fen: '1r2kb2/ppB1p3/2P2p2/2p1N3/B7/8/8/3R4 w - - 0 1',
  }, { // tricky
    goal: attack,
    fen: '8/pk1N4/n7/b7/6B1/1r3b2/8/1RR5 w - - 0 1',
  }, { // tricky
    goal: attack,
    fen: 'r1b5/ppp5/2N2kpN/5q2/8/Q7/8/4B3 w - - 0 1',
  }].map(function(l, i) {
    l.nbMoves = 1;
    l.failure = [assert.not(assert.mate)];
    l.success = [assert.mate];
    l.showFailureFollowUp = true;
    return util.toLevel(l, i);
  }),
  complete: 'Congratulations! TODO'
};
