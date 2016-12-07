var m = require('mithril');

var xhrConfig = function(xhr) {
  xhr.setRequestHeader('X-Requested-With', 'XMLHttpRequest');
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

function nextPuzzle() {
  return m.request({
    method: 'GET',
    url: uncache('/training/new'),
    config: xhrConfig,
    background: true
  });
}

module.exports = {
  round: round,
  vote: vote,
  nextPuzzle: nextPuzzle
};
