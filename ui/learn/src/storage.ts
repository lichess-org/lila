import type { LearnProgress } from '../learn';
import { Stage } from './stage/list';

export interface Storage {
  data: LearnProgress;
  saveScore(stage: Stage, level: { id: number }, score: number): void;
  reset(): void;
}

const key = 'learn.progress';

const defaultValue: LearnProgress = {
  stages: {},
};

function xhrSaveScore(stageKey: string, levelId: number, score: number) {
  stageKey;
  levelId;
  score;
  // TODO:
  // return m.request({
  //   method: 'POST',
  //   url: '/learn/score',
  //   data: {
  //     stage: stageKey,
  //     level: levelId,
  //     score: score,
  //   },
  // });
}

function xhrReset() {
  // TODO:
  // return m.request({
  //   method: 'POST',
  //   url: '/learn/reset',
  // });
  return Promise.resolve();
}

export default function (d?: LearnProgress): Storage {
  const data: LearnProgress = d || JSON.parse(site.storage.get(key)!) || defaultValue;

  return {
    data: data,
    saveScore: function (stage: Stage, level: { id: number }, score: number) {
      if (!data.stages[stage.key])
        data.stages[stage.key] = {
          scores: [],
        };
      if (data.stages[stage.key].scores[level.id - 1] > score) return;
      data.stages[stage.key].scores[level.id - 1] = score;
      if (data._id) xhrSaveScore(stage.key, level.id, score);
      else site.storage.set(key, JSON.stringify(data));
    },
    reset: function () {
      data.stages = {};
      if (data._id)
        xhrReset().then(function () {
          location.reload();
        });
      else {
        site.storage.remove(key);
        location.reload();
      }
    },
  };
}
