import { isEmpty } from 'common';
import * as licon from 'common/licon';
import modal from 'common/modal';
import { bind, dataIcon, MaybeVNodes } from 'common/snabbdom';
import { h, VNode } from 'snabbdom';
import { AutoplayDelay } from '../autoplay';
import { toggle } from 'common/controls';
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
          'data-icon': licon.StudyBoard,
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
            'data-icon': licon.StudyBoard,
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
    canContinue = !ctrl.ongoing && !ctrl.embed && d.game.variant.key === 'standard';

  const tools: MaybeVNodes = [
    h('div.action-menu__tools', [
      h(
        'a.button.button-empty',
        {
          hook: bind('click', ctrl.flip),
          attrs: {
            'data-icon': licon.ChasingArrows,
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
                'data-icon': licon.Pencil,
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
              attrs: dataIcon(licon.Swords),
            },
            noarg('continueFromHere')
          )
        : null,
      studyButton(ctrl),
    ]),
  ];

  const notationConfig = [
    toggle(
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
      ctrl.trans,
      ctrl.redraw
    ),
  ];

  return h('div.action-menu', [
    ...tools,
    ...notationConfig,
    ...(ctrl.mainline.length > 4 ? [h('h2', noarg('replayMode')), autoplayButtons(ctrl)] : []),
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
