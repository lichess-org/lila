import { AnalyseController, AnalyseData, AutoplayDelay } from './interfaces';

import { router } from 'game';
import { synthetic, bind, dataIcon } from './util';
import * as pgnExport from './pgnExport';
import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode'

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

function deleteButton(data: AnalyseData, userId: string) {
  const g = data.game;
  if (g.source === 'import' &&
    g.importedBy && g.importedBy === userId)
  return h('form.delete', {
    attrs: {
      method: 'post',
      action: '/' + g.id + '/delete'
    },
    hook: bind('submit', _ => confirm('Delete this imported game?'))
  }, [
    h('button.button.text.thin', {
      attrs: {
        type: 'submit',
        'data-icon': 'q'
      }
    }, 'Delete')
  ]);
  return;
}

function autoplayButtons(ctrl: AnalyseController): Mithril.Renderable {
  const d = ctrl.data;
  let speeds = (d.game.moveCentis && d.game.moveCentis.length) ? allSpeeds : baseSpeeds;
  speeds = d.analysis ? speeds.concat(cplSpeeds) : speeds;
  return h('div.autoplay', speeds.map(speed => {
    return h('a.fbt', {
      class: { active: ctrl.autoplay.active(speed.delay) },
      hook: bind('click', () => ctrl.togglePlay(speed.delay))
    }, speed.name);
  }));
}

function rangeConfig(read: () => number, write: (number) => void) {
  return {
    insert: vnode => {
      const el = vnode.elm as HTMLInputElement;
      el.value = '' + read();
      el.addEventListener('input', _ => write(parseInt(el.value)));
      el.addEventListener('mouseout', _ => el.blur());
    }
  };
}

function formatHashSize(v: number): string {
  if (v < 1000) return v + 'MB';
  else return Math.round(v / 1024) + 'GB';
}

function hiddenInput(name: string, value: string) {
  return h('input', {
    attrs: { 'type': 'hidden', name: name, value: value }
  });
}

function studyButton(ctrl: AnalyseController) {
  if (ctrl.study && ctrl.embed && !ctrl.ongoing) return h('a.fbt', {
    attrs: {
      href: '/study/' + ctrl.study.data.id + '#' + ctrl.study.currentChapter().id,
      target: '_blank'
    }
  }, [
    h('i.icon', {
      attrs: dataIcon('')
    }),
    h('span', 'Open study')
  ]);
  if (ctrl.study || ctrl.ongoing) return;
  const realGame = !synthetic(ctrl.data);
  return h('form', {
    attrs: {
      method: 'post',
      action: '/study/as'
    },
    hook: bind('submit', e => {
      const pgnInput = (e.target as HTMLElement).querySelector('input[name=pgn]') as HTMLInputElement;
      if (pgnInput) pgnInput.value = pgnExport.renderFullTxt(ctrl);
    })
  }, [
    realGame ? hiddenInput('gameId', ctrl.data.game.id) : hiddenInput('pgn', ''),
    hiddenInput('orientation', ctrl.chessground.state.orientation),
    hiddenInput('variant', ctrl.data.game.variant.key),
    hiddenInput('fen', ctrl.tree.root.fen),
    h('button.fbt', { attrs: { type: 'submit' } }),
    h('i.icon', { attrs: dataIcon('') }),
    h('span', 'Study')
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

export function view(ctrl: AnalyseController): VNode {
  const d = ctrl.data;

  const flipOpts = d.userAnalysis ? {
    hook: bind('click', ctrl.flip)
  } : {
    attrs: { href: router.game(d, d.opponent.color, ctrl.embed) + '#' + ctrl.vm.node.ply }
  };

  const canContinue = !ctrl.ongoing && !ctrl.embed && d.game.variant.key === 'standard';
  const ceval = ctrl.getCeval();
  const mandatoryCeval = ctrl.mandatoryCeval();

  return h('div.action_menu', [
    h('div.tools', [
      h('a.fbt', flipOpts, [
        h('i.icon', { attrs: dataIcon('B') }),
        h('span', ctrl.trans('flipBoard'))
      ]),
      ctrl.ongoing ? null : h('a.fbt', {
        attrs: {
          href: d.userAnalysis ? '/editor?fen=' + ctrl.vm.node.fen : '/' + d.game.id + '/edit?fen=' + ctrl.vm.node.fen,
          rel: 'nofollow',
          target: ctrl.embed ? '_blank' : ''
        }
      }, [
        h('i.icon', { attrs: dataIcon('m') }),
        h('span', ctrl.trans('boardEditor'))
      ]),
      canContinue ? h('a.fbt', {
        hook: bind('click', _ => $.modal($('.continue_with.g_' + d.game.id)))
      }, [
        h('i.icon', {
          attrs: dataIcon('U')
        }),
        h('span', ctrl.trans('continueFromHere'))
      ]) : null,
      studyButton(ctrl)
    ]),
    (ceval && ceval.possible) ? [
      h('h2', 'Computer analysis'), [
        (id => {
          return h('div.setting', {
            attrs: { title: mandatoryCeval ? 'Required by practice mode' : 'Use Stockfish 8' }
          }, [
            h('label', { attrs: { 'for': id } }, 'Enable'),
            h('div.switch', [
              h('input.cmn-toggle.cmn-toggle-round#' + id, {
                attrs: {
                  type: 'checkbox',
                  checked: ctrl.vm.showComputer(),
                  disabled: mandatoryCeval
                },
                hook: bind('change', ctrl.toggleComputer),
              }),
              h('label', { attrs: { 'for': id } })
            ])
          ]);
        })('analyse-toggle-all'),
        ctrl.vm.showComputer() ? [
          (id => {
            return h('div.setting', [
              h('label', { attrs: { 'for': id } }, 'Best move arrow'),
              h('div.switch', [
                h('input.cmn-toggle.cmn-toggle-round#' + id, {
                  attrs: {
                    type: 'checkbox',
                    checked: ctrl.vm.showAutoShapes()
                  },
                  hook: bind('change', e => {
                    ctrl.toggleAutoShapes((e.target as HTMLInputElement).checked);
                  })
                }),
                h('label', { attrs: { 'for': id } })
              ])
            ]);
          })('analyse-toggle-shapes'), (function(id) {
            return h('div.setting', [
              h('label', { attrs: { 'for': id } }, 'Evaluation gauge'),
              h('div.switch', [
                h('input.cmn-toggle.cmn-toggle-round#' + id, {
                  attrs: {
                    type: 'checkbox',
                    checked: ctrl.vm.showGauge()
                  },
                  hook: bind('change', () => ctrl.toggleGauge())
                }),
                h('label', { attrs: { 'for': id } })
              ])
            ]);
          })('analyse-toggle-gauge'), (id => {
            return h('div.setting', {
              attrs: { title: 'Removes the depth limit, and keeps your computer warm' }
            }, [
              h('label', { attrs: { 'for': id } }, 'Infinite analysis'),
              h('div.switch', [
                h('input.cmn-toggle.cmn-toggle-round#' + id, {
                  attrs: {
                    type: 'checkbox',
                    checked: ceval.infinite()
                  },
                  hook: bind('change', e => {
                    ctrl.cevalSetInfinite((e.target as HTMLInputElement).checked);
                  })
                }),
                h('label', { attrs: { 'for': id } })
              ])
            ]);
          })('analyse-toggle-infinite'), (id => {
            const max = 5;
            return h('div.setting', [
              h('label', { attrs: { 'for': id } }, 'Multiple lines'),
              h('input#' + id, {
                attrs: {
                  type: 'range',
                  min: 1,
                  max: max,
                  step: 1
                },
                hook: rangeConfig(
                  () => parseInt(ceval!.multiPv()),
                  ctrl.cevalSetMultiPv)
              }),
              h('div.range_value', ceval.multiPv() + ' / ' + max)
            ]);
          })('analyse-multipv'),
          ceval.pnaclSupported ? [
            (function(id) {
              let max = navigator.hardwareConcurrency;
              if (!max) return;
              if (max > 2) max--; // don't overload your computer, you dummy
              return h('div.setting', [
                h('label', { attrs: { 'for': id } }, 'CPUs'),
                h('input#' + id, {
                  attrs: {
                    type: 'range',
                    min: 1,
                    max: max,
                    step: 1
                  },
                  hook: rangeConfig(
                    () => parseInt(ceval!.threads()),
                    ctrl.cevalSetThreads)
                }),
                h('div.range_value', ceval.threads() + ' / ' + max)
              ]);
            })('analyse-threads'), (id => {
              return h('div.setting', [
                h('label', { attrs: { 'for': id } }, 'Memory'),
                h('input#' + id, {
                  attrs: {
                    type: 'range',
                    min: 4,
                    max: 10,
                    step: 1
                  },
                  hook: rangeConfig(
                    () => Math.floor(Math.log2!(parseInt(ceval!.hashSize()))),
                    v => ctrl.cevalSetHashSize(Math.pow(2, v)))
                }),
                h('div.range_value', formatHashSize(parseInt(ceval.hashSize())))
              ]);
            })('analyse-memory')
          ] : null
        ] : null
      ]
    ] : null,
    ctrl.vm.mainline.length > 4 ? [h('h2', 'Replay mode'), autoplayButtons(ctrl)] : null,
    deleteButton(d, ctrl.userId),
    canContinue ? h('div.continue_with.g_' + d.game.id, [
      h('a.button', {
        attrs: {
          href: d.userAnalysis ? '/?fen=' + ctrl.encodeNodeFen() + '#ai' : router.cont(d, 'ai') + '?fen=' + ctrl.vm.node.fen,
          rel: 'nofollow'
        }
      }, ctrl.trans('playWithTheMachine')),
      h('br'),
      h('a.button', {
        attrs: {
          href: d.userAnalysis ? '/?fen=' + ctrl.encodeNodeFen() + '#friend' : router.cont(d, 'friend') + '?fen=' + ctrl.vm.node.fen,
          rel: 'nofollow'
        }
      }, ctrl.trans('playWithAFriend'))
    ]) : null
  ]);
}
