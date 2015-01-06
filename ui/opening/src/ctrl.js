var m = require('mithril');
var chessground = require('chessground');
var Chess = require('chessli.js').Chess;

module.exports = function(cfg, router, i18n) {

  this.data = cfg;
  console.log(this.data);

  this.vm = {
    nbGood: this.data.opening.moves.filter(function(m) {
      return m.quality === 'good';
    }).length,
    figuredOut: [],
    messedUp: [],
    flash: null
  };

  var chess = new Chess(this.data.opening.fen);
  var init = {
    dests: chess.dests(),
    check: chess.in_check()
  };

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

  var submitMove = function(move) {
    var found = this.data.opening.moves.filter(function(m) {
      return m.first === move;
    })[0];
    if (found && found.quality === 'good') {
      if (this.vm.figuredOut.indexOf(move) === -1) this.vm.figuredOut.push(move);
    } else if (found && found.quality === 'dubious') {
      flash('dubious');
    } else if (!found || found.quality === 'bad') {
      if (this.vm.messedUp.indexOf(move) === -1) this.vm.messedUp.push(move);
      flash('bad');
    }
  }.bind(this);

  var flash = function(f) {
    this.vm.flash = f;
    setTimeout(function() {
      this.vm.flash = null;
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
