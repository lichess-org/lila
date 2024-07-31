import * as xhr from 'common/xhr';
import type { LearnProgress } from './learn';
import { Stage } from './stage/list';

export interface Storage {
  data: LearnProgress;
  saveScore(stage: Stage, level: { id: number }, score: number): void;
  reset(): void;
}

const xhrSaveScore = (stageKey: string, levelId: number, score: number) =>
  xhr.jsonAnyResponse('/learn/score', {
    method: 'POST',
    body: xhr.form({
      stage: stageKey,
      level: levelId,
      score: score,
    }),
  });

const xhrReset = () => xhr.jsonAnyResponse('/learn/reset', { method: 'POST' });

export default function (d?: LearnProgress): Storage {
  const key = 'learn.progress';
  const defaultValue: LearnProgress = {
    stages: {},
  };
  const data: LearnProgress = d || JSON.parse(site.storage.get(key)!) || defaultValue;

  return {
    data: data,
    saveScore: (stage: Stage, level: { id: number }, score: number) => {
      if (!data.stages[stage.key])
        data.stages[stage.key] = {
          scores: [],
        };
      if (data.stages[stage.key].scores[level.id - 1] > score) return;
      data.stages[stage.key].scores[level.id - 1] = score;
      data._id ? xhrSaveScore(stage.key, level.id, score) : site.storage.set(key, JSON.stringify(data));
    },
    reset: () => {
      data.stages = {};
      if (data._id) xhrReset().then(() => location.reload());
      else {
        site.storage.remove(key);
        location.reload();
      }
    },
  };
}
