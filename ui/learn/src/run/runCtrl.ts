import { stageStart, stageEnd } from '../sound';
import { type Prop, prop } from 'common';
import type { LearnProgress, LearnOpts } from '../learn';
import { type Stage, type Level, byId as stageById } from '../stage/list';
import { clearTimeouts } from '../timeouts';
import { LevelCtrl } from '../levelCtrl';
import { hashNavigate } from '../hashRouting';
import type { WithGround } from '../util';
import { pubsub } from 'common/pubsub';

export class RunCtrl {
  data: LearnProgress = this.opts.storage.data;

  chessground: CgApi | undefined;
  levelCtrl: LevelCtrl;

  stageStarting: Prop<boolean> = prop(false);
  stageCompleted: Prop<boolean> = prop(false);

  get stage(): Stage {
    return stageById[this.opts.stageId ?? 1]!;
  }

  constructor(
    readonly opts: LearnOpts,
    readonly redraw: () => void,
  ) {
    clearTimeouts();

    this.initializeLevel();

    // Helpful for debugging:
    // site.mousetrap.bind(['shift+enter'], this.levelCtrl.complete);
    pubsub.on('board.change', (is3d: boolean) => {
      this.chessground!.state.addPieceZIndex = is3d;
      this.chessground!.redrawAll();
    });
  }

  initializeLevel = (restarting = false) => {
    this.levelCtrl = new LevelCtrl(
      this.withGround,
      this.stage.levels[Number(this.opts.levelId) - 1],
      {
        onCompleteImmediate: () => {
          this.opts.storage.saveScore(this.stage, this.levelCtrl!.blueprint, this.levelCtrl!.vm.score);
        },
        onComplete: () => {
          if (this.levelCtrl.blueprint.id < this.stage.levels.length) {
            hashNavigate(this.stage.id, this.levelCtrl.blueprint.id + 1);
          } else if (this.stageCompleted()) return;
          else {
            this.stageCompleted(true);
            stageEnd();
          }
          this.redraw();
        },
      },
      this.redraw,
    );

    this.stageStarting(this.levelCtrl.blueprint.id === 1 && this.stageScore() === 0 && !restarting);
    this.stageCompleted(false);

    if (!this.opts.stageId) return;
    if (this.stageStarting()) stageStart();
    else this.levelCtrl.start();
  };

  setChessground = (chessground: CgApi) => {
    this.chessground = chessground;
    this.withGround(this.levelCtrl.initializeWithGround);
  };

  pref = this.opts.pref;

  withGround: WithGround = f => (this.chessground ? f(this.chessground) : undefined);

  stageScore = () => this.data.stages[this.stage.key]?.scores.reduce((a, b) => a + b) ?? 0;

  score = (level: Level) => this.data.stages[this.stage.key]?.scores[level.id - 1] ?? 0;

  getNext = () => stageById[this.stage.id + 1];

  hideStartingPane = () => {
    if (!this.stageStarting()) return;
    this.stageStarting(false);
    this.levelCtrl.start();
    this.redraw();
  };

  restart = () => {
    this.initializeLevel(true);
    this.redraw();
  };
}
