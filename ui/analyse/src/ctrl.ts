import { opposite } from 'chessground/util';
import * as tree from 'tree';
import * as keyboard from './keyboard';
import { Controller as ActionMenuController } from './actionMenu';
import Autoplay from './autoplay';
import * as promotion from './promotion';
import * as util from './util';
import * as chessUtil from 'chess';
import { storedProp, throttle, defined } from 'common';
import makeSocket from './socket';
import forecastCtrl from './forecast/forecastCtrl';
import { ctrl as cevalCtrl, isEvalBetter } from 'ceval';
import explorerCtrl from './explorer/explorerCtrl';
import { router, game } from 'game';
import { valid as crazyValid } from './crazy/crazyCtrl';
import * as makeStudy from './study/studyCtrl';
var makeFork = require('./fork').ctrl;
var makeRetro = require('./retrospect/retroCtrl');
var makePractice = require('./practice/practiceCtrl');
var makeEvalCache = require('./evalCache');
var computeAutoShapes = require('./autoShape').compute;
var nodeFinder = require('./nodeFinder');
var acplUncache = require('./acpl').uncache;
var m = require('mithril');

export default function(opts, redraw: () => void): AnalyseController {

  this.opts = opts;
  this.redraw = redraw;
  this.userId = opts.userId;
  this.embed = opts.embed;

  var initialize = function(data, merge) {
    this.data = data;
    if (!data.game.moveCentis) this.data.game.moveCentis = [];
    this.synthetic = util.synthetic(data);
    this.ongoing = !this.synthetic && game.playable(data);

    var prevTree = merge && this.tree.root;
    this.tree = tree.build(tree.ops.reconstruct(data.treeParts));
    if (prevTree) this.tree.merge(prevTree);

    this.actionMenu = new ActionMenuController();
    this.autoplay = new Autoplay(this);
    if (this.socket) this.socket.clearCache();
    else this.socket = new makeSocket(opts.socketSend, this);
    this.explorer = explorerCtrl(this, opts.explorer, this.explorer ? this.explorer.allowed() : !this.embed, redraw);
    this.gamePath = (this.synthetic || this.ongoing) ? null :
      tree.path.fromNodeList(tree.ops.mainlineNodeList(this.tree.root));
  }.bind(this);

  initialize(opts.data);

  var initialPath = tree.path.root;
  if (opts.initialPly) {
    var locationHash = location.hash;
    var plyStr = opts.initialPly === 'url' ? (locationHash || '').replace(/#/, '') : opts.initialPly;
    // remove location hash - http://stackoverflow.com/questions/1397329/how-to-remove-the-hash-from-window-location-with-javascript-without-page-refresh/5298684#5298684
    if (locationHash) history.pushState("", document.title, location.pathname + location.search);
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
    justCaptured: null,
    keyboardHelp: location.hash === '#keyboard',
    threatMode: false,
    onMainline: true
  };

  this.setPath = function(path) {
    this.vm.path = path;
    this.vm.nodeList = this.tree.getNodeList(path);
    this.vm.node = tree.ops.last(this.vm.nodeList);
    this.vm.mainline = tree.ops.mainlineNodeList(this.tree.root);
    this.vm.onMainline = this.tree.pathIsMainline(path)
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
    if (this.practice) this.restartPractice();
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
  this.getOrientation = this.bottomColor; // required by ui/ceval

    this.turnColor = function() {
      return this.vm.node.ply % 2 === 0 ? 'white' : 'black';
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
    onChange();
    if (!defined(node.dests)) getDests();
    if (this.chessground) {
      this.chessground.set(this.makeCgOpts());
      this.setAutoShapes();
      if (node.shapes) this.chessground.setShapes(node.shapes);
    }
  }.bind(this);

  var getDests = throttle(800, false, function() {
    if (!this.embed && !defined(this.vm.node.dests)) this.socket.sendAnaDests({
      variant: this.data.game.variant.key,
      fen: this.vm.node.fen,
      path: this.vm.path
    });
  }.bind(this));

  this.makeCgOpts = function() {
    var node = this.vm.node;
    var color = this.turnColor();
    var dests = chessUtil.readDests(node.dests);
    var drops = chessUtil.readDrops(node.drops);
    var movableColor = this.practice ? this.bottomColor() : (
      !this.embed && (
        (dests && Object.keys(dests).length > 0) ||
        drops === null || drops.length
      ) ? color : null);
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
      check: !!node.check,
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
    return config;
  }.bind(this);

  var sound = lichess.sound ? {
    move: throttle(50, false, lichess.sound.move),
    capture: throttle(50, false, lichess.sound.capture),
    check: throttle(50, false, lichess.sound.check)
  } : {
    move: $.noop,
    capture: $.noop,
    check: $.noop
  };

  var onChange = opts.onChange ? throttle(300, false, function() {
    var mainlinePly = this.vm.onMainline ? this.vm.node.ply : false;
    opts.onChange(this.vm.node.fen, this.vm.path, mainlinePly);
  }.bind(this)) : $.noop;

  var updateHref = opts.study ? $.noop : throttle(750, false, function() {
    history.replaceState(null, null, '#' + this.vm.node.ply);
  }.bind(this), false);

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
    this.vm.justCaptured = null;
    this.explorer.setNode();
    updateHref();
    this.autoScroll();
    promotion.cancel(this);
    if (pathChanged) {
      if (this.retro) this.retro.onJump();
      if (this.practice) this.practice.onJump();
      if (this.study) this.study.onJump();
    }
    if (this.music) this.music.jump(this.vm.node);
  }.bind(this);

  this.userJump = function(path) {
    this.autoplay.stop();
    this.chessground && this.chessground.selectSquare(null);
    if (this.practice) {
      var prev = this.vm.path;
      this.practice.preUserJump(prev, path);
      this.jump(path);
      this.practice.postUserJump(prev, this.vm.path);
    } else {
      this.jump(path);
    }
  }.bind(this);

  var canJumpTo = function(path) {
    return !this.study || this.study.canJumpTo(path);
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

  this.reloadData = function(data, merge) {
    initialize(data, merge);
    this.vm.redirecting = false;
    this.setPath(tree.path.root);
    instanciateCeval();
    instanciateEvalCache();
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

  this.userNewPiece = function(piece, pos) {
    if (crazyValid(this.chessground, this.vm.node.drops, piece, pos)) {
      this.vm.justPlayed = chessUtil.roleToSan[piece.role] + '@' + pos;
      this.vm.justDropped = piece.role;
      this.vm.justCaptured = null;
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

  this.userMove = function(orig, dest, capture) {
    this.vm.justPlayed = orig;
    this.vm.justDropped = null;
    sound[capture ? 'capture' : 'move']();
    if (!promotion.start(this, orig, dest, capture, sendMove)) sendMove(orig, dest, capture);
  }.bind(this);

  var sendMove = function(orig, dest, capture, prom) {
    var move = {
      orig: orig,
      dest: dest,
      variant: this.data.game.variant.key,
      fen: this.vm.node.fen,
      path: this.vm.path
    };
    if (capture) this.vm.justCaptured = capture;
    if (prom) move.promotion = prom;
    if (this.practice) this.practice.onUserMove();
    this.socket.sendAnaMove(move);
    preparePremoving();
    m.redraw();
  }.bind(this);

  var preparePremoving = function() {
    this.chessground.set({
      turnColor: this.chessground.state.movable.color,
      movable: {
        color: opposite(this.chessground.state.movable.color)
      },
      premovable: {
        enabled: true
      }
    });
  }.bind(this);

  this.addNode = function(node, path) {
    var newPath = this.tree.addNode(node, path);
    if (!newPath) {
      console.log('Cannot addNode', node, path);
      m.redraw();
      return;
    }
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
    this.chessground && this.chessground.playPremove();
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
    redraw();
  }.bind(this);

  this.encodeNodeFen = function() {
    return this.vm.node.fen.replace(/\s/g, '_');
  }.bind(this);

  this.currentEvals = function() {
    var node = this.vm.node;
    return {
      server: node.eval,
      client: node.ceval
    };
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
    if (this.chessground) this.chessground.setAutoShapes(computeAutoShapes(this));
  }.bind(this);

  var onNewCeval = function(ev, path, threatMode) {
    this.tree.updateAt(path, function(node) {
      if (node.fen !== ev.fen && !threatMode) return;
      if (threatMode) {
        if (!node.threat || isEvalBetter(ev, node.threat) || node.threat.maxDepth < ev.maxDepth)
        node.threat = ev;
      } else if (isEvalBetter(ev, node.ceval)) node.ceval = ev;
      else if (node.ceval && ev.maxDepth > node.ceval.maxDepth) node.ceval.maxDepth = ev.maxDepth;

      if (path === this.vm.path) {
        this.setAutoShapes();
        if (!threatMode) {
          if (this.retro) this.retro.onCeval();
          if (this.practice) this.practice.onCeval();
          if (this.studyPractice) this.studyPractice.onCeval();
          this.evalCache.onCeval();
          if (ev.cloud && ev.depth >= this.ceval.effectiveMaxDepth()) this.ceval.stop();
        }
        m.redraw();
      }
    }.bind(this));
  }.bind(this);

  var instanciateCeval = function(failsafe) {
    if (this.ceval) this.ceval.destroy();
    var cfg = {
      variant: this.data.game.variant,
      possible: !this.embed && (
        this.synthetic || !game.playable(this.data)
      ),
      emit: function(ev, work) {
        onNewCeval(ev, work.path, work.threatMode);
      }.bind(this),
      setAutoShapes: this.setAutoShapes,
      failsafe: failsafe,
      onCrash: function(lastError) {
        var ceval = this.vm.node.ceval;
        console.log('Local eval failed after depth ' + (ceval && ceval.depth));
        var env = this.ceval.env();
        var desc = [
          'ceval crash',
          env.pnacl ? 'pnacl' : (env.wasm ? 'wasm' : 'asmjs'),
          'multiPv:' + env.multiPv,
          'threads:' + env.threads,
          'hashSize:' + env.hashSize,
          'depth:' + (ceval && ceval.depth || 0) + '/' + env.maxDepth,
          lastError
        ].join(' ');
        console.log('send exception: ' + desc);
        if (window.ga) window.ga('send', 'exception', {
          exDescription: desc
        });
        if (this.ceval.pnaclSupported) {
          if (ceval && ceval.depth >= 20 && !ceval.retried) {
            console.log('Remain on native stockfish for now');
            ceval.retried = true;
          } else {
            console.log('Fallback to ASMJS now');
            instanciateCeval(true);
            this.startCeval();
          }
        }
      }.bind(this)
    };
    if (opts.study && opts.practice) {
      cfg.storageKeyPrefix = 'practice';
      cfg.multiPvDefault = 1;
    }
    this.ceval = cevalCtrl(cfg);
  }.bind(this);

  instanciateCeval();

  this.getCeval = function() {
    return this.ceval;
  }.bind(this);

  this.gameOver = function(node) {
    var n = node || this.vm.node;
    if (n.dests !== '' || n.drops) return false;
    if (n.check) return 'checkmate';
    return 'draw';
  }.bind(this);

  var canUseCeval = function() {
    return !this.gameOver() && !this.vm.node.threefold;
  }.bind(this);

  this.startCeval = throttle(800, false, function() {
    if (this.ceval.enabled()) {
      if (canUseCeval()) {
        this.ceval.start(this.vm.path, this.vm.nodeList, this.vm.threatMode);
        this.evalCache.fetch(this.vm.path, parseInt(this.ceval.multiPv()));
      } else this.ceval.stop();
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

  this.disableThreatMode = function() {
    return !!this.practice;
  }.bind(this);

  this.mandatoryCeval = function() {
    return !!this.studyPractice;
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

  this.cevalSetInfinite = function(v) {
    this.ceval.infinite(v);
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
    else this.chessground && this.chessground.setAutoShapes([]);
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
      this.chessground && this.chessground.setAutoShapes([]);
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
    var move = chessUtil.decomposeUci(uci);
    if (uci[1] === '@') this.chessground.newPiece({
      color: this.chessground.state.movable.color,
      role: chessUtil.sanToRole[uci[0]]
    }, move[1]);
    else {
      var capture = this.chessground.state.pieces[move[1]];
      var promotion = move[2] && chessUtil.sanToRole[move[2].toUpperCase()];
      sendMove(move[0], move[1], capture, promotion);
    }
  }.bind(this);

  this.explorerMove = function(uci) {
    this.playUci(uci);
    this.explorer.loading(true);
  }.bind(this);

  this.playBestMove = function() {
    var uci = this.nextNodeBest() || (this.vm.node.ceval && this.vm.node.ceval.pvs[0].moves[0]);
    if (uci) this.playUci(uci);
  }.bind(this);

  this.trans = lichess.trans(opts.i18n);

  var canEvalGet = function(node) {
    return opts.study || node.ply < 10
  }.bind(this);

  this.evalCache;
  var instanciateEvalCache = function() {
    this.evalCache = makeEvalCache({
      variant: this.data.game.variant.key,
      canGet: canEvalGet,
      canPut: function(node) {
        return this.data.evalPut && canEvalGet(node) && (
          // if not in study, only put decent opening moves
          opts.study || (node.ply < 10 && !node.ceval.mate && Math.abs(node.ceval.cp) < 99)
        );
      }.bind(this),
      getNode: function() {
        return this.vm.node;
      }.bind(this),
      send: this.socket.send,
      receive: onNewCeval
    });
  }.bind(this);
  instanciateEvalCache();

  showGround();
  onToggleComputer();
  this.startCeval();
  this.explorer.setNode();
  this.study = opts.study ? makeStudy(opts.study, this, (opts.tagTypes || '').split(','), opts.practice) : null;
  this.studyPractice = this.study ? this.study.practice : null;

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
      this.practice = makePractice(this, function() {
        // push to 20 to store AI moves in the cloud
        // lower to 18 after task completion (or failure)
        return this.studyPractice && this.studyPractice.success() === null ? 20 : 18;
      }.bind(this));
    }
    this.setAutoShapes();
  }.bind(this);

  if (location.hash === '#practice' || (this.study && this.study.data.chapter.practice)) this.togglePractice();

  this.restartPractice = function() {
    this.practice = null;
    this.togglePractice();
  }.bind(this);

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
