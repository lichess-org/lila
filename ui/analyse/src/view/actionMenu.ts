import { isEmpty } from 'lib';
import * as licon from 'lib/licon';
import { displayColumns } from 'lib/device';
import {
  domDialog,
  bind,
  dataIcon,
  hl,
  toggle,
  type VNode,
  type LooseVNodes,
  type MaybeVNodes,
  type ToggleSettings,
} from 'lib/view';
import type { AutoplayDelay } from '../autoplay';
import type AnalyseCtrl from '../ctrl';
import { cont as contRoute } from 'lib/game/router';
import * as pgnExport from '../pgnExport';
import { clamp } from 'lib/algo';
import { config as motifConfig } from '../motif/motifView';

interface AutoplaySpeed {
  name: keyof I18n['site'];
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

const ctrlToggle = (t: ToggleSettings, ctrl: AnalyseCtrl) => toggle(t, ctrl.redraw);

function autoplayButtons(ctrl: AnalyseCtrl): VNode {
  const d = ctrl.data;
  const speeds = [
    ...baseSpeeds,
    ...(d.game.speed !== 'correspondence' && !isEmpty(d.game.moveCentis) ? [realtimeSpeed] : []),
    ...(d.analysis ? [cplSpeed] : []),
  ];
  return hl(
    'div.autoplay',
    speeds.map(speed => {
      const active = ctrl.autoplay.getDelay() === speed.delay;
      return hl(
        'a.button',
        {
          class: { active, 'button-empty': !active },
          hook: bind('click', () => ctrl.togglePlay(speed.delay), ctrl.redraw),
        },
        String(i18n.site[speed.name]),
      );
    }),
  );
}

const hiddenInput = (name: string, value: string) => hl('input', { attrs: { type: 'hidden', name, value } });

function studyButton(ctrl: AnalyseCtrl) {
  if (ctrl.study || ctrl.ongoing) return;
  return hl(
    'form',
    {
      attrs: { method: 'post', action: '/study/as' },
      hook: bind('submit', e => {
        const pgnInput = (e.target as HTMLElement).querySelector('input[name=pgn]') as HTMLInputElement;
        if (pgnInput && (ctrl.synthetic || ctrl.idbTree.isDirty)) {
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
      hl('button', { attrs: { type: 'submit', 'data-icon': licon.StudyBoard } }, i18n.site.toStudy),
    ],
  );
}

export function view(ctrl: AnalyseCtrl): VNode {
  const d = ctrl.data,
    canContinue = !ctrl.ongoing && d.game.variant.key === 'standard',
    canPractice = ctrl.isCevalAllowed() && !ctrl.isEmbed && !ctrl.isGamebook() && !ctrl.practice,
    canRetro = ctrl.hasFullComputerAnalysis() && !ctrl.isEmbed && !ctrl.retro,
    linkAttrs = { rel: ctrl.isEmbed ? '' : 'nofollow', target: ctrl.isEmbed ? '_blank' : '' };

  const tools: MaybeVNodes = [
    hl('div.action-menu__tools', [
      hl(
        'a',
        {
          hook: bind('click', () => {
            ctrl.flip();
            ctrl.actionMenu.toggle();
            ctrl.redraw();
          }),
          attrs: { 'data-icon': licon.ChasingArrows, title: 'Hotkey: f' },
        },
        i18n.site.flipBoard,
      ),
      !ctrl.ongoing &&
        hl(
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
          i18n.site.boardEditor,
        ),
      displayColumns() === 1 &&
        canPractice &&
        hl(
          'a',
          { hook: bind('click', () => ctrl.togglePractice()), attrs: dataIcon(licon.Bullseye) },
          'Practice with computer',
        ),
      canRetro &&
        hl(
          'a',
          { hook: bind('click', ctrl.toggleRetro, ctrl.redraw), attrs: dataIcon(licon.GraduateCap) },
          'Learn from your mistakes',
        ),
      canContinue &&
        hl(
          'a',
          {
            hook: bind('click', () =>
              domDialog({ cash: $('.continue-with.g_' + d.game.id), modal: true, show: true }),
            ),
            attrs: dataIcon(licon.Swords),
          },
          i18n.site.continueFromHere,
        ),
      studyButton(ctrl),
      ctrl.idbTree.isDirty &&
        hl(
          'a',
          {
            attrs: {
              title: i18n.site.clearSavedMoves,
              'data-icon': licon.Trash,
            },
            hook: bind('click', ctrl.idbTree.clear),
          },
          i18n.site.clearSavedMoves,
        ),
    ]),
  ];

  const cevalConfig: LooseVNodes = ctrl.study?.isCevalAllowed() !== false && [
    displayColumns() > 1 && hl('h2', i18n.site.computerAnalysis),
    ctrlToggle(
      {
        name: 'Show fishnet analysis',
        title: 'Show fishnet analysis (Hotkey: z)',
        id: 'all',
        checked: ctrl.showFishnetAnalysis(),
        change: ctrl.toggleFishnetAnalysis,
      },
      ctrl,
    ),
    ctrlToggle(
      {
        name: i18n.site.bestMoveArrow,
        title: 'Hotkey: a',
        id: 'shapes',
        checked: ctrl.showBestMoveArrowsProp(),
        change: ctrl.showBestMoveArrowsProp,
      },
      ctrl,
    ),
    ctrl.showBestMoveArrowsProp() &&
      ctrlToggle(
        {
          name: 'Piece maneuver arrows',
          id: 'maneuver-arrows',
          checked: ctrl.showManeuverMoveArrowsProp(),
          change: ctrl.showManeuverMoveArrowsProp,
        },
        ctrl,
      ),
    displayColumns() > 1 &&
      ctrlToggle(
        {
          name: i18n.site.evaluationGauge,
          id: 'gauge',
          checked: ctrl.showGauge(),
          change: ctrl.showGauge,
        },
        ctrl,
      ),
  ];

  const displayConfig = [
    displayColumns() > 1 && hl('h2', 'Display'),
    ctrlToggle(
      {
        name: i18n.site.inlineNotation,
        title: 'Shift+I',
        id: 'inline',
        checked: ctrl.treeView.modePreference() === 'inline',
        change(v) {
          ctrl.treeView.modePreference(v ? 'inline' : 'column');
          ctrl.actionMenu.toggle();
        },
      },
      ctrl,
    ),
    ctrlToggle(
      {
        name: 'Disclosure buttons',
        title: 'Show disclosure buttons to expand/collapse variations',
        id: 'disclosure',
        checked: ctrl.disclosureMode(),
        change: ctrl.disclosureMode,
      },
      ctrl,
    ),
    !ctrl.ongoing &&
      ctrlToggle(
        {
          name: 'Annotations on board',
          title: 'Display analysis symbols on the board',
          id: 'move-annotation',
          checked: ctrl.possiblyShowMoveAnnotationsOnBoard(),
          change: ctrl.togglePossiblyShowMoveAnnotationsOnBoard,
        },
        ctrl,
      ),
  ];

  return hl('div.action-menu', [
    tools,
    displayConfig,
    displayColumns() > 1 && renderVariationOpacitySlider(ctrl),
    cevalConfig,
    ctrl.motifAllowed() ? motifConfig(ctrl) : [],
    displayColumns() === 1 && renderVariationOpacitySlider(ctrl),
    ctrl.mainline.length > 4 && [hl('h2', i18n.site.replayMode), autoplayButtons(ctrl)],
    canContinue &&
      hl('div.continue-with.none.g_' + d.game.id, [
        hl(
          'a.button',
          {
            attrs: {
              href: d.userAnalysis
                ? '/?fen=' + ctrl.encodeNodeFen() + '#ai'
                : contRoute(d, 'ai') + '?fen=' + ctrl.node.fen,
              ...linkAttrs,
            },
          },
          i18n.site.playAgainstComputer,
        ),
        hl(
          'a.button',
          {
            attrs: {
              href: d.userAnalysis
                ? '/?fen=' + ctrl.encodeNodeFen() + '#friend'
                : contRoute(d, 'friend') + '?fen=' + ctrl.node.fen,
              ...linkAttrs,
            },
          },
          i18n.site.challengeAFriend,
        ),
      ]),
  ]);
}

function renderVariationOpacitySlider(ctrl: AnalyseCtrl) {
  return hl('span.setting', [
    hl('label', 'Variation opacity'),
    hl('input.range', {
      key: 'variation-arrows',
      attrs: { min: 0, max: 1, step: 0.1, type: 'range', value: ctrl.variationArrowOpacity() || 0 },
      props: { value: ctrl.variationArrowOpacity() || 0 },
      hook: {
        insert: (vnode: VNode) => {
          const input = vnode.elm as HTMLInputElement;
          input.addEventListener('input', () => {
            ctrl.variationArrowOpacity(parseFloat(input.value));
          });
          input.addEventListener('wheel', e => {
            e.preventDefault();
            ctrl.variationArrowOpacity(
              clamp((ctrl.variationArrowOpacity() || 0) + (e.deltaY > 0 ? -0.1 : 0.1), {
                min: 0,
                max: 1,
              }),
            );
          });
        },
      },
    }),
  ]);
}
