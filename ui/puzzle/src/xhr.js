var m = require('mithril');

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

function round(puzzleId, win) {
  return m.request({
    method: 'POST',
    url: '/training/' + puzzleId + '/round2',
    data: {
      win: win ? 1 : 0
    },
    config: xhrConfig,
    background: true
  });
}

function vote(ctrl, v) {
  m.request({
    method: 'POST',
    url: '/training/' + ctrl.data.puzzle.id + '/vote',
    data: {
      vote: v
    },
    config: xhrConfig,
    background: true
  }).then(function(res) {
    ctrl.data.voted = res[0];
    ctrl.data.puzzle.vote = res[1];
  });
}

function retry(ctrl) {
  showLoading(ctrl);
  m.request({
    method: 'GET',
    url: uncache('/training/' + ctrl.data.puzzle.id + '/load'),
    config: xhrConfig,
    background: true
  }).then(ctrl.reload);
}

function reloadPage() {
  location.href = '/training';
}

function newPuzzle(ctrl) {
  showLoading(ctrl);
  m.request({
    method: 'GET',
    url: uncache('/training/new'),
    config: xhrConfig,
    background: true
  }).then(function(cfg) {
    ctrl.reload(cfg);
    ctrl.pushState(cfg);
  }, reloadPage);
}

module.exports = {
  round: round,
  vote: vote,
  retry: retry,
  newPuzzle: newPuzzle
};
