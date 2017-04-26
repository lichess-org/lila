import { AnalyseController, AutoplayDelay } from './interfaces';

import { GameData, router } from 'game';
import { synthetic, bindOnce } from './util';
import * as pgnExport from './pgnExport';
import * as m from 'mithril';

interface AutoplaySpeed {
  name: string;
  delay: AutoplayDelay;
}

const baseSpeeds: AutoplaySpeed[] = [{
  name: 'fast',
  delay: 1000
}, {
  name: 'slow',
  delay: 5000
}];

const allSpeeds = baseSpeeds.concat({
  name: 'realtime',
  delay: 'realtime'
});

const cplSpeeds: AutoplaySpeed[] = [{
  name: 'by CPL',
  delay: 'cpl_slow'
}];

function deleteButton(data: GameData, userId: string): Mithril.Renderable {
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
  return;
}

function autoplayButtons(ctrl: AnalyseController): Mithril.Renderable {
  var d = ctrl.data;
  var speeds = (d.game.moveCentis && d.game.moveCentis.length) ? allSpeeds : baseSpeeds;
  speeds = d.analysis ? speeds.concat(cplSpeeds) : speeds;
  return m('div.autoplay', speeds.map(function(speed) {
    return m('a', {
      class: 'fbt' + (ctrl.autoplay.active(speed.delay) ? ' active' : ''),
      config: bindOnce('click', () => ctrl.togglePlay(speed.delay))
    }, speed.name);
  }));
}

function rangeConfig(read: () => number, write: (number) => void) {
  return function(el, isUpdate, ctx) {
    if (isUpdate) return;
    el.value = read();
    var handler = function(e) {
      write(e.target.value);
    };
    var blurer = function(e) {
      e.target.blur();
    };
    el.addEventListener('input', handler)
    el.addEventListener('mouseout', blurer)
    ctx.onunload = function() {
      el.removeEventListener('input', handler);
      el.removeEventListener('mouseout', blurer);
    };
  };
}

function formatHashSize(v: number): string {
  if (v < 1000) return v + 'MB';
  else return Math.round(v / 1024) + 'GB';
}

function studyButton(ctrl: AnalyseController) {
  if (ctrl.study && ctrl.embed && !ctrl.ongoing) return m('a.fbt', {
    href: '/study/' + ctrl.study.data.id + '#' + ctrl.study.currentChapter().id,
    target: '_blank'
  }, [
    m('i.icon', {
      'data-icon': ''
    }),
    'Open study'
  ]);
  if (ctrl.study || ctrl.ongoing) return;
  var realGame = !synthetic(ctrl.data);
  return m('form', {
    method: 'post',
    action: '/study/as',
    onsubmit: function(e) {
      var pgnInput = e.target.querySelector('input[name=pgn]');
      if (pgnInput) pgnInput.value = pgnExport.renderFullTxt(ctrl);
    }
  }, [
    realGame ? m('input[type=hidden][name=gameId]', {
      value: ctrl.data.game.id
    }) : m('input[type=hidden][name=pgn]'),
    m('input[type=hidden][name=orientation]', {
      value: ctrl.chessground.state.orientation
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
      'Study')
  ]);
}

export class Controller {
  open: boolean;

  constructor() {
    this.open = location.hash === '#menu';
  }

  toggle(): void {
    this.open = !this.open;
  }
}

export function view(ctrl: AnalyseController): Mithril.Renderable {
  var flipAttrs: Mithril.Attributes = {};
  var d = ctrl.data;
  if (d.userAnalysis) flipAttrs.config = bindOnce('click', ctrl.flip);
  else flipAttrs.href = router.game(d, d.opponent.color, ctrl.embed) + '#' + ctrl.vm.node.ply;
  var canContinue = !ctrl.ongoing && !ctrl.embed && d.game.variant.key === 'standard';
  var ceval = ctrl.getCeval();
  var mandatoryCeval = ctrl.mandatoryCeval();

  return m('div.action_menu', [
    m('div.tools', [
      m('a.fbt', flipAttrs, m('i.icon[data-icon=B]'), ctrl.trans('flipBoard')),
      ctrl.ongoing ? null : m('a.fbt', {
        href: d.userAnalysis ? '/editor?fen=' + ctrl.vm.node.fen : '/' + d.game.id + '/edit?fen=' + ctrl.vm.node.fen,
        rel: 'nofollow',
        target: ctrl.embed ? '_blank' : null
      }, [
        m('i.icon[data-icon=m]'),
        ctrl.trans('boardEditor')
      ]),
      canContinue ? m('a.fbt', {
        config: bindOnce('click', function() {
          $.modal($('.continue_with.g_' + d.game.id));
        })
      }, [
        m('i.icon[data-icon=U]'),
        ctrl.trans('continueFromHere')
      ]) : null,
      studyButton(ctrl)
    ]),
    (ceval && ceval.possible) ? [
      m('h2', 'Computer analysis'), [
        (function(id) {
          return m('div.setting', {
            title: mandatoryCeval ? 'Required by practice mode' : 'Use Stockfish 8'
          }, [
            m('label', {
              'for': id
            }, 'Enable'),
            m('div.switch', [
              m('input', {
                id: id,
                class: 'cmn-toggle cmn-toggle-round',
                type: 'checkbox',
                checked: ctrl.vm.showComputer(),
                config: bindOnce('change', ctrl.toggleComputer),
                disabled: mandatoryCeval
              }),
              m('label', {
                'for': id
              })
            ])
          ]);
        })('analyse-toggle-all'),
        ctrl.vm.showComputer() ? [
          (function(id) {
            return m('div.setting', [
              m('label', {
                'for': id
              }, 'Best move arrow'),
              m('div.switch', [
                m('input', {
                  id: id,
                  class: 'cmn-toggle cmn-toggle-round',
                  type: 'checkbox',
                  checked: ctrl.vm.showAutoShapes(),
                  config: bindOnce('change', function(e) {
                    ctrl.toggleAutoShapes((e.target as HTMLInputElement).checked);
                  })
                }),
                m('label', {
                  'for': id
                })
              ])
            ]);
          })('analyse-toggle-shapes'), (function(id) {
            return m('div.setting', [
              m('label', {
                'for': id
              }, 'Evaluation gauge'),
              m('div.switch', [
                m('input', {
                  id: id,
                  class: 'cmn-toggle cmn-toggle-round',
                  type: 'checkbox',
                  checked: ctrl.vm.showGauge(),
                  config: bindOnce('change', function() {
                    ctrl.toggleGauge();
                  })
                }),
                m('label', {
                  'for': id
                })
              ])
            ]);
          })('analyse-toggle-gauge'), (function(id) {
            return m('div.setting', {
              title: 'Removes the depth limit, and keeps your computer warm'
            }, [
              m('label', {
                'for': id
              }, 'Infinite analysis'),
              m('div.switch', [
                m('input', {
                  id: id,
                  class: 'cmn-toggle cmn-toggle-round',
                  type: 'checkbox',
                  checked: ceval.infinite(),
                  config: bindOnce('change', function(e) {
                    ctrl.cevalSetInfinite((e.target as HTMLInputElement).checked);
                  })
                }),
                m('label', {
                  'for': id
                })
              ])
            ]);
          })('analyse-toggle-infinite'), (function(id) {
            var max = 5;
            return m('div.setting', [
              m('label', {
                'for': id
              }, 'Multiple lines'),
              m('input', {
                id: id,
                type: 'range',
                min: 1,
                max: max,
                step: 1,
                config: rangeConfig(function() {
                  return parseInt(ceval!.multiPv());
                }, function(v) {
                  ctrl.cevalSetMultiPv(parseInt(v));
                })
              }),
              m('div.range_value', ceval.multiPv() + ' / ' + max)
            ]);
          })('analyse-multipv'),
          ceval.pnaclSupported ? [
            (function(id) {
              var max = navigator.hardwareConcurrency;
              if (!max) return;
              if (max > 2) max--; // don't overload your computer, you dummy
              return m('div.setting', [
                m('label', {
                  'for': id
                }, 'CPUs'),
                m('input', {
                  id: id,
                  type: 'range',
                  min: 1,
                  max: max,
                  step: 1,
                  config: rangeConfig(function() {
                    return parseInt(ceval!.threads());
                  }, function(v) {
                    ctrl.cevalSetThreads(parseInt(v));
                  })
                }),
                m('div.range_value', ceval.threads() + ' / ' + max)
              ]);
            })('analyse-threads'), (function(id) {
              return m('div.setting', [
                m('label', {
                  'for': id
                }, 'Memory'),
                m('input', {
                  id: id,
                  type: 'range',
                  min: 4,
                  max: 10,
                  step: 1,
                  config: rangeConfig(function() {
                    return Math.floor(Math.log2!(parseInt(ceval!.hashSize())));
                  }, function(v) {
                    ctrl.cevalSetHashSize(Math.pow(2, parseInt(v)));
                  })
                }),
                m('div.range_value', formatHashSize(parseInt(ceval.hashSize())))
              ]);
            })('analyse-memory')
          ] : null
        ] : null
      ]
    ] : null,
    ctrl.vm.mainline.length > 4 ? [m('h2', 'Replay mode'), autoplayButtons(ctrl)] : null,
    deleteButton(d, ctrl.userId),
    canContinue ? m('div.continue_with.g_' + d.game.id, [
      m('a.button', {
        href: d.userAnalysis ? '/?fen=' + ctrl.encodeNodeFen() + '#ai' : router.cont(d, 'ai') + '?fen=' + ctrl.vm.node.fen,
        rel: 'nofollow'
      }, ctrl.trans('playWithTheMachine')),
      m('br'),
      m('a.button', {
        href: d.userAnalysis ? '/?fen=' + ctrl.encodeNodeFen() + '#friend' : router.cont(d, 'friend') + '?fen=' + ctrl.vm.node.fen,
        rel: 'nofollow'
      }, ctrl.trans('playWithAFriend'))
    ]) : null
  ]);
}
