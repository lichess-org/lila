// import * as util from '../util';
// import { ctrl as makeItems, view as itemView } from './item';
// import * as stages from '../stage/list';
import { Level } from './stage/list';
import * as scoring from './score';
// import { Prop } from 'common';
// import { LearnProgress, SnabbdomLearnOpts } from '../learn';
// import { ctrl as makeProgress, Progress } from '../progress';
// import { Stage } from '../stage/list';
// import { LearnCtrl } from './ctrl';
// import { clearTimeouts } from '../timeouts';
// import { RunCtrl } from './runCtrl';
// import { Scenario } from '../scenario';
import * as timeouts from './timeouts';
import * as sound from './sound';
import { ChessCtrl } from './chess';
import { Scenario } from './scenario';
// import * as promotion from '../promotion';

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
  blueprint: Level;
  vm: LevelVm = {
    lastStep: false,
    completed: false,
    willComplete: false,
    failed: false,
    score: 0,
    nbMoves: 0,
  };

  // scenario: Scenario;

  constructor(
    blueprint: Level,
    readonly opts: LevelOpts,
  ) {
    blueprint;
    // const items = makeItems({
    //   apples: blueprint.apples,
    // });

    // cheat
    site.mousetrap.bind(['shift+enter'], this.complete);

    // const assertData = function (): AssertData {
    //   return {
    //     scenario: scenario,
    //     chess: chess,
    //     this.vm: this.vm,
    //   };
    // };
    // const detectFailure = function () {
    //   const failed = blueprint.failure && blueprint.failure(assertData());
    //   if (failed) sound.failure();
    //   return !!failed;
    // };
    // const detectSuccess = function () {
    //   if (blueprint.success) return blueprint.success(assertData());
    //   else return !items.hasItem('apple');
    // };
    // const detectCapture = function () {
    //   if (!blueprint.detectCapture) return false;
    //   const fun = blueprint.detectCapture === 'unprotected' ? 'findUnprotectedCapture' : 'findCapture';
    //   const move = chess[fun]();
    //   if (!move) return false;
    //   this.vm.failed = true;
    //   ground.stop();
    //   ground.showCapture(move);
    //   sound.failure();
    //   return true;
    // };
    // const sendMove = function (orig: Key, dest: Key, prom?: PromotionRole) {
    //   this.vm.nbMoves++;
    //   const move = chess.move(orig, dest, prom);
    //   if (move) ground.fen(chess.fen(), blueprint.color, {});
    //   else {
    //     // moving into check
    //     this.vm.failed = true;
    //     ground.showCheckmate(chess);
    //     sound.failure();
    //     return m.redraw();
    //   }
    //   let took = false,
    //     inScenario,
    //     captured = false;
    //   items.withItem(move.to, function () {
    //     this.vm.score += scoring.apple;
    //     items.remove(move.to);
    //     took = true;
    //   });
    //   if (!took && move.captured && blueprint.pointsForCapture) {
    //     if (blueprint.showPieceValues) vm.score += scoring.pieceValue(move.captured);
    //     else vm.score += scoring.capture;
    //     took = true;
    //   }
    //   ground.check(chess);
    //   if (scenario.player(move.from + move.to + (move.promotion || ''))) {
    //     vm.score += scoring.scenario;
    //     inScenario = true;
    //   } else {
    //     captured = detectCapture();
    //     vm.failed = vm.failed || captured || detectFailure();
    //   }
    //   if (!vm.failed && detectSuccess()) complete();
    //   if (vm.willComplete) return;
    //   if (took) sound.take();
    //   else if (inScenario) sound.take();
    //   else sound.move();
    //   if (vm.failed) {
    //     if (blueprint.showFailureFollowUp && !captured)
    //       timeouts.setTimeout(function () {
    //         const rm = chess.playRandomMove();
    //         if (!rm) return;
    //         ground.fen(chess.fen(), blueprint.color, {}, [rm.orig, rm.dest]);
    //       }, 600);
    //   } else {
    //     ground.select(dest);
    //     if (!inScenario) {
    //       chess.color(blueprint.color);
    //       ground.color(blueprint.color, makeChessDests());
    //     }
    //   }
    //   m.redraw();
    // };
    // const makeChessDests = function () {
    //   return chess.dests({
    //     illegal: blueprint.offerIllegalMove,
    //   });
    // };
    // const onMove = function (orig: Key, dest: Key) {
    //   const piece = ground.get(dest);
    //   if (!piece || piece.color !== blueprint.color) return;
    //   if (!promotion.start(orig, dest, sendMove)) sendMove(orig, dest);
    // };
    // const chess = makeChess(blueprint.fen, blueprint.emptyApples ? [] : items.appleKeys());
    // const scenario = makeScenario(blueprint.scenario, {
    //   chess: chess,
    //   makeChessDests: makeChessDests,
    // });
    // promotion.reset();
    // ground.set({
    //   chess: chess,
    //   offerIllegalMove: blueprint.offerIllegalMove,
    //   autoCastle: blueprint.autoCastle,
    //   orientation: blueprint.color,
    //   onMove: onMove,
    //   items: {
    //     render: function (_pos: unknown, key: Key) {
    //       return items.withItem(key, itemView);
    //     },
    //   },
    //   shapes: blueprint.shapes,
    // });
  }

  start = () => {
    sound.levelStart();
    // if (this.chess.color() !== this.blueprint.color) timeouts.setTimeout(this.scenario.opponent, 1000);
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
        // ground.stop();
        m.redraw();
        if (!this.blueprint.nextButton) timeouts.setTimeout(this.opts.onComplete, 1200);
      },
      // ground.data().stats.dragged ? 1 :
      250,
    );
  };

  onComplete = () => this.opts.onComplete();
}
