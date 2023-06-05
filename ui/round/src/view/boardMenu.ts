import { h } from 'snabbdom';
import { snabModal } from 'common/modal';
import * as licon from 'common/licon';
import RoundController from '../ctrl';
import { bind } from '../util';
import { game as gameRoute } from 'game/router';
import { toggle, ToggleSettings } from 'common/controls';
import { dataIcon } from 'common/snabbdom';

export const boardMenu = (ctrl: RoundController) =>
  ctrl.menu()
    ? snabModal({
        class: 'board-menu',
        onClose: () => ctrl.menu(false),
        content: [
          h('section', [
            h(
              'button.button.text',
              {
                class: { active: ctrl.flip },
                attrs: {
                  title: 'Hotkey: f',
                  ...dataIcon(licon.ChasingArrows),
                },
                hook: bind('click', () => {
                  const d = ctrl.data;
                  if (d.tv) location.href = '/tv/' + d.tv.channel + (d.tv.flip ? '' : '?flip=1');
                  else if (d.player.spectator) location.href = gameRoute(d, d.opponent.color);
                  else ctrl.flipNow();
                }),
              },
              ctrl.noarg('flipBoard')
            ),
          ]),
          h('section', [
            ctrlToggle(
              {
                name: 'Zen mode',
                id: 'zen',
                checked: $('body').hasClass('zen'),
                change: () => lichess.pubsub.emit('zen'),
              },
              ctrl
            ),
            ctrlToggle(
              {
                name: 'Voice input',
                id: 'voice',
                checked: ctrl.voiceMoveEnabled(),
                change: ctrl.voiceMoveEnabled,
                cls: 'setting--nag',
              },
              ctrl
            ),
            ctrlToggle(
              {
                name: 'Keyboard input',
                id: 'keyboard',
                checked: ctrl.keyboardMoveEnabled(),
                change: ctrl.keyboardMoveEnabled,
              },
              ctrl
            ),
          ]),
          h('section.board-menu__links', [
            h('a', { attrs: { target: '_blank', href: '/account/preferences/display' } }, 'Game display preferences'),
            h(
              'a',
              { attrs: { target: '_blank', href: '/account/preferences/game-behavior ' } },
              'Game behavior preferences'
            ),
          ]),
        ],
      })
    : undefined;

const ctrlToggle = (t: ToggleSettings, ctrl: RoundController) => toggle(t, ctrl.trans, ctrl.redraw);
