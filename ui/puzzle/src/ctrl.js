var m = require('mithril');
var partial = require('lodash-node/modern/functions/partial');
var last = require('lodash-node/modern/arrays/last');
var chessground = require('chessground');
var data = require('./data');
var chess = require('./chess');
var puzzle = require('./puzzle');
var xhr = require('./xhr');

module.exports = function(cfg, router, i18n) {

  this.data = data(cfg);

  this.userMove = function(orig, dest) {
    var res = puzzle.tryMove(this.data, [orig, dest]);
    var newProgress = res[0];
    var newLines = res[1];
    var lastMove = last(newProgress);
    var promotion = lastMove ? lastMove[4] : undefined;
    m.startComputation();
    switch (newLines) {
      case 'retry':
        setTimeout(partial(this.revert, this.data.puzzle.id), 500);
        this.data.comment = 'retry';
        break;
      case 'fail':
        var t = this;
        setTimeout(function() {
          if (t.data.mode == 'play') xhr.attempt(t, false);
          else t.revert(t.data.puzzle.id);
        }, 500);
        this.data.comment = 'fail';
        break;
      default:
        this.userFinalizeMove([orig, dest, promotion], newProgress);
        if (newLines == 'win') xhr.attempt(this, true);
        else setTimeout(partial(this.playOpponentNextMove, this.data.puzzle.id), 1000);
        break;
    }
    m.endComputation(); // give feedback ASAP, don't wait for delayed action
  }.bind(this);

  this.revert = function(id) {
    if (id != this.data.puzzle.id) return;
    this.chessground.reconfigure({
      fen: this.data.chess.fen(),
      lastMove: chess.lastMove(this.data.chess),
      movable: {
        dests: chess.dests(this.data.chess)
      }
    });
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
    },
    animation: {
      enabled: true,
      duration: this.data.animation.duration
    },
    premovable: {
      enabled: true
    }
  });

  this.initiate = function() {
    if (this.data.mode != 'view')
      setTimeout(partial(this.playInitialMove, this.data.puzzle.id), 1000);
    if (this.data.user)
      $.get(this.router.Puzzle.history().url, this.setHistoryHtml);
  }.bind(this);

  this.reload = function(cfg) {
    this.data = data(cfg);
    this.chessground.reset();
    chessground.anim(puzzle.reload, this.chessground.data)(this.data, cfg);
    this.initiate();
  }.bind(this);

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
    setTimeout(this.chessground.playPremove, this.chessground.data.animation.duration);
  }.bind(this);

  this.playOpponentNextMove = function(id) {
    if (id != this.data.puzzle.id) return;
    var move = puzzle.getOpponentNextMove(this.data);
    this.playOpponentMove(puzzle.str2move(move));
    this.data.progress.push(move);
    if (puzzle.getCurrentLines(this.data) == 'win') xhr.attempt(this, true);
  }.bind(this);

  this.playInitialMove = function(id) {
    if (id != this.data.puzzle.id) return;
    this.playOpponentMove(this.data.puzzle.initialMove);
    this.data.startedAt = new Date();
  }.bind(this);

  this.setHistoryHtml = function(html) {
    m.startComputation();
    this.data.historyHtml = html;
    m.endComputation();
  }.bind(this);

  this.jump = function(to) {
    chessground.anim(puzzle.jump, this.chessground.data)(this.data, to);
  }.bind(this);

  this.toggleContinueLinks = function() {
    this.data.showContinueLinks(!this.data.showContinueLinks());
  }.bind(this);

  this.router = router;

  this.trans = function() {
    var str = i18n[arguments[0]]
    Array.prototype.slice.call(arguments, 1).forEach(function(arg) {
      str = str.replace('%s', arg);
    });
    return str;
  };
};
