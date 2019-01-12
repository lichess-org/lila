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

  const reload = () => {
    $.ajax({
      url: (ctrl.data.player.spectator ?
        router.game(ctrl.data, ctrl.data.player.color) :
        router.player(ctrl.data)
      ) + '/text',
      success(html) {
        $(element).html(html).find('form').each(function(this: HTMLFormElement) {
          const $form = $(this).submit(function() {
            let input = $form.find('.move').val();
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
          $form.find('.move').focus();
        });
      }
    });
  }

  reload();

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
