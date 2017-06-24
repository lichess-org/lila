import { opposite } from 'chessground/util';
import { Api as ChessgroundApi } from 'chessground/api';
import { Config as ChessgroundConfig } from 'chessground/config';
import { build as makeTree, path as treePath, ops as treeOps } from 'tree';
import * as keyboard from './keyboard';
import { Controller as ActionMenuController } from './actionMenu';
import Autoplay from './autoplay';
import * as promotion from './promotion';
import * as util from './util';
import * as chessUtil from 'chess';
import { storedProp, throttle, defined, StoredBooleanProp } from 'common';
import { make as makeSocket, Socket } from './socket';
import { make as makeForecast, ForecastController } from './forecast/forecastCtrl';
import { ctrl as cevalCtrl, isEvalBetter, CevalController } from 'ceval';
import explorerCtrl from './explorer/explorerCtrl';
import { router, game } from 'game';
import { valid as crazyValid } from './crazy/crazyCtrl';
import * as makeStudy from './study/studyCtrl';
import { make as makeFork, ForkController } from './fork';
import { make as makeRetro, RetroController } from './retrospect/retroCtrl';
import { make as makePractice, PracticeController } from './practice/practiceCtrl';
import { make as makeEvalCache, EvalCache } from './evalCache';
import { compute as computeAutoShapes } from './autoShape';
import { nextGlyphSymbol } from './nodeFinder';
import { AnalyseOpts, AnalyseData, Key } from './interfaces';

export default class AnalyseController {

  opts: AnalyseOpts;
  data: AnalyseData;
  element: HTMLElement;
  redraw: () => void;

  tree: any; // #TODO Tree.Tree
  socket: Socket;
  chessground: ChessgroundApi;
  trans: Trans;
  ceval: CevalController;
  evalCache: EvalCache;

  // current tree state, cursor, and denormalized node lists
  path: Tree.Path;
  node: Tree.Node;
  nodeList: Tree.Node[];
  mainline: Tree.Node[];

  // sub controllers
  actionMenu: ActionMenuController;
  autoplay: Autoplay;
  explorer: any; // #TODO
  forecast?: ForecastController;
  retro?: RetroController;
  fork: ForkController;
  practice?: PracticeController;
  study?: any;
  studyPractice?: any;

  // state flags
  justPlayed: string; // pos
  justDropped: string; // role
  justCaptured: boolean = false;
  autoScrollRequested: boolean = false;
  redirecting: boolean = false;
  onMainline: boolean = true;
  synthetic: boolean; // false if coming from a real game
  ongoing: boolean; // true if real game is ongoing

  // display flags
  flipped: boolean = false;
  embed: boolean;
  showComments: boolean = true; // whether to display comments in the move tree
    showAutoShapes: StoredBooleanProp = storedProp('show-auto-shapes', true);
  showGauge: StoredBooleanProp = storedProp('show-gauge', true);
  showComputer: StoredBooleanProp = storedProp('show-computer', true);
  keyboardHelp: boolean = location.hash === '#keyboard';
  threatMode: boolean = false;

  // other paths
  initialPath: Tree.Path;
  contextMenuPath: Tree.Path;
  gamePath?: Tree.Path;

  // misc
  cgConfig: any; // latest chessground config (useful for revert)
  music?: any;

  constructor(opts: AnalyseOpts, redraw: () => void) {

    this.opts = opts;
    this.element = opts.element;
    this.embed = opts.embed;
    this.redraw = redraw;

    this.trans = window.lichess.trans(opts.i18n);

    if (opts.data.forecast) this.forecast = makeForecast(opts.data.forecast, router.forecasts(this.data), redraw);

    this.instanciateCeval();

    this.instanciateEvalCache();

    this.showGround();
    this.onToggleComputer();
    this.startCeval();
    this.explorer.setNode();
    this.study = opts.study ? makeStudy(opts.study, this, (opts.tagTypes || '').split(','), opts.practice) : null;
    this.studyPractice = this.study ? this.study.practice : null;

    if (location.hash === '#practice' || (this.study && this.study.data.chapter.practice)) this.togglePractice();

    keyboard.bind(this);

    window.lichess.pubsub.on('jump', this.pubsubJump);

    window.lichess.pubsub.on('sound_set', this.pubsubSoundSet);

    this.initialize(opts.data, false);

    this.initialPath = treePath.root;

    if (opts.initialPly) {
      const loc = window.location;
      const locationHash = loc.hash;
      const plyStr = opts.initialPly === 'url' ? (locationHash || '').replace(/#/, '') : opts.initialPly;
      // remove location hash - http://stackoverflow.com/questions/1397329/how-to-remove-the-hash-from-window-location-with-javascript-without-page-refresh/5298684#5298684
      if (locationHash) window.history.pushState("", document.title, loc.pathname + loc.search);
      const mainline = treeOps.mainlineNodeList(this.tree.root);
      if (plyStr === 'last') this.initialPath = treePath.fromNodeList(mainline);
      else {
        var ply = parseInt(plyStr as string);
        if (ply) this.initialPath = treeOps.takePathWhile(mainline, n => n.ply <= ply);
      }
    }

    this.setPath(this.initialPath);
  }

  initialize(data: AnalyseData, merge: boolean): void {
    this.data = data;
    this.synthetic = util.synthetic(data);
    this.ongoing = !this.synthetic && game.playable(data);

    let prevTree = merge && this.tree.root;
    this.tree = makeTree(treeOps.reconstruct(this.data.treeParts));
    if (prevTree) this.tree.merge(prevTree);

    this.actionMenu = new ActionMenuController();
    this.autoplay = new Autoplay(this);
    if (this.socket) this.socket.clearCache();
    else this.socket = makeSocket(this.opts.socketSend, this);
    this.explorer = explorerCtrl(this, this.opts.explorer, this.explorer ? this.explorer.allowed() : !this.embed, this.redraw);
    this.gamePath = (this.synthetic || this.ongoing) ? undefined :
      treePath.fromNodeList(treeOps.mainlineNodeList(this.tree.root));
    this.fork = makeFork(this);
  }

  setPath(path: Tree.Path): void {
    this.path = path;
    this.nodeList = this.tree.getNodeList(path);
    this.node = treeOps.last(this.nodeList);
    this.mainline = treeOps.mainlineNodeList(this.tree.root);
    this.onMainline = this.tree.pathIsMainline(path)
  }

  flip(): void {
    this.flipped = !this.flipped;
    this.chessground.set({
      orientation: this.bottomColor()
    });
    if (this.retro) {
      this.retro = null;
      this.toggleRetro();
    }
    if (this.practice) this.restartPractice();
    redraw();
  }

  topColor(): Color {
    return opposite(this.bottomColor());
  }

  bottomColor(): Color {
    return this.flipped ? opposite(this.data.orientation) : this.data.orientation;
  }

  getOrientation(): Color { // required by ui/ceval
    return this.bottomColor();
  }

  turnColor(): Color {
    return this.node.ply % 2 === 0 ? 'white' : 'black';
  }

  togglePlay(delay: AutoplayDelay): void {
    this.autoplay.toggle(delay);
    this.actionMenu.open = false;
  }

  private uciToLastMove(uci: Uci): [string, string] {
    if (!uci) return;
    if (uci[1] === '@') return [uci.substr(2, 2), uci.substr(2, 2)];
    return [uci.substr(0, 2), uci.substr(2, 2)];
  };

  private showGround(): void {
    onChange();
    if (!defined(this.node.dests)) getDests();
    if (this.chessground) {
      this.chessground.set(this.makeCgOpts());
      this.setAutoShapes();
      if (this.node.shapes) this.chessground.setShapes(this.node.shapes);
    }
  }

  getDests: () => void = throttle(800, false, () => {
    if (!this.embed && !defined(this.dests)) this.socket.sendAnaDests({
      variant: this.data.game.variant.key,
      fen: this.node.fen,
      path: this.path
    });
  });

  makeCgOpts(): ChessgroundConfig {
    const node = this.node;
    const color = this.turnColor();
    const dests = chessUtil.readDests(node.dests);
    const drops = chessUtil.readDrops(node.drops);
    const movableColor = this.practice ? this.bottomColor() : (
      !this.embed && (
        (dests && Object.keys(dests).length > 0) ||
        drops === null || drops.length
      ) ? color : null);
    const config = {
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
    this.cgConfig = config;
    return config;
  }

  private sound: any = window.lichess.sound ? {
    move: throttle(50, false, window.lichess.sound.move),
    capture: throttle(50, false, window.lichess.sound.capture),
    check: throttle(50, false, window.lichess.sound.check)
  } : {
    move: $.noop,
    capture: $.noop,
    check: $.noop
  };

  private onChange: () => void = opts.onChange ? throttle(300, false, function() {
    const mainlinePly = this.onMainline ? this.node.ply : false;
    opts.onChange(this.node.fen, this.path, mainlinePly);
  }) : $.noop;

  private updateHref: () => void = opts.study ? $.noop : throttle(750, false, function() {
    window.history.replaceState(null, null, '#' + this.node.ply);
  }, false);

  autoScroll(): void {
    this.autoScrollRequested = true;
  }

  jump(path: Tree.Path): void {
    const pathChanged = path !== this.path;
    this.setPath(path);
    showGround();
    if (pathChanged) {
      if (this.study) this.study.setPath(path, this.node);
      if (!this.node.uci) sound.move(); // initial position
      else if (this.node.uci.indexOf(this.justPlayed) !== 0) {
        if (this.node.san.indexOf('x') !== -1) sound.capture();
        else sound.move();
      }
      if (/\+|\#/.test(this.node.san)) sound.check();
      this.threatMode = false;
      this.ceval.stop();
      this.startCeval();
    }
    this.justPlayed = null;
    this.justDropped = null;
    this.justCaptured = null;
    this.explorer.setNode();
    updateHref();
    this.autoScroll();
    promotion.cancel(this);
    if (pathChanged) {
      if (this.retro) this.retro.onJump();
      if (this.practice) this.practice.onJump();
      if (this.study) this.study.onJump();
    }
    if (this.music) this.music.jump(this.node);
  }

  userJump(path: Tree.Path): void {
    this.autoplay.stop();
    if (this.chessground) this.chessground.selectSquare(null);
    if (this.practice) {
      const prev = this.path;
      this.practice.preUserJump(prev, path);
      this.jump(path);
      this.practice.postUserJump(prev, this.path);
    } else {
      this.jump(path);
    }
  }

  private canJumpTo(path: Tree.Path): boolean {
    return !this.study || this.study.canJumpTo(path);
  }

  userJumpIfCan(path: Tree.Path): void {
    if (canJumpTo(path)) this.userJump(path);
  }

  mainlinePathToPly(ply: Ply): Tree.Path {
    return treeOps.takePathWhile(this.mainline, n => n.ply <= ply);
  }

  jumpToMain = (ply: Ply): void => {
    this.userJump(this.mainlinePathToPly(ply));
  }

  jumpToIndex(index: number): void {
    this.jumpToMain(index + 1 + this.data.game.startedAtTurn);
  }

  jumpToGlyphSymbol(color: Color, symbol: string): void {
    const node = nextGlyphSymbol(color, symbol, this.mainline, this.node.ply);
    if (node) this.jumpToMain(node.ply);
    redraw();
  }

  reloadData(data: AnalyseData, merge: boolean): void {
    initialize(data, merge);
    this.redirecting = false;
    this.setPath(treePath.root);
    this.instanciateCeval();
    this.instanciateEvalCache();
  }

  changePgn(pgn: string): void {
    this.redirecting = true;
    $.ajax({
      url: '/analysis/pgn',
      method: 'post',
      data: { pgn },
      success: function(data) {
        this.reloadData(data);
        this.userJump(this.mainlinePathToPly(this.tree.lastPly()));
      }.bind(this),
      error: function(error) {
        console.log(error);
        this.redirecting = false;
        redraw();
      }.bind(this)
    });
  }

  changeFen(fen: Fen): string {
    this.redirecting = true;
    window.location = '/analysis/' + this.data.game.variant.key + '/' + encodeURIComponent(fen).replace(/%20/g, '_').replace(/%2F/g, '/');
  }

  userNewPiece(piece: Piece, pos: Key) {
    if (crazyValid(this.chessground, this.node.drops, piece, pos)) {
      this.justPlayed = chessUtil.roleToSan[piece.role] + '@' + pos;
      this.justDropped = piece.role;
      this.justCaptured = null;
      sound.move();
      const drop = {
        role: piece.role,
        pos: pos,
        variant: this.data.game.variant.key,
        fen: this.node.fen,
        path: this.path
      };
      this.socket.sendAnaDrop(drop);
      preparePremoving();
      redraw();
    } else this.jump(this.path);
  }

  userMove(orig: Key, dest: Key, capture: boolean): void {
    this.justPlayed = orig;
    this.justDropped = null;
    sound[capture ? 'capture' : 'move']();
    if (!promotion.start(this, orig, dest, capture, sendMove)) sendMove(orig, dest, capture);
  }

  sendMove(orig: Key, dest: Key, capture: boolean, prom: Role): void {
    var move = {
      orig: orig,
      dest: dest,
      variant: this.data.game.variant.key,
      fen: this.node.fen,
      path: this.path
    };
    if (capture) this.justCaptured = capture;
    if (prom) move.promotion = prom;
    if (this.practice) this.practice.onUserMove();
    this.socket.sendAnaMove(move);
    preparePremoving();
    redraw();
  }

  private preparePremoving(): void {
    this.chessground.set({
      turnColor: this.chessground.state.movable.color,
      movable: {
        color: opposite(this.chessground.state.movable.color)
      },
      premovable: {
        enabled: true
      }
    });
  }

  addNode(node: Node, path: Tree.Path) {
    const newPath = this.tree.addNode(node, path);
    if (!newPath) {
      console.log('Cannot addNode', node, path);
      return redraw();
    }
    this.jump(newPath);
    redraw();
    this.chessground.playPremove();
  }

  addDests(dests: string, path: Tree.Path, opening?: Tree.Opening): void {
    this.tree.addDests(dests, path, opening);
    if (path === this.path) {
      showGround();
      redraw();
      if (this.gameOver()) this.ceval.stop();
    }
    if (this.chessground) this.chessground.playPremove();
  }

  deleteNode(path: Tree.Path): void {
    const node = this.tree.nodeAtPath(path);
    if (!node) return;
    const count = treeOps.countChildrenAndComments(node);
    if ((count.nodes >= 10 || count.comments > 0) && !confirm(
      'Delete ' + util.plural('move', count.nodes) + (count.comments ? ' and ' + util.plural('comment', count.comments) : '') + '?'
    )) return;
    this.tree.deleteNodeAt(path);
    if (treePath.contains(this.path, path)) this.userJump(treePath.init(path));
    else this.jump(this.path);
    if (this.study) this.study.deleteNode(path);
  }

  promote(path: tree.Path, toMainline: boolean): void {
    this.tree.promoteAt(path, toMainline);
    this.jump(path);
    if (this.study) this.study.promote(path, toMainline);
  }

  reset(): void {
    showGround();
    redraw();
  }

  encodeNodeFen(): Fen {
    return this.node.fen.replace(/\s/g, '_');
  }

  currentEvals() {
    return {
      server: this.node.eval,
      client: this.node.ceval
    };
  }

  nextNodeBest() {
    return treeOps.withMainlineChild(this.node, function(n) {
      return n.eval ? n.eval.best : null;
    });
  }

  setAutoShapes(): void {
    if (this.chessground) this.chessground.setAutoShapes(computeAutoShapes(this));
  }

  private onNewCeval(ev, path, threatMode): void {
    this.tree.updateAt(path, function(node) {
      if (node.fen !== ev.fen && !threatMode) return;
      if (threatMode) {
        if (!node.threat || isEvalBetter(ev, node.threat) || node.threat.maxDepth < ev.maxDepth)
        node.threat = ev;
      } else if (isEvalBetter(ev, node.ceval)) node.ceval = ev;
      else if (node.ceval && ev.maxDepth > node.ceval.maxDepth) node.ceval.maxDepth = ev.maxDepth;

      if (path === this.path) {
        this.setAutoShapes();
        if (!threatMode) {
          if (this.retro) this.retro.onCeval();
          if (this.practice) this.practice.onCeval();
          if (this.studyPractice) this.studyPractice.onCeval();
          this.evalCache.onCeval();
          if (ev.cloud && ev.depth >= this.ceval.effectiveMaxDepth()) this.ceval.stop();
        }
        redraw();
      }
    }.bind(this));
  }

  private instanciateCeval(failsafe: boolean = false): void {
    if (this.ceval) this.ceval.destroy();
    const cfg = {
      variant: this.data.game.variant,
      possible: !this.embed && (
        this.synthetic || !game.playable(this.data)
      ),
      emit: function(ev, work) {
        this.onNewCeval(ev, work.path, work.threatMode);
      }.bind(this),
      setAutoShapes: this.setAutoShapes,
      failsafe: failsafe,
      onCrash: function(lastError) {
        const ceval = this.node.ceval;
        console.log('Local eval failed after depth ' + (ceval && ceval.depth));
        if (this.ceval.pnaclSupported) {
          if (ceval && ceval.depth >= 20 && !ceval.retried) {
            console.log('Remain on native stockfish for now');
            ceval.retried = true;
          } else {
            console.log('Fallback to ASMJS now');
            this.instanciateCeval(true);
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
  }

  getCeval() {
    return this.ceval;
  }

  gameOver(node?: Tree.Node): 'draw' | checkmate | false {
    const n = node || this.node;
    if (n.dests !== '' || n.drops) return false;
    if (n.check) return 'checkmate';
    return 'draw';
  }

  canUseCeval(): boolean {
    return !this.gameOver() && !this.node.threefold;
  }

  startCeval = throttle(800, false, function() {
    if (this.ceval.enabled()) {
      if (canUseCeval()) {
        this.ceval.start(this.path, this.nodeList, this.threatMode);
        this.evalCache.fetch(this.path, parseInt(this.ceval.multiPv()));
      } else this.ceval.stop();
    }
  }.bind(this));

  toggleCeval(): void {
    this.ceval.toggle();
    this.setAutoShapes();
    this.startCeval();
    if (!this.ceval.enabled()) {
      this.threatMode = false;
      if (this.practice) this.togglePractice();
    }
    redraw();
  }

  toggleThreatMode(): void {
    if (this.node.check) return;
    if (!this.ceval.enabled()) this.ceval.toggle();
    if (!this.ceval.enabled()) return;
    this.threatMode = !this.threatMode;
    if (this.threatMode && this.practice) this.togglePractice();
    this.setAutoShapes();
    this.startCeval();
    redraw();
  }

  disableThreatMode(): boolean {
    return !!this.practice;
  }

  mandatoryCeval(): boolean {
    return !!this.studyPractice;
  }

  private cevalReset(): void {
    this.ceval.stop();
    if (!this.ceval.enabled()) this.ceval.toggle();
    this.startCeval();
    redraw();
  }

  cevalSetMultiPv(v: number): void {
    this.ceval.multiPv(v);
    this.tree.removeCeval();
    cevalReset();
  }

  cevalSetThreads(v: number): void {
    this.ceval.threads(v);
    cevalReset();
  }

  cevalSetHashSize(v: number): void {
    this.ceval.hashSize(v);
    cevalReset();
  }

  cevalSetInfinite(v: boolean): void {
    this.ceval.infinite(v);
    cevalReset();
  }

  showEvalGauge(): boolean {
    return this.hasAnyComputerAnalysis() && this.showGauge() && !this.gameOver() && this.showComputer();
  }

  hasAnyComputerAnalysis(): boolean {
    return this.data.analysis || this.ceval.enabled();
  }

  hasFullComputerAnalysis(): boolean {
    return this.mainline[0].eval && Object.keys(this.mainline[0].eval).length;
  }

  private resetAutoShapes() {
    if (this.showAutoShapes()) this.setAutoShapes();
    else this.chessground && this.chessground.setAutoShapes([]);
  }

  toggleAutoShapes(v: boolean) {
    this.showAutoShapes(v);
    resetAutoShapes();
  }

  toggleGauge() {
    this.showGauge(!this.showGauge());
  }

  private onToggleComputer() {
    if (!this.showComputer()) {
      this.tree.removeComputerVariations();
      if (this.ceval.enabled()) this.toggleCeval();
      this.chessground && this.chessground.setAutoShapes([]);
    } else resetAutoShapes();
  }

  toggleComputer() {
    var value = !this.showComputer();
    this.showComputer(value);
    if (!value && this.practice) this.togglePractice();
    if (opts.onToggleComputer) opts.onToggleComputer(value);
    onToggleComputer();
  }

  mergeAnalysisData(data: AnalyseData): void {
    this.tree.merge(data.tree);
    if (!this.showComputer()) this.tree.removeComputerVariations();
    this.data.analysis = data.analysis;
    if (this.retro) this.retro.onMergeAnalysisData();
    redraw();
  }

  playUci(uci: Uci): void {
    const move = chessUtil.decomposeUci(uci);
    if (uci[1] === '@') this.chessground.newPiece({
      color: this.chessground.state.movable.color,
      role: chessUtil.sanToRole[uci[0]]
    }, move[1]);
    else {
      const capture = this.chessground.state.pieces[move[1]];
      const promotion = move[2] && chessUtil.sanToRole[move[2].toUpperCase()];
      sendMove(move[0], move[1], capture, promotion);
    }
  }

  explorerMove(uci: Uci) {
    this.playUci(uci);
    this.explorer.loading(true);
  }

  playBestMove() {
    const uci = this.nextNodeBest() || (this.node.ceval && this.node.ceval.pvs[0].moves[0]);
    if (uci) this.playUci(uci);
  }

  canEvalGet(node: Node): boolean {
    return opts.study || node.ply < 10
  }

  instanciateEvalCache() {
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
        return this.node;
      }.bind(this),
      send: this.socket.send,
      receive: onNewCeval
    });
  }

  toggleRetro(): void {
    if (this.retro) this.retro = null;
    else {
      this.retro = makeRetro(this);
      if (this.practice) this.togglePractice();
      if (this.explorer.enabled()) this.toggleExplorer();
    }
    this.setAutoShapes();
  }

  toggleExplorer(): void {
    if (this.practice) this.togglePractice();
    this.explorer.toggle();
  }

  togglePractice() {
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
  };

  restartPractice() {
    this.practice = null;
    this.togglePractice();
  }

  private pubsubJump = (ply: any) => {
    this.jumpToMain(parseInt(ply));
    redraw();
  }

  private pubsubSoundSet = (set: string) => {
    if (!this.music && set === 'music')
      window.lichess.loadScript('/assets/javascripts/music/replay.js').then(function() {
        this.music = lichessReplayMusic();
      }.bind(this));
    if (this.music && set !== 'music') this.music = null;
  }
};
