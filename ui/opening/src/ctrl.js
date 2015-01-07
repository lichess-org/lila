var m = require('mithril');
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
    };
  }.bind(this);
  initialize();

  this.reload = function(data) {
    this.data = data;
    initialize();
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
        premovable: {
          enabled: false
        },
        movable: {
          dests: init.dests
        }
      });
    }.bind(this), 1000);
    m.redraw();
  }.bind(this);

  this.chessground = new chessground.controller({
    fen: this.data.opening.fen,
    orientation: this.data.opening.color,
    turnColor: this.data.opening.color,
    check: init.check,
    autoCastle: true,
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
      return m.first === move.uci;
    })[0];
    if (known && known.quality === 'good') {
      var alreadyFound = this.vm.figuredOut.filter(function(f) {
        return f.uci === move.uci;
      }).length > 0;
      if (alreadyFound) flashFound(move);
      else {
        flash(move, 'good');
        this.vm.figuredOut.push(move);
      }
    } else if (known && known.quality === 'dubious') {
      flash(move, 'dubious');
    } else {
      if (this.vm.messedUp.indexOf(move.uci) === -1) this.vm.messedUp.push(move);
      flash(move, 'bad');
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

  this.router = router;

  this.trans = function() {
    var str = i18n[arguments[0]] || untranslated[arguments[0]] || arguments[0];
    Array.prototype.slice.call(arguments, 1).forEach(function(arg) {
      str = str.replace('%s', arg);
    });
    return str;
  };

  var untranslated = {
    yourOpeningScoreX: 'Your opening score: %s',
    findNbGoodMoves: 'Find %s good moves',
  };
};
