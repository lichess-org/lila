var Chess = require('chessli.js').Chess;
var chessground = require('chessground');
var data = require('./data');
var analyse = require('./analyse');
var ground = require('./ground');
var keyboard = require('./keyboard');
var treePath = require('./path');

module.exports = function(cfg, router, i18n, onChange) {

  this.data = data({}, cfg);
  this.analyse = new analyse(this.data.game, this.data.analysis);

  var initialPath = cfg.path ? treePath.read(cfg.path) : treePath.default();

  this.vm = {
    path: initialPath,
    pathStr: treePath.write(initialPath),
    situation: null,
    continue: false,
    comments: true
  };

  var situationCache = {};
  var showGround = function() {
    var moves;
    try {
      moves = this.analyse.moveList(this.vm.path);
    } catch (e) {
      this.vm.path = treePath.default();
      moves = this.analyse.moveList(this.vm.path);
    }
    var nbMoves = moves.length;
    var ply, move, cached, fen, hash = '',
      h = '',
      lm;
    for (ply = 1; ply <= nbMoves; ply++) {
      move = moves[ply - 1];
      h += move;
      cached = situationCache[h];
      if (!cached) break;
      hash = h;
      fen = cached.fen;
    }
    if (!cached || ply < nbMoves) {
      var chess = new Chess(
        fen || this.data.game.initialFen,
        this.data.game.variant.key == 'chess960' ? 1 : 0
      );
      for (ply = ply; ply <= nbMoves; ply++) {
        move = moves[ply - 1];
        hash += move;
        lm = chess.move(move);
        var turnColor = ply % 2 === 1 ? 'black' : 'white';
        situationCache[hash] = {
          fen: chess.fen(),
          turnColor: turnColor,
          movable: {
            color: turnColor,
            dests: chess.dests()
          },
          check: chess.in_check(),
          lastMove: [lm.from, lm.to]
        };
      }
    }
    this.vm.situation = situationCache[hash] || {
      fen: this.data.game.initialFen,
      turnColor: 'white',
      movable: {
        color: null
      },
      check: false,
      lastMove: null
    };
    if (!this.chessground)
      this.chessground = ground.make(this.data, this.vm.situation, this.onMove);
    this.chessground.stop();
    this.chessground.set(this.vm.situation);
    onChange(this.vm.situation.fen, this.vm.path);
  }.bind(this);

  this.jump = function(path) {
    this.vm.path = path;
    this.vm.pathStr = treePath.write(path);
    showGround();
  }.bind(this);

  this.jumpToMain = function(ply) {
    this.jump([{
      ply: ply,
      variation: null
    }]);
  }.bind(this);

  this.onMove = function(orig, dest) {
    $.sound.move();
    var chess = new Chess(
      this.vm.situation.fen,
      this.data.game.variant.key == 'chess960' ? 1 : 0
    );
    var move = chess.move({
      from: orig,
      to: dest,
      promotion: (dest[1] == 1 || dest[1] == 8) ? 'q' : null
    });
    if (move) this.jump(this.analyse.explore(this.vm.path, move.san));
    else this.chessground.set(this.vm.situation);
    m.redraw();
  }.bind(this);

  this.router = router;

  this.trans = function() {
    var str = i18n[arguments[0]]
    Array.prototype.slice.call(arguments, 1).forEach(function(arg) {
      str = str.replace('%s', arg);
    });
    return str;
  };

  showGround();
  keyboard(this);
};
