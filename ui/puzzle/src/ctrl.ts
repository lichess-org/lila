import * as speech from './speech';
import * as xhr from './xhr';
import * as router from 'common/router';
import computeAutoShapes from './autoShape';
import { bindHotkeys } from './keyboard';
import moveTest from './moveTest';
import PuzzleSession from './session';
import PuzzleStreak from './streak';
import { Vm, PuzzleOpts, PuzzleData, MoveTest, ThemeKey, NvuiPlugin, ReplayEnd } from './interfaces';
import { Api as CgApi } from 'chessground/api';
import { build as treeBuild, ops as treeOps, path as treePath, TreeWrapper } from 'tree';
import { Chess, normalizeMove } from 'chessops/chess';
import { chessgroundDests, scalachessCharPair } from 'chessops/compat';
import { Config as CgConfig } from 'chessground/config';
import { CevalCtrl } from 'ceval';
import { makeVoiceMove, VoiceMove, RootCtrl as VoiceRoot } from 'voice';
import { ctrl as makeKeyboardMove, KeyboardMove, RootController as KeyboardRoot } from 'keyboardMove';
import { defer } from 'common/defer';
import { defined, prop, Prop, propWithEffect, Toggle, toggle } from 'common';
import { makeSanAndPlay } from 'chessops/san';
import { parseFen, makeFen } from 'chessops/fen';
import { parseSquare, parseUci, makeSquare, makeUci, opposite } from 'chessops/util';
import { pgnToTree, mergeSolution } from './moveTree';
import { PromotionCtrl } from 'chess/promotion';
import { Role, Move, Outcome } from 'chessops/types';
import { StoredProp, ToggleWithUsed, storedToggle } from 'common/storage';
import { fromNodeList } from 'tree/dist/path';
import { last } from 'tree/dist/ops';
import { uciToMove } from 'chessground/util';
import { toggle as boardMenuToggle } from 'board/menu';
import { Redraw } from 'common/snabbdom';
import { ParentCtrl } from 'ceval/src/types';
import PuzzleSounds from './sounds';

export default class PuzzleController implements ParentCtrl {
  vm: Vm = {
    next: defer<PuzzleData>(),
  } as Vm;
  data: PuzzleData;
  tree: TreeWrapper;
  ceval: CevalCtrl;
  autoNext: StoredProp<boolean>;
  rated: Toggle;
  threatMode = toggle(false);
  ground = prop<CgApi | undefined>(undefined) as Prop<CgApi>;
  streak?: PuzzleStreak;
  session: PuzzleSession;
  promotion: PromotionCtrl;
  menu: ToggleWithUsed;
  private streakFailStorage = lichess.storage.make('puzzle.streak.fail');
  sound = new PuzzleSounds();
  music: any;
  flipped = toggle(false);
  trans: Trans;

  keyboardMove?: KeyboardMove;
  voiceMove?: VoiceMove;
  ongoing = false; // implements ParentCtrl with no ongoing game

  constructor(public opts: PuzzleOpts, readonly redraw: Redraw) {
    const hasStreak = !!opts.data.streak;
    this.rated = storedToggle('puzzle.rated', true, this.redraw);
    this.autoNext = storedToggle(`puzzle.autoNext${hasStreak ? '.streak' : ''}`, hasStreak);
    this.streak = hasStreak ? new PuzzleStreak(opts.data) : undefined;
    if (this.streak) {
      opts.data = {
        ...opts.data,
        ...this.streak.data.current,
      };
      this.streakFailStorage.listen(_ => this.failStreak(this.streak!));
    }
    this.session = new PuzzleSession(opts.data.angle.key, opts.data.user?.id, hasStreak);
    this.menu = boardMenuToggle(this.redraw);
    this.trans = lichess.trans(opts.i18n);
    this.promotion = new PromotionCtrl(
      this.withGround,
      () => this.withGround(g => g.set(this.vm.cgConfig)),
      this.redraw
    );

    this.initiate(opts.data);

    bindHotkeys(this);

    // If the page loads while being hidden (like when changing settings),
    // chessground is not displayed, and the first move is not fully applied.
    // Make sure chessground is fully shown when the page goes back to being visible.
    document.addEventListener('visibilitychange', () =>
      lichess.requestIdleCallback(() => this.jump(this.vm.path), 500)
    );

    speech.setup();

    lichess.pubsub.on('sound_set', (set: string) => {
      if (!this.music && set === 'music')
        lichess.loadScript('javascripts/music/play.js').then(() => {
          this.music = lichess.playMusic();
        });
      if (this.music && set !== 'music') this.music = undefined;
    });

    lichess.pubsub.on('zen', () => {
      const zen = $('body').toggleClass('zen').hasClass('zen');
      window.dispatchEvent(new Event('resize'));
      xhr.setZen(zen);
    });
    $('body').addClass('playing'); // for zen
    $('#zentog').on('click', () => lichess.pubsub.emit('zen'));
  }

  setPath = (path: Tree.Path): void => {
    this.vm.path = path;
    this.vm.nodeList = this.tree.getNodeList(path);
    this.vm.node = treeOps.last(this.vm.nodeList)!;
    this.vm.mainline = treeOps.mainlineNodeList(this.tree.root);
  };

  setChessground = (cg: CgApi): void => {
    this.ground(cg);
    const makeRoot = () => ({
      data: {
        game: { variant: { key: 'standard' } },
        player: { color: this.vm.pov },
      },
      chessground: cg,
      sendMove: this.playUserMove,
      redraw: this.redraw,
      flipNow: this.flip,
      userJumpPlyDelta: this.userJumpPlyDelta,
      next: this.nextPuzzle,
      vote: this.vote,
      solve: this.viewSolution,
    });
    if (this.opts.pref.voiceMove)
      makeVoiceMove(makeRoot() as VoiceRoot, this.vm.node.fen).then(vm => {
        this.voiceMove = vm;
      });
    if (this.opts.pref.keyboardMove)
      this.keyboardMove = makeKeyboardMove(makeRoot() as KeyboardRoot, { fen: this.vm.node.fen });
    requestAnimationFrame(() => this.redraw());
  };

  private withGround = <A>(f: (cg: CgApi) => A): A | undefined => {
    const g = this.ground();
    return g && f(g);
  };

  private initiate = (data: PuzzleData): void => {
    this.data = data;
    this.tree = treeBuild(pgnToTree(data.game.pgn.split(' ')));
    const initialPath = treePath.fromNodeList(treeOps.mainlineNodeList(this.tree.root));
    this.vm.mode = 'play';
    this.vm.next = defer();
    this.vm.round = undefined;
    this.vm.justPlayed = undefined;
    this.vm.resultSent = false;
    this.vm.lastFeedback = 'init';
    this.vm.initialPath = initialPath;
    this.vm.initialNode = this.tree.nodeAtPath(initialPath);
    this.vm.pov = this.vm.initialNode.ply % 2 == 1 ? 'black' : 'white';
    this.vm.isDaily = location.href.endsWith('/daily');

    this.setPath(window.LichessPuzzleNvui ? initialPath : treePath.init(initialPath));
    setTimeout(
      () => {
        this.jump(initialPath);
        this.redraw();
      },
      this.opts.pref.animation.duration > 0 ? 500 : 0
    );

    // just to delay button display
    this.vm.canViewSolution = false;
    setTimeout(() => {
      this.vm.canViewSolution = true;
      this.redraw();
    }, 4000);

    this.withGround(g => {
      g.selectSquare(null);
      g.setAutoShapes([]);
      g.setShapes([]);
      this.showGround(g);
    });

    this.instantiateCeval();
  };

  position = (): Chess => Chess.fromSetup(parseFen(this.vm.node.fen).unwrap()).unwrap();

  makeCgOpts = (): CgConfig => {
    const node = this.vm.node;
    const color: Color = node.ply % 2 === 0 ? 'white' : 'black';
    const dests = chessgroundDests(this.position());
    const nextNode = this.vm.node.children[0];
    const canMove = this.vm.mode === 'view' || (color === this.vm.pov && (!nextNode || nextNode.puzzle == 'fail'));
    const movable = canMove
      ? {
          color: dests.size > 0 ? color : undefined,
          dests,
        }
      : {
          color: undefined,
          dests: new Map(),
        };
    const config = {
      fen: node.fen,
      orientation: this.flipped() ? opposite(this.vm.pov) : this.vm.pov,
      turnColor: color,
      movable: movable,
      premovable: {
        enabled: false,
      },
      check: !!node.check,
      lastMove: uciToMove(node.uci),
    };
    if (node.ply >= this.vm.initialNode.ply) {
      if (this.vm.mode !== 'view' && color !== this.vm.pov && !nextNode) {
        config.movable.color = this.vm.pov;
        config.premovable.enabled = true;
      }
    }
    this.vm.cgConfig = config;
    return config;
  };

  showGround = (g: CgApi): void => g.set(this.makeCgOpts());

  userMove = (orig: Key, dest: Key): void => {
    this.vm.justPlayed = orig;
    if (!this.promotion.start(orig, dest, { submit: this.playUserMove, show: this.voiceMove?.showPromotion }))
      this.playUserMove(orig, dest);
    this.voiceMove?.update(this.vm.node.fen);
    this.keyboardMove?.update({ fen: this.vm.node.fen });
  };

  playUci = (uci: Uci): void => this.sendMove(parseUci(uci)!);

  playUciList = (uciList: Uci[]): void => uciList.forEach(this.playUci);

  playUserMove = (orig: Key, dest: Key, promotion?: Role): void =>
    this.sendMove({
      from: parseSquare(orig)!,
      to: parseSquare(dest)!,
      promotion,
    });

  sendMove = (move: Move): void => this.sendMoveAt(this.vm.path, this.position(), move);

  sendMoveAt = (path: Tree.Path, pos: Chess, move: Move): void => {
    move = normalizeMove(pos, move);
    const san = makeSanAndPlay(pos, move);
    const check = pos.isCheck() ? pos.board.kingOf(pos.turn) : undefined;
    this.addNode(
      {
        ply: 2 * (pos.fullmoves - 1) + (pos.turn == 'white' ? 0 : 1),
        fen: makeFen(pos.toSetup()),
        id: scalachessCharPair(move),
        uci: makeUci(move),
        san,
        check: defined(check) ? makeSquare(check) : undefined,
        children: [],
      },
      path
    );
  };

  addNode = (node: Tree.Node, path: Tree.Path): void => {
    const newPath = this.tree.addNode(node, path)!;
    this.jump(newPath);
    this.withGround(g => g.playPremove());

    const progress = moveTest(this.vm, this.data.puzzle);
    if (progress) this.applyProgress(progress);
    this.reorderChildren(path);
    this.redraw();
    speech.node(node, false);
    this.music?.jump(node);
  };

  private reorderChildren = (path: Tree.Path, recursive?: boolean): void => {
    const node = this.tree.nodeAtPath(path);
    node.children.sort((c1, _) => {
      const p = c1.puzzle;
      if (p == 'fail') return 1;
      if (p == 'good' || p == 'win') return -1;
      return 0;
    });
    if (recursive) node.children.forEach(child => this.reorderChildren(path + child.id, true));
  };

  private instantRevertUserMove = (): void => {
    this.withGround(g => g.cancelPremove());
    this.userJump(treePath.init(this.vm.path));
    this.redraw();
  };

  private revertUserMove = (): void => {
    if (window.LichessPuzzleNvui) this.instantRevertUserMove();
    else setTimeout(this.instantRevertUserMove, 100);
  };

  private applyProgress = (progress: undefined | 'fail' | 'win' | MoveTest): void => {
    if (progress === 'fail') {
      this.vm.lastFeedback = 'fail';
      this.revertUserMove();
      if (this.vm.mode === 'play') {
        if (this.streak) {
          this.failStreak(this.streak);
          this.streakFailStorage.fire();
        } else {
          this.vm.canViewSolution = true;
          this.vm.mode = 'try';
          this.sendResult(false);
        }
      }
    } else if (progress == 'win') {
      if (this.streak) this.sound.good();
      this.vm.lastFeedback = 'win';
      if (this.vm.mode != 'view') {
        const sent = this.vm.mode == 'play' ? this.sendResult(true) : Promise.resolve();
        this.vm.mode = 'view';
        this.withGround(this.showGround);
        sent.then(() => (this.autoNext() ? this.nextPuzzle() : this.ceval.startCeval()));
      }
    } else if (progress) {
      this.vm.lastFeedback = 'good';
      setTimeout(() => {
        const pos = Chess.fromSetup(parseFen(progress.fen).unwrap()).unwrap();
        this.sendMoveAt(progress.path, pos, progress.move);
      }, this.opts.pref.animation.duration * (this.autoNext() ? 1 : 1.5));
    }
  };

  private failStreak = (streak: PuzzleStreak): void => {
    this.vm.mode = 'view';
    streak.onComplete(false);
    setTimeout(this.viewSolution, 500);
    this.sound.end();
  };

  sendResult = async (win: boolean): Promise<void> => {
    if (this.vm.resultSent) return Promise.resolve();
    const data = this.data;
    this.vm.resultSent = true;
    this.session.complete(data.puzzle.id, win);
    const res = await xhr.complete(
      data.puzzle.id,
      data.angle.key,
      win,
      this.rated,
      data.replay,
      this.streak,
      this.opts.settings.color
    );
    const next = res.next;
    if (next?.user && data.user) {
      data.user.rating = next.user.rating;
      data.user.provisional = next.user.provisional;
      this.vm.round = res.round;
      if (res.round?.ratingDiff) this.session.setRatingDiff(data.puzzle.id, res.round.ratingDiff);
    }
    if (win) speech.success();
    if (next) {
      this.vm.next.resolve(data.replay && res.replayComplete ? data.replay : next);
      if (this.streak && win) this.streak.onComplete(true, res.next);
    }
    this.redraw();
    if (!next && data.replay) {
      alert('No more puzzles available! Try another theme.');
      lichess.redirect('/training/themes');
    }
  };

  isPuzzleData = (d: PuzzleData | ReplayEnd): d is PuzzleData => 'puzzle' in d;

  nextPuzzle = (): void => {
    if (this.streak && this.vm.lastFeedback != 'win') return;
    if (this.vm.mode !== 'view') return;

    this.ceval.stop();
    this.vm.next.promise.then(n => {
      if (this.isPuzzleData(n)) {
        this.initiate(n);
        this.redraw();
      }
    });

    if (this.data.replay && this.vm.round === undefined) {
      lichess.redirect(`/training/dashboard/${this.data.replay.days}`);
    }

    if (!this.streak && !this.data.replay) {
      const path = router.withLang(`/training/${this.data.angle.key}`);
      if (location.pathname != path) history.replaceState(null, '', path);
    }
  };

  private instantiateCeval = (): void => {
    if (this.ceval) this.ceval.destroy();
    this.ceval = new CevalCtrl({
      redraw: this.redraw,
      storageKeyPrefix: 'puzzle',
      multiPvDefault: 3,
      variant: {
        short: 'Std',
        name: 'Standard',
        key: 'standard',
      },
      initialFen: undefined, // always standard starting position
      possible: true,
      emit: (ev, work) => {
        this.tree.updateAt(work.path, node => {
          if (work.threatMode) {
            const threat = ev as Tree.LocalEval;
            if (!node.threat || node.threat.depth <= threat.depth || node.threat.maxDepth < threat.maxDepth)
              node.threat = threat;
          } else if (!node.ceval || node.ceval.depth <= ev.depth || (node.ceval.maxDepth ?? 0) < ev.maxDepth)
            node.ceval = ev;
          if (work.path === this.vm.path) {
            this.setAutoShapes();
            this.redraw();
          }
        });
      },
      setAutoShapes: this.setAutoShapes,
      engineChanged: () => {
        this.setAutoShapes();
        if (!this.ceval.enabled()) this.threatMode(false);
        this.redraw();
      },
      showServerAnalysis: false,
      getChessground: this.ground,
      tree: this.tree,
      getPath: () => this.vm.path,
      getNode: () => this.vm.node,
      outcome: this.outcome,
      getNodeList: () => this.vm.nodeList,
    });
  };

  private setAutoShapes = (): void =>
    this.withGround(g => {
      g.setAutoShapes(
        computeAutoShapes({
          vm: this.vm,
          ceval: this.ceval,
          ground: g,
          threatMode: this.threatMode(),
          nextNodeBest: this.nextNodeBest(),
        })
      );
    });

  nextNodeBest = () => treeOps.withMainlineChild(this.vm.node, n => n.eval?.best);

  toggleThreatMode = (): void => {
    if (this.vm.node.check) return;
    const type = this.ceval.getEngineType();
    if (type === 'disabled' || type === 'server') this.ceval.setEngineType('local');
    if (!this.ceval.enabled()) return;
    this.threatMode.toggle();
    this.setAutoShapes();
    this.ceval.startCeval();
    this.redraw();
  };

  outcome = (): Outcome | undefined => this.position().outcome();

  private jump = (path: Tree.Path): void => {
    const pathChanged = path !== this.vm.path,
      isForwardStep = pathChanged && path.length === this.vm.path.length + 2;
    this.setPath(path);
    this.withGround(this.showGround);
    if (pathChanged) {
      if (isForwardStep) {
        if (!this.vm.node.uci) this.sound.move();
        // initial position
        else if (!this.vm.justPlayed || this.vm.node.uci.includes(this.vm.justPlayed)) {
          if (this.vm.node.san!.includes('x')) this.sound.capture();
          else this.sound.move();
        }
        if (/\+|#/.test(this.vm.node.san!)) this.sound.check();
      }
      this.threatMode(false);
      this.ceval.stop();
      this.ceval.startCeval();
    }
    this.promotion.cancel();
    this.vm.justPlayed = undefined;
    this.vm.autoScrollRequested = true;
    this.keyboardMove?.update({ fen: this.vm.node.fen });
    this.voiceMove?.update(this.vm.node.fen);
    lichess.pubsub.emit('ply', this.vm.node.ply);
  };

  userJump = (path: Tree.Path): void => {
    if (this.tree.nodeAtPath(path)?.puzzle == 'fail' && this.vm.mode != 'view') return;
    this.withGround(g => g.selectSquare(null));
    this.jump(path);
    speech.node(this.vm.node, true);
    this.music?.jump(this.vm.node);
  };

  private userJumpPlyDelta = (plyDelta: Ply) => {
    // ensure we are jumping to a valid ply
    let maxValidPly = this.vm.mainline.length - 1;
    if (last(this.vm.mainline)?.puzzle == 'fail' && this.vm.mode != 'view') maxValidPly -= 1;
    const newPly = Math.min(Math.max(this.vm.node.ply + plyDelta, 0), maxValidPly);
    this.userJump(fromNodeList(this.vm.mainline.slice(0, newPly + 1)));
  };

  viewSolution = (): void => {
    this.sendResult(false);
    this.vm.mode = 'view';
    mergeSolution(this.tree, this.vm.initialPath, this.data.puzzle.solution, this.vm.pov);
    this.reorderChildren(this.vm.initialPath, true);

    // try to play the solution next move
    const next = this.vm.node.children[0];
    if (next && next.puzzle === 'good') this.userJump(this.vm.path + next.id);
    else {
      const firstGoodPath = treeOps.takePathWhile(this.vm.mainline, node => node.puzzle != 'good');
      if (firstGoodPath) this.userJump(firstGoodPath + this.tree.nodeAtPath(firstGoodPath).children[0].id);
    }

    this.vm.autoScrollRequested = true;
    this.vm.voteDisabled = true;
    this.redraw();
    this.ceval.startCeval();
    setTimeout(() => {
      this.vm.voteDisabled = false;
      this.redraw();
    }, 500);
  };

  skip = () => {
    if (!this.streak || !this.streak.data.skip || this.vm.mode != 'play') return;
    this.streak.skip();
    this.userJump(treePath.fromNodeList(this.vm.mainline));
    const moveIndex = treePath.size(this.vm.path) - treePath.size(this.vm.initialPath);
    const solution = this.data.puzzle.solution[moveIndex];
    this.playUci(solution);
    this.playBestMove();
  };

  flip = () => {
    this.flipped.toggle();
    this.withGround(g => g.toggleOrientation());
    this.redraw();
  };

  vote = (v: boolean) => {
    if (!this.vm.voteDisabled) {
      xhr.vote(this.data.puzzle.id, v);
      this.nextPuzzle();
    }
  };

  voteTheme = (theme: ThemeKey, v: boolean) => {
    if (this.vm.round) {
      this.vm.round.themes = this.vm.round.themes || {};
      if (v === this.vm.round.themes[theme]) {
        delete this.vm.round.themes[theme];
        xhr.voteTheme(this.data.puzzle.id, theme, undefined);
      } else {
        if (v || this.data.puzzle.themes.includes(theme)) this.vm.round.themes[theme] = v;
        else delete this.vm.round.themes[theme];
        xhr.voteTheme(this.data.puzzle.id, theme, v);
      }
      this.redraw();
    }
  };

  autoNexting = () => this.vm.lastFeedback == 'win' && this.autoNext();

  playBestMove = (): void => {
    const uci = this.nextNodeBest() || (this.vm.node.ceval && this.vm.node.ceval.pvs[0].moves[0]);
    if (uci) this.playUci(uci);
  };

  keyboardHelp = propWithEffect(location.hash === '#keyboard', this.redraw);

  currentEvals = () => ({ local: this.vm.node.ceval });
  showEvalGauge = () => this.ceval.enabled() && !this.outcome() && this.ceval.showGauge() && this.vm.mode === 'view';
  getOrientation = () => this.withGround(g => g.state.orientation)!;
  getNode = () => this.vm.node;
  allThemes = this.opts.themes && {
    dynamic: this.opts.themes.dynamic.split(' '),
    static: new Set(this.opts.themes.static.split(' ')),
  };
  nvui = window.LichessPuzzleNvui ? (window.LichessPuzzleNvui(this.redraw) as NvuiPlugin) : undefined;

  showServerAnalysis = false;
  mandatoryCeval = () => false;
  disableThreatMode = () => false;
}
