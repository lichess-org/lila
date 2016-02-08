var partial = require('chessground').util.partial;
var router = require('game').router;
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
    else flipAttrs.href = router.game(ctrl.data, ctrl.data.opponent.color) + '#' + ctrl.vm.step.ply;

    return m('div.action_menu', [
      m('a.button.text[data-icon=B]', flipAttrs, ctrl.trans('flipBoard')),
      ctrl.ongoing ? null : m('a.button.text[data-icon=m]', {
        href: ctrl.data.userAnalysis ? '/editor?fen=' + ctrl.vm.step.fen : '/' + ctrl.data.game.id + '/edit?fen=' + ctrl.vm.step.fen,
        rel: 'nofollow'
      }, ctrl.trans('boardEditor')),
      ctrl.ongoing ? null : m('a.button.text[data-icon=U]', {
        onclick: function() {
          $.modal($('.continue_with.' + ctrl.data.game.id));
        }
      }, ctrl.trans('continueFromHere')),
      ctrl.analyse.tree.length > 4 ?
      speedsOf(ctrl.data).map(function(speed) {
        return m('a.button[data-icon=G]', {
          class: 'text' + (ctrl.autoplay.active(speed.delay) ? ' active' : ''),
          onclick: partial(ctrl.togglePlay, speed.delay)
        }, 'Auto play ' + speed.name);
      }) : null,
      ctrl.hasAnyComputerAnalysis() ? [
        (function(id) {
          return m('div.setting', [
            m('div.switch', [
              m('input', {
                id: id,
                class: 'cmn-toggle cmn-toggle-round',
                type: 'checkbox',
                checked: ctrl.vm.showAutoShapes(),
                onchange: function(e) {
                  ctrl.toggleAutoShapes(e.target.checked);
                }
              }),
              m('label', {
                'for': id
              })
            ]),
            m('label', {
              'for': id
            }, 'Computer arrows')
          ]);
        })('analyse-toggle-ceval'), (function(id) {
          return m('div.setting', [
            m('div.switch', [
              m('input', {
                id: id,
                class: 'cmn-toggle cmn-toggle-round',
                type: 'checkbox',
                checked: ctrl.vm.showGauge(),
                onchange: function(e) {
                  ctrl.toggleGauge(e.target.checked);
                }
              }),
              m('label', {
                'for': id
              })
            ]),
            m('label', {
              'for': id
            }, 'Computer gauge')
          ]);
        })('analyse-toggle-gauge')
      ] : null,
      deleteButton(ctrl.data, ctrl.userId),
      ctrl.ongoing ? null : m('div.continue_with.' + ctrl.data.game.id, [
        m('a.button', {
          href: ctrl.data.userAnalysis ? '/?fen=' + ctrl.encodeStepFen() + '#ai' : router.continue(ctrl.data, 'ai') + '?fen=' + ctrl.vm.step.fen,
          rel: 'nofollow'
        }, ctrl.trans('playWithTheMachine')),
        m('br'),
        m('a.button', {
          href: ctrl.data.userAnalysis ? '/?fen=' + ctrl.encodeStepFen() + '#friend' : router.continue(ctrl.data, 'friend') + '?fen=' + ctrl.vm.step.fen,
          rel: 'nofollow'
        }, ctrl.trans('playWithAFriend'))
      ])
    ]);
  }
};
