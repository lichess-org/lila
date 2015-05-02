var partial = require('chessground').util.partial;
var m = require('mithril');

var baseSpeeds = [{
  name: 'fast',
  delay: 1000
}, {
  name: 'slow',
  delay: 5000
}];

var allSpeeds = baseSpeeds.concat({
  name: 'realtime',
  delay: true
});

function speedsOf(data) {
  return data.game.moveTimes.length ? allSpeeds : baseSpeeds;
}

function deleteButton(data, userId) {
  if (data.game.source === 'import' &&
    data.game.importedBy && data.game.importedBy === userId)
    return m('form.delete', {
      method: 'post',
      action: '/' + data.game.id + '/delete',
      onsubmit: function() {
        return confirm('Delete this imported game?');
      }
    }, m('button.button.text.thin', {
      type: 'submit',
      'data-icon': 'q',
    }, 'Delete'));
}

module.exports = {
  controller: function() {

    this.open = false;

    this.toggle = function() {
      this.open = !this.open
    }.bind(this);
  },
  view: function(ctrl) {
    var flipAttrs = {};
    if (ctrl.data.userAnalysis) flipAttrs.onclick = ctrl.flip;
    else flipAttrs.href = ctrl.router.Round.watcher(ctrl.data.game.id, ctrl.data.opponent.color).url;

    return m('div.action_menu',
      m('div.inner', [
        m('a.button.text[data-icon=B]', flipAttrs, ctrl.trans('flipBoard')),
        m('a.button.text[data-icon=m]', {
          href: ctrl.data.userAnalysis ? '/editor?fen=' + ctrl.vm.step.fen : '/' + ctrl.data.game.id + '/edit?fen=' + ctrl.vm.step.fen,
          rel: 'nofollow'
        }, ctrl.trans('boardEditor')),
        m('a.button.text[data-icon=U]', {
          onclick: function() {
            $.modal($('.continue_with.' + ctrl.data.game.id));
          }
        }, ctrl.trans('continueFromHere')),
        speedsOf(ctrl.data).map(function(speed) {
          return m('a.button[data-icon=G]', {
            class: 'text' + (ctrl.autoplay.active(speed.delay) ? ' active' : ''),
            onclick: partial(ctrl.togglePlay, speed.delay)
          }, 'Auto play ' + speed.name);
        }),
        deleteButton(ctrl.data, ctrl.userId),
        m('div.continue_with.' + ctrl.data.game.id, [
          m('a.button', {
            href: ctrl.data.userAnalysis ? '/?fen=' + ctrl.vm.step.fen + '#ai' : ctrl.router.Round.continue(ctrl.data.game.id, 'ai').url + '?fen=' + ctrl.vm.step.fen,
            rel: 'nofollow'
          }, ctrl.trans('playWithTheMachine')),
          m('br'),
          m('a.button', {
            href: ctrl.data.userAnalysis ? '/?fen=' + ctrl.vm.step.fen + '#friend' : ctrl.router.Round.continue(ctrl.data.game.id, 'friend').url + '?fen=' + ctrl.vm.step.fen,
            rel: 'nofollow'
          }, ctrl.trans('playWithAFriend'))
        ])
      ]));
  }
};
