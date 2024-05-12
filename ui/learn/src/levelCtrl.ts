import { PromotionRole, WithGround, arrow } from './util';
import { Items, ctrl as makeItems } from './item';
import { Level } from './stage/list';
import * as scoring from './score';
import * as timeouts from './timeouts';
import * as sound from './sound';
import makeChess, { ChessCtrl } from './chess';
import makeScenario, { Scenario } from './scenario';
import type { Square as Key } from 'chess.js';
import { CgMove } from './chessground';
import * as cg from 'chessground/types';
import { PromotionCtrl } from './promotionCtrl';

export interface LevelVm {
  score: number;
  completed: boolean;
  failed: boolean;
  lastStep: boolean;
  willComplete: boolean;
  nbMoves: number;
  starting?: boolean;
}

export interface LevelOpts {
  onComplete(): void;
  onCompleteImmediate(): void;
}

export interface AssertData {
  scenario: Scenario;
  chess: ChessCtrl;
  vm: LevelVm;
}

export class LevelCtrl {
  vm: LevelVm = {
    lastStep: false,
    completed: false,
    willComplete: false,
    failed: false,
    score: 0,
    nbMoves: 0,
  };

  items: Items<'apple'>;
  chess: ChessCtrl;
  scenario: Scenario;
  promotionCtrl: PromotionCtrl;

  constructor(
    readonly withGround: WithGround,
    readonly blueprint: Level,
    readonly opts: LevelOpts,
    readonly redraw: () => void,
  ) {
    // cheat
    // site.mousetrap.bind(['shift+enter'], this.complete);

    this.items = makeItems({ apples: blueprint.apples });
    this.chess = makeChess(blueprint.fen, blueprint.emptyApples ? [] : this.items.appleKeys());
    this.scenario = makeScenario(blueprint.scenario, withGround, {
      setFen: this.setFen,
      chess: this.chess,
      makeChessDests: this.makeChessDests,
    });
    this.promotionCtrl = new PromotionCtrl(withGround, redraw);

    // if chessground is available at this time, initialize with it
    withGround(this.initializeWithGround);
  }

  makeChessDests = () => this.chess.dests({ illegal: this.blueprint.offerIllegalMove });

  initializeWithGround = (ground: CgApi) => {
    const { chess, blueprint } = this;
    const sendMove = this.makeSendMove(ground);
    ground.set({
      fen: chess.fen(),
      lastMove: undefined,
      selected: undefined,
      orientation: blueprint.color,
      coordinates: true,
      turnColor: chess.color(),
      check: chess.instance.in_check(),
      autoCastle: blueprint.autoCastle,
      movable: {
        free: false,
        color: chess.color(),
        dests: chess.dests({ illegal: blueprint.offerIllegalMove }),
      },
      events: {
        move: (orig: Key, dest: Key) => {
          const piece = ground.state.pieces.get(dest);
          if (!piece || piece.color !== blueprint.color) return;
          if (!this.promotionCtrl.start(orig, dest, sendMove)) sendMove(orig, dest);
        },
      },
      // items: {
      //   render: function (_pos: unknown, key: Key) {
      //     // TODO:
      //     return items.withItem(key, itemView);
      //   },
      // },
      premovable: { enabled: true },
      drawable: { enabled: true, eraseOnClick: true },
      highlight: { lastMove: true },
      animation: {
        enabled: false, // prevent piece animation during transition
        duration: 200,
      },
      // TODO: doesn't seem to be working
      disableContextMenu: true,
    });
    setTimeout(() => ground.set({ animation: { enabled: true } }), 200);
    if (blueprint.shapes) ground.setShapes(blueprint.shapes.slice(0));
  };

  makeSendMove = (ground: CgApi) => {
    const { items, scenario, chess, blueprint, vm, redraw } = this;

    const assertData = (): AssertData => ({
      scenario: scenario,
      chess: chess,
      vm: vm,
    });
    const detectFailure = () => {
      const failed = blueprint.failure && blueprint.failure(assertData());
      if (failed) sound.failure();
      return !!failed;
    };
    const detectSuccess = () => {
      if (blueprint.success) return blueprint.success(assertData());
      else return !items.hasItem('apple');
    };
    const detectCapture = () => {
      if (!blueprint.detectCapture) return false;
      const fun = blueprint.detectCapture === 'unprotected' ? 'findUnprotectedCapture' : 'findCapture';
      const move = chess[fun]();
      if (!move) return false;
      vm.failed = true;
      ground?.stop();
      this.showCapture(move);
      sound.failure();
      return true;
    };

    return (orig: Key, dest: Key, prom?: PromotionRole) => {
      vm.nbMoves++;
      const move = chess.move(orig, dest, prom);
      if (move) this.setFen(chess.fen(), blueprint.color, new Map());
      else {
        // moving into check
        vm.failed = true;
        this.showCheckmate(chess);
        sound.failure();
        redraw();
        return;
      }
      let took = false,
        inScenario,
        captured = false;
      items.withItem(move.to, () => {
        vm.score += scoring.apple;
        items.remove(move.to);
        took = true;
      });
      if (!took && move.captured && blueprint.pointsForCapture) {
        if (blueprint.showPieceValues) vm.score += scoring.pieceValue(move.captured);
        else vm.score += scoring.capture;
        took = true;
      }
      this.setCheck(chess);
      if (scenario.player(move.from + move.to + (move.promotion || ''))) {
        vm.score += scoring.scenario;
        inScenario = true;
      } else {
        captured = detectCapture();
        vm.failed = vm.failed || captured || detectFailure();
      }
      if (!vm.failed && detectSuccess()) this.complete();
      if (vm.willComplete) return;
      if (took) sound.take();
      else if (inScenario) sound.take();
      else sound.move();
      if (vm.failed) {
        if (blueprint.showFailureFollowUp && !captured)
          timeouts.setTimeout(() => {
            const rm = chess.playRandomMove();
            if (!rm) return;
            this.setFen(chess.fen(), blueprint.color, new Map(), [rm.orig, rm.dest] as [Key, Key]);
          }, 600);
      } else {
        ground.selectSquare(dest);
        if (!inScenario) {
          chess.color(blueprint.color);
          this.setColorDests(blueprint.color, this.makeChessDests());
        }
      }
      redraw();
    };
  };

  setFen = (fen: string, color: Color, dests: cg.Dests, lastMove?: [Key, Key, ...unknown[]]) =>
    this.withGround(g =>
      g.set({
        turnColor: color,
        fen,
        movable: { color, dests },
        // TODO:
        // Casting here instead of declaring lastMove as [Key, Key] right away
        // allows the fen function to accept [orig, dest, promotion] values
        // for lastMove as well.
        lastMove: lastMove as [Key, Key],
      }),
    );

  showCapture = (move: CgMove) =>
    requestAnimationFrame(() => {
      // TODO: the data-key attribute is no longer available
      const $square = $('#learn-app piece[data-key=' + move.orig + ']');
      $square.addClass('wriggle');
      timeouts.setTimeout(() => {
        $square.removeClass('wriggle');
        this.withGround(g => {
          g.setShapes([]);
          g.move(move.orig, move.dest);
        });
      }, 600);
    });

  showCheckmate = (chess: ChessCtrl) =>
    this.withGround(ground => {
      const turn = chess.instance.turn() === 'w' ? 'b' : 'w';
      const fen = [ground.getFen(), turn, '- - 0 1'].join(' ');
      chess.instance.load(fen);
      const kingKey = chess.kingKey(turn === 'w' ? 'black' : 'white');
      const shapes = chess.instance
        .moves({ verbose: true })
        .filter(m => m.to === kingKey)
        .map(m => arrow(m.from + m.to, 'red'));
      // TODO: check that this works instead of the below original
      ground.set({ check: turn === 'w' ? 'black' : 'white' });
      // ground.set({ check: shapes.length ? kingKey : null });
      ground.setShapes(shapes);
    });

  setCheck = (chess: ChessCtrl) =>
    this.withGround(ground => {
      const checks = chess.checks();
      ground.set({
        // TODO: check that this works instead of the below original
        check: true,
        // check: checks ? checks[0].dest : null,
      });
      if (checks) ground.setShapes(checks.map(move => arrow(move.orig + move.dest, 'yellow')));
    });

  setColorDests = (color: Color, dests: cg.Dests) =>
    this.withGround(ground =>
      ground.set({
        turnColor: color,
        movable: { color, dests },
      }),
    );

  start = () => {
    sound.levelStart();
    if (this.chess.color() !== this.blueprint.color) timeouts.setTimeout(this.scenario.opponent, 1000);
  };

  complete = () => {
    this.vm.willComplete = true;
    this.vm.score += scoring.getLevelBonus(this.blueprint, this.vm.nbMoves);
    this.opts.onCompleteImmediate();
    this.withGround(g =>
      timeouts.setTimeout(
        () => {
          this.vm.lastStep = false;
          this.vm.completed = true;
          sound.levelEnd();
          this.withGround(g => g.stop());
          this.redraw();
          if (!this.blueprint.nextButton) timeouts.setTimeout(this.opts.onComplete, 1200);
        },
        g.state.stats.dragged ? 1 : 250,
      ),
    );
  };

  onComplete = () => this.opts.onComplete();
}
