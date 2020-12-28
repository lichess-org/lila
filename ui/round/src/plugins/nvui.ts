import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode'
import { sanWriter, SanToUci } from './sanWriter';
import RoundController from '../ctrl';
import { renderClock } from '../clock/clockView';
import { renderTableWatch, renderTablePlay, renderTableEnd } from '../view/table';
import { makeConfig as makeCgConfig } from '../ground';
import { Chessground } from 'chessground';
import renderCorresClock from '../corresClock/corresClockView';
import { renderResult } from '../view/replay';
import { plyStep } from '../round';
import { onInsert } from '../util';
import { Step, Dests, Position, Redraw } from '../interfaces';
import * as game from 'game';
import { renderSan, renderPieces, renderBoard, styleSetting, pieceSetting, prefixSetting, positionSetting, boardSetting } from 'nvui/chess';
import { renderSetting } from 'nvui/setting';
import { boardCommandsHandler, possibleMovesHandler, lastCapturedCommandHandler, selectionHandler, arrowKeyHandler, positionJumpHandler, pieceJumpingHandler } from 'nvui/chess';
import { Notify } from 'nvui/notify';
import { castlingFlavours, supportedVariant, Style } from 'nvui/chess';
import { commands } from 'nvui/command';
import { throttled } from '../sound';

const selectSound = throttled('select');
const wrapSound = throttled('wrapAround');
const borderSound = throttled('outOfBound');
const errorSound = throttled('error');

lichess.RoundNVUI = function(redraw: Redraw) {

  const notify = new Notify(redraw),
    moveStyle = styleSetting(),
    prefixStyle = prefixSetting(),
    pieceStyle = pieceSetting(),
    positionStyle = positionSetting(),
    boardStyle = boardSetting();

  lichess.pubsub.on('socket.in.message', line => {
    if (line.u === 'lichess') notify.set(line.t);
  });
  lichess.pubsub.on('round.suggestion', notify.set);

  return {
    render(ctrl: RoundController): VNode {
      const d = ctrl.data, step = plyStep(d, ctrl.ply), style = moveStyle.get(),
        variantNope = !supportedVariant(d.game.variant.key) && 'Sorry, this variant is not supported in blind mode.';
      if (!ctrl.chessground) {
        ctrl.setChessground(Chessground(document.createElement("div"), {
          ...makeCgConfig(ctrl),
          animation: { enabled: false },
          drawable: { enabled: false },
          coordinates: false
        }));
        if (variantNope) setTimeout(() => notify.set(variantNope), 3000);
      }
      return h('div.nvui', {
        hook: onInsert(_ => setTimeout(() => notify.set(gameText(ctrl)), 2000))
      }, [
        h('h1', gameText(ctrl)),
        h('h2', 'Game info'),
        ...(['white', 'black'].map((color: Color) => h('p', [
          color + ' player: ',
          playerHtml(ctrl, ctrl.playerByColor(color))
        ]))),
        h('p', `${d.game.rated ? 'Rated' : 'Casual'} ${d.game.perf}`),
        d.clock ? h('p', `Clock: ${d.clock.initial / 60} + ${d.clock.increment}`) : null,
        h('h2', 'Moves'),
        h('p.moves', {
          attrs: {
            role: 'log',
            'aria-live': 'off'
          }
        }, renderMoves(d.steps.slice(1), style)),
        h('h2', 'Pieces'),
        h('div.pieces', renderPieces(ctrl.chessground.state.pieces, style)),
        h('h2', 'Game status'),
        h('div.status', {
          attrs: {
            role: 'status',
            'aria-live': 'assertive',
            'aria-atomic': true
          }
        }, [ctrl.data.game.status.name === 'started' ? 'Playing' : renderResult(ctrl)]),
        h('h2', 'Last move'),
        h('p.lastMove', {
          attrs: {
            'aria-live': 'assertive',
            'aria-atomic': true
          }
        }, renderSan(step.san, step.uci, style)),
        ...(ctrl.isPlaying() ? [
          h('h2', 'Move form'),
          h('form', {
            hook: onInsert(el => {
              const $form = $(el as HTMLFormElement),
                $input = $form.find('.move').val('');
              $input[0]!.focus();
              $form.on('submit', onSubmit(ctrl, notify.set, moveStyle.get, $input));
            })
          }, [
            h('label', [
              d.player.color === d.game.player ? 'Your move' : 'Waiting',
              h('input.move.mousetrap', {
                attrs: {
                  name: 'move',
                  'type': 'text',
                  autocomplete: 'off',
                  autofocus: true,
                  disabled: !!variantNope,
                  title: variantNope
                }
              })
            ])
          ])
        ] : []),
        h('h2', 'Your clock'),
        h('div.botc', anyClock(ctrl, 'bottom')),
        h('h2', 'Opponent clock'),
        h('div.topc', anyClock(ctrl, 'top')),
        notify.render(),
        h('h2', 'Actions'),
        ...(ctrl.data.player.spectator ? renderTableWatch(ctrl) : (
          game.playable(ctrl.data) ? renderTablePlay(ctrl) : renderTableEnd(ctrl)
        )),
        h('h2', 'Board'),
        h('div.board', {
          hook: onInsert(el => {
            const $board = $(el as HTMLElement);
            $board.on('keypress', boardCommandsHandler());
            $board.on('keypress', () => console.log(ctrl));
            // NOTE: This is the only line different from analysisBoardListenerSetup
            $board.on('keypress', lastCapturedCommandHandler(() => ctrl.data.steps.map(step => step.fen), pieceStyle.get(), prefixStyle.get()));
            const $buttons = $board.find('button');
            $buttons.on('click', selectionHandler(ctrl.data.opponent.color, selectSound));
            $buttons.on('keydown', arrowKeyHandler(ctrl.data.player.color, borderSound));
            $buttons.on('keypress', possibleMovesHandler(() => ctrl.data.possibleMoves, () => ctrl.chessground.state.pieces));
            $buttons.on('keypress', positionJumpHandler());
            $buttons.on('keypress', pieceJumpingHandler(wrapSound, errorSound));
          })}, renderBoard(ctrl.chessground.state.pieces, ctrl.data.player.color, pieceStyle.get(), prefixStyle.get(), positionStyle.get(), boardStyle.get())),
        h('div.boardstatus', {
          attrs: {
            'aria-live': 'polite',
            'aria-atomic': true
          }
        }, ''),
       // h('p', takes(ctrl.data.steps.map(data => data.fen))),
        h('h2', 'Settings'),
        h('label', [
          'Move notation',
          renderSetting(moveStyle, ctrl.redraw)
        ]),
        h('h3', 'Board Settings'),
        h('label', [
          'Piece style',
          renderSetting(pieceStyle, ctrl.redraw)
        ]),
        h('label', [
          'Piece prefix style',
          renderSetting(prefixStyle, ctrl.redraw)
        ]),
        h('label', [
          'Show position',
          renderSetting(positionStyle, ctrl.redraw)
        ]),
        h('label', [
          'Board layout',
          renderSetting(boardStyle, ctrl.redraw)
        ]),
        h('h2', 'Commands'),
        h('p', [
          'Type these commands in the move input.', h('br'),
          'c: Read clocks.', h('br'),
          'l: Read last move.', h('br'),
          'o: Read name and rating of the opponent.', h('br'),
          commands.piece.help, h('br'),
          commands.scan.help, h('br'),
          'abort: Abort game.', h('br'),
          'resign: Resign game.', h('br'),
          'draw: Offer or accept draw.', h('br'),
          'takeback: Offer or accept take back.', h('br')
        ]),
        h('h2', 'Board Mode commands'),
        h('p', [
          'Use these commands when focused on the board itself.', h('br'),
          'o: announce current position.', h('br'),
          'c: announce last move\'s captured piece.', h('br'),
          'l: display last move.', h('br'),
          't: display clocks.', h('br'),
          'arrow keys: move left, right, up or down.', h('br'),
          'kqrbnp/KQRBNP: move forward/backward to a piece.', h('br'),
          '1-8: move to rank 1-8.', h('br'),
          'Shift+1-8: move to file a-h.', h('br'),
          '', h('br')
        ]),
        h('h2', 'Promotion'),
        h('p', [
          'Standard PGN notation selects the piece to promote to. Example: a8=n promotes to a knight.',
          h('br'),
          'Omission results in promotion to queen'
        ])
      ]);
    }
  };
}

const promotionRegex = /^([a-h]x?)?[a-h](1|8)=\w$/;

function onSubmit(ctrl: RoundController, notify: (txt: string) => void, style: () => Style, $input: Cash) {
  return () => {
    let input = castlingFlavours(($input.val() as string).trim());
    if (isShortCommand(input)) input = '/' + input;
    if (input[0] === '/') onCommand(ctrl, notify, input.slice(1), style());
    else {
      const d = ctrl.data,
        legalUcis = destsToUcis(ctrl.chessground.state.movable.dests!),
        legalSans: SanToUci = sanWriter(plyStep(d, ctrl.ply).fen, legalUcis) as SanToUci;
      let uci = sanToUci(input, legalSans) || input,
        promotion = '';

      if (input.match(promotionRegex)) {
        uci = sanToUci(input.slice(0, -2), legalSans) || input;
        promotion = input.slice(-1).toLowerCase();
      }

      if (legalUcis.includes(uci.toLowerCase())) ctrl.socket.send("move", {
        u: uci + promotion
      }, { ackable: true });
      else notify(d.player.color === d.game.player ? `Invalid move: ${input}` : 'Not your turn');
    }
    $input.val('');
    return false;
  };
}

const shortCommands = ['c', 'clock', 'l', 'last', 'abort', 'resign', 'draw', 'takeback', 'p', 's', 'o', 'opponent'];

function isShortCommand(input: string): boolean {
  return shortCommands.includes(input.split(' ')[0].toLowerCase());
}

function onCommand(ctrl: RoundController, notify: (txt: string) => void, c: string, style: Style) {
  const lowered = c.toLowerCase();
  if (lowered == 'c' || lowered == 'clock') notify($('.nvui .botc').text() + ', ' + $('.nvui .topc').text());
  else if (lowered == 'l' || lowered == 'last') notify($('.lastMove').text());
  else if (lowered == 'abort') $('.nvui button.abort').trigger('click');
  else if (lowered == 'resign') $('.nvui button.resign-confirm').trigger('click');
  else if (lowered == 'draw') $('.nvui button.draw-yes').trigger('click');
  else if (lowered == 'takeback') $('.nvui button.takeback-yes').trigger('click');
  else if (lowered == 'o' || lowered == 'opponent') notify(playerText(ctrl, ctrl.data.opponent));
  else {
    const pieces = ctrl.chessground.state.pieces;
    notify(
      commands.piece.apply(c, pieces, style) ||
      commands.scan.apply(c, pieces, style) ||
      `Invalid command: ${c}`
    );
  }
}

function anyClock(ctrl: RoundController, position: Position) {
  const d = ctrl.data, player = ctrl.playerAt(position);
  return (ctrl.clock && renderClock(ctrl, player, position)) || (
    d.correspondence && renderCorresClock(ctrl.corresClock!, ctrl.trans, player.color, position, d.game.player)
  ) || undefined;
}

function destsToUcis(dests: Dests) {
  const ucis: string[] = [];
  for (const [orig, d] of dests) {
    if (d) d.forEach(function(dest) {
      ucis.push(orig + dest);
    });
  }
  return ucis;
}

function sanToUci(san: string, legalSans: SanToUci): Uci | undefined {
  if (san in legalSans) return legalSans[san];
  const lowered = san.toLowerCase();
  for (let i in legalSans)
    if (i.toLowerCase() === lowered) return legalSans[i];
  return;
}

function renderMoves(steps: Step[], style: Style) {
  const res: Array<string | VNode> = [];
  steps.forEach(s => {
    if (s.ply & 1) res.push((Math.ceil(s.ply / 2)) + ' ');
    res.push(renderSan(s.san, s.uci, style) + ', ');
    if (s.ply % 2 === 0) res.push(h('br'));
  });
  return res;
}

function renderAi(ctrl: RoundController, level: number): string {
  return ctrl.trans('aiNameLevelAiLevel', 'Stockfish', level);
}

function playerHtml(ctrl: RoundController, player: game.Player) {
  if (player.ai) return renderAi(ctrl, player.ai);
  const d = ctrl.data,
    user = player.user,
    perf = user ? user.perfs[d.game.perf] : null,
    rating = player.rating ? player.rating : (perf && perf.rating),
    rd = player.ratingDiff,
    ratingDiff = rd ? (rd > 0 ? '+' + rd : (rd < 0 ? '−' + (-rd) : '')) : '';
  return user ? h('span', [
    h('a', {
      attrs: { href: '/@/' + user.username }
    }, user.title ? `${user.title} ${user.username}` : user.username),
    rating ? ` ${rating}` : ``,
    ' ' + ratingDiff,
  ]) : 'Anonymous';
}

function playerText(ctrl: RoundController, player: game.Player) {
  if (player.ai) return renderAi(ctrl, player.ai);
  const d = ctrl.data,
    user = player.user,
    perf = user ? user.perfs[d.game.perf] : null,
    rating = player.rating ? player.rating : (perf && perf.rating);
  if (!user) return 'Anonymous';
  return `${user.title || ''} ${user.username} rated ${rating || 'unknown'}`;
}

function gameText(ctrl: RoundController) {
  const d = ctrl.data;
  return [
    d.game.status.name == 'started' ? (
      ctrl.isPlaying() ? 'You play the ' + ctrl.data.player.color + ' pieces.' : 'Spectating.'
    ) : 'Game over.',
    d.game.rated ? 'Rated' : 'Casual',
    d.clock ? `${d.clock.initial / 60} + ${d.clock.increment}` : '',
    d.game.perf,
    'game versus',
    playerText(ctrl, ctrl.data.opponent)
  ].join(' ');
}
