import sanWriter from './sanWriter';
import RoundController from '../ctrl';
import { router } from 'game';
import { throttle } from 'common';
import { plyStep } from '../round';
import { DecodedDests } from '../interfaces';
import { files } from 'chessground/types';
import { invRanks } from 'chessground/util';
import * as xhr from '../xhr';

type Sans = {
  [key: string]: Uci;
}

window.lichess.NVUI = function(element: HTMLElement, ctrl: RoundController) {

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
            const legalUcis = destsToUcis(ctrl.chessground.state.movable.dests!);
            const sans: Sans = sanWriter(currentFen(), legalUcis) as Sans;
            const uci = sanToUci(input, sans) || input;
            if (legalUcis.indexOf(uci.toLowerCase()) >= 0) ctrl.socket.send("move", {
              from: uci.substr(0, 2),
              to: uci.substr(2, 2),
              promotion: uci.substr(4, 1)
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
          ['pgn', 'fen', 'status', 'lastMove'].forEach(r => {
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

function sanToUci(san: string, sans: Sans): Uci | undefined {
  if (san in sans) return sans[san];
  const lowered = san.toLowerCase();
  for (let i in sans)
    if (i.toLowerCase() === lowered) return sans[i];
  return;
}

/*
   H  G  F  E  D  C  B  A
1  R  N  B  K  Q  B  N  R   1
2  P  P  P  P  P  P  +  P   2
3  -  +  -  +  -  +  -  +   3
4  +  -  +  -  +  -  +  -   4
5  -  +  -  p  -  +  P  +   5
6  +  -  +  -  +  -  +  -   6
7  p  p  p  +  p  p  p  p   7
8  r  n  b  k  q  b  n  r   8
   H  G  F  E  D  C  B  A
 */
const letters = { pawn: 'p', rook: 'r', knight: 'n', bishop: 'b', queen: 'q', king: 'k' };

function textBoard(ctrl: RoundController) {
  const pieces = ctrl.chessground.state.pieces;
  const board = [[' ', ...files, ' ']];
  for(let rank of invRanks) {
    let line = [];
    for(let file of files) {
      let key = file + rank;
      const piece = pieces[key];
      if (piece) {
        const letter = letters[piece.role];
        line.push(piece.color === 'white' ? letter.toUpperCase() : letter);
      } else line.push((file.charCodeAt(0) + rank) % 2 ? '-' : '+');
    }
    board.push(['' + rank, ...line, '' + rank]);
  }
  board.push([' ', ...files, ' ']);
  if (ctrl.data.player.color === 'black') {
    board.reverse();
    board.forEach(r => r.reverse());
  }
  return board.map(line => line.join(' ')).join('\n');
}
