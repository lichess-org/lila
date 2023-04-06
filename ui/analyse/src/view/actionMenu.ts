import { isEmpty } from 'common';
import modal from 'common/modal';
import { bind, bindNonPassive, dataIcon, MaybeVNodes } from 'common/snabbdom';
import { h, VNode } from 'snabbdom';
import { AutoplayDelay } from '../autoplay';
import { config as externalEngineConfig } from './externalEngine';
import { toggle, ToggleSettings, rangeConfig } from 'common/controls';
import AnalyseCtrl from '../ctrl';
import { cont as contRoute } from 'game/router';
import * as pgnExport from '../pgnExport';

interface AutoplaySpeed {
  name: string;
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

const ctrlToggle = (t: ToggleSettings, ctrl: AnalyseCtrl) => toggle(t, ctrl.trans, ctrl.redraw);

function deleteButton(ctrl: AnalyseCtrl, userId?: string): VNode | undefined {
  const g = ctrl.data.game;
  if (g.source === 'import' && g.importedBy && g.importedBy === userId)
    return h(
      'form.delete',
      {
        attrs: {
          method: 'post',
          action: `/${g.id}/delete`,
        },
        hook: bindNonPassive('submit', _ => confirm(ctrl.trans.noarg('deleteThisImportedGame'))),
      },
      [
        h(
          'button.button.text.button-thin.button-red',
          {
            attrs: {
              type: 'submit',
              'data-icon': '',
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
      const active = ctrl.autoplay.getDelay() == speed.delay;
      return h(
        'a.button',
        {
          class: {
            active,
            'button-empty': !active,
          },
          hook: bind('click', () => ctrl.togglePlay(speed.delay), ctrl.redraw),
        },
        ctrl.trans.noarg(speed.name)
      );
    })
  );
}

const formatHashSize = (v: number): string => (v < 1000 ? v + 'MB' : Math.round(v / 1024) + 'GB');

const hiddenInput = (name: string, value: string) => h('input', { attrs: { type: 'hidden', name, value } });

export function studyButton(ctrl: AnalyseCtrl) {
  if (ctrl.study && ctrl.embed && !ctrl.ongoing)
    return h(
      'a.button.button-empty',
      {
        attrs: {
          href: `/study/${ctrl.study.data.id}#${ctrl.study.currentChapter().id}`,
          target: '_blank',
          rel: 'noopener',
          'data-icon': '',
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
        const pgnInput = (e.target as HTMLElement).querySelector('input[name=pgn]') as HTMLInputElement;
        if (pgnInput && (!ctrl.persistence || ctrl.persistence.isDirty)) {
          pgnInput.value = pgnExport.renderFullTxt(ctrl);
        }
      }),
    },
    [
      !ctrl.synthetic ? hiddenInput('gameId', ctrl.data.game.id) : null,
      hiddenInput('pgn', ''),
      hiddenInput('orientation', ctrl.bottomColor()),
      hiddenInput('variant', ctrl.data.game.variant.key),
      hiddenInput('fen', ctrl.tree.root.fen),
      h(
        'button.button.button-empty',
        {
          attrs: {
            type: 'submit',
            'data-icon': '',
          },
        },
        ctrl.trans.noarg('toStudy')
      ),
    ]
  );
}

export function view(ctrl: AnalyseCtrl): VNode {
  const d = ctrl.data,
    noarg = ctrl.trans.noarg,
    canContinue = !ctrl.ongoing && !ctrl.embed && d.game.variant.key === 'standard',
    ceval = ctrl.getCeval(),
    mandatoryCeval = ctrl.mandatoryCeval();

  const tools: MaybeVNodes = [
    h('div.action-menu__tools', [
      h(
        'a.button.button-empty',
        {
          hook: bind('click', ctrl.flip),
          attrs: {
            'data-icon': '',
            title: 'Hotkey: f',
          },
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
                  ? '/editor?' +
                    new URLSearchParams({
                      fen: ctrl.node.fen,
                      variant: d.game.variant.key,
                      color: ctrl.chessground.state.orientation,
                    })
                  : `/${d.game.id}/edit?fen=${ctrl.node.fen}`,
                'data-icon': '',
                ...(ctrl.embed
                  ? {
                      target: '_blank',
                      rel: 'noopener nofollow',
                    }
                  : {
                      rel: 'nofollow',
                    }),
              },
            },
            noarg('boardEditor')
          ),
      canContinue
        ? h(
            'a.button.button-empty',
            {
              hook: bind('click', _ =>
                modal({
                  content: $('.continue-with.g_' + d.game.id),
                })
              ),
              attrs: dataIcon(''),
            },
            noarg('continueFromHere')
          )
        : null,
      studyButton(ctrl),
    ]),
  ];

  const notSupported = (ceval?.technology == 'external' ? 'Engine' : 'Browser') + ' does not support this option';

  const cevalConfig: MaybeVNodes =
    ceval?.possible && ceval.allowed()
      ? [
          h('h2', noarg('computerAnalysis')),
          ctrlToggle(
            {
              name: 'enable',
              title: (mandatoryCeval ? 'Required by practice mode' : 'Stockfish') + ' (Hotkey: z)',
              id: 'all',
              checked: ctrl.showComputer(),
              disabled: mandatoryCeval,
              change: ctrl.toggleComputer,
            },
            ctrl
          ),
          ...(ctrl.showComputer()
            ? [
                ctrlToggle(
                  {
                    name: 'bestMoveArrow',
                    title: 'Hotkey: a',
                    id: 'shapes',
                    checked: ctrl.showAutoShapes(),
                    change: ctrl.toggleAutoShapes,
                  },
                  ctrl
                ),
                ctrlToggle(
                  {
                    name: 'evaluationGauge',
                    id: 'gauge',
                    checked: ctrl.showGauge(),
                    change: ctrl.toggleGauge,
                  },
                  ctrl
                ),
                ctrlToggle(
                  {
                    name: 'Annotations on board',
                    title: 'Display analysis symbols on the board',
                    id: 'move-annotation',
                    checked: ctrl.showMoveAnnotation(),
                    change: ctrl.toggleMoveAnnotation,
                  },
                  ctrl
                ),
                ctrlToggle(
                  {
                    name: 'infiniteAnalysis',
                    title: 'removesTheDepthLimit',
                    id: 'infinite',
                    checked: ceval.infinite(),
                    change: ctrl.cevalSetInfinite,
                  },
                  ctrl
                ),
                ceval.technology != 'external'
                  ? ctrlToggle(
                      {
                        name: 'Use NNUE',
                        title: ceval.platform.supportsNnue
                          ? 'Downloads 6 MB neural network evaluation file (page reload required after change)'
                          : notSupported,
                        id: 'enable-nnue',
                        checked: ceval.platform.supportsNnue && ceval.enableNnue(),
                        change: ceval.enableNnue,
                        disabled: !ceval.platform.supportsNnue,
                      },
                      ctrl
                    )
                  : null,
                (id => {
                  const max = 5;
                  return h('div.setting', [
                    h('label', { attrs: { for: id } }, noarg('multipleLines')),
                    h('input#' + id, {
                      attrs: {
                        type: 'range',
                        min: 0,
                        max,
                        step: 1,
                      },
                      hook: rangeConfig(() => ceval!.multiPv(), ctrl.cevalSetMultiPv),
                    }),
                    h('div.range_value', ceval.multiPv() + ' / ' + max),
                  ]);
                })('analyse-multipv'),
                (id => {
                  return h('div.setting', [
                    h('label', { attrs: { for: id } }, noarg('cpus')),
                    h('input#' + id, {
                      attrs: {
                        type: 'range',
                        min: 1,
                        max: ceval.platform.maxThreads,
                        step: 1,
                        disabled: ceval.platform.maxThreads <= 1,
                        ...(ceval.platform.maxThreads <= 1 ? { title: notSupported } : null),
                      },
                      hook: rangeConfig(() => ceval.threads(), ctrl.cevalSetThreads),
                    }),
                    h('div.range_value', `${ceval.threads ? ceval.threads() : 1} / ${ceval.platform.maxThreads}`),
                  ]);
                })('analyse-threads'),
                (id =>
                  h('div.setting', [
                    h('label', { attrs: { for: id } }, noarg('memory')),
                    h('input#' + id, {
                      attrs: {
                        type: 'range',
                        min: 4,
                        max: Math.floor(Math.log2(ceval.platform.maxHashSize())),
                        step: 1,
                        disabled: ceval.platform.maxHashSize() <= 16,
                        ...(ceval.platform.maxHashSize() <= 16 ? { title: notSupported } : null),
                      },
                      hook: rangeConfig(
                        () => Math.floor(Math.log2(ceval.hashSize())),
                        v => ctrl.cevalSetHashSize(Math.pow(2, v))
                      ),
                    }),
                    h('div.range_value', formatHashSize(ceval.hashSize())),
                  ]))('analyse-memory'),
              ]
            : []),
        ]
      : [];

  const notationConfig = [
    ctrlToggle(
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

  return h('div.action-menu', [
    ...tools,
    ...notationConfig,
    ...cevalConfig,
    ...externalEngineConfig(ctrl),
    ...(ctrl.mainline.length > 4 ? [h('h2', noarg('replayMode')), autoplayButtons(ctrl)] : []),
    deleteButton(ctrl, ctrl.opts.userId),
    canContinue
      ? h('div.continue-with.none.g_' + d.game.id, [
          h(
            'a.button',
            {
              attrs: {
                href: d.userAnalysis
                  ? '/?fen=' + ctrl.encodeNodeFen() + '#ai'
                  : contRoute(d, 'ai') + '?fen=' + ctrl.node.fen,
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
                  ? '/?fen=' + ctrl.encodeNodeFen() + '#friend'
                  : contRoute(d, 'friend') + '?fen=' + ctrl.node.fen,
                rel: 'nofollow',
              },
            },
            noarg('playWithAFriend')
          ),
        ])
      : null,
  ]);
}
