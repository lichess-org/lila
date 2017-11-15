import { throttle } from 'common';
import { router } from 'game';
import RoundController from './ctrl';

let element: HTMLElement;

export const reload = throttle(1000, (ctrl: RoundController) => {
  $.ajax({
    url: (ctrl.data.player.spectator ?
      router.game(ctrl.data, ctrl.data.player.color) :
      router.player(ctrl.data)
    ) + '/text',
    success(html) {
      $(element).html(html).find('form').submit(function(this: HTMLElement) {
        var text = $(this).find('.move').val();
        var move = {
          from: text.substr(0, 2),
          to: text.substr(2, 2),
          promotion: text.substr(4, 1)
        };
        ctrl.socket.send("move", move, {
          ackable: true
        });
        return false;
      }).find('.move').focus();
    }
  });
});

export function init(el: HTMLElement, ctrl: RoundController) {
  element = el;
  reload(ctrl);
}
