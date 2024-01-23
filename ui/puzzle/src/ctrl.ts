import * as xhr from './xhr';
import * as router from 'common/router';
import computeAutoShapes from './autoShape';
import keyboard from './keyboard';
import moveTest from './moveTest';
import PuzzleSession from './session';
import PuzzleStreak from './streak';
import throttle from 'common/throttle';
import { PuzzleOpts, PuzzleData, MoveTest, ThemeKey, ReplayEnd, NvuiPlugin, PuzzleRound } from './interfaces';
import { Api as CgApi } from 'chessground/api';
import { build as treeBuild, ops as treeOps, path as treePath, TreeWrapper } from 'tree';
import { Chess, normalizeMove } from 'chessops/chess';
import { chessgroundDests, scalachessCharPair } from 'chessops/compat';
import { Config as CgConfig } from 'chessground/config';
import { CevalCtrl } from 'ceval';
import { makeVoiceMove, VoiceMove } from 'voice';
import { ctrl as makeKeyboardMove, KeyboardMove, KeyboardMoveRootCtrl } from 'keyboardMove';
import { Deferred, defer } from 'common/defer';
import { defined, prop, Prop, propWithEffect, Toggle, toggle } from 'common';
import { makeSanAndPlay } from 'chessops/san';
import { parseFen, makeFen } from 'chessops/fen';
import { parseSquare, parseUci, makeSquare, makeUci, opposite } from 'chessops/util';
import { pgnToTree, mergeSolution } from './moveTree';
import { PromotionCtrl } from 'chess/promotion';
import { Role, Move, Outcome } from 'chessops/types';
import { StoredProp, storedBooleanProp, storedBooleanPropWithEffect } from 'common/storage';
import { fromNodeList } from 'tree/dist/path';
import { last } from 'tree/dist/ops';
import { uciToMove } from 'chessground/util';
import { Redraw } from 'common/snabbdom';
import { ParentCtrl } from 'ceval/src/types';

export default class PuzzleCtrl implements ParentCtrl {
  data: PuzzleData;
  next: Deferred<PuzzleData | ReplayEnd> = defer<PuzzleData>();
  trans: Trans;
  tree: TreeWrapper;
  ceval: CevalCtrl;
  autoNext: StoredProp<boolean>;
  rated: StoredProp<boolean>;
  ground: Prop<CgApi> = prop<CgApi | undefined>(undefined) as Prop<CgApi>;
  threatMode: Toggle = toggle(false);
  streak?: PuzzleStreak;
  streakFailStorage = lichess.storage.make('puzzle.streak.fail');
  session: PuzzleSession;
  menu: Toggle;
  flipped = toggle(false);
  keyboardMove?: KeyboardMove;
  voiceMove?: VoiceMove;
  promotion: PromotionCtrl;
  keyboardHelp: Prop<boolean>;
  cgConfig?: CgConfig;
  path: Tree.Path;
  node: Tree.Node;
  nodeList: Tree.Node[];
  mainline: Tree.Node[];
  initialPath: Tree.Path;
  initialNode: Tree.Node;
  pov: Color;
  mode: 'play' | 'view' | 'try';
  round?: PuzzleRound;
  justPlayed?: Key;
  resultSent: boolean;
  lastFeedback: 'init' | 'fail' | 'win' | 'good' | 'retry';
  canViewSolution = toggle(false);
  autoScrollRequested: boolean;
  autoScrollNow: boolean;
  voteDisabled?: boolean;
  isDaily: boolean;
  blindfolded = false;

  constructor(
    readonly opts: PuzzleOpts,
    readonly redraw: Redraw,
    readonly nvui?: NvuiPlugin,
  ) {
    this.trans = lichess.trans(opts.i18n);
    this.rated = storedBooleanPropWithEffect('puzzle.rated', true, this.redraw);
    this.autoNext = storedBooleanProp(
      `puzzle.autoNext${opts.data.streak ? '.streak' : ''}`,
      !!opts.data.streak,
    );
    this.streak = opts.data.streak ? new PuzzleStreak(opts.data) : undefined;
    if (this.streak) {
      opts.data = { ...opts.data, ...this.streak.data.current };
      this.streakFailStorage.listen(_ => this.failStreak(this.streak!));
    }
    this.session = new PuzzleSession(opts.data.angle.key, opts.data.user?.id, !!opts.data.streak);
    this.menu = toggle(false, redraw);

    this.initiate(opts.data);
    this.promotion = new PromotionCtrl(
      this.withGround,
      () => this.withGround(g => g.set(this.cgConfig!)),
      redraw,
    );

    this.keyboardHelp = propWithEffect(location.hash === '#keyboard', this.redraw);
    keyboard(this);

    // If the page loads while being hidden (like when changing settings),
    // chessground is not displayed, and the first move is not fully applied.
    // Make sure chessground is fully shown when the page goes back to being visible.
    document.addEventListener('visibilitychange', () =>
      lichess.requestIdleCallback(() => this.jump(this.path), 500),
    );

    lichess.pubsub.on('zen', () => {
      const zen = $('body').toggleClass('zen').hasClass('zen');
      window.dispatchEvent(new Event('resize'));
      if (!$('body').hasClass('zen-auto')) xhr.setZen(zen);
    });
    $('body').addClass('playing'); // for zen
    $('#zentog').on('click', () => lichess.pubsub.emit('zen'));
  }

  private loadSound = (file: string, volume?: number) => {
    lichess.sound.load(file, `${lichess.sound.baseUrl}/${file}`);
    return () => lichess.sound.play(file, volume);
  };
  sound = {
    good: this.loadSound('lisp/PuzzleStormGood', 0.7),
    end: this.loadSound('lisp/PuzzleStormEnd', 1),
  };

  setPath = (path: Tree.Path): void => {
    this.path = path;
    this.nodeList = this.tree.getNodeList(path);
    this.node = treeOps.last(this.nodeList)!;
    this.mainline = treeOps.mainlineNodeList(this.tree.root);
  };

  setChessground = (cg: CgApi): void => {
    this.ground(cg);
    const makeRoot = (): KeyboardMoveRootCtrl => ({
      data: {
        game: { variant: { key: 'standard' } },
        player: { color: this.pov },
      },
      sendMove: this.playUserMove,
      auxMove: this.auxMove,
      redraw: this.redraw,
      flipNow: this.flip,
      userJumpPlyDelta: this.userJumpPlyDelta,
      nextPuzzle: this.nextPuzzle,
      vote: this.vote,
      solve: this.viewSolution,
      blindfold: this.blindfold,
    });
    const up = { fen: this.node.fen, canMove: true, cg };
    if (this.opts.pref.voiceMove) {
      if (this.voiceMove) this.voiceMove.update(up);
      else this.voiceMove = makeVoiceMove(makeRoot(), up);
    }
    if (this.opts.pref.keyboardMove) {
      this.keyboardMove ??= makeKeyboardMove(makeRoot());
      this.keyboardMove.update(up);
    }
    requestAnimationFrame(() => this.redraw());
  };

  pref = this.opts.pref;

  withGround = <A>(f: (cg: CgApi) => A): A | undefined => {
    const g = this.ground();
    return g && f(g);
  };

  initiate = (fromData: PuzzleData): void => {
    this.data = fromData;
    this.tree = treeBuild(pgnToTree(this.data.game.pgn.split(' ')));
    const initialPath = treePath.fromNodeList(treeOps.mainlineNodeList(this.tree.root));
    this.mode = 'play';
    this.next = defer();
    this.round = undefined;
    this.justPlayed = undefined;
    this.resultSent = false;
    this.lastFeedback = 'init';
    this.initialPath = initialPath;
    this.initialNode = this.tree.nodeAtPath(initialPath);
    this.pov = this.initialNode.ply % 2 == 1 ? 'black' : 'white';
    this.isDaily = location.href.endsWith('/daily');

    this.setPath(lichess.blindMode ? initialPath : treePath.init(initialPath));
    setTimeout(
      () => {
        this.jump(initialPath);
        this.redraw();
      },
      this.opts.pref.animation.duration > 0 ? 500 : 0,
    );

    // just to delay button display
    setTimeout(
      () => {
        this.canViewSolution(true);
        this.redraw();
      },
      this.rated() ? 4000 : 1000,
    );

    this.withGround(g => {
      g.selectSquare(null);
      g.setAutoShapes([]);
      g.setShapes([]);
      this.showGround(g);
    });

    this.instanciateCeval();
  };

  position = (): Chess => {
    const setup = parseFen(this.node.fen).unwrap();
    return Chess.fromSetup(setup).unwrap();
  };

  makeCgOpts = (): CgConfig => {
    const node = this.node;
    const color: Color = node.ply % 2 === 0 ? 'white' : 'black';
    const dests = chessgroundDests(this.position());
    const nextNode = this.node.children[0];
    const canMove = this.mode === 'view' || (color === this.pov && (!nextNode || nextNode.puzzle == 'fail'));
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
      orientation: this.flipped() ? opposite(this.pov) : this.pov,
      turnColor: color,
      movable: movable,
      premovable: {
        enabled: false,
      },
      check: !!node.check,
      lastMove: uciToMove(node.uci),
    };
    if (node.ply >= this.initialNode.ply) {
      if (this.mode !== 'view' && color !== this.pov && !nextNode) {
        config.movable.color = this.pov;
        config.premovable.enabled = true;
      }
    }
    this.cgConfig = config;
    return config;
  };

  showGround = (g: CgApi): void => g.set(this.makeCgOpts());

  auxMove = (orig: Key, dest: Key, role?: Role) => {
    if (role) this.playUserMove(orig, dest, role);
    else
      this.withGround(g => {
        g.move(orig, dest);
        g.state.movable.dests = undefined;
        g.state.turnColor = opposite(g.state.turnColor);
      });
  };

  auxUpdate = (fen: string): void => {
    this.voiceMove?.update({ fen, canMove: true });
    this.keyboardMove?.update({ fen, canMove: true });
  };

  userMove = (orig: Key, dest: Key): void => {
    this.justPlayed = orig;
    if (
      !this.promotion.start(orig, dest, { submit: this.playUserMove, show: this.voiceMove?.promotionHook() })
    )
      this.playUserMove(orig, dest);
    this.auxUpdate(this.node.fen);
  };

  playUci = (uci: Uci): void => this.sendMove(parseUci(uci)!);

  playUciList = (uciList: Uci[]): void => uciList.forEach(this.playUci);

  playUserMove = (orig: Key, dest: Key, promotion?: Role): void =>
    this.sendMove({
      from: parseSquare(orig)!,
      to: parseSquare(dest)!,
      promotion,
    });

  sendMove = (move: Move): void => this.sendMoveAt(this.path, this.position(), move);

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
      path,
    );
  };

  addNode = (node: Tree.Node, path: Tree.Path): void => {
    const newPath = this.tree.addNode(node, path)!;
    this.jump(newPath);
    this.withGround(g => g.playPremove());

    const progress = moveTest(this);
    if (progress === 'fail') lichess.sound.say('incorrect');
    if (progress) this.applyProgress(progress);
    this.reorderChildren(path);
    this.redraw();
  };

  reorderChildren = (path: Tree.Path, recursive?: boolean): void => {
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
    this.withGround(g => {
      g.cancelPremove();
      g.selectSquare(null);
    });
    this.jump(treePath.init(this.path));
    this.redraw();
  };

  revertUserMove = (): void => {
    if (lichess.blindMode) this.instantRevertUserMove();
    else setTimeout(this.instantRevertUserMove, 100);
  };

  applyProgress = (progress: undefined | 'fail' | 'win' | MoveTest): void => {
    if (progress === 'fail') {
      this.lastFeedback = 'fail';
      this.revertUserMove();
      if (this.mode === 'play') {
        if (this.streak) {
          this.failStreak(this.streak);
          this.streakFailStorage.fire();
        } else {
          this.canViewSolution(true);
          this.mode = 'try';
          this.sendResult(false);
        }
      }
    } else if (progress == 'win') {
      if (this.streak) this.sound.good();
      this.lastFeedback = 'win';
      if (this.mode != 'view') {
        const sent = this.mode == 'play' ? this.sendResult(true) : Promise.resolve();
        this.mode = 'view';
        this.withGround(this.showGround);
        sent.then(_ => (this.autoNext() ? this.nextPuzzle() : this.startCeval()));
      }
    } else if (progress) {
      this.lastFeedback = 'good';
      setTimeout(
        () => {
          const pos = Chess.fromSetup(parseFen(progress.fen).unwrap()).unwrap();
          this.sendMoveAt(progress.path, pos, progress.move);
        },
        this.opts.pref.animation.duration * (this.autoNext() ? 1 : 1.5),
      );
    }
  };

  failStreak = (streak: PuzzleStreak): void => {
    this.mode = 'view';
    streak.onComplete(false);
    setTimeout(this.viewSolution, 500);
    this.sound.end();
  };

  sendResult = async (win: boolean): Promise<void> => {
    if (this.resultSent) return Promise.resolve();
    this.resultSent = true;
    this.session.complete(this.data.puzzle.id, win);
    const res = await xhr.complete(
      this.data.puzzle.id,
      this.data.angle.key,
      win,
      this.rated,
      this.data.replay,
      this.streak,
      this.opts.settings.color,
    );
    const next = res.next;
    if (next?.user && this.data.user) {
      this.data.user.rating = next.user.rating;
      this.data.user.provisional = next.user.provisional;
      this.round = res.round;
      if (res.round?.ratingDiff) this.session.setRatingDiff(this.data.puzzle.id, res.round.ratingDiff);
    }
    if (win) lichess.sound.say('Success!');
    if (next) {
      this.next.resolve(this.data.replay && res.replayComplete ? this.data.replay : next);
      if (this.streak && win) this.streak.onComplete(true, res.next);
    }
    this.redraw();
    if (!next) {
      if (!this.data.replay) {
        alert('No more puzzles available! Try another theme.');
        lichess.redirect('/training/themes');
      }
    }
  };

  private isPuzzleData = (d: PuzzleData | ReplayEnd): d is PuzzleData => 'puzzle' in d;

  nextPuzzle = (): void => {
    if (this.streak && this.lastFeedback != 'win') return;
    if (this.mode !== 'view') return;

    this.ceval.stop();
    this.next.promise.then(n => {
      if (this.isPuzzleData(n)) {
        this.initiate(n);
        this.redraw();
      }
    });

    if (this.data.replay && this.round === undefined) {
      lichess.redirect(`/training/dashboard/${this.data.replay.days}`);
    }

    if (!this.streak && !this.data.replay) {
      const path = router.withLang(`/training/${this.data.angle.key}`);
      if (location.pathname != path) history.replaceState(null, '', path);
    }
  };

  instanciateCeval = (): void => {
    this.ceval?.destroy();
    this.ceval = new CevalCtrl({
      redraw: this.redraw,
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
            if (!node.threat || node.threat.depth <= threat.depth) node.threat = threat;
          } else if (!node.ceval || node.ceval.depth <= ev.depth) node.ceval = ev;
          if (work.path === this.path) {
            this.setAutoShapes();
            this.redraw();
          }
        });
      },
      setAutoShapes: this.setAutoShapes,
    });
  };

  setAutoShapes = (): void =>
    this.withGround(g =>
      g.setAutoShapes(
        computeAutoShapes({
          ...this,
          ground: g,
          node: this.node,
        }),
      ),
    );

  canUseCeval = (): boolean => this.mode === 'view' && !this.outcome();

  startCeval = (): void => {
    if (this.ceval.enabled() && this.canUseCeval()) this.doStartCeval();
  };

  private doStartCeval = throttle(800, () => this.ceval.start(this.path, this.nodeList, this.threatMode()));

  nextNodeBest = () => treeOps.withMainlineChild(this.node, n => n.eval?.best);

  toggleCeval = (): void => {
    this.ceval.toggle();
    this.setAutoShapes();
    this.startCeval();
    if (!this.ceval.enabled()) this.threatMode(false);
    this.autoScrollRequested = true;
    this.redraw();
  };

  restartCeval = (): void => {
    this.ceval.stop();
    this.startCeval();
    this.redraw();
  };

  toggleThreatMode = (): void => {
    if (this.node.check) return;
    if (!this.ceval.enabled()) this.ceval.toggle();
    if (!this.ceval.enabled()) return;
    this.threatMode.toggle();
    this.setAutoShapes();
    this.startCeval();
    this.redraw();
  };

  outcome = (): Outcome | undefined => this.position().outcome();

  jump = (path: Tree.Path): void => {
    const pathChanged = path !== this.path,
      isForwardStep = pathChanged && path.length === this.path.length + 2;
    this.setPath(path);
    this.withGround(this.showGround);
    if (pathChanged) {
      if (isForwardStep) {
        lichess.sound.saySan(this.node.san);
        lichess.sound.move(this.node);
      }
      this.threatMode(false);
      this.ceval.stop();
      this.startCeval();
    }
    this.promotion.cancel();
    this.justPlayed = undefined;
    this.autoScrollRequested = true;
    this.auxUpdate(this.node.fen);
    lichess.pubsub.emit('ply', this.node.ply);
  };

  userJump = (path: Tree.Path): void => {
    if (this.tree.nodeAtPath(path)?.puzzle == 'fail' && this.mode != 'view') return;
    this.withGround(g => g.selectSquare(null));
    this.jump(path);
  };

  userJumpPlyDelta = (plyDelta: Ply) => {
    // ensure we are jumping to a valid ply
    let maxValidPly = this.mainline.length - 1;
    if (last(this.mainline)?.puzzle == 'fail' && this.mode != 'view') maxValidPly -= 1;
    const newPly = Math.min(Math.max(this.node.ply + plyDelta, 0), maxValidPly);
    this.userJump(fromNodeList(this.mainline.slice(0, newPly + 1)));
  };

  viewSolution = (): void => {
    this.sendResult(false);
    this.mode = 'view';
    mergeSolution(this.tree, this.initialPath, this.data.puzzle.solution, this.pov);
    this.reorderChildren(this.initialPath, true);

    // try to play the solution next move
    const next = this.node.children[0];
    if (next && next.puzzle === 'good') this.userJump(this.path + next.id);
    else {
      const firstGoodPath = treeOps.takePathWhile(this.mainline, node => node.puzzle != 'good');
      if (firstGoodPath) this.userJump(firstGoodPath + this.tree.nodeAtPath(firstGoodPath).children[0].id);
    }

    this.autoScrollRequested = true;
    this.voteDisabled = true;
    this.redraw();
    this.startCeval();
    setTimeout(() => {
      this.voteDisabled = false;
      this.redraw();
    }, 500);
  };

  skip = () => {
    if (!this.streak || !this.streak.data.skip || this.mode != 'play') return;
    this.streak.skip();
    this.userJump(treePath.fromNodeList(this.mainline));
    const moveIndex = treePath.size(this.path) - treePath.size(this.initialPath);
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
    if (!this.voteDisabled) {
      xhr.vote(this.data.puzzle.id, v);
      this.nextPuzzle();
    }
  };

  voteTheme = (theme: ThemeKey, v: boolean) => {
    if (this.round) {
      this.round.themes = this.round.themes || {};
      if (v === this.round.themes[theme]) {
        delete this.round.themes[theme];
        xhr.voteTheme(this.data.puzzle.id, theme, undefined);
      } else {
        if (v || this.data.puzzle.themes.includes(theme)) this.round.themes[theme] = v;
        else delete this.round.themes[theme];
        xhr.voteTheme(this.data.puzzle.id, theme, v);
      }
      this.redraw();
    }
  };
  blindfold = (v?: boolean): boolean => {
    if (v !== undefined && v !== this.blindfolded) {
      this.blindfolded = v;
      this.redraw();
    }
    return this.blindfolded;
  };
  playBestMove = (): void => {
    const uci = this.nextNodeBest() || (this.node.ceval && this.node.ceval.pvs[0].moves[0]);
    if (uci) this.playUci(uci);
  };
  autoNexting = () => this.lastFeedback == 'win' && this.autoNext();
  currentEvals = () => ({ client: this.node.ceval });
  showEvalGauge = () => this.showComputer() && this.ceval.enabled() && !this.outcome();
  getOrientation = () => this.withGround(g => g.state.orientation)!;
  allThemes = this.opts.themes && {
    dynamic: this.opts.themes.dynamic.split(' '),
    static: new Set(this.opts.themes.static.split(' ')),
  };
  toggleRated = () => this.rated(!this.rated());
  // implement cetal ParentCtrl:
  getCeval = () => this.ceval;
  ongoing = false;
  getNode = () => this.node;
  showComputer = () => this.mode === 'view';
}
