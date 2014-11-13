var m = require('mithril');
var data = require('./data');

var xhrConfig = function(xhr) {
  xhr.setRequestHeader('X-Requested-With', 'XMLHttpRequest');
}

function attempt(ctrl, win) {
  m.request({
    method: 'POST',
    url: ctrl.router.Puzzle.attempt(ctrl.data.puzzle.id).url,
    data: {
      win: win ? 1 : 0,
      time: new Date().getTime() - (ctrl.data.startedAt || new Date()).getTime()
    },
    config: xhrConfig
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
    },
    config: xhrConfig
  }).then(function(res) {
    ctrl.data.attempt.vote = res[0];
    ctrl.data.puzzle.vote = res[1];
  });
}

function retry(ctrl) {
  m.request({
    method: 'GET',
    url: ctrl.router.Puzzle.load(ctrl.data.puzzle.id).url,
    config: xhrConfig
  }).then(ctrl.reload);
}

function setDifficulty(ctrl, d) {
  m.request({
    method: 'POST',
    url: '/training/difficulty',
    data: {
      difficulty: d
    },
    config: xhrConfig
  }).then(ctrl.reload);
}

function newPuzzle(ctrl) {
  m.request({
    method: 'GET',
    url: '/training/new',
    config: xhrConfig
  }).then(ctrl.reload);
}

module.exports = {
  attempt: attempt,
  vote: vote,
  retry: retry,
  setDifficulty: setDifficulty,
  newPuzzle: newPuzzle
};
