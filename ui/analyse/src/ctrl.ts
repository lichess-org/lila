import { opposite } from 'draughtsground/util';
import { countGhosts } from 'draughtsground/fen';
import { Api as DraughtsgroundApi } from 'draughtsground/api';
import { DrawShape } from 'draughtsground/draw';
import * as cg from 'draughtsground/types';
import { Config as DraughtsgroundConfig } from 'draughtsground/config';
import { build as makeTree, path as treePath, ops as treeOps, TreeWrapper } from 'tree';
import * as keyboard from './keyboard';
import { Ctrl as ActionMenuCtrl } from './actionMenu';
import { Autoplay, AutoplayDelay } from './autoplay';
import * as util from './util';
import * as draughtsUtil from 'draughts';
import { defined, prop, Prop } from 'common';
import throttle from 'common/throttle';
import { storedProp, StoredBooleanProp } from 'common/storage';
import { make as makeSocket, Socket } from './socket';
import { ForecastCtrl } from './forecast/interfaces';
import { make as makeForecast } from './forecast/forecastCtrl';
import { ctrl as cevalCtrl, isEvalBetter, CevalCtrl, Work as CevalWork, CevalOpts, scan2uci } from 'ceval';
import explorerCtrl from './explorer/explorerCtrl';
import { ExplorerCtrl } from './explorer/interfaces';
import * as game from 'game';
import makeStudy from './study/studyCtrl';
import { StudyCtrl } from './study/interfaces';
import { StudyPracticeCtrl } from './study/practice/interfaces';
import { make as makeFork, ForkCtrl } from './fork';
import { make as makeRetro, RetroCtrl } from './retrospect/retroCtrl';
import { make as makePractice, PracticeCtrl } from './practice/practiceCtrl';
import { make as makeEvalCache, EvalCache } from './evalCache';
import { compute as computeAutoShapes } from './autoShape';
import { getCompChild, nextGlyphSymbol } from './nodeFinder';
import * as speech from './speech';
import { AnalyseOpts, AnalyseData, ServerEvalData, Key, DgDests, JustCaptured, NvuiPlugin, Redraw } from './interfaces';
import GamebookPlayCtrl from './study/gamebook/gamebookPlayCtrl';
import { calcDests } from './study/gamebook/gamebookEmbed';
import { ctrl as treeViewCtrl, TreeView, findCurrentPath } from './treeView/treeView';

const li = window.lidraughts;

export default class AnalyseCtrl {

  data: AnalyseData;
  element: HTMLElement;

  tree: TreeWrapper;
  socket: Socket;
  draughtsground: DraughtsgroundApi;
  trans: Trans;
  ceval: CevalCtrl;
  evalCache: EvalCache;

  // current tree state, cursor, and denormalized node lists
  path: Tree.Path;
  node: Tree.Node;
  nodeList: Tree.Node[];
  mainline: Tree.Node[];

  // sub controllers
  actionMenu: ActionMenuCtrl;
  autoplay: Autoplay;
  explorer: ExplorerCtrl;
  forecast?: ForecastCtrl;
  retro?: RetroCtrl;
  fork: ForkCtrl;
  practice?: PracticeCtrl;
  study?: StudyCtrl;
  studyPractice?: StudyPracticeCtrl;

  // state flags
  justPlayed?: string; // pos
  justDropped?: string; // role
  justCaptured?: JustCaptured;
  autoScrollRequested: boolean = false;
  redirecting: boolean = false;
  onMainline: boolean = true;
  synthetic: boolean; // false if coming from a real game
  ongoing: boolean; // true if real game is ongoing

  // display flags
  flipped: boolean;
  embed: boolean;
  showComments: boolean = true; // whether to display comments in the move tree
  showAutoShapes: StoredBooleanProp = storedProp('show-auto-shapes', true);
  showGauge: StoredBooleanProp = storedProp('show-gauge', true);
  showComputer: StoredBooleanProp = storedProp('show-computer', true);
  keyboardHelp: boolean = location.hash === '#keyboard';
  threatMode: Prop<boolean> = prop(false);
  treeView: TreeView;
  cgVersion = {
    js: 1, // increment to recreate draughtsground
    dom: 1
  };

  // other paths
  initialPath: Tree.Path;
  contextMenuPath?: Tree.Path;
  gamePath?: Tree.Path;

  // misc
  cgConfig: any; // latest draughtsground config (useful for revert)
  music?: any;
  nvui?: NvuiPlugin;
  initDests: boolean; // set to true when dests have been loaded on init

  constructor(readonly opts: AnalyseOpts, readonly redraw: Redraw) {

    this.data = opts.data;
    this.element = opts.element;
    this.embed = opts.embed;
    this.trans = opts.trans;
    this.treeView = treeViewCtrl(opts.embed ? 'inline' : 'column');
    this.flipped = this.data.puzzleEditor;

    if (this.data.forecast) this.forecast = makeForecast(this.data.forecast, this.data, redraw);

    if (li.AnalyseNVUI) this.nvui = li.AnalyseNVUI(redraw) as NvuiPlugin;

    this.instanciateEvalCache();

    this.initialize(this.data, false);

    this.instanciateCeval();

    this.initialPath = treePath.root;

    if (opts.initialPly) {
      const loc = window.location,
        intHash = loc.hash === '#last' ? this.tree.lastPly(true) : parseInt(loc.hash.substr(1)),
        plyStr = opts.initialPly === 'url' ? (intHash || '') : opts.initialPly;
      // remove location hash - http://stackoverflow.com/questions/1397329/how-to-remove-the-hash-from-window-location-with-javascript-without-page-refresh/5298684#5298684
      if (intHash) window.history.pushState("", document.title, loc.pathname + loc.search);
      const mainline = treeOps.mainlineNodeList(this.tree.root);
      if (plyStr === 'last') this.initialPath = treePath.fromNodeList(mainline);
      else {
        const ply = parseInt(plyStr as string);
        if (ply) this.initialPath = treeOps.takePathWhile(mainline, n => (n.displayPly ? n.displayPly : n.ply) <= ply);
      }
    }

    this.setPath(this.initialPath);

    if (this.forecast && this.data.game.turns) {
      if (!this.initialPath) {
        this.initialPath = treeOps.takePathWhile(this.mainline, n => n.ply <= this.data.game.turns);
      }
      const gameNodeList = this.tree.getNodeList(this.initialPath),
        skipNodes = this.tree.getCurrentNodesAfterPly(gameNodeList, this.mainline, this.data.game.turns);
      let skipSteps = 0;
      for (let skipNode of skipNodes) {
        skipSteps += skipNode.uci ? (skipNode.uci.length - 2) / 2 : 1;
      }
      this.forecast.skipSteps = skipSteps;
    }

    this.study = opts.study ? makeStudy(opts.study, this, (opts.tagTypes || '').split(','), opts.practice, opts.relay) : undefined;
    this.studyPractice = this.study ? this.study.practice : undefined;

    this.showGround(undefined, this.initDests);
    this.onToggleComputer();
    this.startCeval();
    this.explorer.setNode();

    if (location.hash === '#practice' || (this.study && this.study.data.chapter.practice)) this.togglePractice();
    else if (location.hash === '#menu') li.requestIdleCallback(this.actionMenu.toggle);

    keyboard.bind(this);

    li.pubsub.on('jump', (ply: any) => {
      this.jumpToMain(parseInt(ply));
      this.redraw();
    });

    li.pubsub.on('sound_set', (set: string) => {
      if (!this.music && set === 'music')
        li.loadScript('javascripts/music/replay.js').then(() => {
          this.music = window.lidraughtsReplayMusic();
        });
      if (this.music && set !== 'music') this.music = null;
    });

    li.pubsub.on('analysis.change.trigger', this.onChange);
    li.pubsub.on('analysis.chart.click', index => {
      this.jumpToIndex(index);
      this.redraw()
    });

    li.sound && speech.setup();
  }

  initialize(data: AnalyseData, merge: boolean): void {
    this.data = data;
    this.synthetic = data.game.id === 'synthetic';
    this.ongoing = !this.synthetic && game.playable(data);

    const prevTree = merge && this.tree.root;
    this.tree = makeTree(treeOps.reconstruct(this.data.treeParts, this.coordSystem()));
    if (prevTree) this.tree.merge(prevTree);

    this.actionMenu = new ActionMenuCtrl();
    this.autoplay = new Autoplay(this);
    if (this.socket) this.socket.clearCache();
    else this.socket = makeSocket(this.opts.socketSend, this);
    this.explorer = explorerCtrl(this, this.opts.explorer, this.explorer ? this.explorer.allowed() : !this.embed);
    this.gamePath = this.synthetic || this.ongoing ? undefined :
      treePath.fromNodeList(treeOps.mainlineNodeList(this.tree.root));
    this.fork = makeFork(this);
  }

  private setPath = (path: Tree.Path): void => {
    this.path = path;
    this.nodeList = this.tree.getNodeList(path);
    this.node = treeOps.last(this.nodeList) as Tree.Node;
    this.mainline = treeOps.mainlineNodeList(this.tree.root);
    this.onMainline = this.tree.pathIsMainline(path)
  }

  setBookmark = (index: number) => {
    const bookmarkData = this.study ? [this.study.data.id, this.study.currentChapter().id] : [];
    li.storage.set('analysis.bookmark.' + index, [this.path].concat(bookmarkData).join(' '));
  }

  restoreBookmark = (index: number) => {
    var bookmarkStorage = li.storage.make('analysis.bookmark.' + index), 
      storedBookmark = bookmarkStorage.get();
    if (!storedBookmark) return;
    const pieces = storedBookmark.split(' ');
    if (this.study && pieces.length > 2 && 
      this.study.data.id === pieces[1] && 
      this.study.currentChapter().id !== pieces[2] && 
      this.study.chapters.get(pieces[2])) {
      this.study.setChapter(pieces[2], false, pieces[0]);
    } else if (this.tree.longestValidPath(pieces[0]) === pieces[0]) {
      // only jump when entire path exists
      if (this.canJumpTo(pieces[0])) {
        this.userJump(pieces[0]);
        this.redraw();
      }
    }
  }

  flip = () => {
    this.flipped = !this.flipped;
    this.draughtsground.set({
      orientation: this.bottomColor()
    });
    if (this.retro) {
      this.retro = undefined;
      this.toggleRetro();
    }
    if (this.practice) this.restartPractice();
    this.redraw();
  }

  topColor(): Color {
    return opposite(this.bottomColor());
  }

  bottomColor(): Color {
    return this.flipped ? opposite(this.data.orientation) : this.data.orientation;
  }

  bottomIsWhite = () => this.bottomColor() === 'white';

  getOrientation(): Color { // required by ui/ceval
    return this.bottomColor();
  }
  getNode(): Tree.Node { // required by ui/ceval
    return this.node;
  }
  getCevalNode(): Tree.Node { // required by ui/ceval
    return (this.nodeList.length > 1 && this.node.displayPly && this.node.displayPly !== this.node.ply) ? this.nodeList[this.nodeList.length - 2] : this.node;
  }

  turnColor(): Color {
    return util.plyColor(this.node.ply);
  }

  togglePlay(delay: AutoplayDelay): void {
    this.autoplay.toggle(delay);
    this.actionMenu.open = false;
  }

  private uciToLastMove(uci?: Uci): Key[] | undefined {
    if (!uci) return;
    else return draughtsUtil.decomposeUci(uci);
  };

  private missingFullCaptureDests(): Boolean {
    return (this.node.captLen && this.node.captLen > 1 && this.data.pref.fullCapture && !defined(this.node.destsUci))
  }

  private missingDests(): Boolean {
    return !defined(this.node.dests) || this.missingFullCaptureDests();
  }

  private showGround(noCaptSequences: boolean = false, ignoreDests: boolean = false): void {
    this.onChange();
    if (!ignoreDests && this.missingDests()) this.getDests();
    this.withCg(cg => {
      const cgOps = this.makeCgOpts();
      cg.set(cgOps, noCaptSequences);
      this.setAutoShapes();
      if (this.node.shapes) cg.setShapes(this.node.shapes as DrawShape[]);
    });
  }

  getDests: () => void = throttle(800, () => {
    const gamebook = this.gamebookPlay();
    if (this.embed && gamebook && !defined(this.node.dests)) {
      let dests = calcDests(this.node.fen, this.data.game.variant);
      if (dests.length > 1 && dests[0] === '#') {
        const nextUci = gamebook.nextUci();
        if (nextUci && nextUci.length >= 4) {
          this.node.captLen = nextUci.length / 2 - 1;
          dests = "#" + this.node.captLen.toString() + dests.substr(2);
        }
      }
      this.addDests(dests, this.path);
    } else if (!this.embed && this.missingDests() && (this.node.destreq || 0) < 3) {
      if (defined(this.node.dests) && this.missingFullCaptureDests()) {
        this.node.dests = undefined; // prevent temporarily showing wrong dests
      }
      const dests: any = {
        variant: this.data.game.variant.key,
        fen: this.node.fen,
        path: this.path
      }
      if (this.data.pref.fullCapture) dests.fullCapture = true;
      this.socket.sendAnaDests(dests, this.data.puzzleEditor);
      this.node.destreq = (this.node.destreq || 0) + 1;
      this.initDests = true;
    }
  });

  makeCgOpts(): DraughtsgroundConfig {
    const node = this.node,
      dests = draughtsUtil.readDests(this.node.dests),
      drops = draughtsUtil.readDrops(this.node.drops),
      captLen = draughtsUtil.readCaptureLength(this.node.dests) || this.node.captLen,
      color = this.turnColor(),
      movableColor = (this.practice || this.gamebookPlay()) ? this.bottomColor() : (
        !this.embed && (
          (dests && Object.keys(dests).length > 0) ||
          drops === null || drops.length
        ) ? color : undefined),
      config: DraughtsgroundConfig = {
        fen: node.fen,
        turnColor: color,
        captureLength: captLen,
        movable: (this.embed && !this.gamebookPlay()) ? {
          color: undefined,
          dests: {} as DgDests
        } : {
            color: movableColor,
            dests: (movableColor === color ? (dests || {}) : {}) as DgDests,
            captureUci: (this.data.pref.fullCapture && this.node.destsUci && this.node.destsUci.length) ? this.node.destsUci : undefined
          },
        lastMove: this.uciToLastMove(node.uci),
      };
    if (!dests && !node.check) {
      // premove while dests are loading from server
      // can't use when in check because it highlights the wrong king
      config.turnColor = opposite(color);
      config.movable!.color = color;
    }
    config.premovable = {
      enabled: config.movable!.color && config.turnColor !== config.movable!.color
    };
    this.cgConfig = config;
    return config;
  }

  private sound = li.sound ? {
    move: throttle(50, li.sound.move),
    capture: throttle(50, li.sound.capture),
    check: throttle(50, li.sound.check)
  } : {
      move: $.noop,
      capture: $.noop,
      check: $.noop
    };

  private onChange: () => void = throttle(300, () => {
    li.pubsub.emit('analysis.change', this.node.fen, this.path, this.onMainline ? (this.node.displayPly ? this.node.displayPly : this.node.ply) : false);
  });

  private updateHref: () => void = li.debounce(() => {
    if (!this.opts.study) window.history.replaceState(null, '', '#' + this.node.ply);
  }, 750);

  autoScroll(): void {
    this.autoScrollRequested = true;
  }

  playedLastMoveMyself = () =>
    !!this.justPlayed && !!this.node.uci && this.node.uci.substr(this.node.uci.length - 4, 2) === this.justPlayed;

  jump(path: Tree.Path): void {
    const pathChanged = path !== this.path;
    const oldPly = this.node.displayPly ? this.node.displayPly : this.node.ply;
    this.setPath(path);
    this.showGround(Math.abs(oldPly - (this.node.displayPly ? this.node.displayPly : this.node.ply)) > 1);
    if (pathChanged) {
      const playedMyself = this.playedLastMoveMyself();
      if (this.study) this.study.setPath(path, this.node, playedMyself);
      if (!this.node.uci) this.sound.move(); // initial position
      else if (!playedMyself) {
        if (this.node.san!.includes('x')) this.sound.capture();
        else this.sound.move();
      }
      this.threatMode(false);
      this.ceval.stop();
      this.startCeval();
      const mergedNodes = this.node.mergedNodes,
        prevSan = playedMyself && mergedNodes && mergedNodes.length > 1 && mergedNodes[mergedNodes.length - 2].san,
        captSan = prevSan ? prevSan.indexOf('x') : -1,
        captKey = (prevSan && captSan !== -1) ? prevSan.slice(captSan + 1) as Key : undefined;
      speech.node(this.node, captKey);
    }
    this.justPlayed = this.justDropped = this.justCaptured = undefined;
    this.explorer.setNode();
    this.updateHref();
    this.autoScroll();
    if (pathChanged) {
      if (this.retro) this.retro.onJump();
      if (this.practice) this.practice.onJump();
      if (this.study) this.study.onJump();
    }
    if (this.music) this.music.jump(this.node);
    li.pubsub.emit('ply', this.node.ply);
  }

  userJump = (path: Tree.Path): void => {

    this.autoplay.stop();
    this.withCg(cg => cg.selectSquare(null));
    if (this.practice) {
      const prev = this.path;
      this.practice.preUserJump(prev, path);
      this.jump(path);
      this.practice.postUserJump(prev, this.path);
    } else this.jump(path);
  }

  jumpToCurrentPath = () => {
    var currentPath = findCurrentPath(this);
    if (currentPath && this.canJumpTo(currentPath)) {
      this.userJump(currentPath);
      this.redraw();
    }
  }

  private canJumpTo(path: Tree.Path): boolean {
    return !this.study || this.study.canJumpTo(path);
  }

  userJumpIfCan(path: Tree.Path): void {
    if (this.canJumpTo(path)) this.userJump(path);
  }

  mainlinePathToPly(ply: Ply): Tree.Path {
    return treeOps.takePathWhile(this.mainline, n => (n.displayPly ? n.displayPly : n.ply) <= ply);
  }

  jumpToMain = (ply: Ply): void => {
    this.userJump(this.mainlinePathToPly(ply));
  }

  jumpToIndex = (index: number): void => {
    this.jumpToMain(index + 1 + this.tree.root.ply);
  }

  jumpToGlyphSymbol(color: Color, symbol: string): void {
    const node = nextGlyphSymbol(color, symbol, this.mainline, this.node.ply);
    if (node) this.jumpToMain(node.ply);
    this.redraw();
  }

  reloadData(data: AnalyseData, merge: boolean): void {
    if (data.puzzleEditor === undefined)
      data.puzzleEditor = this.data.puzzleEditor;
    this.initialize(data, merge);
    this.redirecting = false;
    this.setPath(treePath.root);
    this.instanciateCeval();
    this.instanciateEvalCache();
    this.cgVersion.js++;
  }

  changePdn(pdn: string): void {
    this.redirecting = true;
    $.ajax({
      url: '/analysis/pdn',
      method: 'post',
      data: { pdn },
      success: (data: AnalyseData) => {
        this.reloadData(data, false);
        this.userJump(this.mainlinePathToPly(this.tree.lastPly()));
        this.redraw();
      },
      error: error => {
        console.log(error);
        this.redirecting = false;
        this.redraw();
      }
    });
  }

  isAlgebraic(): boolean {
    return this.data.pref.coordSystem === 1 && this.data.game.variant.board.key === '64';
  }

  coordSystem(): number {
    return this.isAlgebraic() ? 1 : 0;
  }

  changeFen(fen: Fen): void {
    this.redirecting = true;
    const cleanFen = draughtsUtil.fenFromTag(fen);
    window.location.href = '/analysis/' + (this.data.puzzleEditor ? 'puzzle/' : '') + this.data.game.variant.key + '/' + encodeURIComponent(cleanFen).replace(/%20/g, '_').replace(/%2F/g, '/');
  }

  userMove = (orig: Key, dest: Key, capture?: JustCaptured): void => {
    this.justDropped = undefined;
    this.sound[capture ? 'capture' : 'move']();
    if (!this.embed && this.data.pref.fullCapture && this.node.destsUci) {
      const uci = this.node.destsUci.find(u => u.slice(0, 2) === orig && u.slice(-2) === dest)
      if (uci) {
        this.justPlayed = uci.substr(uci.length - 4, 2);
        this.sendMove(orig, dest, capture, uci);
        return;    
      }
    }
    this.justPlayed = orig;
    this.sendMove(orig, dest, capture);
  }

  private moreCaptures(boardFen: string): boolean {
     const fenParts = this.node.fen.split(':');
     var fen = this.node.fen[0] + ":" + boardFen;
     if (fenParts.length > 3) fen += ":" + fenParts.slice(3).join(':');
     const dests = calcDests(fen, this.data.game.variant);
     return dests.length > 1 && dests[0] === "#";
  }

  private gamebookMove(orig: Key, dest: Key, gamebook: GamebookPlayCtrl, capture?: JustCaptured): void {
    const key2id = (key: Key) => String.fromCharCode(35 + parseInt(key) - 1),
      ghosts = countGhosts(this.node.fen),
      uci: string = (ghosts == 0 || !this.node.uci) ? (orig + dest) : (this.node.uci + dest),
      boardFen = this.draughtsground.getFen(),
      continueCapture = capture && this.moreCaptures(boardFen),
      nextColor = continueCapture ? this.node.fen[0] : (this.node.fen[0] == 'W' ? 'B' : 'W'),
      nextFen = nextColor + ":" +  boardFen;

    let treeNode = continueCapture ? (ghosts === 0 ? gamebook.peekChild() : this.node) : gamebook.tryJump(uci, nextFen);
    if (treeNode) {
      if (this.node.captLen && (ghosts != 0 || this.node.captLen > 1)) {
        treeNode = treeOps.copyNode(treeNode);
        treeNode.uci = uci.substr(uci.length - 4);
        if (treeNode.san) {
          const capt = treeNode.san.indexOf('x');
          if (capt !== -1) {
            if (!ghosts)
              treeNode.san = treeNode.san.substr(0, capt + 1) + parseInt(uci.substr(uci.length - 2, 2)).toString();
            else
              treeNode.san = treeNode.san.substr(capt + 1) + "x" + parseInt(uci.substr(uci.length - 2, 2)).toString();
          }
        }
        if (!ghosts) {
          treeNode.id = treeNode.id.substr(0, 1) + key2id(dest);
          treeNode.comments = undefined;
        } else
          treeNode.id = treeNode.id.substr(1, 1) + key2id(dest);
        treeNode.captLen = this.node.captLen - 1;
        if (!treeNode.captLen) {
          treeNode.ply = this.node.ply + 1;
          treeNode.displayPly = treeNode.ply;
        } else {
          treeNode.ply = this.node.ply;
          treeNode.displayPly = treeNode.ply + 1;
          treeNode.shapes = undefined;
        }
        const sideToMove = treeNode.captLen ? this.node.fen[0] : (this.node.fen[0] == 'W' ? 'B' : 'W');
        const fenParts = treeNode.fen.split(':');
        treeNode.fen = sideToMove + ":" + boardFen;
        if (fenParts.length > 3)
          treeNode.fen += ":" + fenParts.slice(3).join(':');
        if (treeNode.captLen > 0) {
          treeNode.dests = calcDests(treeNode.fen, this.data.game.variant);
          treeNode.dests = "#" + treeNode.captLen.toString() + treeNode.dests.substr(2);
        } else
          treeNode.dests = undefined;
      }
      if (!treeNode.dests) {
        treeNode.dests = calcDests(treeNode.fen, this.data.game.variant);
        if (treeNode.dests.length > 1 && treeNode.dests[0] === '#') {
          const nextChild = gamebook.peekNextChild();
          if (nextChild && nextChild.uci && nextChild.uci.length > 4) {
            treeNode.captLen = nextChild.uci.length / 2 - 1;
            treeNode.dests = "#" + treeNode.captLen.toString() + treeNode.dests.substr(2);
          }
        }
      }
      this.addNode(treeNode, this.path);
    }
  }

  sendMove = (orig: Key, dest: Key, capture?: JustCaptured, uci?: string): void => {
    const move: any = {
      orig,
      dest,
      variant: this.data.game.variant.key,
      fen: this.node.fen,
      path: this.path
    };
    if (capture) this.justCaptured = capture;
    if (uci) move.uci = uci;
    if (this.practice) this.practice.onUserMove();
    const gamebook = this.gamebookPlay();
    if (this.embed && gamebook) {
      this.gamebookMove(orig, dest, gamebook, capture);
    } else {
      if (this.data.pref.fullCapture) move.fullCapture = true;
      this.socket.sendAnaMove(move, this.data.puzzleEditor);
      this.preparePremoving();
    }
    this.redraw();
  }

  private preparePremoving(): void {
    this.draughtsground.set({
      //turnColor: this.draughtsground.state.movable.color as cg.Color,
      turnColor: opposite(this.draughtsground.state.turnColor as cg.Color),
      movable: {
        //color: opposite(this.draughtsground.state.movable.color as cg.Color)
        color: this.draughtsground.state.turnColor as cg.Color
      },
      premovable: {
        enabled: true
      }
    });
  }

  private setAlternatives(node: Tree.Node, parent: Tree.Node) {
    if (node.uci && node.uci.length > 6 && countGhosts(node.fen) === 0) {
      if (parent.alternatives && parent.alternatives.length > 1) {
        const alts = new Array<Tree.Alternative>();
        for (const alt of parent.alternatives) {
          if (draughtsUtil.fenCompare(node.fen.slice(0, 2) + alt.fen, node.fen) &&
            !parent.children.find((c) => draughtsUtil.fenCompare(node.fen, c.fen) && alt.uci === c.uci)) {
              alts.push(alt);
          }
        }
        for (const c of parent.children)
          if (draughtsUtil.fenCompare(node.fen, c.fen))
            c.missingAlts = alts;
      }
    }
  }

  onPremoveSet = () => {
    if (this.study) this.study.onPremoveSet();
  }

  addNode(node: Tree.Node, path: Tree.Path) {
    const newPath = this.tree.addNode(node, path, this.data.puzzleEditor, this.coordSystem());
    if (!newPath) {
      console.log("Can't addNode", node, path);
      return this.redraw();
    }
    this.jump(newPath);
    if (this.data.puzzleEditor && this.nodeList.length > 1)
      this.setAlternatives(this.node, this.nodeList[this.nodeList.length - 2]);
    this.redraw();
    this.draughtsground.playPremove();
  }

  addDests(dests: string, path: Tree.Path, opening?: Tree.Opening, alternatives?: Tree.Alternative[], destsUci?: Uci[]): void {
    const node = this.tree.addDests(dests, path, opening, alternatives, destsUci);
    if (path === this.path) {
      this.showGround();
      if (this.gameOver()) this.ceval.stop();
    }
    if (this.data.puzzleEditor && node && node.alternatives && node.alternatives.length > 1 && node.children.length > 0) {
      node.children.forEach(child => this.setAlternatives(child, node));
      this.redraw();
    }
    this.withCg(cg => cg.playPremove());
  }

  deleteNode(path: Tree.Path): void {
    const node = this.tree.nodeAtPath(path);
    if (!node) return;
    const count = treeOps.countChildrenAndComments(node);
    if ((count.nodes >= 10 || count.comments > 0) && !confirm(
      'Delete ' + util.plural('move', count.nodes) + (count.comments ? ' and ' + util.plural('comment', count.comments) : '') + '?'
    )) return;
    this.tree.deleteNodeAt(path);
    if (this.data.puzzleEditor && this.nodeList.length > 1) {
      const parent = this.tree.nodeAtPath(treePath.init(path));
      if (parent)
        this.setAlternatives(node, parent);
    }
    if (treePath.contains(this.path, path)) this.userJump(treePath.init(path));
    else this.jump(this.path);
    if (this.study) this.study.deleteNode(path);
  }

  generatePuzzleJson(): string {
    const nodesUci = new Array<string[]>();
    treeOps.allVariationsNodeList(this.tree.root).map(variation => treeOps.expandMergedNodes(variation)).forEach(
      moves => {
        const movesUci = new Array<string>(); // moves.map(move => move.uci!);
        for (const move of moves) {
          const uci = move.uci;
          if (uci && uci.length > 0) {
            if (uci.length > 4) {
              for (let i = 0; i + 4 <= uci.length; i += 2) {
                movesUci.push(uci.slice(i, i + 4));
              }
            } else {
              movesUci.push(uci);
            }
          }
        }
        nodesUci.push(movesUci)
      }
    );
    return JSON.stringify({
      category: "Puzzles",
      last_pos: this.tree.root.fen,
      last_move: nodesUci[0][0],
      move_list: nodesUci.map(variation => variation.slice(1)),
      game_id: "custom"
    });
  };

  expandVariations(path: Tree.Path): void {
    const node = this.tree.nodeAtPath(path);
    if (!this.data.puzzleEditor || !node || !node.missingAlts || node.missingAlts.length == 0) return;
    const parentPath = treePath.init(path), parent = this.tree.nodeAtPath(parentPath);
    if (!parent) return;
    for (const alt of node.missingAlts) {
      var copy = treeOps.copyNode(node, false, this.coordSystem());
      copy.uci = alt.uci;
      copy.children = node.children;
      this.tree.setAmbs(copy, parent);
      this.tree.addNode(copy, parentPath, this.data.puzzleEditor, this.coordSystem());
    }
    for (const c of parent.children) {
      if (draughtsUtil.fenCompare(node.fen, c.fen))
        c.missingAlts = [];
    }
  }

  promote(path: Tree.Path, toMainline: boolean): void {
    this.tree.promoteAt(path, toMainline);
    this.jump(path);
    if (this.study) this.study.promote(path, toMainline);
  }

  reset(): void {
    this.showGround();
    this.redraw();
  }

  encodeNodeFen(): Fen {
    return this.node.fen.replace(/\s/g, '_');
  }

  currentEvals() {
    const evalNode = this.getCevalNode();
    return {
      server: evalNode.eval,
      client: evalNode.ceval
    };
  }

  private pickUci(compChild?: Tree.Node, nextBest?: string) {
    if (!nextBest)
      return undefined;
    else if (!!compChild && compChild.uci && compChild.uci.length > nextBest.length && compChild.uci.slice(0, 2) === nextBest.slice(0, 2) && compChild.uci.slice(compChild.uci.length - 2) === nextBest.slice(nextBest.length - 2))
      return compChild.uci;
    else
      return nextBest;
  }

  nextNodeBest() {
    return treeOps.withMainlineChild(this.node, (n: Tree.Node) => n.eval ? this.pickUci(getCompChild(this.node), n.eval.best) : undefined);
  }

  setAutoShapes = (): void => {
    this.withCg(cg => cg.setAutoShapes(computeAutoShapes(this)));
  }

  private onNewCeval = (ev: Tree.ClientEval, path: Tree.Path, isThreat: boolean): void => {
    this.tree.updateAt(path, (node: Tree.Node) => {
      if (node.fen !== ev.fen && !isThreat) return;
      if (isThreat) {
        if (!node.threat || isEvalBetter(ev, node.threat) || node.threat.maxDepth < ev.maxDepth)
          node.threat = ev;
      } else if (isEvalBetter(ev, node.ceval)) node.ceval = ev;
      else if (node.ceval && ev.maxDepth > node.ceval.maxDepth) node.ceval.maxDepth = ev.maxDepth;

      if (path === this.path) {
        this.setAutoShapes();
        if (!isThreat) {
          if (this.retro) this.retro.onCeval();
          if (this.practice) this.practice.onCeval();
          if (this.studyPractice) this.studyPractice.onCeval();
          this.evalCache.onCeval();
          if (ev.cloud && ev.depth >= this.ceval.effectiveMaxDepth()) this.ceval.stop();
        }
        this.redraw();
      }
    });
  }

  private instanciateCeval(failsafe: boolean = false): void {
    if (this.ceval) this.ceval.destroy();
    const cfg: CevalOpts = {
      variant: this.data.game.variant,
      possible: !this.embed &&
        (this.synthetic || !game.playable(this.data)),
      emit: (ev: Tree.ClientEval, work: CevalWork) => {
        this.onNewCeval(ev, work.path, work.threatMode);
      },
      setAutoShapes: this.setAutoShapes,
      failsafe,
      onCrash: lastError => {
        const ceval = this.node.ceval;
        console.log('Local eval failed after depth ' + (ceval && ceval.depth), lastError);
        if (this.ceval.pnaclSupported) {
          if (ceval && ceval.depth >= 20 && !ceval.retried) {
            console.log('Remain on native WASM/ASMJS for now');
            ceval.retried = true;
          } else {
            console.log('Fallback to ASMJS now');
            this.instanciateCeval(true);
            this.startCeval();
          }
        }
      },
      redraw: this.redraw
    };
    if (this.opts.study && this.opts.practice) {
      cfg.storageKeyPrefix = 'practice';
      cfg.multiPvDefault = 1;
    }
    this.ceval = cevalCtrl(cfg);
  }

  getCeval() {
    return this.ceval;
  }

  gameOver(node?: Tree.Node): 'draw' | 'checkmate' | false {
    const n = node || this.node;
    if (n.dests !== '' || n.drops) return false;
    if (n.check) return 'checkmate';
    return 'draw';
  }

  canUseCeval(): boolean {
    return !this.gameOver() && !this.node.threefold;
  }

  startCeval = throttle(800, () => {
    if (this.ceval.enabled()) {
      if (this.canUseCeval()) {
        // only analyze startingposition of multicaptures
        const ghostEnd = (this.nodeList.length > 0 && this.node.displayPly && this.node.displayPly !== this.node.ply);
        const path = ghostEnd ? this.path.slice(2) : this.path;
        const nodeList = ghostEnd ? this.nodeList.slice(1) : this.nodeList;
        this.ceval.start(path, nodeList, this.threatMode(), false);
        this.evalCache.fetch(path, parseInt(this.ceval.multiPv()));
      } else this.ceval.stop();
    }
  });

  toggleCeval = () => {
    if (!this.showComputer()) return;
    this.ceval.toggle();
    this.setAutoShapes();
    this.startCeval();
    if (!this.ceval.enabled()) {
      this.threatMode(false);
      if (this.practice) this.togglePractice();
    }
    this.redraw();
  }

  toggleThreatMode = () => {
    if (this.node.displayPly && this.node.displayPly !== this.node.ply) return;
    if (!this.ceval.enabled()) this.ceval.toggle();
    if (!this.ceval.enabled()) return;
    this.threatMode(!this.threatMode());
    if (this.threatMode() && this.practice) this.togglePractice();
    this.setAutoShapes();
    this.startCeval();
    this.redraw();
  }

  disableThreatMode = (): boolean => {
    return !!this.practice;
  }

  mandatoryCeval = (): boolean => {
    return !!this.studyPractice;
  }

  private cevalReset(): void {
    this.ceval.stop();
    if (!this.ceval.enabled()) this.ceval.toggle();
    this.startCeval();
    this.redraw();
  }

  cevalSetMultiPv = (v: number): void => {
    this.ceval.multiPv(v);
    this.tree.removeCeval();
    this.cevalReset();
  }

  cevalSetThreads = (v: number): void => {
    this.ceval.threads(v);
    this.cevalReset();
  }

  cevalSetHashSize = (v: number): void => {
    this.ceval.hashSize(v);
    this.cevalReset();
  }

  cevalSetInfinite = (v: boolean): void => {
    this.ceval.infinite(v);
    this.cevalReset();
  }

  showEvalGauge(): boolean {
    return this.hasAnyComputerAnalysis() && this.showGauge() && !this.gameOver() && this.showComputer();
  }

  hasAnyComputerAnalysis(): boolean {
    return this.data.analysis ? true : this.ceval.enabled();
  }

  hasFullComputerAnalysis = (): boolean => {
    return this.isFullAnalysis(this.mainline);
  }

  private isFullAnalysis(nodes: Tree.Node[]) {
    let count = 0;
    for (let i = 0; i < nodes.length - 2; i++) {
      const skip = i > 0 && nodes[i].ply === nodes[i - 1].ply;
      if (!skip) {
        count++;
        if (count > 200) return false; // max 200 ply of analysis
        const e = nodes[i].eval;
        if (!e || !Object.keys(e).length)
          return false;
      }
    }
    return true;
  }

  private resetAutoShapes() {
    if (this.showAutoShapes()) this.setAutoShapes();
    else this.draughtsground && this.draughtsground.setAutoShapes([]);
  }

  toggleAutoShapes = (v: boolean): void => {
    this.showAutoShapes(v);
    this.resetAutoShapes();
  }

  toggleGauge = () => {
    this.showGauge(!this.showGauge());
  }

  private onToggleComputer() {
    if (!this.showComputer()) {
      this.tree.removeComputerVariations();
      if (this.ceval.enabled()) this.toggleCeval();
      this.draughtsground && this.draughtsground.setAutoShapes([]);
    } else this.resetAutoShapes();
  }

  toggleComputer = () => {
    if (this.ceval.enabled()) this.toggleCeval();
    const value = !this.showComputer();
    this.showComputer(value);
    if (!value && this.practice) this.togglePractice();
    this.onToggleComputer();
    li.pubsub.emit('analysis.comp.toggle', value);
  }

  mergeAnalysisData(data: ServerEvalData): void {
    if (this.study && this.study.data.chapter.id !== data.ch) return;
    this.tree.merge(data.tree);
    if (!this.showComputer()) this.tree.removeComputerVariations();
    this.data.analysis = data.analysis;
    const dataNodeList = treeOps.mainlineNodeList(data.tree);
    if (data.analysis) data.analysis.partial = !this.isFullAnalysis(dataNodeList);
    if (data.division) this.data.game.division = data.division;
    if (this.retro) this.retro.onMergeAnalysisData();
    if (this.study) this.study.serverEval.onMergeAnalysisData();
    li.pubsub.emit('analysis.server.progress', { game: this.data.game, analysis: data.analysis, treeParts: dataNodeList });
    this.redraw();
  }

  getChartData() {
    const d = this.data;
    return {
      analysis: d.analysis,
      game: d.game,
      player: d.player,
      opponent: d.opponent,
      treeParts: treeOps.mainlineNodeList(this.tree.root)
    };
  }

  playUci(uci: Uci): void {
    const move = draughtsUtil.decomposeUci(uci);
    this.sendMove(move[0], move[move.length - 1], undefined, move.length > 2 ? uci : undefined);
  }

  explorerMove(uci: Uci) {
    this.playUci(uci);
    this.explorer.loading(true);
  }

  playBestMove() {
    const uci = this.nextNodeBest() || (this.node.ceval && scan2uci(this.node.ceval.pvs[0].moves[0]));
    if (uci) this.playUci(uci);
  }

  canEvalGet = (node: Tree.Node): boolean => this.opts.study || node.ply < 15;

  instanciateEvalCache() {
    this.evalCache = makeEvalCache({
      variant: this.data.game.variant.key,
      canGet: this.canEvalGet,
      canPut: (node: Tree.Node) => {
        return this.data.evalPut && this.canEvalGet(node) && (
          // if not in study, only put decent opening moves
          this.opts.study || (!node.ceval!.win && Math.abs(node.ceval!.cp!) < 99)
        );
      },
      getNode: () => this.node,
      send: this.opts.socketSend,
      receive: this.onNewCeval
    });
  }

  toggleRetro = (): void => {
    if (this.retro) this.retro = undefined;
    else {
      this.retro = makeRetro(this);
      if (this.practice) this.togglePractice();
      if (this.explorer.enabled()) this.toggleExplorer();
    }
    this.setAutoShapes();
  }

  toggleExplorer = (): void => {
    if (this.practice) this.togglePractice();
    this.explorer.toggle();
  }

  togglePractice = () => {
    if (this.practice || !this.ceval.possible) this.practice = undefined;
    else {
      if (this.retro) this.toggleRetro();
      if (this.explorer.enabled()) this.toggleExplorer();
      this.practice = makePractice(this, () => {
        // push to 20 to store AI moves in the cloud
        // lower to 18 after task completion (or failure)
        return this.studyPractice && this.studyPractice.success() === null ? 20 : 18;
      });
    }
    this.setAutoShapes();
  };

  restartPractice() {
    this.practice = undefined;
    this.togglePractice();
  }

  gamebookPlay = (): GamebookPlayCtrl | undefined => {
    return this.study && this.study.gamebookPlay();
  }

  isGamebook = (): boolean => !!(this.study && this.study.data.chapter.gamebook);

  withCg<A>(f: (cg: DraughtsgroundApi) => A): A | undefined {
    if (this.draughtsground && this.cgVersion.js === this.cgVersion.dom)
      return f(this.draughtsground);
  }

};
