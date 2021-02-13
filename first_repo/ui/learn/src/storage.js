var m = require('mithril');

var key = 'learn.progress';

var defaultValue = {
  stages: {},
};

function xhrSaveScore(stageKey, levelId, score) {
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

module.exports = function (data) {
  data = data || JSON.parse(lichess.storage.get(key)) || defaultValue;

  return {
    data: data,
    saveScore: function (stage, level, score) {
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
};
