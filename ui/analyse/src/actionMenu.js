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
    var d = ctrl.data;
    if (d.userAnalysis) flipAttrs.onclick = ctrl.flip;
    else flipAttrs.href = router.game(d, d.opponent.color) + '#' + ctrl.vm.node.ply;
    var canContinue = !ctrl.ongoing && d.game.variant.key === 'standard';

    return m('div.action_menu', [
      m('a.button.text[data-icon=B]', flipAttrs, ctrl.trans('flipBoard')),
      ctrl.ongoing ? null : m('a.button.text[data-icon=m]', {
        href: d.userAnalysis ? '/editor?fen=' + ctrl.vm.node.fen : '/' + d.game.id + '/edit?fen=' + ctrl.vm.node.fen,
        rel: 'nofollow'
      }, ctrl.trans('boardEditor')),
      canContinue ? m('a.button.text[data-icon=U]', {
        onclick: function() {
          $.modal($('.continue_with.' + d.game.id));
        }
      }, ctrl.trans('continueFromHere')) : null,
      ctrl.vm.mainline.length > 4 ?
      speedsOf(d).map(function(speed) {
        return m('a.button[data-icon=G]', {
          class: 'text' + (ctrl.autoplay.active(speed.delay) ? ' active' : ''),
          onclick: partial(ctrl.togglePlay, speed.delay)
        }, 'Auto play ' + speed.name);
      }) : null, [
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
        })('analyse-toggle-gauge'),
        (ctrl.study || ctrl.ongoing) ? null : m('form', {
          method: 'post',
          action: '/study',
        }, m('button.button.text', {
          'data-icon': 'a',
          type: 'submit'
        }, 'Host a study [beta]'))
      ],
      deleteButton(d, ctrl.userId),
      ctrl.ongoing ? null : m('div.continue_with.' + d.game.id, [
        m('a.button', {
          href: d.userAnalysis ? '/?fen=' + ctrl.encodeNodeFen() + '#ai' : router.continue(d, 'ai') + '?fen=' + ctrl.vm.node.fen,
          rel: 'nofollow'
        }, ctrl.trans('playWithTheMachine')),
        m('br'),
        m('a.button', {
          href: d.userAnalysis ? '/?fen=' + ctrl.encodeNodeFen() + '#friend' : router.continue(d, 'friend') + '?fen=' + ctrl.vm.node.fen,
          rel: 'nofollow'
        }, ctrl.trans('playWithAFriend'))
      ])
    ]);
  }
};
