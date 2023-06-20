import { LearnProgress } from './main';
import m from './mithrilFix';
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
  return m.request({
    method: 'POST',
    url: '/learn/score',
    data: {
      stage: stageKey,
      level: levelId,
      score: score,
    },
  });
}

function xhrReset() {
  return m.request({
    method: 'POST',
    url: '/learn/reset',
  });
}

export default function (d?: LearnProgress): Storage {
  const data: LearnProgress = d || JSON.parse(lichess.storage.get(key)!) || defaultValue;

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
      else lichess.storage.set(key, JSON.stringify(data));
    },
    reset: function () {
      data.stages = {};
      if (data._id)
        xhrReset().then(function () {
          location.reload();
        });
      else {
        lichess.storage.remove(key);
        location.reload();
      }
    },
  };
}
