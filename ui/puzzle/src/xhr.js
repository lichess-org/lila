var m = require('mithril');

function attempt(ctrl, win) {
  m.request({
    method: 'POST',
    url: ctrl.router.Puzzle.attempt(ctrl.data.puzzle.id).url,
    data: {
      win: win ? 1 : 0,
      time: new Date().getTime() - ctrl.data.startedAt.getTime()
    }
  }).then(ctrl.reloadWithProgress);
}

module.exports = {
  attempt: attempt
};
