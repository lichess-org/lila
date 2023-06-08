import * as cg from 'chessground/types';
import * as chessUtil from 'chess';
import * as game from 'game';
import * as keyboard from './keyboard';
import * as speech from './speech';
import * as util from './util';
import { plural } from './view/util';
import debounce from 'common/debounce';
import GamebookPlayCtrl from './study/gamebook/gamebookPlayCtrl';
import type makeStudyCtrl from './study/studyCtrl';
import throttle from 'common/throttle';
import { AnalyseOpts, AnalyseData, ServerEvalData, Key, JustCaptured, NvuiPlugin, Redraw } from './interfaces';
import { Api as ChessgroundApi } from 'chessground/api';
import { Autoplay, AutoplayDelay } from './autoplay';
import { build as makeTree, path as treePath, ops as treeOps, TreeWrapper } from 'tree';
import { compute as computeAutoShapes } from './autoShape';
import { Config as ChessgroundConfig } from 'chessground/config';
import { CevalCtrl, sanIrreversible, EvalMeta } from 'ceval';
import { ctrl as treeViewCtrl, TreeView } from './treeView/treeView';
import { defined, toggle, Toggle } from 'common';
import { DrawShape } from 'chessground/draw';
import { ForecastCtrl } from './forecast/interfaces';
import { lichessRules } from 'chessops/compat';
import { make as makeEvalCache, EvalCache } from './evalCache';
import { make as makeForecast } from './forecast/forecastCtrl';
import { make as makeFork, ForkCtrl } from './fork';
import { make as makePractice, PracticeCtrl } from './practice/practiceCtrl';
import { make as makeRetro, RetroCtrl } from './retrospect/retroCtrl';
import { make as makeSocket, Socket } from './socket';
import { nextGlyphSymbol } from './nodeFinder';
import { opposite, parseUci, makeSquare, roleToChar } from 'chessops/util';
import { Outcome, isNormal } from 'chessops/types';
import { parseFen } from 'chessops/fen';
import { Position, PositionError } from 'chessops/chess';
import { Result } from '@badrap/result';
import { setupPosition } from 'chessops/variant';
import { AnaMove, StudyCtrl } from './study/interfaces';
import { StudyPracticeCtrl } from './study/practice/interfaces';
import { valid as crazyValid } from './crazy/crazyCtrl';
import { PromotionCtrl } from 'chess/promotion';
import wikiTheory, { wikiClear, WikiTheory } from './wiki';
import ExplorerCtrl from './explorer/explorerCtrl';
import { uciToMove } from 'chessground/util';
import Persistence from './persistence';
import pgnImport from './pgnImport';
import { NodeEvals, ParentCtrl } from 'ceval/src/types';
import { textRaw as xhrTextRaw } from 'common/xhr';

export default class AnalyseCtrl implements ParentCtrl {
  data: AnalyseData;
  element: HTMLElement;
  tree: TreeWrapper;
  socket: Socket;
  chessground: ChessgroundApi;
  trans: Trans;
  ceval: CevalCtrl;
  evalCache: EvalCache;
  persistence?: Persistence;
  actionMenu: Toggle = toggle(false);

  // current tree state, cursor, and denormalized node lists
  path: Tree.Path;
  node: Tree.Node;
  nodeList: Tree.Node[];
  mainline: Tree.Node[];

  // sub controllers
  autoplay: Autoplay;
  explorer: ExplorerCtrl;
  forecast?: ForecastCtrl;
  retro?: RetroCtrl;
  fork: ForkCtrl;
  practice?: PracticeCtrl;
  study?: StudyCtrl;
  studyPractice?: StudyPracticeCtrl;
  promotion: PromotionCtrl;
  wiki?: WikiTheory;

  // state flags
  justPlayed?: string; // pos
  justDropped?: string; // role
  justCaptured?: JustCaptured;
  autoScrollRequested = false;
  redirecting = false;
  onMainline = true;
  synthetic: boolean; // false if coming from a real game
  ongoing: boolean; // true if real game is ongoing
  showServerAnalysis: boolean;

  // display flags
  flipped = false;
  embed: boolean;
  showComments = true; // whether to display comments in the move tree
  keyboardHelp: boolean = location.hash === '#keyboard';
  treeView: TreeView;
  isStudy: boolean;
  treeVersion = 1; // increment to recreate tree

  // underboard inputs
  fenInput?: string;
  pgnInput?: string;

  // other paths
  initialPath: Tree.Path;
  contextMenuPath?: Tree.Path;
  gamePath?: Tree.Path;

  // misc
  requestInitialPly?: number; // start ply from the URL location hash
  cgConfig: any; // latest chessground config (useful for revert)
  music?: any;
  nvui?: NvuiPlugin;
  pvUciQueue: Uci[] = [];

  constructor(readonly opts: AnalyseOpts, readonly redraw: Redraw, makeStudy?: typeof makeStudyCtrl) {
    this.data = opts.data;
    this.element = opts.element;
    this.embed = opts.embed;
    this.trans = opts.trans;
    this.treeView = treeViewCtrl(opts.embed ? 'inline' : 'column');
    this.promotion = new PromotionCtrl(this.withCg, () => this.withCg(g => g.set(this.cgConfig)), this.redraw);
    this.isStudy = !!opts.study;

    if (this.data.forecast) this.forecast = makeForecast(this.data.forecast, this.data, redraw);
    if (this.opts.wiki) this.wiki = wikiTheory();
    if (window.LichessAnalyseNvui) this.nvui = window.LichessAnalyseNvui(this) as NvuiPlugin;

    this.instantiateEvalCache();

    if (opts.inlinePgn) this.data = this.changePgn(opts.inlinePgn, false) || this.data;

    this.initialize(this.data, false);

    this.showServerAnalysis = !this.synthetic || !!this.opts.study;

    this.persistence = this.embed || opts.study || this.synthetic ? undefined : new Persistence(this);

    this.instantiateCeval();

    this.initialPath = this.makeInitialPath();
    this.setPath(this.initialPath);

    this.showGround();
    this.ceval.startCeval();
    this.explorer.setNode();
    this.study = opts.study
      ? makeStudy?.(opts.study, this, (opts.tagTypes || '').split(','), opts.practice, opts.relay)
      : undefined;
    this.studyPractice = this.study ? this.study.practice : undefined;

    if (location.hash === '#practice' || (this.study && this.study.data.chapter.practice)) this.togglePractice();
    else if (location.hash === '#menu') lichess.requestIdleCallback(this.actionMenu.toggle, 500);

    keyboard.bind(this);

    const urlEngine = new URLSearchParams(location.search).get('engine');
    if (urlEngine) this.ceval.setEngineType(`external-${urlEngine}`);

    lichess.pubsub.on('jump', (ply: string) => {
      this.jumpToMain(parseInt(ply));
      this.redraw();
    });

    lichess.pubsub.on('sound_set', (set: string) => {
      if (!this.music && set === 'music')
        lichess.loadScript('javascripts/music/play.js').then(() => {
          this.music = lichess.playMusic();
        });
      if (this.music && set !== 'music') this.music = undefined;
    });

    lichess.pubsub.on('ply.trigger', () =>
      lichess.pubsub.emit('ply', this.node.ply, this.tree.lastMainlineNode(this.path).ply === this.node.ply)
    );
    lichess.pubsub.on('analysis.chart.click', index => {
      this.jumpToIndex(index);
      this.redraw();
    });
    lichess.pubsub.on('theme.change', redraw);
    lichess.pubsub.on('analysis.server.start', () => {
      this.analysisStarted = true;
      this.redraw();
    });
    speech.setup();
    this.persistence?.merge();
  }

  initialize(data: AnalyseData, merge: boolean): void {
    this.data = data;
    this.synthetic = data.game.id === 'synthetic';
    this.ongoing = !this.synthetic && game.playable(data);

    const prevTree = merge && this.tree.root;
    this.tree = makeTree(util.treeReconstruct(this.data.treeParts, this.data.sidelines));
    if (prevTree) this.tree.merge(prevTree);

    this.autoplay = new Autoplay(this);
    if (this.socket) this.socket.clearCache();
    else this.socket = makeSocket(this.opts.socketSend, this);
    if (this.explorer) this.explorer.destroy();
    this.explorer = new ExplorerCtrl(this, this.opts.explorer, this.explorer);
    this.gamePath =
      this.synthetic || this.ongoing ? undefined : treePath.fromNodeList(treeOps.mainlineNodeList(this.tree.root));
    this.fork = makeFork(this);

    lichess.sound.preloadBoardSounds();
  }

  private makeInitialPath = (): string => {
    // if correspondence, always use latest actual move to set 'current' style
    if (this.ongoing) return treePath.fromNodeList(treeOps.mainlineNodeList(this.tree.root));

    const loc = window.location,
      hashPly = loc.hash === '#last' ? this.tree.lastPly() : parseInt(loc.hash.slice(1)),
      startPly = hashPly >= 0 ? hashPly : this.opts.inlinePgn ? this.tree.lastPly() : undefined;
    if (defined(startPly)) {
      // remove location hash - https://stackoverflow.com/questions/1397329/how-to-remove-the-hash-from-window-location-with-javascript-without-page-refresh/5298684#5298684
      window.history.replaceState(null, '', loc.pathname + loc.search);
      this.requestInitialPly = startPly;
      const mainline = treeOps.mainlineNodeList(this.tree.root);
      return treeOps.takePathWhile(mainline, n => n.ply <= startPly);
    } else return treePath.root;
  };

  enableWiki = (v: boolean) => {
    this.wiki = v ? wikiTheory() : undefined;
    if (this.wiki) this.wiki(this.nodeList);
    else wikiClear();
  };

  private setPath = (path: Tree.Path): void => {
    this.path = path;
    this.nodeList = this.tree.getNodeList(path);
    this.node = treeOps.last(this.nodeList) as Tree.Node;
    this.mainline = treeOps.mainlineNodeList(this.tree.root);
    this.onMainline = this.tree.pathIsMainline(path);
    this.fenInput = undefined;
    this.pgnInput = undefined;
    if (this.wiki && this.data.game.variant.key == 'standard') this.wiki(this.nodeList);
    this.persistence?.save();
  };

  flip = () => {
    this.flipped = !this.flipped;
    this.study?.onFlip();
    this.chessground.set({
      orientation: this.bottomColor(),
    });
    if (this.retro && this.data.game.variant.key !== 'racingKings') {
      this.retro = makeRetro(this, this.bottomColor());
    }
    if (this.practice) this.restartPractice();
    this.explorer.onFlip();
    this.onChange();
    this.persistence?.save(true);
    this.redraw();
  };

  topColor(): Color {
    return opposite(this.bottomColor());
  }

  bottomColor(): Color {
    if (this.data.game.variant.key === 'racingKings') return this.flipped ? 'black' : 'white';
    return this.flipped ? opposite(this.data.orientation) : this.data.orientation;
  }

  bottomIsWhite = () => this.bottomColor() === 'white';

  getOrientation(): Color {
    // required by ui/ceval
    return this.bottomColor();
  }
  getNode(): Tree.Node {
    // required by ui/ceval
    return this.node;
  }

  turnColor(): Color {
    return util.plyColor(this.node.ply);
  }

  togglePlay(delay: AutoplayDelay): void {
    this.autoplay.toggle(delay);
    this.actionMenu(false);
  }

  private showGround(): void {
    this.onChange();
    if (!defined(this.node.dests)) this.getDests();
    this.withCg(cg => {
      cg.set(this.makeCgOpts());
      this.setAutoShapes();
      if (this.node.shapes) cg.setShapes(this.node.shapes as DrawShape[]);
    });
  }

  private getDests: () => void = throttle(800, () => {
    if (!this.embed && !defined(this.node.dests))
      this.socket.sendAnaDests({
        variant: this.data.game.variant.key,
        fen: this.node.fen,
        path: this.path,
      });
  });

  makeCgOpts(): ChessgroundConfig {
    const node = this.node,
      color = this.turnColor(),
      dests = chessUtil.readDests(this.node.dests),
      drops = chessUtil.readDrops(this.node.drops),
      gamebookPlay = this.gamebookPlay(),
      movableColor = gamebookPlay
        ? gamebookPlay.movableColor()
        : this.practice
        ? this.bottomColor()
        : !this.embed && ((dests && dests.size > 0) || drops === null || drops.length)
        ? color
        : undefined,
      config: ChessgroundConfig = {
        fen: node.fen,
        turnColor: color,
        movable: this.embed
          ? {
              color: undefined,
              dests: new Map(),
            }
          : {
              color: movableColor,
              dests: (movableColor === color && dests) || new Map(),
            },
        check: !!node.check,
        lastMove: uciToMove(node.uci),
      };
    if (!dests && !node.check) {
      // premove while dests are loading from server
      // can't use when in check because it highlights the wrong king
      config.turnColor = opposite(color);
      config.movable!.color = color;
    }
    config.premovable = {
      enabled: config.movable!.color && config.turnColor !== config.movable!.color,
    };
    this.cgConfig = config;
    return config;
  }

  private throttleSound = (name: string) => throttle(100, () => lichess.sound.play(name));

  private sound = {
    move: this.throttleSound('move'),
    capture: this.throttleSound('capture'),
    check: this.throttleSound('check'),
  };

  private onChange: () => void = throttle(300, () => {
    lichess.pubsub.emit('analysis.change', this.node.fen, this.path);
  });

  private updateHref: () => void = debounce(() => {
    if (!this.opts.study) window.history.replaceState(null, '', '#' + this.node.ply);
  }, 750);

  autoScroll(): void {
    this.autoScrollRequested = true;
  }

  playedLastMoveMyself = () => !!this.justPlayed && !!this.node.uci && this.node.uci.startsWith(this.justPlayed);

  jump(path: Tree.Path): void {
    const pathChanged = path !== this.path,
      isForwardStep = pathChanged && path.length == this.path.length + 2;
    this.setPath(path);
    if (pathChanged) {
      if (this.study) this.study.setPath(path, this.node);
      if (isForwardStep) {
        if (!this.node.uci) this.sound.move();
        // initial position
        else if (!this.playedLastMoveMyself()) {
          if (this.node.san!.includes('x')) this.sound.capture();
          else this.sound.move();
        }
        if (/\+|#/.test(this.node.san!)) this.sound.check();
      }
      this.ceval.threatMode(false);
      this.ceval.stop();
      this.ceval.startCeval();
      speech.node(this.node);
    }
    this.justPlayed = this.justDropped = this.justCaptured = undefined;
    this.explorer.setNode();
    this.updateHref();
    this.autoScroll();
    this.promotion.cancel();
    if (pathChanged) {
      if (this.retro) this.retro.onJump();
      if (this.practice) this.practice.onJump();
      if (this.study) this.study.onJump();
    }
    if (this.music) this.music.jump(this.node);
    lichess.pubsub.emit('ply', this.node.ply, this.tree.lastMainlineNode(this.path).ply === this.node.ply);
    this.showGround();
  }

  userJump = (path: Tree.Path): void => {
    this.autoplay.stop();
    if (!this.gamebookPlay()) this.withCg(cg => cg.selectSquare(null));
    if (this.practice) {
      const prev = this.path;
      this.practice.preUserJump(prev, path);
      this.jump(path);
      this.withCg(cg => cg.cancelPremove());
      this.practice.postUserJump(prev, this.path);
    } else this.jump(path);
  };

  canJumpTo = (path: Tree.Path): boolean => !this.study || this.study.canJumpTo(path);

  userJumpIfCan(path: Tree.Path): void {
    if (this.canJumpTo(path)) this.userJump(path);
  }

  mainlinePathToPly(ply: Ply): Tree.Path {
    return treeOps.takePathWhile(this.mainline, n => n.ply <= ply);
  }

  jumpToMain = (ply: Ply): void => {
    this.userJump(this.mainlinePathToPly(ply));
  };

  jumpToIndex = (index: number): void => {
    this.jumpToMain(index + 1 + this.tree.root.ply);
  };

  jumpToGlyphSymbol(color: Color, symbol: string): void {
    const node = nextGlyphSymbol(color, symbol, this.mainline, this.node.ply);
    if (node) this.jumpToMain(node.ply);
    this.redraw();
  }

  reloadData(data: AnalyseData, merge: boolean): void {
    this.initialize(data, merge);
    this.redirecting = false;
    this.setPath(treePath.root);
    this.instantiateCeval();
    this.instantiateEvalCache();
    this.ceval.cgVersion.js++;
  }

  changePgn(pgn: string, andReload: boolean): AnalyseData | undefined {
    try {
      const data: AnalyseData = {
        ...pgnImport(pgn),
        orientation: this.bottomColor(),
        pref: this.data.pref,
      } as AnalyseData;
      if (andReload) {
        this.reloadData(data, false);
        this.userJump(this.mainlinePathToPly(this.tree.lastPly()));
        this.redraw();
      }
      return data;
    } catch (err) {
      console.log(err);
    }
    return undefined;
  }

  changeFen(fen: Fen): void {
    this.redirecting = true;
    window.location.href =
      '/analysis/' +
      this.data.game.variant.key +
      '/' +
      encodeURIComponent(fen).replace(/%20/g, '_').replace(/%2F/g, '/');
  }

  userNewPiece = (piece: cg.Piece, pos: Key): void => {
    if (crazyValid(this.chessground, this.node.drops, piece, pos)) {
      this.justPlayed = roleToChar(piece.role).toUpperCase() + '@' + pos;
      this.justDropped = piece.role;
      this.justCaptured = undefined;
      this.sound.move();
      const drop = {
        role: piece.role,
        pos,
        variant: this.data.game.variant.key,
        fen: this.node.fen,
        path: this.path,
      };
      this.socket.sendAnaDrop(drop);
      this.preparePremoving();
      this.redraw();
    } else this.jump(this.path);
  };

  userMove = (orig: Key, dest: Key, capture?: JustCaptured): void => {
    this.justPlayed = orig;
    this.justDropped = undefined;
    const piece = this.chessground.state.pieces.get(dest);
    const isCapture = capture || (piece && piece.role == 'pawn' && orig[0] != dest[0]);
    this.sound[isCapture ? 'capture' : 'move']();
    if (!this.promotion.start(orig, dest, { submit: (orig, dest, prom) => this.sendMove(orig, dest, capture, prom) })) {
      this.sendMove(orig, dest, capture);
    }
  };

  sendMove = (orig: Key, dest: Key, capture?: JustCaptured, prom?: cg.Role): void => {
    const move: AnaMove = {
      orig,
      dest,
      variant: this.data.game.variant.key,
      fen: this.node.fen,
      path: this.path,
    };
    if (capture) this.justCaptured = capture;
    if (prom) move.promotion = prom;
    if (this.practice) this.practice.onUserMove();
    this.socket.sendAnaMove(move);
    this.preparePremoving();
    this.redraw();
  };

  private preparePremoving(): void {
    this.chessground.set({
      turnColor: this.chessground.state.movable.color as cg.Color,
      movable: {
        color: opposite(this.chessground.state.movable.color as cg.Color),
      },
      premovable: {
        enabled: true,
      },
    });
  }

  onPremoveSet = () => {
    if (this.study) this.study.onPremoveSet();
  };

  addNode(node: Tree.Node, path: Tree.Path) {
    this.persistence?.onAddNode(node, path);
    const newPath = this.tree.addNode(node, path);
    if (!newPath) {
      console.log("Can't addNode", node, path);
      return this.redraw();
    }
    this.jump(newPath);
    this.redraw();
    const queuedUci = this.pvUciQueue.shift();
    if (queuedUci) this.playUci(queuedUci, this.pvUciQueue);
    else this.chessground.playPremove();
  }

  addDests(dests: string, path: Tree.Path): void {
    this.tree.addDests(dests, path);
    if (path === this.path) {
      this.showGround();
      if (this.outcome()) this.ceval.stop();
    }
    this.withCg(cg => cg.playPremove());
  }

  deleteNode(path: Tree.Path): void {
    const node = this.tree.nodeAtPath(path);
    if (!node) return;
    const count = treeOps.countChildrenAndComments(node);
    if (
      (count.nodes >= 10 || count.comments > 0) &&
      !confirm(
        'Delete ' +
          plural('move', count.nodes) +
          (count.comments ? ' and ' + plural('comment', count.comments) : '') +
          '?'
      )
    )
      return;
    this.tree.deleteNodeAt(path);
    if (treePath.contains(this.path, path)) this.userJump(treePath.init(path));
    else this.jump(this.path);
    if (this.study) this.study.deleteNode(path);
  }

  promote(path: Tree.Path, toMainline: boolean): void {
    this.tree.promoteAt(path, toMainline);
    this.jump(path);
    if (this.study) this.study.promote(path, toMainline);
    this.treeVersion++;
  }

  forceVariation(path: Tree.Path, force: boolean): void {
    this.tree.forceVariationAt(path, force);
    this.jump(path);
    if (this.study) this.study.forceVariation(path, force);
    this.treeVersion++;
  }

  reset(): void {
    this.showGround();
    this.redraw();
  }

  encodeNodeFen(): Fen {
    return this.node.fen.replace(/\s/g, '_');
  }

  currentEvals(): NodeEvals {
    return {
      server: this.node.eval,
      local: this.node.ceval,
    };
  }

  nextNodeBest() {
    return treeOps.withMainlineChild(this.node, (n: Tree.Node) => n.eval?.best);
  }

  setAutoShapes = (): void => {
    this.withCg(cg => cg.setAutoShapes(computeAutoShapes(this)));
  };

  private instantiateCeval(): void {
    if (this.ceval) this.ceval.destroy();
    this.ceval = new CevalCtrl({
      variant: this.data.game.variant,
      initialFen: this.data.game.initialFen,
      possible: !this.embed && (this.synthetic || !game.playable(this.data)),
      emit: (ev: Tree.ClientEval, work: EvalMeta) => {
        this.ceval.onNewCeval(ev, work.path, work.threatMode);
      },
      setAutoShapes: this.setAutoShapes,
      redraw: this.redraw,
      ...(this.opts.study && this.opts.practice
        ? {
            storageKeyPrefix: 'practice',
            multiPvDefault: 1,
          }
        : {}),
      externalEngines:
        this.data.externalEngines?.map(engine => ({
          ...engine,
          endpoint: this.opts.externalEngineEndpoint,
        })) || [],
      engineChanged: () => {
        this.setAutoShapes();
        if (!this.ceval.enabled()) {
          this.ceval.threatMode(false);
          if (this.practice) this.togglePractice();
        }
        this.redraw();
      },
      getChessground: () => this.chessground,
      tree: this.tree,
      getPath: () => this.path,
      togglePractice: this.togglePractice,
      getNode: () => this.node,
      outcome: () => this.outcome(),
      getNodeList: () => this.nodeList,
      getRetro: () => this.retro,
      getPractice: () => this.practice,
      getStudyPractice: () => this.studyPractice,
      evalCache: this.evalCache,
      showServerAnalysis: this.showServerAnalysis,
    });
  }

  outcome(node?: Tree.Node): Outcome | undefined {
    return this.position(node || this.node).unwrap(
      pos => pos.outcome(),
      _ => undefined
    );
  }

  position(node: Tree.Node): Result<Position, PositionError> {
    const setup = parseFen(node.fen).unwrap();
    return setupPosition(lichessRules(this.data.game.variant.key), setup);
  }

  disableThreatMode = (): boolean => {
    return !!this.practice;
  };

  mandatoryCeval = (): boolean => {
    return !!this.studyPractice;
  };

  showEvalGauge(): boolean {
    return (
      this.hasAnyComputerAnalysis() &&
      this.ceval.showGauge() &&
      !this.outcome() &&
      this.ceval.getEngineType() !== 'disabled'
    );
  }

  hasAnyComputerAnalysis(): boolean {
    return this.data.analysis ? true : this.ceval.enabled();
  }

  hasFullComputerAnalysis = (): boolean => {
    return Object.keys(this.mainline[0].eval || {}).length > 0;
  };

  private analysisStarted: boolean = $('#acpl-chart').length !== 0;
  hasServerEval() {
    return this.study?.serverEval?.requested || this.analysisStarted || !!this.data.analysis;
  }

  canRequestServerEval() {
    return !this.study || this.mainline.length > 4;
  }
  requestServerEval() {
    if (this.study) {
      this.study.serverEval.request();
      return;
    }
    xhrTextRaw(`/${this.data.game.id}/request-analysis`, { method: 'POST' }).then(res => {
      if (res.ok) this.redraw();
      else
        res.text().then(t => {
          if (t && !t.startsWith('<!DOCTYPE html>')) alert(t);
          lichess.reload();
        });
    });
    lichess.pubsub.emit('analysis.server.start');
  }

  mergeAnalysisData(data: ServerEvalData) {
    if (this.study && this.study.data.chapter.id !== data.ch) return;
    this.tree.merge(data.tree);
    if (!this.ceval.enabled()) this.tree.removeComputerVariations();
    this.data.analysis = data.analysis;
    if (data.analysis)
      data.analysis.partial = !!treeOps.findInMainline(data.tree, n => !n.eval && !!n.children.length && n.ply <= 300);
    if (data.division) this.data.game.division = data.division;
    if (this.retro) this.retro.onMergeAnalysisData();
    if (this.study) this.study.serverEval.onMergeAnalysisData();
    lichess.pubsub.emit('analysis.server.progress', this.data);
    this.redraw();
  }

  playUci(uci: Uci, uciQueue?: Uci[]) {
    this.pvUciQueue = uciQueue ?? [];
    const move = parseUci(uci)!;
    const to = makeSquare(move.to);
    if (isNormal(move)) {
      const piece = this.chessground.state.pieces.get(makeSquare(move.from));
      const capture = this.chessground.state.pieces.get(to);
      this.sendMove(
        makeSquare(move.from),
        to,
        capture && piece && capture.color !== piece.color ? capture : undefined,
        move.promotion
      );
    } else
      this.chessground.newPiece(
        {
          color: this.chessground.state.movable.color as Color,
          role: move.role,
        },
        to
      );
  }

  playUciList(uciList: Uci[]): void {
    this.pvUciQueue = uciList;
    const firstUci = this.pvUciQueue.shift();
    if (firstUci) this.playUci(firstUci, this.pvUciQueue);
  }

  explorerMove(uci: Uci) {
    this.playUci(uci);
    this.explorer.loading(true);
  }

  playBestMove() {
    const uci = this.node.ceval?.pvs[0].moves[0] || this.nextNodeBest();
    if (uci) this.playUci(uci);
  }

  canEvalGet(): boolean {
    if (this.node.ply >= 15 && !this.opts.study) return false;

    // cloud eval does not support threefold repetition
    const fens = new Set();
    for (let i = this.nodeList.length - 1; i >= 0; i--) {
      const node = this.nodeList[i];
      const fen = node.fen.split(' ').slice(0, 4).join(' ');
      if (fens.has(fen)) return false;
      if (node.san && sanIrreversible(this.data.game.variant.key, node.san)) return true;
      fens.add(fen);
    }
    return true;
  }

  instantiateEvalCache() {
    this.evalCache = makeEvalCache({
      variant: this.data.game.variant.key,
      canGet: () => this.canEvalGet(),
      canPut: () =>
        !!(
          this.ceval?.isCachable() &&
          this.canEvalGet() &&
          // if not in study, only put decent opening moves
          (this.opts.study || (!this.node.ceval!.mate && Math.abs(this.node.ceval!.cp!) < 99))
        ),
      getNode: () => this.node,
      send: this.opts.socketSend,
      receive: (...args) => this.ceval.onNewCeval(...args),
    });
  }

  closeTools = () => {
    if (this.retro) this.retro = undefined;
    if (this.practice) this.togglePractice();
    if (this.explorer.enabled()) this.explorer.toggle();
    this.persistence?.toggleOpen(false);
    this.actionMenu(false);
  };

  toggleRetro = (): void => {
    if (this.retro) this.retro = undefined;
    else {
      this.closeTools();
      this.retro = makeRetro(this, this.bottomColor());
    }
    this.setAutoShapes();
  };

  toggleExplorer = (): void => {
    const wasOpen = this.explorer.enabled() && !this.actionMenu();
    this.closeTools();
    if (!wasOpen && this.explorer.allowed()) this.explorer.toggle();
  };

  togglePractice = () => {
    if (this.practice || !this.ceval.possible) {
      this.practice = undefined;
      this.showGround();
    } else {
      this.closeTools();
      this.practice = makePractice(this, () => {
        // push to 20 to store AI moves in the cloud
        // lower to 18 after task completion (or failure)
        return this.studyPractice && this.studyPractice.success() === null ? 20 : 18;
      });
      this.setAutoShapes();
    }
  };

  togglePersistence = () => {
    const isOpen = this.persistence?.open();
    this.closeTools();
    this.persistence?.toggleOpen(!isOpen);
  };

  restartPractice() {
    this.practice = undefined;
    this.togglePractice();
  }

  gamebookPlay = (): GamebookPlayCtrl | undefined => this.study && this.study.gamebookPlay();

  isGamebook = (): boolean => !!(this.study && this.study.data.chapter.gamebook);

  withCg = <A>(f: (cg: ChessgroundApi) => A): A | undefined => {
    if (!this.chessground) return;
    if (this.ceval.cgVersion.js !== this.ceval.cgVersion.dom) return;
    return f(this.chessground);
  };
}
