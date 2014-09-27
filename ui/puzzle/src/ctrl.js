var partial = require('lodash-node/modern/functions/partial');
var reduce = require('lodash-node/modern/collections/reduce');
var chessground = require('chessground');
var data = require('./data');
var chess = require('./chess');
var puzzle = require('./puzzle');

module.exports = function(cfg, router, i18n) {

  this.data = data(cfg);

  this.userMove = function(orig, dest) {
    var res = puzzle.tryMove(this.data, [orig, dest]);
    var newProgress = res[0];
    var newLines = res[1];
    switch (newLines) {
      case 'retry':
        setTimeout(this.revert, 500);
        this.data.comment = 'retry';
        break;
      case 'fail':
        if (this.data.mode == 'play') throw 'hum'; //xhr.attempt(this.data, false);
        else setTimeout(this.revert, 500);
        this.data.comment = 'fail';
        break;
      case 'win':
        this.userFinalizeMove([orig, dest], newProgress);
        // xhr.attempt(this.data, true);
        this.data.comment = 'retry';
        break;
      default:
        this.userFinalizeMove([orig, dest], newProgress);
        setTimeout(this.playOpponentNextMove, 1000);
    }
  }.bind(this);

  this.userFinalizeMove = function(move, newProgress) {
    chess.move(this.data.chess, move);
    this.data.comment = 'great';
    this.data.progress = newProgress;
    this.chessground.reconfigure({
      fen: this.data.chess.fen(),
      lastMove: move,
      turnColor: this.data.puzzle.opponentColor
    });
  }.bind(this);

  this.chessground = new chessground.controller({
    fen: cfg.puzzle.fen,
    orientation: this.data.puzzle.color,
    turnColor: this.data.puzzle.opponentColor,
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

  this.playOpponentNextMove = function() {
    var move = puzzle.getOpponentNextMove(this.data);
    this.playOpponentMove(puzzle.str2move(move));
    this.data.progress.push(move);
    var newLines = puzzle.getCurrentLines(this.data);
    if (newLines == 'win') throw '(xhr/attempt new-state true))';
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
