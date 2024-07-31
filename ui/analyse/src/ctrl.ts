import * as cg from 'chessground/types';
import * as chessUtil from 'chess';
import * as game from 'game';
import * as keyboard from './keyboard';
import * as util from './util';
import { plural } from './view/util';
import debounce from 'common/debounce';
import GamebookPlayCtrl from './study/gamebook/gamebookPlayCtrl';
import StudyCtrl from './study/studyCtrl';
import { isTouchDevice } from 'common/device';
import throttle from 'common/throttle';
import {
  AnalyseOpts,
  AnalyseData,
  ServerEvalData,
  Key,
  JustCaptured,
  NvuiPlugin,
  Redraw,
} from './interfaces';
import { Api as ChessgroundApi } from 'chessground/api';
import { Autoplay, AutoplayDelay } from './autoplay';
import { build as makeTree, path as treePath, ops as treeOps, TreeWrapper } from 'tree';
import { compute as computeAutoShapes } from './autoShape';
import { Config as ChessgroundConfig } from 'chessground/config';
import { CevalCtrl, isEvalBetter, sanIrreversible, EvalMeta } from 'ceval';
import { TreeView } from './treeView/treeView';
import { defined, prop, Prop, toggle, Toggle, requestIdleCallback } from 'common';
import { DrawShape } from 'chessground/draw';
import { lichessRules } from 'chessops/compat';
import EvalCache from './evalCache';
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
import { storedBooleanProp } from 'common/storage';
import { AnaMove } from './study/interfaces';
import { StudyPracticeCtrl } from './study/practice/interfaces';
import { valid as crazyValid } from './crazy/crazyCtrl';
import { PromotionCtrl } from 'chess/promotion';
import wikiTheory, { wikiClear, WikiTheory } from './wiki';
import ExplorerCtrl from './explorer/explorerCtrl';
import { uciToMove } from 'chessground/util';
import Persistence from './persistence';
import pgnImport from './pgnImport';
import ForecastCtrl from './forecast/forecastCtrl';
import { ArrowKey, KeyboardMove, ctrl as makeKeyboardMove } from 'keyboardMove';
import * as control from './control';
import { PgnError } from 'chessops/pgn';

export default class AnalyseCtrl {
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
  isEmbed: boolean;

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

  // display flags
  flipped = false;
  showComments = true; // whether to display comments in the move tree
  showAutoShapes = storedBooleanProp('analyse.show-auto-shapes', true);
  variationArrowsProp = storedBooleanProp('analyse.show-variation-arrows', true);
  showGauge = storedBooleanProp('analyse.show-gauge', true);
  showComputer = storedBooleanProp('analyse.show-computer', true);
  showMoveAnnotation = storedBooleanProp('analyse.show-move-annotation', true);
  keyboardHelp: boolean = location.hash === '#keyboard';
  threatMode: Prop<boolean> = prop(false);
  treeView: TreeView;
  treeVersion = 1; // increment to recreate tree
  cgVersion = {
    js: 1, // increment to recreate chessground
    dom: 1,
  };

  // underboard inputs
  fenInput?: string;
  pgnInput?: string;
  pgnError?: string;

  // other paths
  initialPath: Tree.Path;
  contextMenuPath?: Tree.Path;
  gamePath?: Tree.Path;

  // misc
  requestInitialPly?: number; // start ply from the URL location hash
  cgConfig: any; // latest chessground config (useful for revert)
  nvui?: NvuiPlugin;
  pvUciQueue: Uci[] = [];
  keyboardMove?: KeyboardMove;

  constructor(
    readonly opts: AnalyseOpts,
    readonly redraw: Redraw,
    makeStudy?: typeof StudyCtrl,
  ) {
    this.data = opts.data;
    this.element = opts.element;
    this.trans = opts.trans;
    this.isEmbed = !!opts.embed;
    this.treeView = new TreeView('column');
    this.promotion = new PromotionCtrl(
      this.withCg,
      () => this.withCg(g => g.set(this.cgConfig)),
      this.redraw,
    );

    if (this.data.forecast) this.forecast = new ForecastCtrl(this.data.forecast, this.data, redraw);
    if (this.opts.wiki) this.wiki = wikiTheory();
    if (site.blindMode)
      site.asset.loadEsm<NvuiPlugin>('analyse.nvui', { init: this }).then(nvui => (this.nvui = nvui));

    this.instanciateEvalCache();

    if (opts.inlinePgn) this.data = this.changePgn(opts.inlinePgn, false) || this.data;

    this.initialize(this.data, false);

    this.persistence = opts.study ? undefined : new Persistence(this);

    this.configureCeval();

    this.initialPath = this.makeInitialPath();
    this.setPath(this.initialPath);

    this.showGround();
    this.onToggleComputer();
    this.explorer.setNode();
    this.study =
      opts.study && makeStudy
        ? new makeStudy(opts.study, this, (opts.tagTypes || '').split(','), opts.practice, opts.relay)
        : undefined;
    this.studyPractice = this.study ? this.study.practice : undefined;

    if (location.hash === '#practice' || (this.study && this.study.data.chapter.practice))
      this.togglePractice();
    else if (location.hash === '#menu') requestIdleCallback(this.actionMenu.toggle, 500);
    this.startCeval();
    keyboard.bind(this);

    const urlEngine = new URLSearchParams(location.search).get('engine');
    if (urlEngine) {
      try {
        this.ceval.engines.select(urlEngine);
        this.ensureCevalRunning();
      } catch (e) {
        console.info(e);
      }
      site.redirect('/analysis');
    }

    site.pubsub.on('jump', (ply: string) => {
      this.jumpToMain(parseInt(ply));
      this.redraw();
    });

    site.pubsub.on('ply.trigger', () =>
      site.pubsub.emit('ply', this.node.ply, this.tree.lastMainlineNode(this.path).ply === this.node.ply),
    );
    site.pubsub.on('analysis.chart.click', index => {
      this.jumpToIndex(index);
      this.redraw();
    });
    site.pubsub.on('board.change', redraw);
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
      this.synthetic || this.ongoing
        ? undefined
        : treePath.fromNodeList(treeOps.mainlineNodeList(this.tree.root));
    this.fork = makeFork(this);

    site.sound.preloadBoardSounds();
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
    for (let i = 0; i < this.nodeList.length; i++) {
      this.nodeList[i].collapsed = false;
    }
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
    this.chessground?.set({
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
    if (!defined(this.node.dests))
      this.socket.sendAnaDests({
        variant: this.data.game.variant.key,
        fen: this.node.fen,
        path: this.path,
      });
  });

  serverMainline = () => this.mainline.slice(0, game.playedTurns(this.data) + 1);

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
        : (dests && dests.size > 0) || drops === null || drops.length
        ? color
        : undefined,
      config: ChessgroundConfig = {
        fen: node.fen,
        turnColor: color,
        movable: {
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

  setChessground = (cg: CgApi) => {
    this.chessground = cg;

    if (this.data.pref.keyboardMove) {
      this.keyboardMove ??= makeKeyboardMove({
        ...this,
        data: { ...this.data, player: { color: 'both' } },
        flipNow: this.flip,
      });
      this.keyboardMove.update({ fen: this.node.fen, canMove: true, cg });
      requestAnimationFrame(() => this.redraw());
    }

    this.setAutoShapes();
    if (this.node.shapes) this.chessground.setShapes(this.node.shapes as DrawShape[]);
    this.cgVersion.dom = this.cgVersion.js;
    site.pubsub.on('board.change', (is3d: boolean) => {
      this.chessground.state.addPieceZIndex = is3d;
      this.chessground.redrawAll();
    });
  };

  private onChange: () => void = throttle(300, () => {
    site.pubsub.emit('analysis.change', this.node.fen, this.path);
  });

  private updateHref: () => void = debounce(() => {
    if (!this.opts.study) window.history.replaceState(null, '', '#' + this.node.ply);
  }, 750);

  autoScroll(): void {
    this.autoScrollRequested = true;
  }

  playedLastMoveMyself = () =>
    !!this.justPlayed && !!this.node.uci && this.node.uci.startsWith(this.justPlayed);

  jump(path: Tree.Path): void {
    const pathChanged = path !== this.path,
      isForwardStep = pathChanged && path.length == this.path.length + 2;
    this.setPath(path);
    if (pathChanged) {
      if (this.study) this.study.setPath(path, this.node);
      if (isForwardStep) site.sound.move(this.node);
      this.threatMode(false);
      this.ceval?.stop();
      this.startCeval();
      site.sound.saySan(this.node.san, true);
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
    site.pubsub.emit('ply', this.node.ply, this.tree.lastMainlineNode(this.path).ply === this.node.ply);
    this.showGround();
    this.pluginUpdate(this.node.fen);
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

  mainlinePlyToPath(ply: Ply): Tree.Path {
    return treeOps.takePathWhile(this.mainline, n => n.ply <= ply);
  }

  jumpToMain = (ply: Ply): void => {
    this.userJump(this.mainlinePlyToPath(ply));
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
    this.configureCeval();
    this.instanciateEvalCache();
    this.cgVersion.js++;
  }

  changePgn(pgn: string, andReload: boolean): AnalyseData | undefined {
    this.pgnError = '';
    try {
      const data: AnalyseData = {
        ...pgnImport(pgn),
        orientation: this.bottomColor(),
        pref: this.data.pref,
        externalEngines: this.data.externalEngines,
      } as AnalyseData;
      if (andReload) {
        this.reloadData(data, false);
        this.userJump(this.mainlinePlyToPath(this.tree.lastPly()));
        this.redraw();
      }
      return data;
    } catch (err) {
      this.pgnError = (err as PgnError).message;
      this.redraw();
    }
    return undefined;
  }

  changeFen(fen: cg.FEN): void {
    this.redirecting = true;
    window.location.href =
      '/analysis/' +
      this.data.game.variant.key +
      '/' +
      encodeURIComponent(fen).replace(/%20/g, '_').replace(/%2F/g, '/');
  }

  crazyValid = (role: cg.Role, key: cg.Key): boolean => {
    const color = this.chessground.state.movable.color;
    return (
      (color === 'white' || color === 'black') &&
      crazyValid(this.chessground, this.node.drops, { color, role }, key)
    );
  };

  getCrazyhousePockets = () => this.node.crazy?.pockets;

  sendNewPiece = (role: cg.Role, key: cg.Key): void => {
    const color = this.chessground.state.movable.color;
    if (color === 'white' || color === 'black') this.userNewPiece({ color, role }, key);
  };

  userNewPiece = (piece: cg.Piece, pos: Key): void => {
    if (crazyValid(this.chessground, this.node.drops, piece, pos)) {
      this.justPlayed = roleToChar(piece.role).toUpperCase() + '@' + pos;
      this.justDropped = piece.role;
      this.justCaptured = undefined;
      site.sound.move();
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
    if (
      !this.promotion.start(orig, dest, {
        submit: (orig, dest, prom) => this.sendMove(orig, dest, capture, prom),
      })
    ) {
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

    const relayPath = this.study?.data.chapter.relayPath;
    if (relayPath && relayPath != newPath) this.forceVariation(newPath, true);
    else this.jump(newPath);

    this.redraw();
    const queuedUci = this.pvUciQueue.shift();
    if (queuedUci) this.playUci(queuedUci, this.pvUciQueue);
    else this.chessground.playPremove();
  }

  addDests(dests: string, path: Tree.Path): void {
    this.tree.addDests(dests, path);
    if (path === this.path) {
      this.showGround();
      this.pluginUpdate(this.node.fen);
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
          '?',
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

  setCollapsed(path: Tree.Path, collapsed: boolean): void {
    this.tree.setCollapsedAt(path, collapsed);
    this.redraw();
  }

  setAllCollapsed(path: Tree.Path, collapsed: boolean): void {
    // Also update parent
    const parentPath = treePath.init(path);
    this.tree.setCollapsedAt(parentPath, collapsed);
    this.tree.setCollapsedRecursive(path, collapsed);
    this.redraw();
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

  encodeNodeFen(): cg.FEN {
    return this.node.fen.replace(/\s/g, '_');
  }

  currentEvals() {
    return {
      server: this.node.eval,
      client: this.node.ceval,
    };
  }

  nextNodeBest() {
    return treeOps.withMainlineChild(this.node, (n: Tree.Node) => n.eval?.best);
  }

  setAutoShapes = (): void => {
    this.chessground?.setAutoShapes(computeAutoShapes(this));
  };

  private onNewCeval = (ev: Tree.ClientEval, path: Tree.Path, isThreat?: boolean): void => {
    this.tree.updateAt(path, (node: Tree.Node) => {
      if (node.fen !== ev.fen && !isThreat) return;
      if (isThreat) {
        const threat = ev as Tree.LocalEval;
        if (!node.threat || isEvalBetter(threat, node.threat)) node.threat = threat;
      } else if (!node.ceval || isEvalBetter(ev, node.ceval)) node.ceval = ev;
      else if (!ev.cloud) {
        if (node.ceval.cloud && this.ceval.isDeeper()) node.ceval = ev;
      }

      if (path === this.path) {
        this.setAutoShapes();
        if (!isThreat) {
          this.retro?.onCeval();
          this.practice?.onCeval();
          this.studyPractice?.onCeval();
          this.study?.multiCloudEval.onLocalCeval(node, ev);
          this.evalCache.onLocalCeval();
        }
        this.redraw();
      }
    });
  };

  private configureCeval(): void {
    const opts = {
      variant: this.data.game.variant,
      initialFen: this.data.game.initialFen,
      possible: this.synthetic || !game.playable(this.data),
      emit: (ev: Tree.ClientEval, work: EvalMeta) => {
        this.onNewCeval(ev, work.path, work.threatMode);
      },
      setAutoShapes: this.setAutoShapes,
      redraw: this.redraw,
      externalEngines:
        this.data.externalEngines?.map(engine => ({
          ...engine,
          endpoint: this.opts.externalEngineEndpoint,
        })) || [],
      onSelectEngine: () => {
        this.configureCeval();
        this.redraw();
      },
      search: this.practice?.search,
    };
    if (this.ceval) this.ceval.configure(opts);
    else this.ceval = new CevalCtrl(opts);
  }

  getCeval = () => this.ceval;

  outcome(node?: Tree.Node): Outcome | undefined {
    return this.position(node || this.node).unwrap(
      pos => pos.outcome(),
      _ => undefined,
    );
  }

  position(node: Tree.Node): Result<Position, PositionError> {
    const setup = parseFen(node.fen).unwrap();
    return setupPosition(lichessRules(this.data.game.variant.key), setup);
  }

  canUseCeval(): boolean {
    return !this.node.threefold && !this.outcome();
  }

  startCeval = throttle(800, () => {
    if (this.ceval?.enabled()) {
      if (this.canUseCeval()) {
        this.ceval.start(this.path, this.nodeList, this.threatMode());
        this.evalCache.fetch(this.path, this.ceval.search.multiPv);
      } else this.ceval.stop();
    }
  });

  ensureCevalRunning = () => {
    if (!this.showComputer()) this.toggleComputer();
    if (!this.ceval.enabled()) this.toggleCeval();
    if (this.threatMode()) this.toggleThreatMode();
  };

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
  };

  toggleThreatMode = () => {
    if (this.node.check || !this.showComputer()) return;
    if (!this.ceval.enabled()) this.ceval.toggle();
    if (!this.ceval.enabled()) return;
    this.threatMode(!this.threatMode());
    if (this.threatMode() && this.practice) this.togglePractice();
    this.setAutoShapes();
    this.startCeval();
    this.redraw();
  };

  disableThreatMode = (): boolean => {
    return !!this.practice;
  };

  mandatoryCeval = (): boolean => {
    return !!this.studyPractice;
  };

  restartCeval(): void {
    this.ceval.stop();
    this.startCeval();
    this.redraw();
  }

  clearCeval(): void {
    this.tree.removeCeval();
    this.evalCache.clear();
    this.restartCeval();
  }

  showEvalGauge(): boolean {
    return this.hasAnyComputerAnalysis() && this.showGauge() && !this.outcome() && this.showComputer();
  }

  hasAnyComputerAnalysis(): boolean {
    return this.data.analysis ? true : this.ceval.enabled();
  }

  hasFullComputerAnalysis = (): boolean => {
    return Object.keys(this.mainline[0].eval || {}).length > 0;
  };

  private resetAutoShapes() {
    if (this.showAutoShapes() || this.variationArrowsProp() || this.showMoveAnnotation())
      this.setAutoShapes();
    else this.chessground && this.chessground.setAutoShapes([]);
  }

  showVariationArrows() {
    const chap = this.study?.data.chapter;
    return (
      !isTouchDevice() &&
      !chap?.practice &&
      chap?.conceal === undefined &&
      !this.study?.gamebookPlay &&
      !this.retro?.isSolving() &&
      this.variationArrowsProp() &&
      this.node.children.filter(x => !x.comp || this.showComputer()).length > 1
    );
  }

  toggleAutoShapes = (v: boolean): void => {
    this.showAutoShapes(v);
    this.resetAutoShapes();
  };

  toggleVariationArrows = (v?: boolean): void => {
    this.variationArrowsProp(v ?? !this.variationArrowsProp());
    this.resetAutoShapes();
  };

  toggleGauge = () => {
    this.showGauge(!this.showGauge());
  };

  toggleMoveAnnotation = (v: boolean): void => {
    this.showMoveAnnotation(v);
    this.resetAutoShapes();
  };

  private onToggleComputer() {
    if (!this.showComputer()) {
      if (this.ceval.enabled()) this.toggleCeval();
    }
    this.resetAutoShapes();
  }

  toggleComputer = () => {
    if (this.ceval.enabled()) this.toggleCeval();
    const value = !this.showComputer();
    this.showComputer(value);
    if (!value && this.practice) this.togglePractice();
    this.onToggleComputer();
    site.pubsub.emit('analysis.comp.toggle', value);
  };

  mergeAnalysisData(data: ServerEvalData) {
    if (this.study && this.study.data.chapter.id !== data.ch) return;
    this.tree.merge(data.tree);
    this.data.analysis = data.analysis;
    if (data.analysis)
      data.analysis.partial = !!treeOps.findInMainline(data.tree, this.partialAnalysisCallback);
    if (data.division) this.data.game.division = data.division;
    if (this.retro) this.retro.onMergeAnalysisData();
    site.pubsub.emit('analysis.server.progress', this.data);
    this.redraw();
  }

  partialAnalysisCallback(n: Tree.Node) {
    return !n.eval && !!n.children.length && n.ply <= 300 && n.ply > 0;
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
        move.promotion,
      );
    } else
      this.chessground.newPiece(
        {
          color: this.chessground.state.movable.color as Color,
          role: move.role,
        },
        to,
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

  canEvalGet = (): boolean => {
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
  };

  instanciateEvalCache = () => {
    this.evalCache = new EvalCache({
      variant: this.data.game.variant.key,
      canGet: this.canEvalGet,
      canPut: () =>
        !!(
          this.ceval?.isCacheable &&
          this.canEvalGet() &&
          // if not in study, only put decent opening moves
          (this.opts.study || (!this.node.ceval!.mate && Math.abs(this.node.ceval!.cp!) < 99))
        ),
      getNode: () => this.node,
      send: this.opts.socketSend,
      receive: this.onNewCeval,
      upgradable: this.evalCache?.upgradable(),
    });
  };

  closeTools = () => {
    if (this.retro) this.retro = undefined;
    if (this.practice) this.togglePractice();
    if (this.explorer.enabled()) this.explorer.toggle();
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
    this.ceval.customSearch = this.practice?.search;
  };

  restartPractice() {
    this.practice = undefined;
    this.togglePractice();
  }

  gamebookPlay = (): GamebookPlayCtrl | undefined => this.study?.gamebookPlay;

  isGamebook = (): boolean => !!(this.study && this.study.data.chapter.gamebook);

  withCg = <A>(f: (cg: ChessgroundApi) => A): A | undefined =>
    this.chessground && this.cgVersion.js === this.cgVersion.dom ? f(this.chessground) : undefined;

  handleArrowKey = (arrowKey: ArrowKey) => {
    if (arrowKey === 'ArrowUp') {
      if (this.fork.prev()) this.setAutoShapes();
      else control.first(this);
    } else if (arrowKey === 'ArrowDown') {
      if (this.fork.next()) this.setAutoShapes();
      else control.last(this);
    } else if (arrowKey === 'ArrowLeft') control.prev(this);
    else if (arrowKey === 'ArrowRight') control.next(this);
    this.redraw();
  };

  pluginMove = (orig: cg.Key, dest: cg.Key, prom: cg.Role | undefined) => {
    const capture = this.chessground.state.pieces.get(dest);
    this.sendMove(orig, dest, capture, prom);
  };

  pluginUpdate = (fen: string) => {
    // if controller and chessground board state differ, ignore this update. once the chessground
    // state is updated to match, pluginUpdate will be called again.
    if (!fen.startsWith(this.chessground?.getFen())) return;

    this.keyboardMove?.update({ fen, canMove: true });
  };
}
