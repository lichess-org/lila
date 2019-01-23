import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode'
import sanWriter from './sanWriter';
import RoundController from '../ctrl';
import { bind } from '../util';
import { renderClock } from '../clock/clockView';
import { renderInner as tableInner } from '../view/table';
import { render as renderGround } from '../ground';
import renderCorresClock from '../corresClock/corresClockView';
import { renderResult } from '../view/replay';
import { plyStep } from '../round';
import { Step, DecodedDests, Position, Redraw } from '../interfaces';
import { Player } from 'game';
import { files } from 'chessground/types';
import { invRanks } from 'chessground/util';

type Sans = {
  [key: string]: Uci;
}

type Notification = {
  text: string;
  date: Date;
}

window.lichess.RoundNVUI = function(redraw: Redraw) {

  let notification: Notification | undefined;

  function notify(msg: string) {
    notification = { text: msg, date: new Date() };
    redraw();
  }

  const settings = {
    moveNotation: makeSetting({
      choices: [
        ['san', 'SAN: Nxf3'],
        ['uci', 'UCI: g1f3'],
        ['literate', 'Literate: knight takes f 3'],
        ['anna', 'Anna: knight takes felix 3'],
        ['full', 'Full: gustav 1 knight takes on felix 3']
      ],
      default: 'anna',
      storage: window.lichess.storage.make('nvui.moveNotation')
    })
  };

  window.lichess.pubsub.on('socket.in.message', line => {
    if (line.u === 'lichess') notify(line.t);
  });

  return {
    render(ctrl: RoundController) {
      const d = ctrl.data,
        step = plyStep(d, ctrl.ply),
        style = settings.moveNotation.get();
      return ctrl.chessground ? h('div.nvui', [
        h('h1', 'Textual representation'),
        h('h2', 'Game info'),
        ...(['white', 'black'].map((color: Color) => h('p', [
          color + ' player: ',
          renderPlayer(ctrl, ctrl.playerByColor(color))
        ]))),
        h('p', `${d.game.rated ? 'Rated' : 'Casual'} ${d.game.perf}`),
        d.clock ? h('p', `Clock: ${d.clock.initial / 60} + ${d.clock.increment}`) : null,
        h('h2', 'Moves'),
        h('p.pgn', {
          attrs: {
            role : 'log',
            'aria-live': 'off'
          }
        }, movesHtml(d.steps.slice(1), style)),
        h('h2', 'Pieces'),
        h('div.pieces', piecesHtml(ctrl, style)),
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
        }, readSan(step, style)),
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
                  else notify(d.player.color === d.game.player ? `Invalid move: ${input}` : 'Not your turn');
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
        h('h2', 'Board'),
        h('pre.board', tableBoard(ctrl)),
        h('h2', 'Settings'),
        h('label', [
          'Move notation',
          renderSetting(settings.moveNotation, ctrl.redraw)
        ]),
        h('div.notify', {
          attrs: {
            'aria-live': 'assertive',
            'aria-atomic' : true
          }
        }, (notification && notification.date.getTime() > (Date.now() - 1000 * 3)) ? notification.text : '')
      ]) : renderGround(ctrl);
    }
  };
}

function renderSetting(setting: any, redraw: Redraw) {
  const v = setting.get();
  return h('select', {
    hook: bind('change', e => {
      setting.set((e.target as HTMLSelectElement).value);
      redraw();
    })
  }, setting.choices.map((choice: [string, string]) => {
    const [key, name] = choice;
    return h('option', {
      attrs: {
        value: key,
        selected: key === v
      }
    }, name)
  }));
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

function movesHtml(steps: Step[], style: any) {
  const res: Array<string | VNode> = [];
  steps.forEach(s => {
    res.push(readSan(s, style) + ', ');
    if (s.ply % 2 === 0) res.push(h('br'));
  });
  return res;
}

function piecesHtml(ctrl: RoundController, style: string): VNode {
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
      h('h3', `${color} pieces`),
      ...lists.map((l: any) =>
        `${l[0]}: ${l.slice(1).map((k: string) => annaKey(k, style)).join(', ')}`
      ).join(', ')
    ]);
  }));
}

const letters = { pawn: 'p', rook: 'r', knight: 'n', bishop: 'b', queen: 'q', king: 'k' };

function tableBoard(ctrl: RoundController): string {
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

const roles: { [letter: string]: string } = { P: 'pawn', R: 'rook', N: 'knight', B: 'bishop', Q: 'queen', K: 'king' };

function readSan(s: Step, style: string) {
  if (!s.san) return '';
  const has = window.lichess.fp.contains;
  let move: string;
  if (has(s.san, 'O-O-O')) move = 'long castling';
  else if (has(s.san, 'O-O')) move = 'short castling';
  else if (style === 'san') move = s.san.replace(/[\+#]/, '');
  else if (style === 'uci') move = s.uci;
  else {
    if (style === 'literate' || style == 'anna') move = s.san.replace(/[\+#]/, '').split('').map(c => {
      const code = c.charCodeAt(0);
      if (code > 48 && code < 58) return c;
      if (c == 'x') return 'takes';
      if (c == '+') return 'check';
      if (c == '#') return 'checkmate';
      if (c == '=') return 'promotion';
      if (c.toUpperCase() === c) return roles[c];
      if (style === 'anna' && anna[c]) return anna[c];
      return c;
    }).join(' ');
    else {
      const role = roles[s.san[0]] || 'pawn';
      const orig = annaKey(s.uci.slice(0, 2), style);
      const dest = annaKey(s.uci.slice(2, 4), style);
      const goes = has(s.san, 'x') ? 'takes on' : 'moves to';
      move = `${orig} ${role} ${goes} ${dest}`;
      const prom = s.uci[4];
      if (prom) move += ' promotes to ' + roles[prom.toUpperCase()];
    }
  }
  if (has(s.san, '+')) move += ' check';
  if (has(s.san, '#')) move += ' checkmate';
  return move;
}

const anna: { [letter: string]: string } = { a: 'anna', b: 'bella', c: 'cesar', d: 'david', e: 'eva', f: 'felix', g: 'gustav', h: 'hector' };
function annaKey(key: string, style: string): string {
  return (style === 'anna' || style === 'full') ? `${anna[key[0]]} ${key[1]}` : key;
}

function renderPlayer(ctrl: RoundController, player: Player) {
  return player.ai ? ctrl.trans('aiNameLevelAiLevel', 'Stockfish', player.ai) : userHtml(ctrl, player);
}

function userHtml(ctrl: RoundController, player: Player) {
  const d = ctrl.data,
    user = player.user,
    perf = user ? user.perfs[d.game.perf] : null,
    rating = player.rating ? player.rating : (perf && perf.rating),
    rd = player.ratingDiff,
    ratingDiff = rd ? (rd > 0 ? '+' + rd : ( rd < 0 ? '−' + (-rd) : '')) : '';
  return user ? h('span', [
    h('a', {
      attrs: { href: '/@/' + user.username }
    }, user.title ? `${user.title} ${user.username}` : user.username),
    rating ? ` ${rating}` : ``,
    ' ' + ratingDiff,
  ]) : 'Anonymous';
}

function makeSetting(opts: any) {
  return {
    choices: opts.choices,
    get: () => opts.storage.get() || opts.default,
    set: opts.storage.set
  }
}
