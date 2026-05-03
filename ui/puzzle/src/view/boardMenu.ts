import { h } from 'snabbdom';

import { toggle } from 'lib';
import { renderNodesTxt } from 'lib/game/nodePGN';
import * as licon from 'lib/licon';
import { boardMenu as menuDropdown, boolPrefXhrToggle, hl } from 'lib/view';

import type PuzzleCtrl from '../ctrl';

export default function (ctrl: PuzzleCtrl) {
  return menuDropdown(ctrl.redraw, ctrl.menu, menu => [
    h('section', [
      menu.flip(i18n.site.flipBoard, ctrl.flipped(), () => {
        ctrl.flip();
        ctrl.menu.toggle();
      }),
    ]),
    h('section', [
      menu.zenMode(true),
      menu.blindfold(
        toggle(ctrl.blindfold(), v => ctrl.blindfold(v)),
        true,
      ),
      menu.voiceInput(boolPrefXhrToggle('voice', !!ctrl.voiceMove), true),
      menu.keyboardInput(boolPrefXhrToggle('keyboardMove', !!ctrl.keyboardMove), true),
    ]),
    studyButton(ctrl),
    h('section.board-menu__links', [
      h(
        'a.text',
        { attrs: { target: '_blank', href: '/account/preferences/display', 'data-icon': licon.Gear } },
        i18n.preferences.display,
      ),
    ]),
  ]);
}

const hiddenInput = (name: string, value: string) => h('input', { attrs: { type: 'hidden', name, value } });

function renderPgnInput(ctrl: PuzzleCtrl): string {
  const puzURL = `${location.origin}/training/${ctrl.data.puzzle.id}`;
  const tags = [
    ['Site', puzURL],
    ['FEN', ctrl.initialNode.fen],
  ]
    .map(([k, v]) => `[${k} "${v}"]\n`)
    .join('');
  const linkPreamble = ` {${puzURL}} `;
  return tags + linkPreamble + renderNodesTxt(ctrl.initialNode, true);
}

const studyButton = (ctrl: PuzzleCtrl) =>
  ctrl.mode === 'play'
    ? undefined
    : h(
        'section.board-menu__study',
        hl('form', { attrs: { action: '/study/as', method: 'post', target: '_blank' } }, [
          hiddenInput('pgn', renderPgnInput(ctrl)),
          hiddenInput('fen', ctrl.initialNode.fen),
          hiddenInput('orientation', ctrl.pov),
          hiddenInput('mode', 'gamebook'),
          hl(
            'button.button.text',
            { attrs: { type: 'submit', 'data-icon': licon.StudyBoard } },
            i18n.site.toStudy,
          ),
        ]),
      );
