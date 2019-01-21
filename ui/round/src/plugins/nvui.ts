import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode'
import sanWriter from './sanWriter';
import RoundController from '../ctrl';
import { renderClock } from '../clock/clockView';
import { renderInner as tableInner } from '../view/table';
import { render as renderGround } from '../ground';
import renderCorresClock from '../corresClock/corresClockView';
import { userHtml } from '../view/user';
import { renderResult } from '../view/replay';
import { plyStep } from '../round';
import { Step, DecodedDests, Position } from '../interfaces';
import { files } from 'chessground/types';
import { invRanks } from 'chessground/util';

type Sans = {
  [key: string]: Uci;
}

type Notification = {
  text: string;
  date: Date;
}

window.lichess.RoundNVUI = function() {

  let notification: Notification | undefined;

  return {
    render(ctrl: RoundController) {
      const d = ctrl.data,
        step = plyStep(d, ctrl.ply);
      return ctrl.chessground ? h('div.nvui', [
        h('h1', 'Textual representation'),
        h('div', [
          ...(ctrl.isPlaying() ? [
            h('h2', 'Your color: ' + d.player.color),
            h('h2', [
              'Opponent: ',
              userHtml(ctrl, d.player)
            ])
          ] : ['white', 'black'].map((color: Color) => h('h2', [
            color + ' player: ',
            userHtml(ctrl, ctrl.playerByColor(color))
          ]))
          ),
          h('h2', 'Moves'),
          h('p.pgn', {
            attrs: {
              role : 'log',
              'aria-live': 'off'
            }
          }, d.steps.slice(1).map(s => h('span', readSan(s) + ', '))),
          h('h2', 'Pieces'),
          h('div.pieces', piecesHtml(ctrl)),
          // h('h2', 'FEN'),
          // h('p.fen', step.fen),
          h('h2', 'Game status'),
          h('div.status', {
            attrs: {
              role : 'status',
              'aria-live' : 'assertive',
              'aria-atomic' : true
            }
          }, [ctrl.data.game.status.name === 'started' ? 'Playing' : renderResult(ctrl)]),
          h('h2', 'Last move'),
          h('p.lastMove', {
            attrs: {
              'aria-live' : 'assertive',
              'aria-atomic' : true
            }
          }, readSan(step)),
          ...(ctrl.isPlaying() ? [
            h('h2', 'Move form'),
            h('form', {
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
                      notification = {
                        text: d.player.color === d.game.player ? `Invalid move: ${input}` : 'Not your turn',
                        date: new Date()
                      };
                      ctrl.redraw();
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
                    name: 'move',
                    'type': 'text',
                    autocomplete: 'off',
                    autofocus: true
                  }
                })
              ])
            ])
          ]: []),
          h('h2', 'Your clock'),
          h('div.botc', anyClock(ctrl, 'bottom')),
          h('h2', 'Opponent clock'),
          h('div.topc', anyClock(ctrl, 'top')),
          h('h2', 'Actions'),
          h('div.actions', tableInner(ctrl)),
          h('h2', 'Board table'),
          h('div.board', tableBoard(ctrl)),
          h('div.notify', {
            attrs: {
              'aria-live': 'assertive',
              'aria-atomic' : true
            }
          }, (notification && notification.date.getTime() > (Date.now() - 1000 * 3)) ? notification.text : '')
        ])
      ]) : renderGround(ctrl);
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

function piecesHtml(ctrl: RoundController): VNode {
  const pieces = ctrl.chessground.state.pieces;
  return h('div', ['white', 'black'].map(color => {
    const lists: any = [];
    ['king', 'queen', 'rook', 'bishop', 'knight', 'pawn'].forEach(role => {
      const keys = [];
      for (let key in pieces) {
        if (pieces[key]!.color === color && pieces[key]!.role === role) keys.push(key);
      }
      if (keys.length) lists.push([`${role}${keys.length > 1 ? 's' : ''}`, ...keys]);
    });
    return h('div', [
      h('dt', color),
      h('dd', [
        ...lists.map((l: any) => h('span', [
          h('span', l[0]),
          ...l.slice(1).map((k: string) => h('span', annaKey(k)))
        ]))
      ])
    ])
  }));
}

const letters = { pawn: 'p', rook: 'r', knight: 'n', bishop: 'b', queen: 'q', king: 'k' };

function tableBoard(ctrl: RoundController): VNode {
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
}

const roles: { [letter: string]: string } = { P: 'pawn', R: 'rook', N: 'knight', B: 'bishop', Q: 'queen', K: 'king' };

function readSan(s: Step) {
  if (!s.san) return '';
  const has = window.lichess.fp.contains;
  const base = s.san.toLowerCase().replace(/[\+\#x]/g, '');
  let move: string;
  if (base === 'o-o') move = 'Short castling';
  else if (base === 'o-o-o') move = 'Long castling';
  else {
    const role = roles[s.san[0]] || 'pawn';
    const orig = annaKey(s.uci.slice(0, 2));
    const dest = annaKey(s.uci.slice(2, 4));
    const goes = has(s.san, 'x') ? 'takes on' : 'moves to';
    move = `${orig} ${role} ${goes} ${dest}`
    const prom = s.uci[4];
    if (prom) move += ' promotes to ' + roles[prom.toUpperCase()];
  }
  if (has(s.san, '+')) move += ' check';
  if (has(s.san, '#')) move += ' checkmate';
  return move;
}

const anna: { [letter: string]: string } = { a: 'anna', b: 'bella', c: 'cesar', d: 'david', e: 'eva', f: 'felix', g: 'gustav', h: 'hector' };
function annaKey(key: string): string {
  return `${anna[key[0]]} ${key[1]}`;
}
