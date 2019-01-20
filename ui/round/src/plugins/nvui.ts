import sanWriter from './sanWriter';
import RoundController from '../ctrl';
import { router } from 'game';
import { throttle } from 'common';
import { plyStep } from '../round';
import { DecodedDests } from '../interfaces';
import { pos2key } from 'draughtsground/util';
import * as xhr from '../xhr';

type Sans = {
  [key: string]: Uci;
}

window.lidraughts.NVUI = function(element: HTMLElement, ctrl: RoundController) {

  let $form: JQuery;

  const currentFen = () => plyStep(ctrl.data, ctrl.ply).fen

  const reload = (first: boolean = false) => {
    $.ajax({
      url: (ctrl.data.player.spectator ?
        router.game(ctrl.data, ctrl.data.player.color) :
        router.player(ctrl.data)
      ) + '/nvui',
      headers: first ? {} : xhr.headers,
      success(res) {
        if (first) {
          $(element).html(res);
          $form = $(element).find('form').submit(function() {
            const input = $form.find('.move').val();
            const legalUcis = destsToUcis(ctrl.draughtsground.state.movable.dests!);
            const sans: Sans = sanWriter(currentFen(), legalUcis, ctrl.draughtsground.state.movable.captLen) as Sans;
            const uci = sanToUci(input, sans) || input;
            if (legalUcis.indexOf(uci.toLowerCase()) >= 0) ctrl.socket.send("move", {
              from: uci.substr(0, 2),
              to: uci.substr(2, 2),
            }, { ackable: true });
            else {
              $(element).find('.notify').text('Invalid move');
            }
            $form.find('.move').val('');
            return false;
          });
          $form.find('.move').val('').focus();
        }
        else {
          $(element).find('.notify').text('');
          ['pdn', 'fen', 'status', 'lastMove'].forEach(r => {
            if ($(element).find('.' + r).text() != res[r]) {
              $(element).find('.' + r).text(res[r]);
            }
          });
        }
        $(element).siblings('.board').find('pre').text(textBoard(ctrl));
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

/*
      1     2     3     4     5
   -  M  -  M  -  M  -  M  -  M  
6  M  -  M  -  M  -  M  -  M  -  
   -  M  -  M  -  M  -  M  -  M  
16 M  -  M  -  M  -  M  -  M  -  
   -  +  -  +  -  +  -  +  -  +
26 +  -  +  -  +  -  +  -  +  -
   -  m  -  m  -  m  -  m  -  m  
36 m  -  m  -  m  -  m  -  m  -  
   -  m  -  m  -  m  -  m  -  m  
46 m  -  m  -  m  -  m  -  m  -  
   46    47    48    49    50
 */
const filesTop = [' ', '1', ' ', '2', ' ', '3', ' ', '4', ' ', '5'],
      filesBottom = ['46', '', '47', '', '48', '', '49', '', '50'];
const ranks = ['  ', ' 6', '  ', '16', '  ', '26', '  ', '36', '  ', '46'],
      ranksInv = [' 5', '  ', '15', '  ', '25', '  ', '35', '  ', '45', '  '];
const letters = { man: 'm', king: 'k', ghostman: 'x', ghostking: 'x' };

function textBoard(ctrl: RoundController) {
  const pieces = ctrl.draughtsground.state.pieces, white = ctrl.data.player.color === 'white';
  const board = [white ? ['  ', ...filesTop] : [...filesTop, '  ']];
  for(let y = 1; y <= 10; y++) {
    let line = [];
    for(let x = 0; x < 10; x++) {
      const piece = (x % 2 !== y % 2) ? undefined : pieces[pos2key([(x - y % 2) / 2 + 1, y])];
      if (piece) {
        const letter = letters[piece.role];
        line.push(piece.color === 'white' ? letter.toUpperCase() : letter);
      } else line.push((x % 2 !== y % 2) ? '-' : '+');
    }
    board.push(white ? ['' + ranks[y - 1], ...line] : [...line, '' + ranksInv[y - 1]]);
  }
  board.push(white ? ['  ', ...filesBottom] : [...filesBottom, ' ', '  ']);
  if (!white) {
    board.reverse();
    board.forEach(r => r.reverse());
  }
  return board.map(line => line.join(' ')).join('\n');
}
