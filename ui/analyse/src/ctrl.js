var chessground = require('chessground');
var opposite = chessground.util.opposite;
var tree = require('./tree/tree');
var treePath = require('./tree/path');
var treeOps = require('./tree/ops');
var ground = require('./ground');
var keyboard = require('./keyboard');
var actionMenu = require('./actionMenu').controller;
var autoplay = require('./autoplay');
var control = require('./control');
var promotion = require('./promotion');
var util = require('./util');
var throttle = require('./util').throttle;
var socket = require('./socket');
var forecastCtrl = require('./forecast/forecastCtrl');
var cevalCtrl = require('./ceval/cevalCtrl');
var explorerCtrl = require('./explorer/explorerCtrl');
var router = require('game').router;
var game = require('game').game;
var crazyValid = require('./crazy/crazyValid');
var tour = require('./tour');
var studyCtrl = require('./study/studyCtrl');
var m = require('mithril');

module.exports = function(opts) {

  this.userId = opts.userId;
  this.canStudy = opts.canStudy;

  var initialize = function(data) {
    this.data = data;
    if (!data.game.moveTimes) this.data.game.moveTimes = [];
    this.ongoing = !util.synthetic(this.data) && game.playable(this.data);
    this.tree = tree(treeOps.reconstruct(this.data.treeParts));
    this.actionMenu = new actionMenu();
    this.autoplay = new autoplay(this);
    this.socket = new socket(opts.socketSend, this);
    this.explorer = explorerCtrl(this, opts.explorer, this.explorer ? this.explorer.allowed() : true);
  }.bind(this);

  initialize(opts.data);

  var initialPath = treePath.root;
  if (opts.path) {
    var mainline = treeOps.mainlineNodeList(this.tree.root);
    if (opts.path === 'last') initialPath = treePath.fromNodeList(mainline);
    else {
      var ply = parseInt(opts.path);
      if (ply) initialPath = treeOps.takePathWhile(mainline, function(n) {
        return n.ply <= ply;
      });
    }
  }

  this.vm = {
    initialPath: initialPath,
    cgConfig: null,
    comments: true,
    flip: false,
    showAutoShapes: util.storedProp('show-auto-shapes', true),
    showGauge: util.storedProp('show-gauge', true),
    autoScroll: null,
    element: opts.element,
    redirecting: false,
    contextMenuPath: null
  };

  this.setPath = function(path) {
    this.vm.path = path;
    this.vm.nodeList = this.tree.getNodeList(path);
    this.vm.node = treeOps.last(this.vm.nodeList);
    this.vm.mainline = treeOps.mainlineNodeList(this.tree.root);
  }.bind(this);

  this.setPath(initialPath);

  this.flip = function() {
    this.vm.flip = !this.vm.flip;
    this.chessground.set({
      orientation: this.vm.flip ? this.data.opponent.color : this.data.player.color
    });
  }.bind(this);

  this.togglePlay = function(delay) {
    this.autoplay.toggle(delay);
    this.actionMenu.open = false;
  }.bind(this);

  var uciToLastMove = function(uci) {
    if (!uci) return;
    if (uci[1] === '@') return [uci.substr(2, 2), uci.substr(2, 2)];
    return [uci.substr(0, 2), uci.substr(2, 2)];
  };

  var showGround = function() {
    var node = this.vm.node;
    var color = node.ply % 2 === 0 ? 'white' : 'black';
    var dests = util.readDests(node.dests);
    var drops = util.readDrops(node.drops);
    var config = {
      fen: node.fen,
      turnColor: color,
      movable: {
        color: (dests && Object.keys(dests).length > 0) || drops === null || drops.length ? color : null,
        dests: dests || {}
      },
      check: node.check,
      lastMove: uciToLastMove(node.uci)
    };
    if (!dests && !node.check) {
      // premove while dests are loading from server
      // can't use when in check because it highlights the wrong king
      config.turnColor = opposite(color);
      config.movable.color = color;
    }
    this.vm.cgConfig = config;
    if (!this.chessground)
      this.chessground = ground.make(this.data, config, userMove, userNewPiece);
    this.chessground.set(config);
    onChange();
    if (!dests) getDests();
    this.setAutoShapes();
    if (node.shapes) this.chessground.setShapes(node.shapes);
  }.bind(this);

  var getDests = throttle(800, false, function() {
    if (this.vm.node.dests) return;
    this.socket.sendAnaDests({
      variant: this.data.game.variant.key,
      fen: this.vm.node.fen,
      path: this.vm.path
    });
  }.bind(this));

  var sound = {
    move: throttle(50, false, $.sound.move),
    capture: throttle(50, false, $.sound.capture),
    check: throttle(50, false, $.sound.check)
  };

  var onChange = opts.onChange ? throttle(300, false, function() {
    var mainlinePly = this.tree.pathIsMainline(this.vm.path) ? this.vm.node.ply : false;
    opts.onChange(this.vm.node.fen, this.vm.path, mainlinePly);
  }.bind(this)) : $.noop;

  var updateHref = (!opts.study && window.history.replaceState) ? throttle(750, false, function() {
    window.history.replaceState(null, null, '#' + this.vm.node.ply);
  }.bind(this), false) : $.noop;

  this.autoScroll = function() {
    this.vm.autoScroll && this.vm.autoScroll();
  }.bind(this);

  this.jump = function(path) {
    var tellStudy = this.study && path !== this.vm.path;
    this.setPath(path);
    showGround();
    if (tellStudy) this.study.setPath(path, this.vm.node);
    if (!this.vm.node.uci) sound.move(); // initial position
    else if (this.vm.justPlayed !== this.vm.node.uci) {
      if (this.vm.node.san.indexOf('x') !== -1) sound.capture();
      else sound.move();
      this.vm.justPlayed = null;
    }
    if (/\+|\#/.test(this.vm.node.san)) sound.check();
    this.ceval.stop();
    startCeval();
    this.explorer.setNode();
    updateHref();
    this.autoScroll();
    promotion.cancel(this);
  }.bind(this);

  this.userJump = function(path) {
    this.autoplay.stop();
    this.chessground.selectSquare(null);
    this.jump(path);
  }.bind(this);

  this.mainlinePathToPly = function(ply) {
    return treeOps.takePathWhile(this.vm.mainline, function(n) {
      return n.ply <= ply;
    });
  }.bind(this);

  this.jumpToMain = function(ply) {
    this.userJump(this.mainlinePathToPly(ply));
  }.bind(this);

  this.jumpToIndex = function(index) {
    this.jumpToMain(index + 1 + this.data.game.startedAtTurn);
  }.bind(this);

  this.jumpToGlyphSymbol = function(color, symbol) {
    var ply = this.tree.plyOfNextGlyphSymbol(color, symbol, this.vm.mainline, this.vm.node.ply);
    if (ply) this.jumpToMain(ply);
    m.redraw();
  }.bind(this);

  this.jumpToLast = function() {
    this.userJump(treePath.fromNodeList(this.vm.mainline));
  }.bind(this);

  this.reloadData = function(data) {
    initialize(data);
    this.vm.redirecting = false;
    this.setPath(treePath.root);
  }.bind(this);

  this.changePgn = function(pgn) {
    this.vm.redirecting = true;
    $.ajax({
      url: '/analysis/pgn',
      method: 'post',
      data: {
        pgn: pgn
      },
      success: function(data) {
        this.reloadData(data);
        this.userJump(this.mainlinePathToPly(this.tree.lastPly()));
      }.bind(this),
      error: function(error) {
        console.log(error);
        this.vm.redirecting = false;
        m.redraw();
      }.bind(this)
    });
  }.bind(this);

  this.changeFen = function(fen) {
    this.vm.redirecting = true;
    window.location = makeUrl(this.data.game.variant.key, fen);
  }.bind(this);

  var makeUrl = function(variantKey, fen) {
    return '/analysis/' + variantKey + '/' + encodeURIComponent(fen).replace(/%20/g, '_').replace(/%2F/g, '/');
  }

  var roleToSan = {
    pawn: 'P',
    knight: 'N',
    bishop: 'B',
    rook: 'R',
    queen: 'Q'
  };
  var sanToRole = {
    P: 'pawn',
    N: 'knight',
    B: 'bishop',
    R: 'rook',
    Q: 'queen'
  };

  var userNewPiece = function(piece, pos) {
    if (crazyValid.drop(this.chessground, this.vm.node.drops, piece, pos)) {
      this.vm.justPlayed = roleToSan[piece.role] + '@' + pos;
      sound.move();
      var drop = {
        role: piece.role,
        pos: pos,
        variant: this.data.game.variant.key,
        fen: this.vm.node.fen,
        path: this.vm.path
      };
      this.socket.sendAnaDrop(drop);
      preparePremoving();
    } else this.jump(this.vm.path);
  }.bind(this);

  var userMove = function(orig, dest, capture) {
    this.vm.justPlayed = orig + dest;
    sound[capture ? 'capture' : 'move']();
    if (!promotion.start(this, orig, dest, sendMove)) sendMove(orig, dest);
  }.bind(this);

  var sendMove = function(orig, dest, prom) {
    var move = {
      orig: orig,
      dest: dest,
      variant: this.data.game.variant.key,
      fen: this.vm.node.fen,
      path: this.vm.path
    };
    if (prom) move.promotion = prom;
    this.socket.sendAnaMove(move);
    preparePremoving();
  }.bind(this);

  var preparePremoving = function() {
    this.chessground.set({
      turnColor: this.chessground.data.movable.color,
      movable: {
        color: opposite(this.chessground.data.movable.color)
      }
    });
  }.bind(this);

  this.addNode = function(node, path) {
    var newPath = this.tree.addNode(node, path);
    this.jump(newPath);
    m.redraw();
    this.chessground.playPremove();
  }.bind(this);

  this.addDests = function(dests, path, opening) {
    this.tree.addDests(dests, path, opening);
    if (path === this.vm.path) {
      showGround();
      m.redraw();
      if (dests === '') this.ceval.stop();
    }
    this.chessground.playPremove();
  }.bind(this);

  this.deleteNode = function(path) {
    var node = this.tree.nodeAtPath(path);
    if (!node) return;
    var count = treeOps.countChildrenAndComments(node);
    if ((count.nodes >= 10 || count.comments > 0) && !confirm(
      'Delete ' + util.plural('move', count.nodes) + (count.comments ? ' and ' + util.plural('comment', count.comments) : '') + '?'
    )) return;
    this.tree.deleteNodeAt(path);
    if (treePath.contains(this.vm.path, path)) this.userJump(treePath.init(path));
    else this.jump(this.vm.path);
    this.study && this.study.deleteNode(path);
  }.bind(this);

  this.promoteNode = function(path) {
    this.tree.promoteNodeAt(path);
    this.jump(this.vm.path);
    this.study && this.study.promoteNode(path);
  }.bind(this);

  this.reset = function() {
    showGround();
    m.redraw();
  }.bind(this);

  this.encodeNodeFen = function() {
    return this.vm.node.fen.replace(/\s/g, '_');
  }.bind(this);

  this.currentEvals = function() {
    var node = this.vm.node;
    return node && (node.eval || node.ceval) ? {
      server: node.eval,
      client: node.ceval,
      fav: node.eval || node.ceval
    } : null;
  }.bind(this);

  this.forecast = opts.data.forecast ? forecastCtrl(
    opts.data.forecast,
    router.forecasts(this.data)) : null;

  this.nextNodeBest = function() {
    return this.tree.ops.withMainlineChild(this.vm.node, function(n) {
      return n.eval ? n.eval.best : null;
    });
  }.bind(this);

  var cevalVariants = ['standard', 'fromPosition', 'chess960'];
  var cevalPossible = function() {
    return (util.synthetic(this.data) || !game.playable(this.data)) &&
      cevalVariants.indexOf(this.data.game.variant.key) !== -1;
  }.bind(this);

  this.ceval = cevalCtrl(cevalPossible, this.data.game.variant, function(res) {
    this.tree.updateAt(res.work.path, function(node) {
      if (node.ceval && node.ceval.depth >= res.eval.depth) return;
      node.ceval = res.eval;
      if (res.work.path === this.vm.path) {
        this.setAutoShapes();
        m.redraw();
      }
    }.bind(this));
  }.bind(this));

  var canUseCeval = function() {
    return this.vm.node.dests !== '' && (!this.vm.node.eval || !this.nextNodeBest());
  }.bind(this);

  var startCeval = throttle(800, false, function() {
    if (this.ceval.enabled() && canUseCeval())
      this.ceval.start(this.vm.path, this.vm.nodeList);
  }.bind(this));

  this.toggleCeval = function() {
    this.ceval.toggle();
    this.setAutoShapes();
    startCeval();
  }.bind(this);

  this.showEvalGauge = function() {
    return this.hasAnyComputerAnalysis() && this.vm.showGauge() && this.vm.node.dests !== '';
  }.bind(this);

  this.hasAnyComputerAnalysis = function() {
    return this.data.analysis || this.ceval.enabled();
  }

  this.toggleAutoShapes = function(v) {
    if (this.vm.showAutoShapes(v)) this.setAutoShapes();
    else this.chessground.setAutoShapes([]);
  }.bind(this);

  this.toggleGauge = function(v) {
    this.vm.showGauge(!this.vm.showGauge());
  }.bind(this);

  this.setAutoShapes = function() {
    var n = this.vm.node,
      shapes = [],
      explorerUci = this.explorer.hoveringUci();
    if (explorerUci) shapes.push(makeAutoShapeFromUci(explorerUci, 'paleBlue'));
    if (this.vm.showAutoShapes()) {
      if (n.eval && n.eval.best) shapes.push(makeAutoShapeFromUci(n.eval.best, 'paleGreen'));
      if (!explorerUci) {
        var nextNodeBest = this.nextNodeBest();
        if (nextNodeBest) shapes.push(makeAutoShapeFromUci(nextNodeBest, 'paleBlue'));
        else if (this.ceval.enabled() && n.ceval && n.ceval.best) shapes.push(makeAutoShapeFromUci(n.ceval.best, 'paleBlue'));
      }
    }
    this.chessground.setAutoShapes(shapes);
  }.bind(this);

  var decomposeUci = function(uci) {
    return [uci.slice(0, 2), uci.slice(2, 4), uci.slice(4, 5)];
  };

  var makeAutoShapeFromUci = function(uci, brush) {
    var move = decomposeUci(uci);
    return uci[1] === '@' ? {
      orig: move[1],
      brush: brush
    } : {
      orig: move[0],
      dest: move[1],
      brush: brush
    };
  }

  this.explorerMove = function(uci) {
    var move = decomposeUci(uci);
    if (uci[1] === '@') this.chessground.apiNewPiece({
        color: this.chessground.data.movable.color,
        role: sanToRole[uci[0]]
      },
      move[1])
    else if (!move[2]) this.chessground.apiMove(move[0], move[1])
    else sendMove(move[0], move[1], sanToRole[move[2].toUpperCase()]);
    this.explorer.loading(true);
  }.bind(this);

  this.socketReceive = function(type, data) {
    this.socket.receive(type, data);
  }.bind(this);

  this.trans = lichess.trans(opts.i18n);

  showGround();
  keyboard(this);
  startCeval();
  this.explorer.setNode();
  tour.init(this.explorer);
  this.study = opts.study ? studyCtrl.init(opts.study, opts.chat, this) : null;
};
