import { LearnOpts, LearnProgress, Score } from './interfaces';

const defaultProgress: LearnProgress = {
  stages: {},
};
const key = 'learn.progress';

export class ProgressStorage {
  progress: LearnProgress;

  constructor(opts: LearnOpts) {
    const storedProgess = window.lishogi.storage.get(key);

    this.progress = opts.data || (storedProgess ? JSON.parse(storedProgess) : defaultProgress);
  }

  has(stageKey: string): boolean {
    if (stageKey in this.progress.stages) {
      const scores = this.progress.stages[stageKey];
      return !!scores && scores.scores.some(s => s > 0);
    }
    return false;
  }

  get(stageKey: string): number[] {
    return this.progress.stages[stageKey]?.scores || [];
  }

  reset(): void {
    if (this.progress._id) xhrReset();
    else window.lishogi.storage.remove(key);
    this.progress.stages = {};
  }

  saveScore(stageKey: string, levelId: number, score: Score): void {
    if (!this.progress.stages[stageKey]) this.progress.stages[stageKey] = { scores: [] };

    if (this.progress.stages[stageKey].scores[levelId - 1] >= score) return;
    this.progress.stages[stageKey].scores[levelId - 1] = score;

    if (this.progress._id) xhrSaveScore(stageKey, levelId, score);
    else window.lishogi.storage.set(key, JSON.stringify(this.progress));
  }
}

function xhrReset(): void {
  $.post('/learn/reset');
}

function xhrSaveScore(stageKey: string, levelId: number, score: number): void {
  $.post('/learn/score', {
    stage: stageKey,
    level: levelId,
    score: score,
  });
}
