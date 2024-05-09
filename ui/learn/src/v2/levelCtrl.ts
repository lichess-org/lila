import { PromotionRole } from './util';
import { Items, ctrl as makeItems } from './item';
import * as ground from './ground';
import { Level } from './stage/list';
import * as scoring from './score';
import * as timeouts from './timeouts';
import * as sound from './sound';
import makeChess, { ChessCtrl } from './chess';
import makeScenario, { Scenario } from './scenario';
import * as promotion from './promotion';
import type { Square as Key } from 'chess.js';
import { RunCtrl } from './run/runCtrl';
import { getPiece, setChessground } from './chessground';

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

  constructor(
    readonly blueprint: Level,
    readonly opts: LevelOpts,
    readonly ctrl: RunCtrl,
    readonly redraw: () => void,
  ) {
    // cheat
    // site.mousetrap.bind(['shift+enter'], this.complete);

    this.items = makeItems({ apples: blueprint.apples });
    this.chess = makeChess(blueprint.fen, blueprint.emptyApples ? [] : this.items.appleKeys());
    this.scenario = makeScenario(blueprint.scenario, {
      chess: this.chess,
      makeChessDests: this.makeChessDests,
    });
    promotion.reset();
  }

  makeChessDests = () => this.chess.dests({ illegal: this.blueprint.offerIllegalMove });

  initializeWithCg = () => {
    const { items, scenario, chess, blueprint, vm, redraw, ctrl } = this;

    const assertData = (): AssertData => ({
      scenario: scenario,
      chess: chess,
      vm: vm,
    });
    const detectFailure = function () {
      const failed = blueprint.failure && blueprint.failure(assertData());
      if (failed) sound.failure();
      return !!failed;
    };
    const detectSuccess = function () {
      if (blueprint.success) return blueprint.success(assertData());
      else return !items.hasItem('apple');
    };
    const detectCapture = () => {
      if (!blueprint.detectCapture) return false;
      const fun = blueprint.detectCapture === 'unprotected' ? 'findUnprotectedCapture' : 'findCapture';
      const move = chess[fun]();
      if (!move) return false;
      vm.failed = true;
      ground.stop();
      ground.showCapture(move);
      sound.failure();
      return true;
    };

    const sendMove = (orig: Key, dest: Key, prom?: PromotionRole) => {
      vm.nbMoves++;
      const move = chess.move(orig, dest, prom);
      if (move) ground.fen(chess.fen(), blueprint.color, {});
      else {
        // moving into check
        vm.failed = true;
        ground.showCheckmate(chess);
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
      ground.check(chess);
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
          timeouts.setTimeout(function () {
            const rm = chess.playRandomMove();
            if (!rm) return;
            ground.fen(chess.fen(), blueprint.color, {}, [rm.orig, rm.dest]);
          }, 600);
      } else {
        // TODO:
        // ground.select(dest);
        if (!inScenario) {
          chess.color(blueprint.color);
          ground.color(blueprint.color, this.makeChessDests());
        }
      }
      redraw();
    };

    setChessground(ctrl, {
      chess: chess,
      offerIllegalMove: blueprint.offerIllegalMove,
      autoCastle: blueprint.autoCastle,
      orientation: blueprint.color,
      onMove: (orig: Key, dest: Key) => {
        const piece = getPiece(ctrl, dest);
        if (!piece || piece.color !== blueprint.color) return;
        if (!promotion.start(orig, dest, sendMove)) sendMove(orig, dest);
      },
      items: {
        render: function (_pos: unknown, key: Key) {
          // TODO:
          key;
          console.log('rendering item, known to be broken');
          // return items.withItem(key, itemView);
          return undefined;
        },
      },
      shapes: blueprint.shapes,
    });
  };

  start = () => {
    sound.levelStart();
    if (this.chess.color() !== this.blueprint.color) timeouts.setTimeout(this.scenario.opponent, 1000);
  };

  complete = () => {
    this.vm.willComplete = true;
    this.vm.score += scoring.getLevelBonus(this.blueprint, this.vm.nbMoves);
    this.opts.onCompleteImmediate();
    timeouts.setTimeout(
      () => {
        this.vm.lastStep = false;
        this.vm.completed = true;
        sound.levelEnd();
        ground.stop();
        this.redraw();
        if (!this.blueprint.nextButton) timeouts.setTimeout(this.opts.onComplete, 1200);
      },
      // TODO:
      // ground.data().stats.dragged ? 1 : 250,
      250,
    );
  };

  onComplete = () => this.opts.onComplete();
}
