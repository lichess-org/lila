import { h, VNode } from 'snabbdom';
import RoundController from '../ctrl';
import { renderClock } from '../clock/clockView';
import { renderTableWatch, renderTablePlay, renderTableEnd } from '../view/table';
import { makeConfig as makeCgConfig } from '../ground';
import { Chessground } from 'chessground';
import renderCorresClock from '../corresClock/corresClockView';
import { renderResult } from '../view/replay';
import { plyStep } from '../round';
import { onInsert } from '../util';
import { Step, Position, Redraw, NvuiPlugin } from '../interfaces';
import * as game from 'game';
import {
  renderSan,
  renderPieces,
  renderBoard,
  styleSetting,
  pieceSetting,
  prefixSetting,
  positionSetting,
  boardSetting,
  boardCommandsHandler,
  possibleMovesHandler,
  lastCapturedCommandHandler,
  selectionHandler,
  arrowKeyHandler,
  positionJumpHandler,
  pieceJumpingHandler,
  castlingFlavours,
  supportedVariant,
  Style,
  inputToLegalUci,
} from 'nvui/chess';
import { renderSetting } from 'nvui/setting';
import { Notify } from 'nvui/notify';
import { commands } from 'nvui/command';
import { throttled } from '../sound';

const selectSound = throttled('select');
const wrapSound = throttled('wrapAround');
const borderSound = throttled('outOfBound');
const errorSound = throttled('error');

export default function (redraw: Redraw): NvuiPlugin {
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
    premoveInput: '',
    playPremove(ctrl: RoundController) {
      const nvui = ctrl.nvui!;
      nvui.submitMove?.(true);
      nvui.premoveInput = '';
    },
    submitMove: undefined,
    render(ctrl: RoundController): VNode {
      const d = ctrl.data,
        nvui = ctrl.nvui!,
        step = plyStep(d, ctrl.ply),
        style = moveStyle.get(),
        variantNope =
          !supportedVariant(d.game.variant.key) &&
          'Sorry, the variant ' + d.game.variant.key + ' is not supported in blind mode.';
      if (!ctrl.chessground) {
        ctrl.setChessground(
          Chessground(document.createElement('div'), {
            ...makeCgConfig(ctrl),
            animation: { enabled: false },
            drawable: { enabled: false },
            coordinates: false,
          })
        );
        if (variantNope) setTimeout(() => notify.set(variantNope), 3000);
      }
      return h(
        'div.nvui',
        {
          hook: onInsert(_ => setTimeout(() => notify.set(gameText(ctrl)), 2000)),
        },
        [
          h('h1', gameText(ctrl)),
          h('h2', 'Game info'),
          ...['white', 'black'].map((color: Color) =>
            h('p', [color + ' player: ', playerHtml(ctrl, ctrl.playerByColor(color))])
          ),
          h('p', `${d.game.rated ? 'Rated' : 'Casual'} ${d.game.perf}`),
          d.clock ? h('p', `Clock: ${d.clock.initial / 60} + ${d.clock.increment}`) : null,
          h('h2', 'Moves'),
          h(
            'p.moves',
            {
              attrs: {
                role: 'log',
                'aria-live': 'off',
              },
            },
            renderMoves(d.steps.slice(1), style)
          ),
          h('h2', 'Pieces'),
          h('div.pieces', renderPieces(ctrl.chessground.state.pieces, style)),
          h('h2', 'Game status'),
          h(
            'div.status',
            {
              attrs: {
                role: 'status',
                'aria-live': 'assertive',
                'aria-atomic': 'true',
              },
            },
            [ctrl.data.game.status.name === 'started' ? 'Playing' : renderResult(ctrl)]
          ),
          h('h2', 'Last move'),
          h(
            'p.lastMove',
            {
              attrs: {
                'aria-live': 'assertive',
                'aria-atomic': 'true',
              },
            },
            // make sure consecutive moves are different so that they get re-read
            renderSan(step.san, step.uci, style) + (ctrl.ply % 2 === 0 ? '' : ' ')
          ),
          ...(ctrl.isPlaying()
            ? [
                h('h2', 'Move form'),
                h(
                  'form',
                  {
                    hook: onInsert(el => {
                      const $form = $(el as HTMLFormElement),
                        $input = $form.find('.move').val('');
                      $input[0]!.focus();
                      nvui.submitMove = createSubmitHandler(ctrl, notify.set, moveStyle.get, $input);
                      $form.on('submit', () => nvui.submitMove?.());
                    }),
                  },
                  [
                    h('label', [
                      d.player.color === d.game.player ? 'Your move' : 'Waiting',
                      h('input.move.mousetrap', {
                        attrs: {
                          name: 'move',
                          type: 'text',
                          autocomplete: 'off',
                          autofocus: true,
                          disabled: !!variantNope,
                          title: variantNope,
                        },
                      }),
                    ]),
                  ]
                ),
              ]
            : []),
          h('h2', 'Your clock'),
          h('div.botc', anyClock(ctrl, 'bottom')),
          h('h2', 'Opponent clock'),
          h('div.topc', anyClock(ctrl, 'top')),
          notify.render(),
          h('h2', 'Actions'),
          ...(ctrl.data.player.spectator
            ? renderTableWatch(ctrl)
            : game.playable(ctrl.data)
            ? renderTablePlay(ctrl)
            : renderTableEnd(ctrl)),
          h('h2', 'Board'),
          h(
            'div.board',
            {
              hook: onInsert(el => {
                const $board = $(el as HTMLElement);
                $board.on('keypress', () => console.log(ctrl));
                // NOTE: This is the only line different from analysis board listener setup
                const $buttons = $board.find('button');
                $buttons.on(
                  'click',
                  selectionHandler(() => ctrl.data.opponent.color, selectSound)
                );
                $buttons.on('keydown', arrowKeyHandler(ctrl.data.player.color, borderSound));
                $buttons.on('keypress', boardCommandsHandler());
                $buttons.on(
                  'keypress',
                  lastCapturedCommandHandler(
                    () => ctrl.data.steps.map(step => step.fen),
                    pieceStyle.get(),
                    prefixStyle.get()
                  )
                );
                $buttons.on(
                  'keypress',
                  possibleMovesHandler(
                    ctrl.data.player.color,
                    () => ctrl.chessground.state.turnColor,
                    ctrl.chessground.getFen,
                    () => ctrl.chessground.state.pieces,
                    ctrl.data.game.variant.key,
                    () => ctrl.chessground.state.movable.dests,
                    () => ctrl.data.steps
                  )
                );
                $buttons.on('keypress', positionJumpHandler());
                $buttons.on('keypress', pieceJumpingHandler(wrapSound, errorSound));
              }),
            },
            renderBoard(
              ctrl.chessground.state.pieces,
              ctrl.data.player.color,
              pieceStyle.get(),
              prefixStyle.get(),
              positionStyle.get(),
              boardStyle.get()
            )
          ),
          h(
            'div.boardstatus',
            {
              attrs: {
                'aria-live': 'polite',
                'aria-atomic': 'true',
              },
            },
            ''
          ),
          // h('p', takes(ctrl.data.steps.map(data => data.fen))),
          h('h2', 'Settings'),
          h('label', ['Move notation', renderSetting(moveStyle, ctrl.redraw)]),
          h('h3', 'Board Settings'),
          h('label', ['Piece style', renderSetting(pieceStyle, ctrl.redraw)]),
          h('label', ['Piece prefix style', renderSetting(prefixStyle, ctrl.redraw)]),
          h('label', ['Show position', renderSetting(positionStyle, ctrl.redraw)]),
          h('label', ['Board layout', renderSetting(boardStyle, ctrl.redraw)]),
          h('h2', 'Commands'),
          h('p', [
            'Type these commands in the move input.',
            h('br'),
            'c: Read clocks.',
            h('br'),
            'l: Read last move.',
            h('br'),
            'o: Read name and rating of the opponent.',
            h('br'),
            commands.piece.help,
            h('br'),
            commands.scan.help,
            h('br'),
            'abort: Abort game.',
            h('br'),
            'resign: Resign game.',
            h('br'),
            'draw: Offer or accept draw.',
            h('br'),
            'takeback: Offer or accept take back.',
            h('br'),
          ]),
          h('h2', 'Board Mode commands'),
          h('p', [
            'Use these commands when focused on the board itself.',
            h('br'),
            'o: announce current position.',
            h('br'),
            "c: announce last move's captured piece.",
            h('br'),
            'l: announce last move.',
            h('br'),
            't: announce clocks.',
            h('br'),
            'm: announce possible moves for the selected piece.',
            h('br'),
            'shift+m: announce possible moves for the selected pieces which capture..',
            h('br'),
            'arrow keys: move left, right, up or down.',
            h('br'),
            'kqrbnp/KQRBNP: move forward/backward to a piece.',
            h('br'),
            '1-8: move to rank 1-8.',
            h('br'),
            'Shift+1-8: move to file a-h.',
            h('br'),
          ]),
          h('h2', 'Promotion'),
          h('p', [
            'Standard PGN notation selects the piece to promote to. Example: a8=n promotes to a knight.',
            h('br'),
            'Omission results in promotion to queen',
          ]),
        ]
      );
    },
  };
}

function createSubmitHandler(ctrl: RoundController, notify: (txt: string) => void, style: () => Style, $input: Cash) {
  return (submitStoredPremove = false) => {
    const nvui = ctrl.nvui!;

    if (submitStoredPremove && nvui.premoveInput === '') return false;
    if (!submitStoredPremove && $input.val() === '') {
      if (nvui.premoveInput !== '') {
        // if this is not a premove submission, the input is empty, and we have a stored premove, clear it
        nvui.premoveInput = '';
        notify('Cleared premove');
      } else notify('Invalid move');
      return false;
    }

    let input = submitStoredPremove ? nvui.premoveInput : castlingFlavours(($input.val() as string).trim());

    // commands may be submitted with or without a leading /
    if (isShortCommand(input)) input = '/' + input;
    if (input[0] === '/') {
      onCommand(ctrl, notify, input.slice(1), style());
      $input.val('');
      return false;
    }

    const uci = inputToLegalUci(input, plyStep(ctrl.data, ctrl.ply).fen, ctrl.chessground);
    if (uci) ctrl.socket.send('move', { u: uci }, { ackable: true });
    else if (ctrl.data.player.color !== ctrl.data.game.player) {
      // if it is not the user's turn, store this input as a premove
      nvui.premoveInput = input;
      notify(`Will attempt to premove: ${input}. Enter to cancel`);
    } else notify(`Invalid move: ${input}`);
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
  else if (lowered == 'resign') $('.nvui button.resign').trigger('click');
  else if (lowered == 'draw') $('.nvui button.draw-yes').trigger('click');
  else if (lowered == 'takeback') $('.nvui button.takeback-yes').trigger('click');
  else if (lowered == 'o' || lowered == 'opponent') notify(playerText(ctrl, ctrl.data.opponent));
  else {
    const pieces = ctrl.chessground.state.pieces;
    notify(commands.piece.apply(c, pieces, style) || commands.scan.apply(c, pieces, style) || `Invalid command: ${c}`);
  }
}

function anyClock(ctrl: RoundController, position: Position) {
  const d = ctrl.data,
    player = ctrl.playerAt(position);
  return (
    (ctrl.clock && renderClock(ctrl, player, position)) ||
    (d.correspondence && renderCorresClock(ctrl.corresClock!, ctrl.trans, player.color, position, d.game.player)) ||
    undefined
  );
}

function renderMoves(steps: Step[], style: Style) {
  const res: Array<string | VNode> = [];
  steps.forEach(s => {
    if (s.ply & 1) res.push(Math.ceil(s.ply / 2) + ' ');
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
    rating = player.rating ? player.rating : perf && perf.rating,
    rd = player.ratingDiff,
    ratingDiff = rd ? (rd > 0 ? '+' + rd : rd < 0 ? '−' + -rd : '') : '';
  return user
    ? h('span', [
        h(
          'a',
          {
            attrs: { href: '/@/' + user.username },
          },
          user.title ? `${user.title} ${user.username}` : user.username
        ),
        rating ? ` ${rating}` : ``,
        ' ' + ratingDiff,
      ])
    : 'Anonymous';
}

function playerText(ctrl: RoundController, player: game.Player) {
  if (player.ai) return renderAi(ctrl, player.ai);
  const d = ctrl.data,
    user = player.user,
    perf = user ? user.perfs[d.game.perf] : null,
    rating = player.rating ? player.rating : perf && perf.rating;
  if (!user) return 'Anonymous';
  return `${user.title || ''} ${user.username} rated ${rating || 'unknown'}`;
}

function gameText(ctrl: RoundController) {
  const d = ctrl.data;
  return [
    d.game.status.name == 'started'
      ? ctrl.isPlaying()
        ? 'You play the ' + ctrl.data.player.color + ' pieces.'
        : 'Spectating.'
      : 'Game over.',
    d.game.rated ? 'Rated' : 'Casual',
    d.clock ? `${d.clock.initial / 60} + ${d.clock.increment}` : '',
    d.game.perf,
    'game versus',
    playerText(ctrl, ctrl.data.opponent),
  ].join(' ');
}
