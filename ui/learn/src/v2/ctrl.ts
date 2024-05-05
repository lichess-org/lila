import { LearnOpts, LearnProgress } from '../learn';

export class LearnCtrl {
  data: LearnProgress = this.opts.storage.data;
  trans: Trans = site.trans(this.opts.i18n);

  constructor(
    readonly opts: LearnOpts,
    readonly redraw: () => void,
  ) {}

  isStageIdComplete = (id: number) => false && id;
}
