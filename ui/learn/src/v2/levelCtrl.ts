import { PromotionRole } from './util';
import { ctrl as makeItems, view as itemView } from './item';
import * as ground from './ground';
import { Level } from './stage/list';
import * as scoring from './score';
import * as timeouts from './timeouts';
import * as sound from './sound';
import makeChess, { ChessCtrl } from './chess';
import makeScenario, { Scenario } from './scenario';
import * as promotion from './promotion';
import type { Square as Key } from 'chess.js';

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

  scenario: Scenario;
  chess: ChessCtrl;

  constructor(
    readonly blueprint: Level,
    readonly opts: LevelOpts,
  ) {
    const items = makeItems({
      apples: blueprint.apples,
    });

    // cheat
    // site.mousetrap.bind(['shift+enter'], this.complete);

    const assertData = (): AssertData => ({
      scenario: scenario,
      chess: chess,
      vm: this.vm,
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
      this.vm.failed = true;
      ground.stop();
      ground.showCapture(move);
      sound.failure();
      return true;
    };
    const sendMove = (orig: Key, dest: Key, prom?: PromotionRole) => {
      this.vm.nbMoves++;
      const move = chess.move(orig, dest, prom);
      if (move) ground.fen(chess.fen(), blueprint.color, {});
      else {
        // moving into check
        this.vm.failed = true;
        ground.showCheckmate(chess);
        sound.failure();
        // TODO:
        // return m.redraw();
        return;
      }
      let took = false,
        inScenario,
        captured = false;
      items.withItem(move.to, () => {
        this.vm.score += scoring.apple;
        items.remove(move.to);
        took = true;
      });
      if (!took && move.captured && blueprint.pointsForCapture) {
        if (blueprint.showPieceValues) this.vm.score += scoring.pieceValue(move.captured);
        else this.vm.score += scoring.capture;
        took = true;
      }
      ground.check(chess);
      if (scenario.player(move.from + move.to + (move.promotion || ''))) {
        this.vm.score += scoring.scenario;
        inScenario = true;
      } else {
        captured = detectCapture();
        this.vm.failed = this.vm.failed || captured || detectFailure();
      }
      if (!this.vm.failed && detectSuccess()) this.complete();
      if (this.vm.willComplete) return;
      if (took) sound.take();
      else if (inScenario) sound.take();
      else sound.move();
      if (this.vm.failed) {
        if (blueprint.showFailureFollowUp && !captured)
          timeouts.setTimeout(function () {
            const rm = chess.playRandomMove();
            if (!rm) return;
            ground.fen(chess.fen(), blueprint.color, {}, [rm.orig, rm.dest]);
          }, 600);
      } else {
        ground.select(dest);
        if (!inScenario) {
          chess.color(blueprint.color);
          ground.color(blueprint.color, makeChessDests());
        }
      }
      // TODO:
      // m.redraw();
    };
    const makeChessDests = function () {
      return chess.dests({
        illegal: blueprint.offerIllegalMove,
      });
    };
    const onMove = function (orig: Key, dest: Key) {
      const piece = ground.get(dest);
      if (!piece || piece.color !== blueprint.color) return;
      if (!promotion.start(orig, dest, sendMove)) sendMove(orig, dest);
    };
    const chess = makeChess(blueprint.fen, blueprint.emptyApples ? [] : items.appleKeys());
    this.chess = chess;
    const scenario = makeScenario(blueprint.scenario, {
      chess: chess,
      makeChessDests: makeChessDests,
    });
    promotion.reset();
    ground.set({
      chess: chess,
      offerIllegalMove: blueprint.offerIllegalMove,
      autoCastle: blueprint.autoCastle,
      orientation: blueprint.color,
      onMove: onMove,
      items: {
        render: function (_pos: unknown, key: Key) {
          // TODO:
          console.log('rendering item, known to be broken');
          return items.withItem(key, itemView);
        },
      },
      shapes: blueprint.shapes,
    });
  }

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
        // TODO:
        // m.redraw();
        if (!this.blueprint.nextButton) timeouts.setTimeout(this.opts.onComplete, 1200);
      },
      ground.data().stats.dragged ? 1 : 250,
    );
  };

  onComplete = () => this.opts.onComplete();
}
