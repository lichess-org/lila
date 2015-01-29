var Chess = require('chessli.js').Chess;
var chessground = require('chessground');
var data = require('./data');
var analyse = require('./analyse');
var ground = require('./ground');
var keyboard = require('./keyboard');
var treePath = require('./path');
var actionMenu = require('./actionMenu').controller;
var autoplay = require('./autoplay');
var control = require('./control');
var m = require('mithril');

module.exports = function(cfg, router, i18n, onChange) {

  this.data = data({}, cfg);
  this.analyse = new analyse(this.data.game, this.data.analysis);
  this.actionMenu = new actionMenu();
  this.autoplay = new autoplay(this);

  var initialPath = cfg.path ? treePath.read(cfg.path) : treePath.default();

  this.vm = {
    path: initialPath,
    pathStr: treePath.write(initialPath),
    situation: null,
    comments: true,
    flip: false
  };

  this.flip = function() {
    this.vm.flip = !this.vm.flip;
    this.chessground.set({
      orientation: this.vm.flip ? this.data.opponent.color : this.data.player.color
    });
  }.bind(this);

  this.togglePlay = function() {
    this.autoplay.toggle();
    this.actionMenu.open = false;
  }.bind(this);

  this.control = function(command) {
    this.autoplay.stop();
    control[command](this);
  }.bind(this);

  var gameVariantChessId = function() {
    switch (this.data.game.variant.key) {
      case 'chess960':
      case 'fromPosition':
        return 1;
      case 'antichess':
        return 2;
      case 'atomic':
        return 3;
      default:
        return 0;
    }
  }.bind(this);

  var situationCache = {};

  var showGround = function() {
    var moves;
    try {
      moves = this.analyse.moveList(this.vm.path);
    } catch (e) {
      console.log(e);
      this.vm.path = treePath.default();
      moves = this.analyse.moveList(this.vm.path);
    }
    var nbMoves = moves.length;
    var ply, move, cached, fen, hash = '',
      h = '',
      lm;
    if (nbMoves == 0) {
      var variant = this.data.game.variant.key;

      var chess = new Chess(
        this.data.game.initialFen, gameVariantChessId());
      var turnColor = chess.turn() == 'w' ? 'white' : 'black';
      this.vm.situation = {
        fen: this.data.game.initialFen,
        turnColor: turnColor,
        movable: {
          color: turnColor,
          dests: chess.dests()
        },
        check: false,
        lastMove: null
      };
    } else {
      for (ply = 1; ply <= nbMoves; ply++) {
        move = moves[ply - 1];
        h += move;
        cached = situationCache[h];
        if (!cached) break;
        hash = h;
        fen = cached.fen;
      }
      if (!cached || ply < nbMoves) {
        try {
          var chess = new Chess(
            fen || this.data.game.initialFen, gameVariantChessId())
          for (ply = ply; ply <= nbMoves; ply++) {
            move = moves[ply - 1];
            hash += move;
            lm = chess.move(move);
            var turnColor = chess.turn() == 'w' ? 'white' : 'black';
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
        } catch (e) {
          console.log(e);
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
    }
    if (!this.chessground)
      this.chessground = ground.make(this.data, this.vm.situation, this.onMove);
    this.chessground.stop();
    this.chessground.set(this.vm.situation);
    if (onChange) onChange(this.vm.situation.fen, this.vm.path);
  }.bind(this);

  this.jump = function(path) {
    this.vm.path = path;
    this.vm.pathStr = treePath.write(path);
    if (window.history.replaceState)
      window.history.replaceState(null, null, '#' + path[0].ply);
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
      this.vm.situation.fen, gameVariantChessId());
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

  this.trans = function(key) {
    var str = i18n[key] || key;
    Array.prototype.slice.call(arguments, 1).forEach(function(arg) {
      str = str.replace('%s', arg);
    });
    return str;
  };

  showGround();
  keyboard(this);
};
