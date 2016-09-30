var m = require('mithril');
var data = require('./data');

var xhrConfig = function(xhr) {
  xhr.setRequestHeader('X-Requested-With', 'XMLHttpRequest');
}

function showLoading(ctrl) {
  ctrl.vm.loading = true;
  m.redraw();
}

function uncache(url) {
  return url + '?_=' + new Date().getTime();
}

function round(ctrl, win) {
  showLoading(ctrl);
  m.request({
    method: 'POST',
    url: '/training/' + ctrl.data.puzzle.id + '/round',
    data: {
      win: win ? 1 : 0
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
    url: '/training/' + ctrl.data.puzzle.id + '/vote',
    data: {
      vote: v
    },
    config: xhrConfig
  }).then(function(res) {
    ctrl.data.round.vote = res[0];
    ctrl.data.puzzle.vote = res[1];
  });
}

function retry(ctrl) {
  showLoading(ctrl);
  m.request({
    method: 'GET',
    url: uncache('/training/' + ctrl.data.puzzle.id + '/load'),
    config: xhrConfig
  }).then(ctrl.reload);
}

function reloadPage() {
  location.href = '/training';
}

function setDifficulty(ctrl, d) {
  showLoading(ctrl);
  m.request({
    method: 'POST',
    url: '/training/difficulty',
    data: {
      difficulty: d
    },
    config: xhrConfig
  }).then(ctrl.reload, reloadPage);
}

function newPuzzle(ctrl) {
  showLoading(ctrl);
  m.request({
    method: 'GET',
    url: uncache('/training/new'),
    config: xhrConfig
  }).then(function(cfg) {
    ctrl.reload(cfg);
    ctrl.pushState(cfg);
  }, reloadPage);
}

module.exports = {
  round: round,
  vote: vote,
  retry: retry,
  setDifficulty: setDifficulty,
  newPuzzle: newPuzzle
};
