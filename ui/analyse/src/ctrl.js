var chessground = require('chessground');
var opposite = chessground.util.opposite;
var tree = require('tree');
var ground = require('./ground');
var keyboard = require('./keyboard');
var actionMenu = require('./actionMenu').controller;
var autoplay = require('./autoplay');
var promotion = require('./promotion');
var util = require('./util');
var readDests = require('chess').readDests;
var readDrops = require('chess').readDrops;
var sanToRole = require('chess').sanToRole;
var roleToSan = require('chess').roleToSan;
var decomposeUci = require('chess').decomposeUci;
var storedProp = require('common').storedProp;
var throttle = require('common').throttle;
var socket = require('./socket');
var forecastCtrl = require('./forecast/forecastCtrl');
var cevalCtrl = require('ceval').ctrl;
var explorerCtrl = require('./explorer/explorerCtrl');
var router = require('game').router;
var game = require('game').game;
var crazyValid = require('./crazy/crazyValid');
var makeStudy = require('./study/studyCtrl');
var makeFork = require('./fork').ctrl;
var makeRetro = require('./retrospect/retroCtrl');
var makePractice = require('./practice/practiceCtrl');
var computeAutoShapes = require('./autoShape').compute;
var nodeFinder = require('./nodeFinder');
var acplUncache = require('./acpl').uncache;
var m = require('mithril');

module.exports = function(opts) {

  this.userId = opts.userId;
  this.embed = opts.embed;

  var initialize = function(data) {
    this.data = data;
    if (!data.game.moveTimes) this.data.game.moveTimes = [];
    this.ongoing = !util.synthetic(this.data) && game.playable(this.data);
    this.tree = tree.build(tree.ops.reconstruct(this.data.treeParts));
    this.actionMenu = new actionMenu();
    this.autoplay = new autoplay(this);
    this.socket = new socket(opts.socketSend, this);
    this.explorer = explorerCtrl(this, opts.explorer, this.explorer ? this.explorer.allowed() : !this.embed);
  }.bind(this);

  initialize(opts.data);

  var initialPath = tree.path.root;
  if (opts.initialPly) {
    var plyStr = opts.initialPly === 'url' ? (location.hash ? location.hash.replace(/#/, '') : tree.path.root) : opts.initialPly;
    var mainline = tree.ops.mainlineNodeList(this.tree.root);
    if (plyStr === 'last') initialPath = tree.path.fromNodeList(mainline);
    else {
      var ply = parseInt(plyStr);
      if (ply) initialPath = tree.ops.takePathWhile(mainline, function(n) {
        return n.ply <= ply;
      });
    }
  }

  this.vm = {
    initialPath: initialPath,
    cgConfig: null,
    comments: true,
    flip: false,
    showAutoShapes: storedProp('show-auto-shapes', true),
    showGauge: storedProp('show-gauge', true),
    showComputer: storedProp('show-computer', true),
    autoScrollRequested: false,
    element: opts.element,
    redirecting: false,
    contextMenuPath: null,
    justPlayed: null,
    justDropped: null,
    keyboardHelp: location.hash === '#keyboard',
    threatMode: false
  };

  this.setPath = function(path) {
    this.vm.path = path;
    this.vm.nodeList = this.tree.getNodeList(path);
    this.vm.node = tree.ops.last(this.vm.nodeList);
    this.vm.mainline = tree.ops.mainlineNodeList(this.tree.root);
  }.bind(this);

  this.setPath(initialPath);

  this.flip = function() {
    this.vm.flip = !this.vm.flip;
    this.chessground.set({
      orientation: this.bottomColor()
    });
    if (this.retro) {
      this.retro = null;
      this.toggleRetro();
    }
    if (this.practice) {
      this.practice = null;
      this.togglePractice();
    }
    m.redraw();
  }.bind(this);

  this.unflip = function() {
    this.vm.flip = false;
  }.bind(this);

  this.topColor = function() {
    return opposite(this.bottomColor());
  }.bind(this);
  this.bottomColor = function() {
    return this.vm.flip ? opposite(this.data.orientation) : this.data.orientation;
  }.bind(this);
  this.getOrientation = function() {
    return this.data.orientation;
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

  this.fork = makeFork(this);

  var showGround = function() {
    var node = this.vm.node;
    var color = node.ply % 2 === 0 ? 'white' : 'black';
    var dests = readDests(node.dests);
    var drops = readDrops(node.drops);
    var movableColor = this.practice ? this.bottomColor() : (
      this.embed ||
      (dests && Object.keys(dests).length > 0) ||
      drops === null ||
      drops.length ? color : null);
    var config = {
      fen: node.fen,
      turnColor: color,
      movable: this.embed ? {
        color: null,
        dests: {}
      } : {
        color: movableColor,
        dests: movableColor === color ? (dests || {}) : {}
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
    config.premovable = {
      enabled: config.movable.color && config.turnColor !== config.movable.color
    };

    this.vm.cgConfig = config;
    if (!this.chessground)
      this.chessground = ground.make(this.data, config, userMove, userNewPiece, !!opts.study);
    this.chessground.set(config);
    onChange();
    if (!dests) getDests();
    this.setAutoShapes();
    if (node.shapes) this.chessground.setShapes(node.shapes);
  }.bind(this);

  var getDests = throttle(800, false, function() {
    if (!this.vm.node.dests) this.socket.sendAnaDests({
      variant: this.data.game.variant.key,
      fen: this.vm.node.fen,
      path: this.vm.path
    });
  }.bind(this));

  var sound = $.sound ? {
    move: throttle(50, false, $.sound.move),
    capture: throttle(50, false, $.sound.capture),
    check: throttle(50, false, $.sound.check)
  } : {
    move: $.noop,
    capture: $.noop,
    check: $.noop
  };

  var onChange = opts.onChange ? throttle(300, false, function() {
    var mainlinePly = this.tree.pathIsMainline(this.vm.path) ? this.vm.node.ply : false;
    opts.onChange(this.vm.node.fen, this.vm.path, mainlinePly);
  }.bind(this)) : $.noop;

  var updateHref = (!opts.study && window.history.replaceState) ? throttle(750, false, function() {
    window.history.replaceState(null, null, '#' + this.vm.node.ply);
  }.bind(this), false) : $.noop;

  this.autoScroll = function() {
    this.vm.autoScrollRequested = true;
  }.bind(this);

  this.jump = function(path) {
    var pathChanged = path !== this.vm.path;
    this.setPath(path);
    showGround();
    if (pathChanged) {
      if (this.study) this.study.setPath(path, this.vm.node);
      if (!this.vm.node.uci) sound.move(); // initial position
      else if (this.vm.node.uci.indexOf(this.vm.justPlayed) !== 0) {
        if (this.vm.node.san.indexOf('x') !== -1) sound.capture();
        else sound.move();
      }
      if (/\+|\#/.test(this.vm.node.san)) sound.check();
      this.vm.threatMode = false;
      this.ceval.stop();
      this.startCeval();
    }
    this.vm.justPlayed = null;
    this.vm.justDropped = null;
    this.explorer.setNode();
    updateHref();
    this.autoScroll();
    promotion.cancel(this);
    if (this.retro) this.retro.onJump();
    if (this.practice) this.practice.onJump();
    if (this.music) this.music.jump(this.vm.node);
  }.bind(this);

  this.userJump = function(path) {
    this.autoplay.stop();
    this.chessground.selectSquare(null);
    if (this.practice) {
      var prev = this.vm.path;
      this.jump(path);
      this.practice.onUserJump(prev, this.vm.path);
    } else
      this.jump(path);
  }.bind(this);

  var canJumpTo = function(path) {
    return this.study ? this.study.canJumpTo(path) : true;
  }.bind(this);

  this.userJumpIfCan = function(path) {
    if (canJumpTo(path)) this.userJump(path);
  }.bind(this);

  this.mainlinePathToPly = function(ply) {
    return tree.ops.takePathWhile(this.vm.mainline, function(n) {
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
    var node = nodeFinder.nextGlyphSymbol(color, symbol, this.vm.mainline, this.vm.node.ply);
    if (node) this.jumpToMain(node.ply);
    m.redraw();
  }.bind(this);

  this.reloadData = function(data) {
    initialize(data);
    this.vm.redirecting = false;
    this.setPath(tree.path.root);
    instanciateCeval();
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

  var userNewPiece = function(piece, pos) {
    if (crazyValid.drop(this.chessground, this.vm.node.drops, piece, pos)) {
      this.vm.justPlayed = roleToSan[piece.role] + '@' + pos;
      this.vm.justDropped = {
        ply: this.vm.node.ply,
        role: piece.role
      };
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
      m.redraw();
    } else this.jump(this.vm.path);
  }.bind(this);

  var userMove = function(orig, dest, capture) {
    this.vm.justPlayed = orig;
    this.vm.justDropped = null;
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
      if (this.gameOver()) this.ceval.stop();
    }
    this.chessground.playPremove();
  }.bind(this);

  this.deleteNode = function(path) {
    var node = this.tree.nodeAtPath(path);
    if (!node) return;
    var count = tree.ops.countChildrenAndComments(node);
    if ((count.nodes >= 10 || count.comments > 0) && !confirm(
      'Delete ' + util.plural('move', count.nodes) + (count.comments ? ' and ' + util.plural('comment', count.comments) : '') + '?'
    )) return;
    this.tree.deleteNodeAt(path);
    if (tree.path.contains(this.vm.path, path)) this.userJump(tree.path.init(path));
    else this.jump(this.vm.path);
    this.study && this.study.deleteNode(path);
  }.bind(this);

  this.promote = function(path, toMainline) {
    this.tree.promoteAt(path, toMainline);
    this.jump(path);
    if (this.study) this.study.promote(path, toMainline);
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
    return tree.ops.withMainlineChild(this.vm.node, function(n) {
      return n.eval ? n.eval.best : null;
    });
  }.bind(this);

  this.setAutoShapes = function() {
    this.chessground.setAutoShapes(computeAutoShapes(this));
  }.bind(this);

  var instanciateCeval = function(failsafe) {
    if (this.ceval) this.ceval.destroy();
    this.ceval = cevalCtrl({
      variant: this.data.game.variant,
      possible: !this.embed && (
        util.synthetic(this.data) || !game.playable(this.data)
      ),
      emit: function(res) {
        this.tree.updateAt(res.work.path, function(node) {
          if (res.work.threatMode) {
            if (node.threat && node.threat.depth >= res.eval.depth) return;
            node.threat = res.eval;
          } else {
            if (node.ceval && node.ceval.depth >= res.eval.depth) return;
            node.ceval = res.eval;
          }
          if (res.work.path === this.vm.path) {
            this.setAutoShapes();
            if (this.retro) this.retro.onCeval();
            if (this.practice) this.practice.onCeval();
            m.redraw();
          }
        }.bind(this));
      }.bind(this),
      setAutoShapes: this.setAutoShapes,
      failsafe: failsafe,
      onCrash: function(e) {
        console.log('Local eval failed!', e);
        if (this.ceval.pnaclSupported) {
          console.log('Retrying in failsafe mode');
          instanciateCeval(true);
          this.startCeval();
        }
      }.bind(this)
    });
  }.bind(this);

  instanciateCeval();

  this.getCeval = function() {
    return this.ceval;
  }.bind(this);

  this.gameOver = function() {
    if (this.vm.node.dests !== '') return false;
    if (this.vm.node.check) return 'checkmate';
    if (this.vm.node.crazy) {
      // no stalemate with full crazyhouse pockets
      var wtm = this.vm.node.fen.indexOf(' w ') !== -1;
      var p = this.vm.node.crazy.pockets[wtm ? 0 : 1];
      if (p.pawn || p.knight || p.bishop || p.rook || p.queen) return false;
    }
    return 'stalemate';
  }.bind(this);

  var canUseCeval = function() {
    return !this.gameOver();
  }.bind(this);

  this.startCeval = throttle(800, false, function() {
    if (this.ceval.enabled()) {
      if (canUseCeval()) this.ceval.start(this.vm.path, this.vm.nodeList, this.vm.threatMode);
      else this.ceval.stop();
    }
  }.bind(this));

  this.toggleCeval = function() {
    this.ceval.toggle();
    this.setAutoShapes();
    this.startCeval();
    if (!this.ceval.enabled()) {
      this.vm.threatMode = false;
      if (this.practice) this.togglePractice();
    }
    m.redraw();
  }.bind(this);

  this.toggleThreatMode = function() {
    if (this.vm.node.check) return;
    if (!this.ceval.enabled()) this.ceval.toggle();
    if (!this.ceval.enabled()) return;
    this.vm.threatMode = !this.vm.threatMode;
    if (this.vm.threatMode && this.practice) this.togglePractice();
    this.setAutoShapes();
    this.startCeval();
    m.redraw();
  }.bind(this);

  var cevalReset = function(f) {
    this.ceval.stop();
    if (!this.ceval.enabled()) this.ceval.toggle();
    this.startCeval();
    m.redraw();
  }.bind(this);

  this.cevalSetMultiPv = function(v) {
    this.ceval.multiPv(v);
    this.tree.removeCeval();
    cevalReset();
  }.bind(this);

  this.cevalSetThreads = function(v) {
    this.ceval.threads(v);
    cevalReset();
  }.bind(this);

  this.cevalSetHashSize = function(v) {
    this.ceval.hashSize(v);
    cevalReset();
  }.bind(this);

  this.showEvalGauge = function() {
    return this.hasAnyComputerAnalysis() && this.vm.showGauge() && !this.gameOver() && this.vm.showComputer();
  }.bind(this);

  this.hasAnyComputerAnalysis = function() {
    return this.data.analysis || this.ceval.enabled();
  }.bind(this);

  this.hasFullComputerAnalysis = function() {
    return this.vm.mainline[0].eval && Object.keys(this.vm.mainline[0].eval).length;
  }.bind(this);

  var resetAutoShapes = function() {
    if (this.vm.showAutoShapes()) this.setAutoShapes();
    else this.chessground.setAutoShapes([]);
  }.bind(this);

  this.toggleAutoShapes = function(v) {
    this.vm.showAutoShapes(v);
    resetAutoShapes();
  }.bind(this);

  this.toggleGauge = function() {
    this.vm.showGauge(!this.vm.showGauge());
  }.bind(this);

  var onToggleComputer = function() {
    if (!this.vm.showComputer()) {
      this.tree.removeComputerVariations();
      if (this.ceval.enabled()) this.toggleCeval();
      this.chessground.setAutoShapes([]);
    } else resetAutoShapes();
  }.bind(this);

  this.toggleComputer = function() {
    var value = !this.vm.showComputer();
    this.vm.showComputer(value);
    if (!value && this.practice) this.togglePractice();
    if (opts.onToggleComputer) opts.onToggleComputer(value);
    onToggleComputer();
  }.bind(this);

  this.mergeAnalysisData = function(data) {
    this.tree.merge(data.tree);
    if (!this.vm.showComputer()) this.tree.removeComputerVariations();
    this.data.analysis = data.analysis;
    if (this.retro) this.retro.onMergeAnalysisData();
    m.redraw();
  }.bind(this);

  this.playUci = function(uci) {
    var move = decomposeUci(uci);
    if (uci[1] === '@') this.chessground.apiNewPiece({
        color: this.chessground.data.movable.color,
        role: sanToRole[uci[0]]
      },
      move[1])
    else if (!move[2]) sendMove(move[0], move[1])
    else sendMove(move[0], move[1], sanToRole[move[2].toUpperCase()]);
  }.bind(this);

  this.explorerMove = function(uci) {
    this.playUci(uci);
    this.explorer.loading(true);
  }.bind(this);

  this.playBestMove = function() {
    var uci = this.nextNodeBest() || (this.vm.node.ceval && this.vm.node.ceval.best);
    if (uci) this.playUci(uci);
  }.bind(this);

  this.socketReceive = function(type, data) {
    this.socket.receive(type, data);
  }.bind(this);

  this.trans = lichess.trans(opts.i18n);

  showGround();
  onToggleComputer();
  this.startCeval();
  this.explorer.setNode();
  this.study = opts.study ? makeStudy(opts.study, this, (opts.tagTypes || '').split(',')) : null;

  this.retro = null;

  this.toggleRetro = function() {
    if (this.retro) this.retro = null;
    else {
      this.retro = makeRetro(this);
      if (this.practice) this.togglePractice();
      if (this.explorer.enabled()) this.toggleExplorer();
    }
    acplUncache();
    this.setAutoShapes();
  }.bind(this);

  this.toggleExplorer = function() {
    if (this.practice) this.togglePractice();
    this.explorer.toggle();
  }.bind(this);

  this.practice = null;

  this.togglePractice = function() {
    if (this.practice || !this.ceval.possible) this.practice = null;
    else {
      if (this.retro) this.toggleRetro();
      if (this.explorer.enabled()) this.toggleExplorer();
      this.practice = makePractice(this);
    }
    this.setAutoShapes();
  }.bind(this);
  if (location.hash === '#practice') this.togglePractice();

  keyboard.bind(this);

  lichess.pubsub.on('jump', function(ply) {
    this.jumpToMain(parseInt(ply));
    m.redraw();
  }.bind(this));

  this.music = null;
  lichess.pubsub.on('sound_set', function(set) {
    if (!this.music && set === 'music')
      lichess.loadScript('/assets/javascripts/music/replay.js').then(function() {
        this.music = lichessReplayMusic();
      }.bind(this));
    if (this.music && set !== 'music') this.music = null;
  }.bind(this));
};
