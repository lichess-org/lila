import sanWriter from './sanWriter';
import RoundController from '../ctrl';
import { router } from 'game';
import { throttle } from 'common';
import { plyStep } from '../round';
import { DecodedDests } from '../interfaces';

type Sans = {
  [key: string]: Uci;
}

window.lichess.NVUI = function(element: HTMLElement, ctrl: RoundController) {

  let $form: JQuery;

  const reload = (first: boolean = false) => {
    $.ajax({
      url: (ctrl.data.player.spectator ?
        router.game(ctrl.data, ctrl.data.player.color) :
        router.player(ctrl.data)
      ) + '/text',
      success(html) {
        if (first) {
          $(element).html(html);
          $form = $(element).find('form').submit(function() {
            const input = $form.find('.move').val();
            const sans: Sans = sanWriter(
              plyStep(ctrl.data, ctrl.ply).fen,
              destsToUcis(ctrl.chessground.state.movable.dests!)
            ) as Sans;
            const uci = sanToUci(input, sans) || input;
            ctrl.socket.send("move", {
              from: uci.substr(0, 2),
              to: uci.substr(2, 2),
              promotion: uci.substr(4, 1)
            }, {
              ackable: true
            });
            return false;
          });
        }
        else ['pgn', 'fen', 'status'].forEach(r => {
          $(element).find('.' + r).html($(html).find('.' + r).html());
        });
        $form.find('.move').val('').focus();
      }
    });
  }

  reload(true);

  return {
    reload: throttle(1000, reload)
  };

}

function destsToUcis(dests: DecodedDests) {
  const ucis: string[] = [];
  Object.keys(dests).forEach(function(orig) {
    dests[orig].forEach(function(dest) {
      ucis.push(orig + dest);
    });
  });
  return ucis;
}

function sanToUci(san: string, sans: Sans): Uci | undefined {
  if (san in sans) return sans[san];
  const lowered = san.toLowerCase();
  for (let i in sans)
    if (i.toLowerCase() === lowered) return sans[i];
  return;
}
