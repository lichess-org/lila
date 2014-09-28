var m = require('mithril');
var data = require('./data');

function attempt(ctrl, win) {
  m.request({
    method: 'POST',
    url: ctrl.router.Puzzle.attempt(ctrl.data.puzzle.id).url,
    data: {
      win: win ? 1 : 0,
      time: new Date().getTime() - (ctrl.data.startedAt || new Date()).getTime()
    }
  }).then(function(cfg) {
    cfg.progress = ctrl.data.progress;
    ctrl.reload(cfg);
  });
}

function vote(ctrl, v) {
  m.request({
    method: 'POST',
    url: ctrl.router.Puzzle.vote(ctrl.data.puzzle.id).url,
    data: {
      vote: v
    }
  }).then(function(res) {
    ctrl.data.attempt.vote = res[0];
    ctrl.data.puzzle.vote = res[1];
  });
}

function retry(ctrl) {
  m.request({
    method: 'GET',
    url: ctrl.router.Puzzle.load(ctrl.data.puzzle.id).url
  }).then(ctrl.reload);
}

function setDifficulty(ctrl, d) {
  m.request({
    method: 'POST',
    url: ctrl.router.Puzzle.difficulty().url,
    data: {
      difficulty: d
    }
  }).then(ctrl.reload);
}

function newPuzzle(ctrl) {
  m.request({
    method: 'GET',
    url: ctrl.router.Puzzle.newPuzzle().url
  }).then(ctrl.reload);
}

module.exports = {
  attempt: attempt,
  vote: vote,
  retry: retry,
  setDifficulty: setDifficulty,
  newPuzzle: newPuzzle
};
