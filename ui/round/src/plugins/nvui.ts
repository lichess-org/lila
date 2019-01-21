import { h } from 'snabbdom'
import sanWriter from './sanWriter';
import RoundController from '../ctrl';
import { renderClock } from '../clock/clockView';
import { renderInner as tableInner } from '../view/table';
import renderCorresClock from '../corresClock/corresClockView';
import { userHtml } from '../view/user';
import { plyStep } from '../round';
import { DecodedDests, Position } from '../interfaces';
import { files } from 'chessground/types';
import { invRanks } from 'chessground/util';
import { view as gameView } from 'game';

type Sans = {
  [key: string]: Uci;
}

window.lichess.RoundNVUI = function() {

  return {
    render(ctrl: RoundController) {
      const d = ctrl.data,
        step = plyStep(d, ctrl.ply);
      return h('div.nvui', [
        h('h1', 'Textual representation'),
        h('dl', [
          ...(ctrl.isPlaying() ? [
            h('dt', 'Your color'),
            h('dd', d.player.color),
            h('dt', 'Opponent'),
            h('dd', userHtml(ctrl, d.player))
          ] : [
            h('dt', 'White player'),
            h('dd', userHtml(ctrl, ctrl.playerByColor('white'))),
            h('dt', 'Black player'),
            h('dd', userHtml(ctrl, ctrl.playerByColor('black')))
          ]),
          h('dt', 'PGN'),
          h('dd.pgn', {
            attrs: {
              role : 'log',
              'aria-live': 'off'
            }
          }, d.steps.slice(1).map(s => h('span', s.san))),
          h('dt', 'FEN'),
          h('dd.fen', step.fen),
          h('dt', 'Game status'),
          h('dd.status', {
            attrs: {
              role : 'status',
              'aria-live' : 'assertive',
              'aria-atomic' : true
            }
          }, ctrl.data.game.status.name === 'started' ? 'Playing' : gameView.status(ctrl)),
          h('dt', 'Last move'),
          h('dd.lastMove', {
            attrs: {
              'aria-live' : 'assertive',
              'aria-atomic' : true
            }
          }, step.san),
          ctrl.isPlaying() ? h('form', {
            hook: {
              insert(vnode) {
                const el = vnode.elm as HTMLFormElement;
                const d = ctrl.data;
                const $form = $(el).submit(function() {
                  const input = $form.find('.move').val();
                  const legalUcis = destsToUcis(ctrl.chessground.state.movable.dests!);
                  const sans: Sans = sanWriter(plyStep(d, ctrl.ply).fen, legalUcis) as Sans;
                  const uci = sanToUci(input, sans) || input;
                  if (legalUcis.indexOf(uci.toLowerCase()) >= 0) ctrl.socket.send("move", {
                    from: uci.substr(0, 2),
                    to: uci.substr(2, 2),
                    promotion: uci.substr(4, 1)
                  }, { ackable: true });
                  else {
                    $(el).siblings('.notify').text('Invalid move');
                  }
                  $form.find('.move').val('');
                  return false;
                });
                $form.find('.move').val('').focus();
              }
            }
          }, [
            h('label', [
              d.player.color === d.game.player ? 'Your move' : 'Waiting',
              h('input.move', {
                attrs: {
                  name : 'move',
                  'type' : 'text',
                  autocomplete : 'off'
                }
              })
            ])
          ]) : null,
          h('dt', 'Your clock'),
          h('dd.botc', anyClock(ctrl, 'bottom')),
          h('dt', 'Opponent clock'),
          h('dd.topc', anyClock(ctrl, 'top')),
          h('dt', 'Actions'),
          h('dd.actions', tableInner(ctrl)),
          h('dt', 'Board table'),
          h('dd.board', tableBoard(ctrl)),
          h('div.notify', {
            'aria-live': "assertive",
            'aria-atomic' : true
          })
        ])
      ]);
    }
  };
}

function anyClock(ctrl: RoundController, position: Position) {
  const d = ctrl.data, player = ctrl.playerAt(position);
  return (ctrl.clock && renderClock(ctrl, player, position)) || (
    d.correspondence && renderCorresClock(ctrl.corresClock!, ctrl.trans, player.color, position, d.game.player)
  ) || undefined;
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

function tableBoard(ctrl: RoundController) {
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
  return h('table', [
    h('thead', h('tr', board[0].map(x => h('th', x)))),
    h('tbody', board.slice(1, 9).map(row =>
      h('tr', [
        h('th', row[0]),
        ...row.slice(1, 9).map(sq => h('td', sq)),
        h('th', row[9])
      ])
    )),
    h('thead', h('tr', board[9].map(x => h('th', x))))
  ]);

  // return board.map(line => line.join(' ')).join('\n');
}
