import { isEmpty } from 'common/common';
import { editor, encodeSfen, setup } from 'common/links';
import { MaybeVNodes, bind, bindNonPassive, dataIcon } from 'common/snabbdom';
import { cont as contRoute } from 'game/router';
import { Hooks, VNode, h } from 'snabbdom';
import { AutoplayDelay } from './autoplay';
import { BoolSetting, boolSetting } from './boolSetting';
import AnalyseCtrl from './ctrl';
import * as kifExport from './notationExport';

interface AutoplaySpeed {
  name: I18nKey;
  delay: AutoplayDelay;
}

const baseSpeeds: AutoplaySpeed[] = [
  {
    name: 'fast',
    delay: 1000,
  },
  {
    name: 'slow',
    delay: 5000,
  },
];

const realtimeSpeed: AutoplaySpeed = {
  name: 'realtimeReplay',
  delay: 'realtime',
};

const cplSpeed: AutoplaySpeed = {
  name: 'byCPL',
  delay: 'cpl',
};

function deleteButton(ctrl: AnalyseCtrl, userId: string | null): VNode | undefined {
  const g = ctrl.data.game;
  if (g.source === 'import' && g.importedBy && g.importedBy === userId)
    return h(
      'form.delete',
      {
        attrs: {
          method: 'post',
          action: '/' + g.id + '/delete',
        },
        hook: bindNonPassive('submit', _ => confirm(ctrl.trans.noarg('deleteThisImportedGame'))),
      },
      [
        h(
          'button.button.text.thin',
          {
            attrs: {
              type: 'submit',
              'data-icon': 'q',
            },
          },
          ctrl.trans.noarg('delete')
        ),
      ]
    );
  return;
}

function autoplayButtons(ctrl: AnalyseCtrl): VNode {
  const d = ctrl.data;
  const speeds = [
    ...baseSpeeds,
    ...(d.game.speed !== 'correspondence' && !isEmpty(d.game.moveCentis) ? [realtimeSpeed] : []),
    ...(d.analysis ? [cplSpeed] : []),
  ];
  return h(
    'div.autoplay',
    speeds.map(speed => {
      return h(
        'a.button.button-empty',
        {
          hook: bind('click', () => ctrl.togglePlay(speed.delay), ctrl.redraw),
        },
        ctrl.trans.noarg(speed.name)
      );
    })
  );
}

function rangeConfig(read: () => number, write: (value: number) => void): Hooks {
  return {
    insert: vnode => {
      const el = vnode.elm as HTMLInputElement;
      el.value = '' + read();
      el.addEventListener('input', _ => write(parseInt(el.value)));
      el.addEventListener('mouseout', _ => el.blur());
    },
  };
}

function formatHashSize(v: number): string {
  if (v < 1000) return v + 'MB';
  else return Math.round(v / 1024) + 'GB';
}

function hiddenInput(name: string, value: string) {
  return h('input', {
    attrs: { type: 'hidden', name, value },
  });
}

function studyButton(ctrl: AnalyseCtrl) {
  if (ctrl.study && ctrl.embed && !ctrl.ongoing)
    return h(
      'a.button.button-empty',
      {
        attrs: {
          href: '/study/' + ctrl.study.data.id + '#' + ctrl.study.currentChapter().id,
          target: '_blank',
          'data-icon': '4',
        },
      },
      ctrl.trans.noarg('openStudy')
    );
  if (ctrl.study || ctrl.ongoing || ctrl.embed) return;
  return h(
    'form',
    {
      attrs: {
        method: 'post',
        action: '/study/as',
      },
      hook: bind('submit', e => {
        const kifInput = (e.target as HTMLElement).querySelector('input[name=notation]') as HTMLInputElement;
        if (kifInput) kifInput.value = kifExport.renderFullKif(ctrl);
      }),
    },
    [
      !ctrl.synthetic ? hiddenInput('gameId', ctrl.data.game.id) : hiddenInput('notation', ''),
      hiddenInput('orientation', ctrl.shogiground.state.orientation),
      hiddenInput('variant', ctrl.data.game.variant.key),
      hiddenInput('sfen', ctrl.tree.root.sfen),
      h(
        'button.button.button-empty',
        {
          attrs: {
            type: 'submit',
            'data-icon': '4',
          },
        },
        ctrl.trans.noarg('toStudy')
      ),
    ]
  );
}

export class Ctrl {
  open: boolean = false;
  toggle = () => (this.open = !this.open);
}

export function view(ctrl: AnalyseCtrl): VNode {
  const d = ctrl.data,
    noarg = ctrl.trans.noarg,
    canContinue = !ctrl.ongoing && !ctrl.embed,
    ceval = ctrl.getCeval(),
    mandatoryCeval = ctrl.mandatoryCeval();

  const tools: MaybeVNodes = [
    h('div.action-menu__tools', [
      h(
        'a.button.button-empty',
        {
          hook: bind('click', ctrl.flip),
          attrs: dataIcon('B'),
        },
        noarg('flipBoard')
      ),
      ctrl.ongoing
        ? null
        : h(
            'a.button.button-empty',
            {
              attrs: {
                href: d.userAnalysis
                  ? editor(d.game.variant.key, ctrl.node.sfen, ctrl.shogiground.state.orientation)
                  : '/' + d.game.id + '/edit?sfen=' + encodeSfen(ctrl.node.sfen, true),
                rel: 'nofollow',
                target: ctrl.embed ? '_blank' : '',
                'data-icon': 'm',
              },
            },
            noarg('boardEditor')
          ),
      canContinue
        ? h(
            'a.button.button-empty',
            {
              hook: bind('click', _ => $.modal($('.continue-with.g_' + d.game.id))),
              attrs: dataIcon('U'),
            },
            noarg('continueFromHere')
          )
        : null,
      studyButton(ctrl),
    ]),
  ];

  const notSupported = 'Browser does not support this option';

  const cevalConfig: MaybeVNodes =
    ceval && ceval.possible && ceval.allowed()
      ? ([h('h2', noarg('computerAnalysis'))] as MaybeVNodes)
          .concat([
            ctrlBoolSetting(
              {
                name: noarg('enable'),
                title: (mandatoryCeval ? 'Required by practice mode' : 'Engine') + ' (Hotkey: z)',
                id: 'all',
                checked: ctrl.showComputer(),
                disabled: mandatoryCeval,
                change: ctrl.toggleComputer,
              },
              ctrl
            ),
          ])
          .concat(
            ctrl.showComputer()
              ? [
                  ctrlBoolSetting(
                    {
                      name: noarg('bestMoveArrow'),
                      title: 'Hotkey: a',
                      id: 'shapes',
                      checked: ctrl.showAutoShapes(),
                      change: ctrl.toggleAutoShapes,
                    },
                    ctrl
                  ),
                  ctrlBoolSetting(
                    {
                      name: noarg('evaluationGauge'),
                      id: 'gauge',
                      checked: ctrl.showGauge(),
                      change: ctrl.toggleGauge,
                    },
                    ctrl
                  ),
                  ctrlBoolSetting(
                    {
                      name: 'Move annotation',
                      id: 'move-annotation',
                      checked: ctrl.showMoveAnnotation(),
                      change: ctrl.toggleMoveAnnotation,
                    },
                    ctrl
                  ),
                  ctrlBoolSetting(
                    {
                      name: noarg('infiniteAnalysis'),
                      title: noarg('removesTheDepthLimit'),
                      id: 'infinite',
                      checked: ceval.infinite(),
                      change: ctrl.cevalSetInfinite,
                    },
                    ctrl
                  ),
                  ctrlBoolSetting(
                    {
                      name: 'Use NNUE',
                      title: ceval.supportsNnue ? 'Use NNUE  (page reload required after change)' : notSupported,
                      id: 'enable-nnue',
                      checked: ceval.supportsNnue && ceval.enableNnue(),
                      change: ctrl.cevalSetEnableNnue,
                      disabled: !ceval.supportsNnue,
                    },
                    ctrl
                  ),
                  ctrlBoolSetting(
                    {
                      name: noarg('impasse') + ' - ' + noarg('computerAnalysis'),
                      title: ceval.supportsNnue ? 'YaneuraOu - EnteringKingRule' : notSupported,
                      id: 'enteringKingRule',
                      checked: ceval.supportsNnue && ceval.enteringKingRule(),
                      change: ctrl.cevalSetEnteringKingRule,
                      disabled: !ceval.supportsNnue,
                    },
                    ctrl
                  ),
                  (id => {
                    const max = 5;
                    return h('div.setting', [
                      h('label', { attrs: { for: id } }, noarg('multipleLines')),
                      h('input#' + id, {
                        attrs: {
                          type: 'range',
                          min: 1,
                          max,
                          step: 1,
                        },
                        hook: rangeConfig(() => parseInt(ceval!.multiPv()), ctrl.cevalSetMultiPv),
                      }),
                      h('div.range_value', ceval.multiPv() + ' / ' + max),
                    ]);
                  })('analyse-multipv'),
                  ceval.threads
                    ? (id => {
                        return h('div.setting', [
                          h(
                            'label',
                            {
                              attrs: {
                                for: id,
                                ...(ceval.maxThreads <= 1 ? { title: notSupported } : null),
                              },
                            },
                            noarg('cpus')
                          ),
                          h('input#' + id, {
                            attrs: {
                              type: 'range',
                              min: 1,
                              max: ceval.maxThreads,
                              step: 1,
                              disabled: ceval.maxThreads <= 1,
                            },
                            hook: rangeConfig(() => ceval.threads!(), ctrl.cevalSetThreads),
                          }),
                          h('div.range_value', `${ceval.threads()} / ${ceval.maxThreads}`),
                        ]);
                      })('analyse-threads')
                    : null,
                  ceval.hashSize
                    ? (id =>
                        h('div.setting', [
                          h(
                            'label',
                            {
                              attrs: {
                                for: id,
                                ...(ceval.maxHashSize <= 16 ? { title: notSupported } : null),
                              },
                            },
                            noarg('memory')
                          ),
                          h('input#' + id, {
                            attrs: {
                              type: 'range',
                              min: 4,
                              max: Math.floor(Math.log2(ceval.maxHashSize)),
                              step: 1,
                              disabled: ceval.maxHashSize <= 16,
                            },
                            hook: rangeConfig(
                              () => Math.floor(Math.log2(ceval.hashSize!())),
                              v => ctrl.cevalSetHashSize(Math.pow(2, v))
                            ),
                          }),
                          h('div.range_value', formatHashSize(ceval.hashSize())),
                        ]))('analyse-memory')
                    : null,
                ]
              : []
          )
      : [];

  const notationConfig = [
    ctrlBoolSetting(
      {
        name: noarg('inlineNotation'),
        title: 'Shift+I',
        id: 'inline',
        checked: ctrl.treeView.inline(),
        change(v) {
          ctrl.treeView.set(v);
          ctrl.actionMenu.toggle();
        },
      },
      ctrl
    ),
  ];

  return h(
    'div.action-menu',
    tools
      .concat(notationConfig)
      .concat(cevalConfig)
      .concat(ctrl.mainline.length > 4 ? [h('h2', noarg('replayMode')), autoplayButtons(ctrl)] : [])
      .concat([
        deleteButton(ctrl, ctrl.opts.userId),
        canContinue
          ? h('div.continue-with.none.g_' + d.game.id, [
              h(
                'a.button',
                {
                  attrs: {
                    href: d.userAnalysis
                      ? setup('/', ctrl.data.game.variant.key, ctrl.node.sfen, 'ai')
                      : setup(contRoute(d, 'ai'), ctrl.data.game.variant.key, ctrl.node.sfen),
                    rel: 'nofollow',
                  },
                },
                noarg('playWithTheMachine')
              ),
              h(
                'a.button',
                {
                  attrs: {
                    href: d.userAnalysis
                      ? setup('/', ctrl.data.game.variant.key, ctrl.node.sfen, 'friend')
                      : setup(contRoute(d, 'friend'), ctrl.data.game.variant.key, ctrl.node.sfen),
                    rel: 'nofollow',
                  },
                },
                noarg('playWithAFriend')
              ),
            ])
          : null,
      ])
  );
}

function ctrlBoolSetting(o: BoolSetting, ctrl: AnalyseCtrl) {
  return boolSetting(o, ctrl.redraw);
}
