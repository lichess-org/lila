var partial = require('lodash-node/modern/functions/partial');
var reduce = require('lodash-node/modern/collections/reduce');
var chessground = require('chessground');
var data = require('./data');
var chess = require('./chess');
var puzzle = require('./puzzle');

module.exports = function(cfg, router, i18n) {

  console.log(cfg);

  this.data = data(cfg);

  this.chessground = new chessground.controller({
    fen: cfg.puzzle.fen,
    orientation: cfg.puzzle.color,
    movable: {
      free: false,
      color: cfg.mode !== 'view' ? cfg.puzzle.color : null,
      events: {
        after: this.userMove
      },
      animation: {
        enabled: true,
        duration: 200
      },
      premovable: {
        enabled: true
      }
    }
  });

  this.playOpponentMove = function(move) {
    chess.move(this.data.chess, move);
    this.chessground.reconfigure({
      fen: this.data.chess.fen(),
      lastMove: move,
      movable: {
        dests: chess.dests(this.data.chess)
      },
      turnColor: this.data.puzzle.color
    });
    this.chessground.playPremove();
  }.bind(this);

  this.playInitialMove = function(data) {
    this.playOpponentMove(this.data.puzzle.initialMove);
    this.data.startedAt = new Date();
  }.bind(this);

  this.initiate = function() {
    if (this.data.mode == 'view') throw 'ahem';
    else setTimeout(this.playInitialMove, 1000);
  }.bind(this);

  this.giveUp = function() {}.bind(this);

  this.router = router;

  this.trans = function() {
    var key = arguments[0];
    var args = Array.prototype.slice.call(arguments, 1);
    return reduce(args, function(str, arg) {
      return str.replace('%s', arg);
    }, i18n[key]);
  };
};
