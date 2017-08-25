import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode'
import { Hooks } from 'snabbdom/hooks'
import { MaybeVNodes } from './interfaces';
import { AutoplayDelay } from './autoplay';
import AnalyseCtrl from './ctrl';
import { router } from 'game';
import { synthetic, bind, dataIcon } from './util';
import * as pgnExport from './pgnExport';

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
  name: 'realtimeReplay',
  delay: 'realtime'
});

const cplSpeeds: AutoplaySpeed[] = [{
  name: 'byCPL',
  delay: 'cpl_slow'
}];

function deleteButton(ctrl: AnalyseCtrl, userId: string | null): VNode | undefined {
  const g = ctrl.data.game;
  if (g.source === 'import' &&
    g.importedBy && g.importedBy === userId)
  return h('form.delete', {
    attrs: {
      method: 'post',
      action: '/' + g.id + '/delete'
    },
    hook: bind('submit', _ => confirm(ctrl.trans.noarg('deleteThisImportedGame')))
  }, [
    h('button.button.text.thin', {
      attrs: {
        type: 'submit',
        'data-icon': 'q'
      }
    }, ctrl.trans.noarg('delete'))
  ]);
  return;
}

function autoplayButtons(ctrl: AnalyseCtrl): VNode {
  const d = ctrl.data;
  let speeds = (d.game.moveCentis && d.game.moveCentis.length) ? allSpeeds : baseSpeeds;
  speeds = d.analysis ? speeds.concat(cplSpeeds) : speeds;
  return h('div.autoplay', speeds.map(speed => {
    return h('a.fbt', {
      class: { active: ctrl.autoplay.active(speed.delay) },
      hook: bind('click', () => ctrl.togglePlay(speed.delay), ctrl.redraw)
    }, ctrl.trans.noarg(speed.name));
  }));
}

function rangeConfig(read: () => number, write: (value: number) => void): Hooks {
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
    attrs: { 'type': 'hidden', name, value }
  });
}

function studyButton(ctrl: AnalyseCtrl) {
  if (ctrl.study && ctrl.embed && !ctrl.ongoing) return h('a.fbt', {
    attrs: {
      href: '/study/' + ctrl.study.data.id + '#' + ctrl.study.currentChapter().id,
      target: '_blank'
    }
  }, [
    h('i.icon', {
      attrs: dataIcon('')
    }),
    ctrl.trans.noarg('openStudy')
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
    h('button.fbt', { attrs: { type: 'submit' } }, [
      h('i.icon', { attrs: dataIcon('') }),
      'Study'
    ])
  ]);
}

export class Ctrl {
  open: boolean;

  constructor() {
    this.open = location.hash === '#menu';
  }

  toggle(): void {
    this.open = !this.open;
  }
}

export function view(ctrl: AnalyseCtrl): VNode {
  const d = ctrl.data;

  const flipOpts = d.userAnalysis ? {
    hook: bind('click', ctrl.flip)
  } : {
    attrs: { href: router.game(d, d.opponent.color, ctrl.embed) + '#' + ctrl.node.ply }
  };

  const canContinue = !ctrl.ongoing && !ctrl.embed && d.game.variant.key === 'standard';
  const ceval = ctrl.getCeval();
  const mandatoryCeval = ctrl.mandatoryCeval();

  const tools: MaybeVNodes = [
    h('div.tools', [
      h('a.fbt', flipOpts, [
        h('i.icon', { attrs: dataIcon('B') }),
        ctrl.trans('flipBoard')
      ]),
      ctrl.ongoing ? null : h('a.fbt', {
        attrs: {
          href: d.userAnalysis ? '/editor?fen=' + ctrl.node.fen : '/' + d.game.id + '/edit?fen=' + ctrl.node.fen,
          rel: 'nofollow',
          target: ctrl.embed ? '_blank' : ''
        }
      }, [
        h('i.icon', { attrs: dataIcon('m') }),
        ctrl.trans('boardEditor')
      ]),
      canContinue ? h('a.fbt', {
        hook: bind('click', _ => $.modal($('.continue_with.g_' + d.game.id)))
      }, [
        h('i.icon', {
          attrs: dataIcon('U')
        }),
        ctrl.trans('continueFromHere')
      ]) : null,
      studyButton(ctrl)
    ])
  ];

  const cevalConfig: MaybeVNodes = (ceval && ceval.possible && ceval.allowed()) ? ([
    h('h2', ctrl.trans.noarg('computerAnalysis'))
  ] as MaybeVNodes).concat([
    boolSetting(ctrl, {
      name: 'enable',
      title: mandatoryCeval ? "Required by practice mode" : window.lichess.engineName,
      id: 'all',
      checked: ctrl.showComputer(),
      disabled: mandatoryCeval,
      change: ctrl.toggleComputer
    })
  ]).concat(
    ctrl.showComputer() ? [
      boolSetting(ctrl, {
        name: 'bestMoveArrow',
        id: 'shapes',
        checked: ctrl.showAutoShapes(),
        change(e) {
          ctrl.toggleAutoShapes((e.target as HTMLInputElement).checked);
        }
      }),
      boolSetting(ctrl, {
        name: 'evaluationGauge',
        id: 'gauge',
        checked: ctrl.showGauge(),
        change: ctrl.toggleGauge
      }),
      boolSetting(ctrl, {
        name: 'infiniteAnalysis',
        title: 'removesTheDepthLimit',
        id: 'infinite',
        checked: ceval.infinite(),
        change(e) {
          ctrl.cevalSetInfinite((e.target as HTMLInputElement).checked);
        }
      }),
      (id => {
        const max = 5;
        return h('div.setting', [
          h('label', { attrs: { 'for': id } }, ctrl.trans.noarg('multipleLines')),
          h('input#' + id, {
            attrs: {
              type: 'range',
              min: 1,
              max,
              step: 1
            },
            hook: rangeConfig(
              () => parseInt(ceval!.multiPv()),
              ctrl.cevalSetMultiPv)
          }),
          h('div.range_value', ceval.multiPv() + ' / ' + max)
        ]);
      })('analyse-multipv'),
      ceval.pnaclSupported ? (id => {
        let max = navigator.hardwareConcurrency;
        if (!max) return;
        if (max > 2) max--; // don't overload your computer, you dummy
        return h('div.setting', [
          h('label', { attrs: { 'for': id } }, ctrl.trans.noarg('cpus')),
          h('input#' + id, {
            attrs: {
              type: 'range',
              min: 1,
              max,
              step: 1
            },
            hook: rangeConfig(
              () => parseInt(ceval!.threads()),
              ctrl.cevalSetThreads)
          }),
          h('div.range_value', ceval.threads() + ' / ' + max)
        ]);
      })('analyse-threads') : null,
      ceval.pnaclSupported ? (id => h('div.setting', [
        h('label', { attrs: { 'for': id } }, ctrl.trans.noarg('memory')),
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
      ]))('analyse-memory') : null
    ] : []) : [];

    return h('div.action_menu',
      tools
        .concat(cevalConfig)
        .concat(ctrl.mainline.length > 4 ? [h('h2', ctrl.trans.noarg('replayMode')), autoplayButtons(ctrl)] : [])
        .concat([
          deleteButton(ctrl, ctrl.opts.userId),
          canContinue ? h('div.continue_with.g_' + d.game.id, [
            h('a.button', {
              attrs: {
                href: d.userAnalysis ? '/?fen=' + ctrl.encodeNodeFen() + '#ai' : router.cont(d, 'ai') + '?fen=' + ctrl.node.fen,
                rel: 'nofollow'
              }
            }, ctrl.trans('playWithTheMachine')),
            h('br'),
            h('a.button', {
              attrs: {
                href: d.userAnalysis ? '/?fen=' + ctrl.encodeNodeFen() + '#friend' : router.cont(d, 'friend') + '?fen=' + ctrl.node.fen,
                rel: 'nofollow'
              }
            }, ctrl.trans('playWithAFriend'))
          ]) : null
        ])
    );
}

interface BoolSetting {
  name: string,
  title?: string,
  id: string,
  checked: boolean;
  disabled?: boolean;
  change(e: MouseEvent): void;
}

function boolSetting(ctrl: AnalyseCtrl, o: BoolSetting) {
  const fullId = 'abset-' + o.id;
  return h('div.setting', o.title ? {
    attrs: { title: ctrl.trans.noarg(o.title) }
  } : {}, [
    h('label', { attrs: { 'for': fullId } }, ctrl.trans.noarg(o.name)),
    h('div.switch', [
      h('input#' + fullId + '.cmn-toggle.cmn-toggle-round', {
        attrs: {
          type: 'checkbox',
          checked: o.checked
        },
        hook: bind('change', o.change, ctrl.redraw)
      }),
      h('label', { attrs: { 'for': fullId } })
    ])
  ]);
}
