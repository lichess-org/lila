import { throttle } from 'common';
import { router } from 'game';
import * as round from './round';
import RoundController from './ctrl';

let element: HTMLElement;

export const reload = throttle(1000, (ctrl: RoundController) => {
  $.ajax({
    url: (ctrl.data.player.spectator ?
      router.game(ctrl.data, ctrl.data.player.color) :
      router.player(ctrl.data)
    ) + '/text',
    success(html) {
      $(element).html(html).find('form').each(function(this: HTMLFormElement) {
        var $form = $(this);
        var $input = $form.find('.move').focus();
        // const movable = root.chessground.state.movable;
        window.lichess.keyboardMove({
          input: $input[0],
          setFocus: $.noop,
          select: function() {
            console.log('select')
          },
          hasSelected: function () { return false; },
          confirmMove: function() {
            console.log('confirm')
          },
          san: function(orig: string, dest: string) {
            console.log(orig, dest);
            ctrl.socket.send("move", {
              from: orig,
              to: dest
            }, {
              ackable: true
            });
          }
        })(round.plyStep(ctrl.data, ctrl.ply).fen, ctrl.chessground.state.movable.dests);
        $form.submit(function() {
          return false;
        });
      });
      console.log($(element).find('form'));
    }
  });
});

export function init(el: HTMLElement, ctrl: RoundController) {
  element = el;
  reload(ctrl);
}
