import { isEmpty } from 'lib';
import { displayColumns } from 'lib/device';
import { cont as contRoute } from 'lib/game/router';
import { licon } from 'lib/licon';
import { domDialog, bind, dataIcon, hl, type VNode, type MaybeVNodes } from 'lib/view';

import type { AutoplayDelay } from '../autoplay';
import type AnalyseCtrl from '../ctrl';
import * as pgnExport from '../pgnExport';
import { showSettingsDialog } from './settingsView';

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
        if (pgnInput && (ctrl.synthetic || ctrl.idbTree.movesDirty)) {
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
          i18n.site.practiceWithComputer,
        ),
      canRetro &&
        hl(
          'a',
          { hook: bind('click', ctrl.toggleRetro, ctrl.redraw), attrs: dataIcon(licon.GraduateCap) },
          i18n.site.learnFromYourMistakes,
        ),
      canContinue &&
        hl(
          'a',
          {
            hook: bind('click', () =>
              domDialog({
                cash: $('.continue-with.g_' + d.game.id),
                modal: true,
                show: true,
                easyClose: 'clickOutside',
              }),
            ),
            attrs: dataIcon(licon.Swords),
          },
          i18n.site.continueFromHere,
        ),
      studyButton(ctrl),
      ctrl.idbTree.movesDirty &&
        hl(
          'a',
          {
            attrs: {
              title: i18n.site.clearSavedMoves,
              'data-icon': licon.Trash,
            },
            hook: bind('click', () => ctrl.idbTree.clear('moves')),
          },
          i18n.site.clearSavedMoves,
        ),
      hl(
        'button',
        {
          attrs: { 'data-icon': licon.Gear, title: i18n.site.settings },
          on: { click: () => showSettingsDialog(ctrl) },
        },
        i18n.site.settings,
      ),
      displayColumns() > 1 &&
        hl(
          'button',
          {
            attrs: { 'data-icon': licon.Move },
            on: { click: () => ctrl.presentationMode(true) },
          },
          'Presentation mode',
        ),
    ]),
  ];

  return hl('div.action-menu.sub-box.reduced', [
    hl('div.title', i18n.site.analysis),
    hl('div.inner', [
      tools,
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
    ]),
  ]);
}
