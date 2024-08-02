import * as sound from '../sound';
import * as stages from '../stage/list';
import { Prop, prop } from 'common';
import { LearnProgress, LearnOpts } from '../learn';
import { Stage } from '../stage/list';
import { LearnCtrl } from '../ctrl';
import { clearTimeouts } from '../timeouts';
import { LevelCtrl } from '../levelCtrl';
import { hashNavigate } from '../hashRouting';
import { WithGround } from '../util';

export class RunCtrl {
  data: LearnProgress = this.opts.storage.data;
  trans: Trans;

  chessground: CgApi | undefined;
  levelCtrl: LevelCtrl;

  stageStarting: Prop<boolean> = prop(false);
  stageCompleted: Prop<boolean> = prop(false);

  get stage(): Stage {
    return stages.byId[this.opts.stageId ?? 1]!;
  }

  constructor(
    ctrl: LearnCtrl,
    readonly opts: LearnOpts,
    readonly redraw: () => void,
  ) {
    clearTimeouts();

    this.trans = ctrl.trans;

    this.initializeLevel();

    // Helpful for debugging:
    // site.mousetrap.bind(['shift+enter'], this.levelCtrl.complete);
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
            sound.stageEnd();
          }
          this.redraw();
        },
      },
      this.redraw,
    );

    this.stageStarting(this.levelCtrl.blueprint.id === 1 && this.stageScore() === 0 && !restarting);
    this.stageCompleted(false);

    if (!this.opts.stageId) return;
    if (this.stageStarting()) sound.stageStart();
    else this.levelCtrl.start();
  };

  setChessground = (chessground: CgApi) => {
    this.chessground = chessground;
    this.withGround(this.levelCtrl.initializeWithGround);
  };

  pref = this.opts.pref;

  withGround: WithGround = f => (this.chessground ? f(this.chessground) : undefined);

  stageScore = () => this.data.stages[this.stage.key]?.scores.reduce((a, b) => a + b) ?? 0;

  score = (level: stages.Level) => this.data.stages[this.stage.key]?.scores[level.id - 1] ?? 0;

  getNext = () => stages.byId[this.stage.id + 1];

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
