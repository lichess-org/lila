import { PromotionRole, WithGround, arrow, oppColor } from './util';
import { Items, ctrl as makeItems } from './item';
import { Level } from './stage/list';
import * as scoring from './score';
import * as timeouts from './timeouts';
import * as sound from './sound';
import makeChess, { ChessCtrl } from './chess';
import makeScenario, { Scenario } from './scenario';
import { SquareName as Key, makeSquare, makeUci } from 'chessops';
import { CgMove } from './chessground';
import * as cg from 'chessground/types';
import { PromotionCtrl } from './promotionCtrl';
import { Prop, prop } from 'common';
import { DrawShape } from 'chessground/draw';
import { makeAppleShape } from './apple';

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

  isAppleLevel: Prop<boolean>;
  items: Items;
  chess: ChessCtrl;
  scenario: Scenario;
  promotionCtrl: PromotionCtrl;

  constructor(
    readonly withGround: WithGround,
    readonly blueprint: Level,
    readonly opts: LevelOpts,
    readonly redraw: () => void,
  ) {
    timeouts.clearTimeouts();

    this.isAppleLevel = prop(blueprint.apples?.length > 0);
    this.items = makeItems({ apples: blueprint.apples });
    this.chess = makeChess(blueprint.fen, blueprint.emptyApples ? [] : this.items.appleKeys());
    this.scenario = makeScenario(blueprint.scenario, {
      setFen: this.setFen,
      setShapes: this.setShapes,
      chess: this.chess,
      makeChessDests: this.makeChessDests,
    });
    this.promotionCtrl = new PromotionCtrl(withGround, redraw);

    // if chessground is available at this time, initialize with it
    withGround(this.initializeWithGround);
  }

  makeChessDests = () => this.chess.dests(this.chess.instance, { illegal: this.blueprint.offerIllegalMove });

  initializeWithGround = (ground: CgApi) => {
    const { chess, blueprint } = this;
    const sendMove = this.makeSendMove(ground);
    ground.set({
      fen: chess.fen(),
      lastMove: undefined,
      selected: undefined,
      orientation: blueprint.color,
      coordinates: true,
      turnColor: chess.getColor(),
      check: chess.instance.isCheck(),
      autoCastle: blueprint.autoCastle,
      movable: {
        free: false,
        color: chess.getColor(),
        dests: this.makeChessDests(),
        rookCastle: false,
      },
      events: {
        move: (orig: Key, dest: Key) => {
          const piece = ground.state.pieces.get(dest);
          if (!piece || piece.color !== blueprint.color) return;
          if (!this.promotionCtrl.start(orig, dest, sendMove)) sendMove(orig, dest);
        },
      },
      premovable: { enabled: true },
      drawable: { enabled: true, eraseOnClick: true },
      highlight: { lastMove: true },
      animation: {
        enabled: false, // prevent piece animation during transition
        duration: 200,
      },
      disableContextMenu: true,
    });
    setTimeout(() => ground.set({ animation: { enabled: true } }), 200);
    this.setShapes(blueprint.shapes);
  };

  makeSendMove = (ground: CgApi) => {
    const { items, scenario, chess, blueprint, vm, redraw } = this;

    const assertData = (): AssertData => ({ scenario, chess, vm });
    const detectFailure = () => {
      const failed = !!blueprint.failure?.(assertData());
      if (failed) sound.failure();
      return failed;
    };
    const detectSuccess = () => (blueprint.success ? blueprint.success(assertData()) : items.isEmpty());
    const detectCapture = () => {
      if (!blueprint.detectCapture) return false;
      const move =
        blueprint.detectCapture === 'unprotected' ? chess.findUnprotectedCapture() : chess.findCapture();
      if (!move) return false;
      vm.failed = true;
      ground.stop();
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
        this.showKingAttackers();
        sound.failure();
        redraw();
        return;
      }
      let took = false,
        inScenario,
        captured = false;
      items.doIfKeyExists(makeSquare(move.to), () => {
        vm.score += scoring.apple;
        items.remove(makeSquare(move.to));
        took = true;
      });
      const pieceAtKey = chess.instance.board.get(move.to);
      if (!took && pieceAtKey && blueprint.pointsForCapture && pieceAtKey.role != 'king') {
        vm.score += blueprint.showPieceValues ? scoring.pieceValue(pieceAtKey.role) : scoring.capture;
        took = true;
      }
      this.setCheck();
      if (scenario.player(makeUci(move))) {
        vm.score += scoring.scenario;
        inScenario = true;
      } else {
        captured = detectCapture();
        vm.failed = vm.failed || captured || detectFailure();
      }
      if (this.isAppleLevel()) this.setShapes();
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
          chess.setColor(blueprint.color);
          this.setColorDests(blueprint.color, this.makeChessDests());
        }
      }
      redraw();
    };
  };

  setFen = (fen: string, color: Color, dests: cg.Dests, lastMove?: [Key, Key]) =>
    this.withGround(g =>
      g.set({
        turnColor: color,
        fen,
        movable: { color, dests },
        lastMove: lastMove,
      }),
    );

  setShapes = (shapes: DrawShape[] = []) =>
    this.withGround(ground => {
      const appleShapes = this.items.appleKeys().map(makeAppleShape);
      ground.setAutoShapes(appleShapes);
      ground.setShapes(shapes);
    });

  showCapture = ({ orig, dest }: CgMove) =>
    this.withGround(ground => {
      this.setShapes([{ orig, label: { text: '!', fill: '#af0000' } }]);
      timeouts.setTimeout(() => {
        this.setShapes([]);
        ground.move(orig, dest);
      }, 600);
    });

  showKingAttackers = () =>
    this.withGround(ground => {
      const turn = this.chess.getColor();
      const kingKey = this.chess.kingKey(oppColor(turn));
      const shapes = this.chess
        .moves(this.chess.instance)
        .filter(m => makeSquare(m.to) === kingKey)
        .map(m => arrow(makeUci(m), 'red'));
      ground.set({ check: turn });
      this.setShapes(shapes);
    });

  setCheck = () =>
    this.withGround(ground => {
      const checks = this.chess.checks();
      const turn = this.chess.instance.turn;
      ground.set({ check: !!checks && turn });
      if (checks) this.setShapes(checks.map(move => arrow(move.orig + move.dest, 'yellow')));
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
    if (this.chess.getColor() !== this.blueprint.color) timeouts.setTimeout(this.scenario.opponent, 1000);
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
