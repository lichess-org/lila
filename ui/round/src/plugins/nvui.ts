import { h } from 'snabbdom'
import sanWriter from './sanWriter';
import RoundController from '../ctrl';
import { renderClock } from '../clock/clockView';
import { renderInner as tableInner } from '../view/table';
import renderCorresClock from '../corresClock/corresClockView';
import { userHtml } from '../view/user';
import { plyStep } from '../round';
import { DecodedDests, Position } from '../interfaces';
import { pos2key } from 'draughtsground/util';
import { view as gameView } from 'game';

type Sans = {
  [key: string]: Uci;
}

window.lidraughts.RoundNVUI = function() {

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
          h('dt', 'PDN'),
          h('dd.pdn', {
            attrs: { role : 'log' }
          }, d.steps.map(s => s.san).join(' ')),
          h('dt', 'FEN'),
          // h('dd.fen')(step.fen),
          h('dt', 'Game status'),
          h('dd.status', {
            attrs: {
              role : 'status',
              'aria.live' : 'assertive',
              'aria.atomic' : true
            }
          }, ctrl.data.game.status.name === 'started' ? 'Playing' : gameView.status(ctrl)),
          h('dt', 'Last move'),
          h('dd.lastMove', {
            attrs: {
              'aria.live' : 'assertive',
              'aria.atomic' : true
            }
          }, step.san),
          ctrl.isPlaying() ? h('form', {
            hook: {
              insert(vnode) {
                const el = vnode.elm as HTMLFormElement;
                const d = ctrl.data;
                const $form = $(el).submit(function() {
                  const input = $form.find('.move').val();
                  const legalUcis = destsToUcis(ctrl.draughtsground.state.movable.dests!);
                  const sans: Sans = sanWriter(plyStep(d, ctrl.ply).fen, legalUcis, ctrl.draughtsground.state.movable.captLen) as Sans;
                  const uci = sanToUci(input, sans) || input;
                  if (legalUcis.indexOf(uci.toLowerCase()) >= 0) ctrl.socket.send("move", {
                    from: uci.substr(0, 2),
                    to: uci.substr(2, 2),
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
              'Your move',
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
          h('dt', 'Board'),
          h('dd', h('pre', textBoard(ctrl))),
          h('div.notify', {
            'aria.live': "assertive",
            'aria.atomic' : true
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
