var partial = require('chessground').util.partial;
var router = require('game').router;
var util = require('./util');
var pgnExport = require('./pgnExport');
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
  delay: 'realtime'
});

var cplSpeeds = [{
  name: 'by CPL',
  delay: 'cpl_slow'
}];

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

function autoplayButtons(ctrl) {
  var d = ctrl.data;
  var speeds = d.game.moveTimes.length ? allSpeeds : baseSpeeds;
  speeds = d.analysis ? speeds.concat(cplSpeeds) : speeds;
  return m('div.autoplay', speeds.map(function(speed, i) {
    return m('a', {
      class: 'fbt' + (ctrl.autoplay.active(speed.delay) ? ' active' : ''),
      config: util.bindOnce('click', partial(ctrl.togglePlay, speed.delay))
    }, speed.name);
  }));
}

function rangeConfig(read, write) {
  return function(el, isUpdate, ctx) {
    if (isUpdate) return;
    el.value = read();
    var handler = function(e) {
      write(e.target.value);
    };
    el.addEventListener('change', handler)
    ctx.onunload = function() {
      el.removeEventListener('change', handler);
    };
  };
}

function studyButton(ctrl) {
  if (ctrl.study || ctrl.ongoing) return;
  var realGame = !util.synthetic(ctrl.data);
  return m('form', {
    method: 'post',
    action: '/study',
    onsubmit: function(e) {
      var pgnInput = e.target.querySelector('input[name=pgn]');
      if (pgnInput) pgnInput.value = pgnExport.renderFullTxt(ctrl);
    }
  }, [
    realGame ? m('input[type=hidden][name=gameId]', {
      value: ctrl.data.game.id
    }) : m('input[type=hidden][name=pgn]'),
    m('input[type=hidden][name=orientation]', {
      value: ctrl.chessground.data.orientation
    }),
    m('input[type=hidden][name=variant]', {
      value: ctrl.data.game.variant.key
    }),
    m('input[type=hidden][name=fen]', {
      value: ctrl.tree.root.fen
    }),
    m('button.fbt', {
        type: 'submit'
      },
      m('i.icon', {
        'data-icon': ''
      }),
      realGame ? 'Create Study' : 'Save to Study')
  ]);
}

module.exports = {
  controller: function() {

    this.open = location.hash === '#menu';

    this.toggle = function() {
      this.open = !this.open;
    }.bind(this);
  },
  view: function(ctrl) {
    var flipAttrs = {};
    var d = ctrl.data;
    if (d.userAnalysis) flipAttrs.config = util.bindOnce('click', ctrl.flip);
    else flipAttrs.href = router.game(d, d.opponent.color) + '#' + ctrl.vm.node.ply;
    var canContinue = !ctrl.ongoing && d.game.variant.key === 'standard';

    return m('div.action_menu', [
      m('h2', 'Settings'), [
        (function(id) {
          return m('div.setting', [
            m('label', {
              'for': id
            }, 'Computer arrows'),
            m('div.switch', [
              m('input', {
                id: id,
                class: 'cmn-toggle cmn-toggle-round',
                type: 'checkbox',
                checked: ctrl.vm.showAutoShapes(),
                config: util.bindOnce('change', function(e) {
                  ctrl.toggleAutoShapes(e.target.checked);
                })
              }),
              m('label', {
                'for': id
              })
            ])
          ]);
        })('analyse-toggle-ceval'), (function(id) {
          return m('div.setting', [
            m('label', {
              'for': id
            }, 'Computer gauge'),
            m('div.switch', [
              m('input', {
                id: id,
                class: 'cmn-toggle cmn-toggle-round',
                type: 'checkbox',
                checked: ctrl.vm.showGauge(),
                config: util.bindOnce('change', function(e) {
                  ctrl.toggleGauge(e.target.checked);
                })
              }),
              m('label', {
                'for': id
              })
            ])
          ]);
        })('analyse-toggle-gauge')
      ],
      m('h2', 'Local Stockfish'), [
        (function(id) {
          return m('div.setting', [
            m('label', {
              'for': id
            }, 'Multiple lines'),
            m('input', {
              id: id,
              type: 'range',
              min: 1,
              max: 5,
              step: 1,
              config: rangeConfig(function() {
                return ctrl.ceval.multiPv();
              }, function(v) {
                ctrl.cevalSetMultiPv(parseInt(v));
              })
            })
          ]);
        })('analyse-multipv'),
        (function(id) {
          return m('div.setting', [
            m('label', {
              'for': id
            }, 'Threads'),
            m('input', {
              id: id,
              type: 'range',
              min: 1,
              max: navigator.hardwareConcurrency || 1,
              step: 1,
              config: rangeConfig(function() {
                return ctrl.ceval.threads();
              }, function(v) {
                ctrl.cevalSetThreads(parseInt(v));
              })
            })
          ]);
        })('analyse-threads')
      ],
      m('h2', 'Tools'),
      m('div.tools', [
        m('a.fbt', flipAttrs, m('i.icon[data-icon=B]'), ctrl.trans('flipBoard')),
        ctrl.ongoing ? null : m('a.fbt', {
          href: d.userAnalysis ? '/editor?fen=' + ctrl.vm.node.fen : '/' + d.game.id + '/edit?fen=' + ctrl.vm.node.fen,
          rel: 'nofollow'
        }, [
          m('i.icon[data-icon=m]'),
          ctrl.trans('boardEditor')
        ]),
        canContinue ? m('a.fbt', {
          config: util.bindOnce('click', function() {
            $.modal($('.continue_with.' + d.game.id));
          })
        }, [
          m('i.icon[data-icon=U]'),
          ctrl.trans('continueFromHere')
        ]) : null,
        studyButton(ctrl)
      ]),
      ctrl.vm.mainline.length > 4 ? [m('h2', 'Replay mode'), autoplayButtons(ctrl)] : null,
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
