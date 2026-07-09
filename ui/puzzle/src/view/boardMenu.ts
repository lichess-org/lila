import { toggle } from 'lib';
import { renderNodesTxt } from 'lib/game/nodePGN';
import { licon } from 'lib/licon';
import { boardMenu as menuDropdown, boolPrefXhrToggle, hl } from 'lib/view';

import type PuzzleCtrl from '../ctrl';

export default function (ctrl: PuzzleCtrl) {
  return menuDropdown(ctrl.redraw, ctrl.menu, menu => [
    hl('section', [
      menu.flip(i18n.site.flipBoard, ctrl.flipped(), () => {
        ctrl.flip();
        ctrl.menu.toggle();
      }),
    ]),
    hl('section', [
      menu.zenMode(true),
      menu.blindfold(
        toggle(ctrl.blindfold(), v => ctrl.blindfold(v)),
        true,
      ),
      menu.voiceInput(boolPrefXhrToggle('voice', !!ctrl.voiceMove), true),
      menu.keyboardInput(boolPrefXhrToggle('keyboardMove', !!ctrl.keyboardMove), true),
    ]),
    studyButton(ctrl),
    hl('section.board-menu__links', [
      hl(
        'a.text',
        { attrs: { target: '_blank', href: '/account/preferences/display', 'data-icon': licon.Gear } },
        i18n.preferences.display,
      ),
    ]),
  ]);
}

const hiddenInput = (name: string, value: string) => hl('input', { attrs: { type: 'hidden', name, value } });

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

function studyButton(ctrl: PuzzleCtrl) {
  if (ctrl.mode === 'play') return undefined;
  return hl(
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
}
