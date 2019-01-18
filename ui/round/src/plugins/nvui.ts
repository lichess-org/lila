import sanWriter from './sanWriter';
import RoundController from '../ctrl';
import { router } from 'game';
import { throttle } from 'common';
import { plyStep } from '../round';
import { DecodedDests } from '../interfaces';

type Sans = {
  [key: string]: Uci;
}

window.lidraughts.NVUI = function(element: HTMLElement, ctrl: RoundController) {

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
              destsToUcis(ctrl.draughtsground.state.movable.dests!),
              ctrl.draughtsground.state.movable.captLen
            ) as Sans;
            const uci = sanToUci(input, sans) || input;
            ctrl.socket.send("move", {
              from: uci.substr(0, 2),
              to: uci.substr(2, 2)
            }, {
              ackable: true
            });
            return false;
          });
        }
        else ['pdn', 'fen', 'status'].forEach(r => {
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

function sanToUci(san: string, sans: Sans): string | undefined {
  if (san in sans) return sans[san];
  if (san.length === 4 && Object.keys(sans).find(key => sans[key] === san)) return san;
  let lowered = san.toLowerCase().replace('x0', 'x').replace('-0', '-');
  if (lowered.slice(0, 1) === '0') lowered = lowered.slice(1)
  if (lowered in sans) return sans[lowered];
  return undefined
}
