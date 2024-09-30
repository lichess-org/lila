import { isEmpty } from 'common';
import * as licon from 'common/licon';
import { isTouchDevice } from 'common/device';
import { domDialog } from 'common/dialog';
import { bind, dataIcon, MaybeVNodes, looseH as h } from 'common/snabbdom';
import { VNode } from 'snabbdom';
import { AutoplayDelay } from '../autoplay';
import { toggle, ToggleSettings } from 'common/controls';
import AnalyseCtrl from '../ctrl';
import { cont as contRoute } from 'game/router';
import * as pgnExport from '../pgnExport';

interface AutoplaySpeed {
  name: string;
  delay: AutoplayDelay;
}

const baseSpeeds: AutoplaySpeed[] = [
  { name: 'fast', delay: 1000 },
  { name: 'slow', delay: 5000 },
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
          class: { active, 'button-empty': !active },
          hook: bind('click', () => ctrl.togglePlay(speed.delay), ctrl.redraw),
        },
        ctrl.trans.noarg(speed.name),
      );
    }),
  );
}

const hiddenInput = (name: string, value: string) => h('input', { attrs: { type: 'hidden', name, value } });

function studyButton(ctrl: AnalyseCtrl) {
  if (ctrl.study && !ctrl.ongoing)
    return h(
      'a',
      {
        attrs: {
          href: `/study/${ctrl.study.data.id}#${ctrl.study.currentChapter().id}`,
          target: '_blank',
          rel: 'noopener',
          'data-icon': licon.StudyBoard,
        },
      },
      ctrl.trans.noarg('openStudy'),
    );
  if (ctrl.study || ctrl.ongoing) return;
  return h(
    'form',
    {
      attrs: { method: 'post', action: '/study/as' },
      hook: bind('submit', e => {
        const pgnInput = (e.target as HTMLElement).querySelector('input[name=pgn]') as HTMLInputElement;
        if (pgnInput && (ctrl.synthetic || ctrl.persistence?.isDirty)) {
          pgnInput.value = pgnExport.renderFullTxt(ctrl);
        }
      }),
    },
    [
      !ctrl.synthetic && hiddenInput('gameId', ctrl.data.game.id),
      hiddenInput('pgn', ''),
      hiddenInput('orientation', ctrl.bottomColor()),
      hiddenInput('variant', ctrl.data.game.variant.key),
      hiddenInput('fen', ctrl.tree.root.fen),
      h('button', { attrs: { type: 'submit', 'data-icon': licon.StudyBoard } }, ctrl.trans.noarg('toStudy')),
    ],
  );
}

export function view(ctrl: AnalyseCtrl): VNode {
  const d = ctrl.data,
    noarg = ctrl.trans.noarg,
    canContinue = !ctrl.ongoing && d.game.variant.key === 'standard',
    ceval = ctrl.getCeval(),
    mandatoryCeval = ctrl.mandatoryCeval(),
    linkAttrs = { rel: ctrl.isEmbed ? '' : 'nofollow', target: ctrl.isEmbed ? '_blank' : '' };

  const tools: MaybeVNodes = [
    h('div.action-menu__tools', [
      h(
        'a',
        { hook: bind('click', ctrl.flip), attrs: { 'data-icon': licon.ChasingArrows, title: 'Hotkey: f' } },
        noarg('flipBoard'),
      ),
      !ctrl.ongoing &&
        h(
          'a',
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
              'data-icon': licon.Pencil,
              ...linkAttrs,
            },
          },
          noarg('boardEditor'),
        ),
      canContinue &&
        h(
          'a',
          {
            hook: bind('click', () => domDialog({ cash: $('.continue-with.g_' + d.game.id), show: 'modal' })),
            attrs: dataIcon(licon.Swords),
          },
          noarg('continueFromHere'),
        ),
      studyButton(ctrl),
      ctrl.persistence?.isDirty &&
        h(
          'a',
          {
            attrs: {
              title: noarg('clearSavedMoves'),
              'data-icon': licon.Trash,
            },
            hook: bind('click', ctrl.persistence.clear),
          },
          noarg('clearSavedMoves'),
        ),
    ]),
  ];

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
            ctrl,
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
                  ctrl,
                ),
                ctrlToggle(
                  {
                    name: 'evaluationGauge',
                    id: 'gauge',
                    checked: ctrl.showGauge(),
                    change: ctrl.toggleGauge,
                  },
                  ctrl,
                ),
              ]
            : []),
        ]
      : [];

  const displayConfig = [
    h('h2', 'Display'),
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
      ctrl,
    ),
    !isTouchDevice() &&
      ctrlToggle(
        {
          name: 'showVariationArrows',
          title: 'Variation navigation arrows',
          id: 'variationArrows',
          checked: ctrl.variationArrowsProp(),
          change: ctrl.toggleVariationArrows,
        },
        ctrl,
      ),
    !ctrl.ongoing &&
      ctrlToggle(
        {
          name: 'Annotations on board',
          title: 'Display analysis symbols on the board',
          id: 'move-annotation',
          checked: ctrl.showMoveAnnotation(),
          change: ctrl.toggleMoveAnnotation,
        },
        ctrl,
      ),
  ];

  return h('div.action-menu', [
    ...tools,
    ...displayConfig,
    ...cevalConfig,
    ...(ctrl.mainline.length > 4 ? [h('h2', noarg('replayMode')), autoplayButtons(ctrl)] : []),
    canContinue &&
      h('div.continue-with.none.g_' + d.game.id, [
        h(
          'a.button',
          {
            attrs: {
              href: d.userAnalysis
                ? '/?fen=' + ctrl.encodeNodeFen() + '#ai'
                : contRoute(d, 'ai') + '?fen=' + ctrl.node.fen,
              ...linkAttrs,
            },
          },
          noarg('playWithTheMachine'),
        ),
        h(
          'a.button',
          {
            attrs: {
              href: d.userAnalysis
                ? '/?fen=' + ctrl.encodeNodeFen() + '#friend'
                : contRoute(d, 'friend') + '?fen=' + ctrl.node.fen,
              ...linkAttrs,
            },
          },
          noarg('playWithAFriend'),
        ),
      ]),
  ]);
}
