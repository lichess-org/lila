import { isEmpty } from 'lib';
import * as licon from 'lib/licon';
import { displayColumns } from 'lib/device';
import { domDialog } from 'lib/view/dialog';
import { type VNode, type LooseVNodes, bind, dataIcon, type MaybeVNodes, hl } from 'lib/snabbdom';
import type { AutoplayDelay } from '../autoplay';
import { toggle, type ToggleSettings } from 'lib/view/controls';
import type AnalyseCtrl from '../ctrl';
import { cont as contRoute } from 'lib/game/router';
import * as pgnExport from '../pgnExport';

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
      hl('button', { attrs: { type: 'submit', 'data-icon': licon.StudyBoard } }, i18n.site.toStudy),
    ],
  );
}

export function view(ctrl: AnalyseCtrl): VNode {
  const d = ctrl.data,
    canContinue = !ctrl.ongoing && d.game.variant.key === 'standard',
    ceval = ctrl.getCeval(),
    mandatoryCeval = ctrl.mandatoryCeval(),
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
      ctrl.persistence?.isDirty &&
        hl(
          'a',
          {
            attrs: {
              title: i18n.site.clearSavedMoves,
              'data-icon': licon.Trash,
            },
            hook: bind('click', ctrl.persistence.clear),
          },
          i18n.site.clearSavedMoves,
        ),
    ]),
  ];

  const cevalConfig: LooseVNodes = ceval?.allowed() && [
    displayColumns() > 1 && hl('h2', i18n.site.computerAnalysis),
    !mandatoryCeval &&
      ctrlToggle(
        {
          name: displayColumns() === 1 ? i18n.site.computerAnalysis : i18n.site.enable,
          title: 'Stockfish (Hotkey: z)',
          id: 'all',
          checked: ctrl.showComputer(),
          change: ctrl.toggleComputer,
        },
        ctrl,
      ),
    ctrl.showComputer() && [
      ctrlToggle(
        {
          name: i18n.site.bestMoveArrow,
          title: 'Hotkey: a',
          id: 'shapes',
          checked: ctrl.showAutoShapes(),
          change: ctrl.toggleAutoShapes,
        },
        ctrl,
      ),
      ctrlToggle(
        {
          name: i18n.site.evaluationGauge,
          id: 'gauge',
          checked: ctrl.showGauge(),
          change: ctrl.toggleGauge,
        },
        ctrl,
      ),
    ],
  ];

  const displayConfig = [
    displayColumns() > 1 && hl('h2', 'Display'),
    ctrlToggle(
      {
        name: i18n.site.inlineNotation,
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
    ctrlToggle(
      {
        name: i18n.site.showVariationArrows,
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

  return hl('div.action-menu', [
    tools,
    displayConfig,
    cevalConfig,
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
          i18n.site.playWithTheMachine,
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
          i18n.site.playWithAFriend,
        ),
      ]),
  ]);
}
