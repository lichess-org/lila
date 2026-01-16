import { playable, playedTurns, fenToEpd, validUci } from 'lib/game';
import * as keyboard from './keyboard';
import { treeReconstruct, plyColor, completeNode, addCrazyData } from './util';
import { plural } from './view/util';
import type GamebookPlayCtrl from './study/gamebook/gamebookPlayCtrl';
import type StudyCtrl from './study/studyCtrl';
import type { AnalyseOpts, AnalyseData, ServerEvalData, JustCaptured, NvuiPlugin } from './interfaces';
import type { Api as ChessgroundApi } from '@lichess-org/chessground/api';
import { Autoplay, type AutoplayDelay } from './autoplay';
import { makeTree, treePath, treeOps, type TreeWrapper } from 'lib/tree';
import { compute as computeAutoShapes } from './autoShape';
import type { Config as ChessgroundConfig } from '@lichess-org/chessground/config';
import type { CevalHandler, EvalMeta, CevalOpts } from 'lib/ceval';
import { CevalCtrl, isEvalBetter, sanIrreversible } from 'lib/ceval';
import { TreeView } from './treeView/treeView';
import type { Prop, Toggle } from 'lib';
import { defined, prop, toggle, debounce, throttle, requestIdleCallback, propWithEffect } from 'lib';
import { pubsub } from 'lib/pubsub';
import type { DrawShape } from '@lichess-org/chessground/draw';
import { chessgroundDests, lichessRules } from 'chessops/compat';
import EvalCache from './evalCache';
import { ForkCtrl } from './fork';
import { make as makePractice, type PracticeCtrl } from './practice/practiceCtrl';
import { make as makeRetro, type RetroCtrl } from './retrospect/retroCtrl';
import { make as makeSocket, type Socket } from './socket';
import { nextGlyphSymbol, add3or5FoldGlyphs } from './nodeFinder';
import { opposite, parseUci, makeSquare, roleToChar, makeUci, parseSquare } from 'chessops/util';
import { type Outcome, isNormal, type Move } from 'chessops/types';
import { makeFen, parseFen } from 'chessops/fen';
import type { Position, PositionError } from 'chessops/chess';
import type { Result } from '@badrap/result';
import { normalizeMove, setupPosition } from 'chessops/variant';
import { storedBooleanProp, storedBooleanPropWithEffect } from 'lib/storage';
import type { AnaMove } from './study/interfaces';
import { valid as crazyValid } from './crazy/crazyCtrl';
import { PromotionCtrl } from 'lib/game/promotion';
import wikiTheory, { wikiClear, type WikiTheory } from './wiki';
import ExplorerCtrl from './explorer/explorerCtrl';
import { uciToMove } from '@lichess-org/chessground/util';
import { IdbTree } from './idbTree';
import pgnImport from './pgnImport';
import ForecastCtrl from './forecast/forecastCtrl';
import { type ArrowKey, type KeyboardMove, ctrl as makeKeyboardMove } from 'keyboardMove';
import * as control from './control';
import type { PgnError } from 'chessops/pgn';
import { ChatCtrl } from 'lib/chat/chatCtrl';
import { confirm } from 'lib/view';
import api from './api';
import { displayColumns } from 'lib/device';
import MotifCtrl from './motif/motifCtrl';
import { makeSanAndPlay } from 'chessops/san';
import type { ClientEval, LocalEval, ServerEval, TreeNode, TreePath } from 'lib/tree/types';

export default class AnalyseCtrl implements CevalHandler {
  data: AnalyseData;
  element: HTMLElement;
  tree: TreeWrapper;
  socket: Socket;
  chessground: ChessgroundApi;
  ceval: CevalCtrl;
  evalCache: EvalCache;
  idbTree: IdbTree = new IdbTree(this);
  actionMenu: Toggle = toggle(false);
  isEmbed: boolean;

  // current tree state, cursor, and denormalized node lists
  path: TreePath;
  node: TreeNode;
  nodeList: TreeNode[];
  mainline: TreeNode[];

  // sub controllers
  autoplay: Autoplay;
  explorer: ExplorerCtrl;
  forecast?: ForecastCtrl;
  retro?: RetroCtrl;
  fork: ForkCtrl;
  practice?: PracticeCtrl;
  study?: StudyCtrl;
  promotion: PromotionCtrl;
  chatCtrl?: ChatCtrl;
  wiki?: WikiTheory;
  motif: MotifCtrl;

  // state flags
  justPlayed?: string; // pos
  justDropped?: string; // role
  justCaptured?: JustCaptured;
  redirecting = false;
  onMainline = true;
  synthetic: boolean; // false if coming from a real game
  ongoing: boolean; // true if real game is ongoing
  private cevalEnabledProp = storedBooleanProp('engine.enabled', false);

  // display flags
  flipped = false;
  showComments = true; // whether to display comments in the move tree
  showBestMoveArrowsProp: Prop<boolean>;
  variationArrowOpacity: Prop<number | false>;
  showGauge = storedBooleanProp('analyse.show-gauge', true);
  private showCevalProp: Prop<boolean> = storedBooleanProp('analyse.show-engine', !!this.cevalEnabledProp());
  showFishnetAnalysis = storedBooleanProp('analyse.show-computer', true);
  possiblyShowMoveAnnotationsOnBoard = storedBooleanProp('analyse.show-move-annotation', true);
  keyboardHelp: boolean = location.hash === '#keyboard';
  threatMode: Prop<boolean> = prop(false);
  disclosureMode = storedBooleanProp('analyse.disclosure.enabled', false);

  treeView: TreeView;
  cgVersion = {
    js: 1, // increment to recreate chessground
    dom: 1,
  };

  // underboard inputs
  fenInput?: string;
  pgnInput?: string;
  pgnError?: string;

  // other paths
  initialPath: TreePath;
  contextMenuPath?: TreePath;
  gamePath?: TreePath;
  pendingCopyPath: Prop<TreePath | null>;
  pendingDeletionPath: Prop<TreePath | null>;

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
    this.isEmbed = !!opts.embed;
    this.treeView = new TreeView(this);
    this.promotion = new PromotionCtrl(
      this.withCg,
      () => this.withCg(g => g.set(this.cgConfig)),
      this.redraw,
    );
    this.motif = new MotifCtrl(this.setAutoShapes);

    if (this.data.forecast) this.forecast = new ForecastCtrl(this.data.forecast, this.data, redraw);
    if (this.opts.wiki) this.wiki = wikiTheory();
    if (site.blindMode)
      site.asset.loadEsm<NvuiPlugin>('analyse.nvui', { init: this }).then(nvui => {
        this.nvui = nvui;
        this.redraw();
      });

    this.instanciateEvalCache();

    if (opts.inlinePgn) this.data = this.changePgn(opts.inlinePgn, false) || this.data;

    this.initialize(this.data, false);
    this.initCeval();
    this.pendingCopyPath = propWithEffect(null, this.redraw);
    this.pendingDeletionPath = propWithEffect(null, this.redraw);
    this.initialPath = this.makeInitialPath();
    this.setPath(this.initialPath);

    this.showGround();

    this.variationArrowOpacity = this.makeVariationOpacityProp();
    this.showBestMoveArrowsProp = storedBooleanPropWithEffect(
      'analyse.auto-shapes',
      true,
      this.setAutoShapes,
    );
    this.resetAutoShapes();
    this.explorer.setNode();
    this.study =
      opts.study && makeStudy
        ? new makeStudy(opts.study, this, (opts.tagTypes || '').split(','), opts.practice, opts.relay)
        : undefined;

    if (location.hash === '#practice' || (this.study && this.study.data.chapter.practice))
      this.togglePractice();
    else if (location.hash === '#menu') requestIdleCallback(this.actionMenu.toggle, 500);
    this.setCevalPracticeOpts();
    this.startCeval();
    keyboard.bind(this);

    const urlEngine = new URLSearchParams(location.search).get('engine');
    if (urlEngine) {
      try {
        this.ceval.engines.select(urlEngine);
        this.cevalEnabled(true);
        this.threatMode(false);
      } catch (e) {
        console.info(e);
      }
      site.redirect('/analysis');
    }
    if (this.opts.chat && !this.isEmbed) {
      this.chatCtrl = new ChatCtrl(
        { ...this.opts.chat, enhance: { plies: true, boards: !!this.study?.relay } },
        this.redraw,
      );
    }
    pubsub.on('jump', (ply: string) => {
      this.jumpToMain(parseInt(ply));
      this.redraw();
    });

    pubsub.on('ply.trigger', () =>
      pubsub.emit('ply', this.node.ply, this.tree.lastMainlineNode(this.path).ply === this.node.ply),
    );
    pubsub.on('analysis.chart.click', index => {
      this.jumpToIndex(index);
      this.redraw();
    });
    pubsub.on('board.change', (is3d: boolean) => {
      if (this.chessground) {
        this.chessground.state.addPieceZIndex = is3d;
        this.chessground.redrawAll();
        redraw();
      }
    });
    this.mergeIdbThenShowTreeView();
    (window as any).lichess.analysis = api(this);
  }

  initialize(data: AnalyseData, merge: boolean): void {
    this.data = data;
    this.synthetic = data.game.id === 'synthetic';
    this.ongoing = !this.synthetic && playable(data);
    this.treeView.hidden = true;
    const prevTree = merge && this.tree.root;
    this.tree = makeTree(treeReconstruct(this.data.treeParts, this.data.sidelines));
    if (prevTree) this.tree.merge(prevTree);
    const mainline = treeOps.mainlineNodeList(this.tree.root);
    if (this.data.game.status.name === 'draw') {
      if (add3or5FoldGlyphs(mainline)) this.data.game.threefold = true;
    }

    this.autoplay = new Autoplay(this);
    this.socket ??= makeSocket(this.opts.socketSend, this);
    if (this.explorer) this.explorer.destroy();
    this.explorer = new ExplorerCtrl(this, this.opts.explorer, this.explorer);
    this.gamePath = this.synthetic || this.ongoing ? undefined : treePath.fromNodeList(mainline);
    this.fork = new ForkCtrl(this);

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

  private setPath = (path: TreePath): void => {
    this.path = path;
    this.nodeList = this.tree.getNodeList(path);
    this.node = treeOps.last(this.nodeList) as TreeNode;
    this.mainline = treeOps.mainlineNodeList(this.tree.root);
    this.onMainline = this.tree.pathIsMainline(path);
    this.fenInput = undefined;
    this.pgnInput = undefined;
    if (this.wiki && this.data.game.variant.key === 'standard') this.wiki(this.nodeList);
    this.idbTree.saveMoves();
    this.idbTree.revealNode();
  };

  flip = () => {
    if (this.study?.onFlip(!this.flipped) === false) return;
    this.flipped = !this.flipped;
    this.chessground?.set({
      orientation: this.bottomColor(),
    });
    if (this.retro && this.data.game.variant.key !== 'racingKings')
      this.retro = makeRetro(this, this.bottomColor());
    if (this.practice) this.startCeval();
    this.explorer.onFlip();
    this.onChange();
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
    return this.bottomColor();
  }

  getNode(): TreeNode {
    return this.node;
  }

  turnColor(): Color {
    return plyColor(this.node.ply);
  }

  togglePlay(delay: AutoplayDelay): void {
    this.autoplay.toggle(delay);
    this.actionMenu(false);
  }

  private showGround(): void {
    this.ensureDests();
    this.onChange();
    this.withCg(cg => {
      cg.set(this.makeCgOpts());
      this.setAutoShapes();
      if (this.node.shapes) cg.setShapes(this.node.shapes.slice() as DrawShape[]);
      cg.playPremove();
    });
  }

  private ensureDests: () => void = () => {
    if (defined(this.node.dests)) return;
    this.position(this.node).unwrap(
      position => {
        this.node.dests = chessgroundDests(position);
        if (this.data.game.variant.key === 'crazyhouse') {
          const drops = position.dropDests();
          if (drops) this.node.drops = Array.from(drops, makeSquare);
        }
        this.node.check = position.isCheck();
        if (position.outcome()) this.ceval.stop();
      },
      err => {
        console.error(err);
        this.node.dests = new Map();
      },
    );
    this.pluginUpdate(this.node.fen);
  };

  serverMainline = () => this.mainline.slice(0, playedTurns(this.data) + 1);

  makeCgOpts(): ChessgroundConfig {
    const node = this.node,
      color = this.turnColor(),
      dests = this.node.dests,
      drops = this.node.drops,
      gamebookPlay = this.gamebookPlay(),
      movableColor = gamebookPlay
        ? gamebookPlay.movableColor()
        : this.practice
          ? this.bottomColor()
          : dests?.size || drops?.length
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

    if (this.data.pref.keyboardMove && !this.study?.relay) {
      this.keyboardMove ??= makeKeyboardMove({
        ...this,
        data: { ...this.data, player: { color: 'both' } },
        flipNow: this.flip,
      });
      this.keyboardMove.update({ fen: this.node.fen, canMove: true, cg });
      requestAnimationFrame(() => this.redraw());
    }

    this.setAutoShapes();
    if (this.node.shapes) this.chessground.setShapes(this.node.shapes.slice() as DrawShape[]);
    this.cgVersion.dom = this.cgVersion.js;
  };

  private onChange: () => void = throttle(300, () => {
    pubsub.emit('analysis.change', this.node.fen, this.path);
  });

  private updateHref: () => void = debounce(() => {
    if (!this.opts.study) window.history.replaceState(null, '', '#' + this.node.ply);
  }, 750);

  playedLastMoveMyself = () =>
    !!this.justPlayed && !!this.node.uci && this.node.uci.startsWith(this.justPlayed);

  jump(path: TreePath): void {
    const pathChanged = path !== this.path,
      isForwardStep = pathChanged && path.length === this.path.length + 2;
    if (this.path !== path)
      this.treeView.requestAutoScroll(treeOps.distance(this.path, path) > 8 ? 'instant' : 'smooth');
    this.setPath(path);
    if (pathChanged) {
      if (this.study) this.study.setPath(path, this.node);
      if (this.retro) this.retro.onJump();
      if (isForwardStep) site.sound.move(this.node);
      this.threatMode(false);
      this.ceval?.stop();
      this.startCeval();
      site.sound.saySan(this.node.san, true);
    }
    this.justPlayed = this.justDropped = this.justCaptured = undefined;
    this.explorer.setNode();
    this.updateHref();
    this.promotion.cancel();
    if (pathChanged) {
      if (this.practice) this.practice.onJump();
      if (this.study) this.study.onJump();
    }
    pubsub.emit('ply', this.node.ply, this.tree.lastMainlineNode(this.path).ply === this.node.ply);
    this.showGround();
    this.pluginUpdate(this.node.fen);
  }

  userJump = (path: TreePath): void => {
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

  canJumpTo = (path: TreePath): boolean => !this.study || this.study.canJumpTo(path);

  userJumpIfCan(path: TreePath, sideStep = false): void {
    if (path === this.path || !this.canJumpTo(path)) return;
    if (sideStep) {
      // when stepping lines, anchor the chessground animation at the parent
      this.node = this.tree.nodeAtPath(path.slice(0, -2));
      this.chessground?.set(this.makeCgOpts());
      this.chessground?.state.dom.redrawNow(true);
    }
    this.userJump(path);
  }

  mainlinePlyToPath(ply: Ply): TreePath {
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
    this.initCeval();
    this.instanciateEvalCache();
    this.cgVersion.js++;
    this.mergeIdbThenShowTreeView();
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
      requestAnimationFrame(this.redraw);
    }
    return undefined;
  }

  changeFen(fen: FEN): void {
    this.redirecting = true;
    window.location.href =
      '/analysis/' +
      this.data.game.variant.key +
      '/' +
      encodeURIComponent(fen).replace(/%20/g, '_').replace(/%2F/g, '/');
  }

  crazyValid = (role: Role, key: Key): boolean => {
    const color = this.chessground.state.movable.color;
    return (
      (color === 'white' || color === 'black') &&
      crazyValid(this.chessground, this.node.drops, { color, role }, key)
    );
  };

  getCrazyhousePockets = () => this.node.crazy?.pockets; // keyboardMove

  sendNewPiece = (role: Role, key: Key): void => {
    const color = this.chessground.state.movable.color;
    if (color === 'white' || color === 'black') this.userNewPiece({ color, role }, key);
  };

  userNewPiece = (piece: Piece, pos: Key): void => {
    if (crazyValid(this.chessground, this.node.drops, piece, pos)) {
      this.justPlayed = roleToChar(piece.role).toUpperCase() + '@' + pos;
      this.justDropped = piece.role;
      this.justCaptured = undefined;
      const drop = {
        role: piece.role,
        pos,
        variant: this.data.game.variant.key,
        fen: this.node.fen,
        path: this.path,
      };
      if (this.study) this.socket.sendAnaDrop(drop);
      this.addNodeLocally({
        role: piece.role,
        to: parseSquare(pos)!,
      });
    } else this.jump(this.path);
  };

  userMove = (orig: Key, dest: Key, capture?: JustCaptured): void => {
    this.justPlayed = orig;
    this.justDropped = undefined;
    if (
      !this.promotion.start(orig, dest, {
        submit: (orig, dest, prom) => this.sendMove(orig, dest, capture, prom),
      })
    )
      this.sendMove(orig, dest, capture);
  };

  sendMove = (orig: Key, dest: Key, capture?: JustCaptured, prom?: Role): void => {
    const move: AnaMove = {
      orig,
      dest,
      variant: this.data.game.variant.key,
      fen: this.node.fen,
      path: this.path,
    };
    if (prom) move.promotion = prom;
    if (capture) this.justCaptured = capture;
    if (this.practice) this.practice.onUserMove();
    if (this.study) this.socket.sendAnaMove(move);
    this.addNodeLocally({
      from: parseSquare(orig)!,
      to: parseSquare(dest)!,
      promotion: prom,
    });
  };

  onPremoveSet = () => {
    if (this.study) this.study.onPremoveSet();
  };

  private addNodeLocally(move: Move): void {
    const pos = this.position(this.node).unwrap();
    move = normalizeMove(pos, move);
    const san = makeSanAndPlay(pos, move);
    const node = completeNode({
      ply: this.node.ply + 1,
      uci: makeUci(move),
      san,
      fen: makeFen(pos.toSetup()),
    });
    addCrazyData(node, pos);
    this.addNode(node, this.path);
  }

  addNode(node: TreeNode, path: TreePath) {
    this.idbTree.onAddNode(node, path);
    const newPath = this.tree.addNode(node, path);
    if (!newPath) {
      console.log("Can't addNode", node, path);
      return this.redraw();
    }

    const relayPath = this.study?.data.chapter.relayPath;
    if (relayPath && relayPath === path) this.forceVariation(newPath, true);
    else this.jump(newPath);

    this.redraw();
    const queuedUci = this.pvUciQueue.shift();
    if (queuedUci) this.playUci(queuedUci, this.pvUciQueue);
    else this.chessground.playPremove();
  }

  async deleteNode(path: TreePath): Promise<void> {
    this.pendingDeletionPath(null);
    const node = this.tree.nodeAtPath(path);
    if (!node) return;
    const count = treeOps.countChildrenAndComments(node);
    if (
      (count.nodes >= 10 || count.comments > 0) &&
      !(await confirm(
        'Delete ' +
          plural('move', count.nodes) +
          (count.comments ? ' and ' + plural('comment', count.comments) : '') +
          '?',
      ))
    )
      return;
    this.tree.deleteNodeAt(path);
    if (treePath.contains(this.path, path)) this.userJump(treePath.init(path));
    else this.jump(this.path);
    if (this.study) this.study.deleteNode(path);
    this.redraw();
  }

  allowedEval(node: TreeNode = this.node): ClientEval | ServerEval | false | undefined {
    return (this.cevalEnabled() && node.ceval) || (this.showFishnetAnalysis() && node.eval);
  }

  motifAllowed = (): boolean => this.study?.isCevalAllowed() !== false;
  motifEnabled = (): boolean => this.motifAllowed() && this.motif.supports(this.data.game.variant.key);

  outcome(node?: TreeNode): Outcome | undefined {
    return this.position(node || this.node).unwrap(
      pos => pos.outcome(),
      _ => undefined,
    );
  }

  position(node: TreeNode): Result<Position, PositionError> {
    const setup = parseFen(node.fen).unwrap();
    return setupPosition(lichessRules(this.data.game.variant.key), setup);
  }

  promote(path: TreePath, toMainline: boolean): void {
    this.tree.promoteAt(path, toMainline);
    this.jump(path);
    if (this.study) this.study.promote(path, toMainline);
  }

  forceVariation(path: TreePath, force: boolean): void {
    this.tree.forceVariationAt(path, force);
    this.jump(path);
    if (this.study) this.study.forceVariation(path, force);
  }

  visibleChildren(node = this.node): TreeNode[] {
    return node.children.filter(
      kid =>
        !kid.comp ||
        (this.showFishnetAnalysis() && !this.retro?.hideComputerLine(kid)) ||
        (treeOps.contains(kid, this.node) && !this.retro?.forceCeval()),
    );
  }

  reset(): void {
    this.showGround();
    this.redraw();
  }

  encodeNodeFen(): FEN {
    return this.node.fen.replace(/\s/g, '_');
  }

  nextNodeBest() {
    return treeOps.withMainlineChild(this.node, (n: TreeNode) => validUci(n.eval?.best));
  }

  setAutoShapes = (): void => {
    if (!site.blindMode) this.chessground?.setAutoShapes(computeAutoShapes(this));
  };

  private onNewCeval = (ev: ClientEval, path: TreePath, isThreat?: boolean): void => {
    this.tree.updateAt(path, (node: TreeNode) => {
      if (node.fen !== ev.fen && !isThreat) return;

      if (isThreat) {
        const threat = ev as LocalEval;
        if (!node.threat || isEvalBetter(threat, node.threat, this.ceval.search.multiPv))
          node.threat = threat;
      } else if (
        (!node.ceval || isEvalBetter(ev, node.ceval, this.ceval.search.multiPv)) &&
        !(ev.cloud && this.ceval.engines.external)
      )
        node.ceval = ev;
      else if (!ev.cloud) {
        if (node.ceval?.cloud && this.ceval.isDeeper()) node.ceval = ev;
      }

      if (path === this.path) {
        this.setAutoShapes();
        if (!isThreat) {
          this.retro?.onCeval();
          this.practice?.onCeval();
          this.study?.practice?.onCeval();
          this.study?.multiCloudEval?.onLocalCeval(node, ev);
          this.evalCache.onLocalCeval();
        }
        if (!(site.blindMode && this.retro)) this.redraw();
      }
    });
  };

  private initCeval(): void {
    const opts: CevalOpts = {
      variant: this.data.game.variant,
      initialFen: this.data.game.initialFen,
      emit: (ev: ClientEval, work: EvalMeta) => this.onNewCeval(ev, work.path, work.threatMode),
      onUciHover: this.setAutoShapes,
      redraw: this.redraw,
      externalEngines:
        this.data.externalEngines?.map(engine => ({
          ...engine,
          endpoint: this.opts.externalEngineEndpoint,
        })) || [],
      onSelectEngine: () => {
        this.initCeval();
        this.redraw();
      },
    };
    if (this.ceval) this.ceval.init(opts);
    else this.ceval = new CevalCtrl(opts);
  }

  isCevalAllowed = () =>
    !this.ongoing &&
    this.study?.isCevalAllowed() !== false &&
    (this.synthetic || !playable(this.data)) &&
    !location.search.includes('evals=0');

  cevalEnabled = (enable?: boolean): boolean | 'force' => {
    const force = Boolean(this.study?.practice || this.practice || this.retro?.forceCeval());
    const unforcedState = this.cevalEnabledProp() && this.isCevalAllowed() && !this.ceval.isPaused;

    if (enable === undefined) return force ? 'force' : unforcedState;
    if (!force) {
      this.showCevalProp(enable);
      this.cevalEnabledProp(enable);
    }
    if (enable && this.ceval.isPaused) this.ceval.resume();
    if (enable !== unforcedState) {
      if (enable) this.startCeval();
      else {
        this.threatMode(false);
        this.ceval.stop();
      }
      this.setAutoShapes();
      this.ceval.showEnginePrefs(false);
      this.redraw();
    }
    return force ? 'force' : enable;
  };

  startCeval = () => {
    if (!this.ceval.download) this.ceval.stop();
    if (this.node.threefold || !this.cevalEnabled() || this.outcome()) return;
    this.ceval.start(this.path, this.nodeList, undefined, this.threatMode());
    this.evalCache.fetch(this.path, this.ceval.search.multiPv);
  };

  clearCeval(): void {
    this.tree.removeCeval();
    this.evalCache.clear();
    this.startCeval();
  }

  showVariationArrows() {
    if (!this.allowLines()) return false;
    const kids = this.variationArrowOpacity() ? this.node.children : [];
    return Boolean(kids.filter(x => !x.comp || this.showFishnetAnalysis()).length);
  }

  showAnalysis() {
    return this.showFishnetAnalysis() || (this.cevalEnabled() && this.isCevalAllowed());
  }

  showMoveGlyphs = (): boolean => (this.study && !this.study.relay) || this.showFishnetAnalysis();

  showMoveAnnotationsOnBoard = (): boolean =>
    this.possiblyShowMoveAnnotationsOnBoard() && this.showMoveGlyphs();

  showEvalGauge(): boolean {
    return (
      this.showGauge() &&
      displayColumns() > 1 &&
      this.showAnalysis() &&
      this.isCevalAllowed() &&
      (this.cevalEnabled() || !!this.node.eval || !!this.node.ceval) &&
      !this.outcome()
    );
  }

  showCeval = (show?: boolean) => {
    const barMode = this.activeControlMode();
    if (show === undefined) return displayColumns() > 1 || barMode === 'ceval' || barMode === 'practice';
    this.ceval.showEnginePrefs(false);
    this.showCevalProp(show);
    if (show) this.cevalEnabled(true);
    return show;
  };

  activeControlMode = () =>
    !!this.study?.practice
      ? 'learn-practice'
      : !!this.practice
        ? 'practice'
        : !!this.retro
          ? 'retro'
          : this.showCevalProp()
            ? 'ceval'
            : false;

  activeControlBarTool() {
    return this.actionMenu() ? 'action-menu' : this.explorer.enabled() ? 'opening-explorer' : false;
  }

  allowLines() {
    const chap = this.study?.data.chapter;
    return (
      !chap?.practice && chap?.conceal === undefined && !this.study?.gamebookPlay && !this.retro?.isSolving()
    );
  }

  toggleDiscloseOf(path = this.path.slice(0, -2)) {
    const disclose = this.idbTree.discloseOf(this.tree.nodeAtPath(path), this.tree.pathIsMainline(path));
    if (disclose) this.idbTree.setCollapsed(path, disclose === 'expanded');
    return Boolean(disclose);
  }

  toggleThreatMode = (v = !this.threatMode()) => {
    if (v === this.threatMode()) return;
    if (this.node.check || !this.showAnalysis()) return;
    if (!this.cevalEnabled()) return;
    this.threatMode(v);
    if (this.threatMode() && this.practice) this.togglePractice();
    this.setAutoShapes();
    this.startCeval();
    this.redraw();
  };

  togglePossiblyShowMoveAnnotationsOnBoard = (v: boolean): void => {
    this.possiblyShowMoveAnnotationsOnBoard(v);
    this.resetAutoShapes();
  };

  toggleFishnetAnalysis = () => {
    this.showFishnetAnalysis(!this.showFishnetAnalysis());
    this.resetAutoShapes();
    pubsub.emit('analysis.comp.toggle', this.showFishnetAnalysis());
  };

  toggleActionMenu = () => {
    if (!this.actionMenu() && this.explorer.enabled()) this.explorer.toggle();
    this.actionMenu.toggle();
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
    if (!this.explorer.allowed()) return;
    if (!this.explorer.enabled()) {
      this.retro = undefined;
      this.actionMenu(false);
    }
    this.explorer.toggle();
  };

  togglePractice = (enable = !this.practice) => {
    if (enable === !!this.practice) return;
    this.practice = undefined;
    if (!enable || !this.isCevalAllowed()) {
      this.setCevalPracticeOpts();
      this.showGround();
    } else {
      this.closeTools();
      this.threatMode(false);
      this.practice = makePractice(this, this.study?.practice?.playableDepth);
      this.setCevalPracticeOpts();
      this.setAutoShapes();
      this.startCeval();
    }
  };

  private setCevalPracticeOpts() {
    this.ceval.setOpts({ custom: this.study?.practice?.customCeval ?? this.practice?.customCeval });
  }

  gamebookPlay = (): GamebookPlayCtrl | undefined => this.study?.gamebookPlay;

  isGamebook = (): boolean => !!this.study?.data.chapter.gamebook;

  private closeTools = () => {
    this.retro = undefined;
    this.togglePractice(false);
    if (this.explorer.enabled()) this.explorer.toggle();
    this.actionMenu(false);
  };

  withCg = <A>(f: (cg: ChessgroundApi) => A): A | undefined =>
    this.chessground && this.cgVersion.js === this.cgVersion.dom ? f(this.chessground) : undefined;

  hasFullComputerAnalysis = (): boolean => {
    return Object.keys(this.mainline[0].eval || {}).length > 0;
  };

  mergeAnalysisData(data: ServerEvalData) {
    if (this.study && this.study.data.chapter.id !== data.ch) return;
    const tree = completeNode(data.tree);
    this.tree.merge(tree);
    this.data.analysis = data.analysis;
    if (data.analysis) data.analysis.partial = !!treeOps.findInMainline(tree, this.partialAnalysisCallback);
    if (data.division) this.data.game.division = data.division;
    if (this.retro) this.retro.onMergeAnalysisData();
    pubsub.emit('analysis.server.progress', this.data);
    this.redraw();
  }

  partialAnalysisCallback(n: TreeNode) {
    return !n.eval && !!n.children.length && n.ply <= 300 && n.ply > 0;
  }

  private canEvalGet = (): boolean => {
    if (this.node.ply >= 15 && !this.opts.study) return false;

    // cloud eval does not support threefold repetition
    const fens = new Set();
    for (let i = this.nodeList.length - 1; i >= 0; i--) {
      const node = this.nodeList[i];
      const epd = fenToEpd(node.fen);
      if (fens.has(epd)) return false;
      if (node.san && sanIrreversible(this.data.game.variant.key, node.san)) return true;
      fens.add(epd);
    }
    return true;
  };

  private instanciateEvalCache = () => {
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

  playUci = (uci: Uci, uciQueue?: Uci[]) => {
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
  };

  playUciList(uciList: Uci[]): void {
    this.pvUciQueue = uciList;
    const firstUci = this.pvUciQueue.shift();
    if (firstUci) this.playUci(firstUci, this.pvUciQueue);
  }

  explorerMove(uci: Uci): void {
    this.playUci(uci);
    this.explorer.loading(true);
  }

  playBestMove(): void {
    const uci = this.node.ceval?.pvs[0].moves[0] || this.nextNodeBest();
    if (uci) this.playUci(uci);
  }

  pluginMove = (orig: Key, dest: Key, prom: Role | undefined): void => {
    const capture = this.chessground.state.pieces.get(dest);
    this.sendMove(orig, dest, capture, prom);
  };

  handleArrowKey = (arrowKey: ArrowKey): void => {
    if (arrowKey === 'ArrowUp') {
      if (this.fork.select('prev')) this.setAutoShapes();
      else control.first(this);
    } else if (arrowKey === 'ArrowDown') {
      if (this.fork.select('next')) this.setAutoShapes();
      else control.last(this);
    } else if (arrowKey === 'ArrowLeft') control.prev(this);
    else if (arrowKey === 'ArrowRight') control.next(this);
    this.redraw();
  };

  toggleVariationArrows = () => {
    const trueValue = this.variationArrowOpacity(false);
    this.variationArrowOpacity(trueValue === 0 ? 0.6 : -trueValue);
  };

  private makeVariationOpacityProp(): Prop<number | false> {
    let value = parseFloat(localStorage.getItem('analyse.variation-arrow-opacity') || '0');
    if (isNaN(value) || value < -1 || value > 1) value = 0;
    return (v?: number | false) => {
      if (v === false) return value;
      if (v === undefined || isNaN(v)) return value > 0 ? value : false;
      value = Math.min(1, Math.max(-1, v));
      localStorage.setItem('analyse.variation-arrow-opacity', value.toString());
      this.setAutoShapes();
      this.chessground.redrawAll();
      this.redraw();
      return value;
    };
  }

  private pluginUpdate = (fen: string) => {
    // If controller and chessground board states differ, ignore this update. Once the chessground
    // state is updated to match, pluginUpdate will be called again.
    if (!fen.startsWith(this.chessground?.getFen())) return;
    this.keyboardMove?.update({ fen, canMove: true });
  };

  showBestMoveArrows = () => this.showBestMoveArrowsProp() && !this.retro?.hideComputerLine(this.node);

  private resetAutoShapes = () => {
    if (
      this.showBestMoveArrows() ||
      this.possiblyShowMoveAnnotationsOnBoard() ||
      this.variationArrowOpacity() ||
      (this.motifEnabled() && this.motif.any())
    )
      this.setAutoShapes();
    else this.chessground?.setAutoShapes([]);
  };

  private async mergeIdbThenShowTreeView() {
    await this.idbTree.merge();
    this.treeView.hidden = false;
    this.idbTree.revealNode();
    this.redraw();
  }
}
