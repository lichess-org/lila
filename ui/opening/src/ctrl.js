var m = require('mithril');
var xhr = require('./xhr');
var chessground = require('chessground');
var Chess = require('chessli.js').Chess;

module.exports = function(cfg, router, i18n) {

  this.data = cfg;
  console.log(this.data);

  this.vm;

  var chess, init;

  var initialize = function() {
    chess = new Chess(this.data.opening.fen);
    init = {
      dests: chess.dests(),
      check: chess.in_check()
    };
    this.vm = {
      figuredOut: [],
      messedUp: [],
      loading: false,
      flash: {},
      flashFound: null,
      comment: null
    };
  }.bind(this);
  initialize();

  this.reload = function(data) {
    this.data = data;
    if (this.data.play) {
      initialize();
      this.chessground.set({
        fen: this.data.opening.fen,
        orientation: this.data.opening.color,
        lastMove: null,
        turnColor: this.data.opening.color,
        check: init.check,
        movable: {
          color: this.data.opening.color,
          dests: init.dests
        }
      });
    } else {
      this.vm.loading = false;
      this.chessground.cancelMove();
      this.chessground.set({
        movable: {
          color: null
        }
      });
    }
  }.bind(this);

  var onMove = function(orig, dest, meta) {
    $.sound.move();
    submitMove(orig + dest);
    setTimeout(function() {
      this.chessground.set({
        fen: this.data.opening.fen,
        lastMove: null,
        turnColor: this.data.opening.color,
        check: init.check,
        movable: {
          dests: init.dests
        }
      });
      if (this.vm.figuredOut.length >= this.data.opening.goal) {
        xhr.attempt(this);
      } else this.chessground.playPremove();
    }.bind(this), 1000);
    m.redraw();
  }.bind(this);

  this.chessground = new chessground.controller({
    fen: this.data.opening.fen,
    orientation: this.data.opening.color,
    turnColor: this.data.opening.color,
    check: init.check,
    autoCastle: true,
    animation: {
      enabled: true,
      duration: this.data.animation.duration
    },
    premovable: {
      enabled: true
    },
    movable: {
      color: this.data.opening.color,
      free: false,
      dests: init.dests,
      events: {
        after: onMove
      }
    },
  });

  var submitMove = function(uci) {
    var chessMove = chess.move({
      from: uci.substr(0, 2),
      to: uci.substr(2, 2)
    });
    if (!chessMove) return;
    chess = new Chess(this.data.opening.fen);
    var move = {
      uci: uci,
      san: chessMove.san
    };
    var known = this.data.opening.moves.filter(function(m) {
      return m.uci === move.uci;
    })[0];
    this.comment = null;
    if (known && known.quality === 'good') {
      var alreadyFound = this.vm.figuredOut.filter(function(f) {
        return f.uci === move.uci;
      }).length > 0;
      if (alreadyFound) flashFound(move);
      else {
        flash(move, 'good');
        this.vm.figuredOut.push(move);
        this.vm.comment = 'good';
      }
    } else if (known && known.quality === 'dubious') {
      flash(move, 'dubious');
      this.vm.comment = 'dubious';
    } else {
      if (this.vm.messedUp.indexOf(move.uci) === -1) this.vm.messedUp.push(move);
      flash(move, 'bad');
      this.vm.comment = 'bad';
    }
  }.bind(this);

  var flash = function(move, quality) {
    this.vm.flash[quality] = move;
    setTimeout(function() {
      delete this.vm.flash[quality];
      m.redraw();
    }.bind(this), 1000);
  }.bind(this);

  var flashFound = function(move) {
    this.vm.flashFound = move;
    setTimeout(function() {
      this.vm.flashFound = null;
      m.redraw();
    }.bind(this), 1000);
  }.bind(this);

  this.notFiguredOut = function() {
    return this.data.opening.moves.filter(function(m) {
      return !this.vm.figuredOut.filter(function(fm) {
        return fm.uci === m.uci;
      }).length
    }.bind(this));
  }.bind(this);

  this.router = router;

  this.trans = function() {
    var str = i18n[arguments[0]] || arguments[0];
    Array.prototype.slice.call(arguments, 1).forEach(function(arg) {
      str = str.replace('%s', arg);
    });
    return str;
  };
};
