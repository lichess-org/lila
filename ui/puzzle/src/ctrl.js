var m = require('mithril');
var k = Mousetrap;
var merge = require('merge');
var last = require('lodash/array/last');
var chessground = require('chessground');
var partial = chessground.util.partial;
var data = require('./data');
var chess = require('./chess');
var puzzle = require('./puzzle');
var xhr = require('./xhr');

module.exports = function(cfg, router, i18n) {

  this.vm = {
    loading: false
  };

  this.data = data(cfg);

  var userMove = function(orig, dest) {
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
        setTimeout(function() {
          if (this.data.mode == 'play') {
            this.chessground.stop();
            xhr.attempt(this, false);
          } else this.revert(this.data.puzzle.id);
        }.bind(this), 500);
        this.data.comment = 'fail';
        break;
      default:
        this.userFinalizeMove([orig, dest, promotion], newProgress);
        if (newLines == 'win' || (Object.keys(newLines).length === 1 && newLines[Object.keys(newLines)[0]] == 'win')) {
          this.chessground.stop();
          xhr.attempt(this, true);
        } else setTimeout(partial(this.playOpponentNextMove, this.data.puzzle.id), 1000);
        break;
    }
    m.endComputation(); // give feedback ASAP, don't wait for delayed action
  }.bind(this);

  var moveSound = function(orig, dest, captured) {
    $.sound[captured ? 'capture' : 'move']();
  }.bind(this);

  this.revert = function(id) {
    if (id != this.data.puzzle.id) return;
    this.chessground.set({
      fen: this.data.chess.fen(),
      lastMove: chess.lastMove(this.data.chess).slice(0, 2),
      turnColor: this.data.puzzle.color,
      check: null,
      movable: {
        dests: this.data.chess.dests()
      }
    });
    m.redraw();
    if (this.data.chess.in_check()) this.chessground.setCheck();
  }.bind(this);

  this.userFinalizeMove = function(move, newProgress) {
    chess.move(this.data.chess, move);
    this.data.comment = 'great';
    this.data.progress = newProgress;
    this.chessground.set({
      fen: this.data.chess.fen(),
      lastMove: move.slice(0, 2),
      turnColor: this.data.puzzle.opponentColor,
      check: null
    });
    if (this.data.chess.in_check()) this.chessground.setCheck();
  }.bind(this);

  this.chessground = new chessground.controller(merge.recursive({
    fen: cfg.puzzle.fen,
    orientation: this.data.puzzle.color,
    coordinates: this.data.pref.coords !== 0,
    turnColor: this.data.puzzle.opponentColor,
    movable: {
      free: false,
      color: cfg.mode !== 'view' ? cfg.puzzle.color : null,
      events: {
        after: userMove
      },
    },
    events: {
      move: moveSound
    },
    animation: {
      enabled: true,
      duration: this.data.animation.duration
    },
    premovable: {
      enabled: true
    },
    drawable: {
      enabled: true
    },
    disableContextMenu: true
  }, this.data.chessground));

  k.bind(['esc'], this.chessground.cancelMove);

  this.initiate = function() {
    if (this.data.mode !== 'view')
      setTimeout(partial(this.playInitialMove, this.data.puzzle.id), 1000);
  }.bind(this);

  this.reload = function(cfg) {
    this.vm.loading = false;
    this.data = data(cfg);
    chessground.board.reset(this.chessground.data);
    chessground.anim(puzzle.reload, this.chessground.data)(this.data, cfg);
    this.initiate();
  }.bind(this);

  this.pushState = function(cfg) {
    if (window.history.pushState)
      window.history.pushState(cfg, null, router.Puzzle.show(cfg.puzzle.id).url);
  }.bind(this);

  window.onpopstate = function(cfg) {
    if (cfg.state) this.reload(cfg.state);
    m.redraw();
  }.bind(this);

  this.playOpponentMove = function(move) {
    moveSound(move[0], move[1], this.chessground.data.pieces[move[1]]);
    m.startComputation();
    chess.move(this.data.chess, move);
    this.chessground.set({
      fen: this.data.chess.fen(),
      lastMove: move.slice(0, 2),
      movable: {
        dests: this.data.chess.dests()
      },
      turnColor: this.data.puzzle.color,
      check: null
    });
    if (this.data.chess.in_check()) this.chessground.setCheck();
    setTimeout(this.chessground.playPremove, this.chessground.data.animation.duration);
    m.endComputation();
  }.bind(this);

  this.playOpponentNextMove = function(id) {
    if (id != this.data.puzzle.id) return;
    var move = puzzle.getOpponentNextMove(this.data);
    this.playOpponentMove(puzzle.str2move(move));
    this.data.progress.push(move);
    if (puzzle.getCurrentLines(this.data) == 'win') {
      this.chessground.stop();
      xhr.attempt(this, true);
    }
  }.bind(this);

  this.playInitialMove = function(id) {
    if (id != this.data.puzzle.id) return;
    this.playOpponentMove(this.data.puzzle.initialMove);
    this.data.startedAt = new Date();
  }.bind(this);

  this.jump = function(to) {
    var prevStep = this.data.replay.step;
    var state = this.data.replay.history[to];
    if (!state) return;
    chessground.anim(puzzle.jump, this.chessground.data)(this.data, to);
    if (prevStep + 1 === to) {
      // step forward, call moveSound
      moveSound(state.move[0], state.move[1], state.capture);
    } else if (prevStep - 1 === to) {
      // step backward, just play move sound
      $.sound.move();
    }
  }.bind(this);

  this.router = router;

  this.trans = lichess.trans(i18n);
};
