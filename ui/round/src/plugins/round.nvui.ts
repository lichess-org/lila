import { type VNode, looseH as h, onInsert } from 'common/snabbdom';
import type RoundController from '../ctrl';
import { renderClock } from '../clock/clockView';
import { renderTableWatch, renderTablePlay, renderTableEnd } from '../view/table';
import { makeConfig as makeCgConfig } from '../ground';
import renderCorresClock from '../corresClock/corresClockView';
import { renderResult } from '../view/replay';
import { plyStep } from '../util';
import type { Step, Position, NvuiPlugin } from '../interfaces';
import { type Player, playable } from 'game';
import {
  type MoveStyle,
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
  inputToLegalUci,
  renderPockets,
  type DropMove,
  pocketsStr,
} from 'nvui/chess';
import { renderSetting } from 'nvui/setting';
import { Notify } from 'nvui/notify';
import { commands, boardCommands, addBreaks } from 'nvui/command';
import { Chessground as makeChessground } from 'chessground';
import { pubsub } from 'common/pubsub';
import { plyToTurn } from 'chess';
import { next, prev } from '../keyboard';

const selectSound = () => site.sound.play('select');
const borderSound = () => site.sound.play('outOfBound');
const errorSound = () => site.sound.play('error');

// esbuild
export function initModule(): NvuiPlugin {
  const notify = new Notify(),
    moveStyle = styleSetting(),
    prefixStyle = prefixSetting(),
    pieceStyle = pieceSetting(),
    positionStyle = positionSetting(),
    boardStyle = boardSetting();

  pubsub.on('socket.in.message', line => {
    if (line.u === 'lichess') notify.set(line.t);
  });
  pubsub.on('round.suggestion', notify.set);

  return {
    premoveInput: '',
    playPremove(ctrl: RoundController) {
      const nvui = ctrl.nvui!;
      nvui.submitMove?.(true);
      nvui.premoveInput = '';
    },
    submitMove: undefined,
    render(ctrl: RoundController): VNode {
      notify.redraw = ctrl.redraw;
      const d = ctrl.data,
        nvui = ctrl.nvui!,
        step = plyStep(d, ctrl.ply),
        style = moveStyle.get(),
        pockets = step.crazy?.pockets,
        clocks = [anyClock(ctrl, 'bottom'), anyClock(ctrl, 'top')];
      if (!ctrl.chessground) {
        ctrl.setChessground(
          makeChessground(document.createElement('div'), {
            ...makeCgConfig(ctrl),
            animation: { enabled: false },
            drawable: { enabled: false },
            coordinates: false,
          }),
        );
      }
      return h('div.nvui', { hook: onInsert(_ => setTimeout(() => notify.set(gameText(ctrl)), 2000)) }, [
        h('h1', gameText(ctrl)),
        h('h2', 'Game info'),
        ...['white', 'black'].map((color: Color) =>
          h('p', [color + ' player: ', playerHtml(ctrl, ctrl.playerByColor(color))]),
        ),
        h('p', `${i18n.site[d.game.rated ? 'rated' : 'casual']} ${d.game.perf}`),
        d.clock ? h('p', `Clock: ${d.clock.initial / 60} + ${d.clock.increment}`) : null,
        h('h2', 'Moves'),
        h('p.moves', { attrs: { role: 'log', 'aria-live': 'off' } }, renderMoves(d.steps.slice(1), style)),
        h('h2', 'Pieces'),
        h('div.pieces', renderPieces(ctrl.chessground.state.pieces, style)),
        pockets && h('div.pockets', renderPockets(pockets)),
        h('h2', 'Game status'),
        h('div.status', { attrs: { role: 'status', 'aria-live': 'assertive', 'aria-atomic': 'true' } }, [
          ctrl.data.game.status.name === 'started' ? i18n.site.playingRightNow : renderResult(ctrl),
        ]),
        h('h2', 'Last move'),
        h(
          'p.lastMove',
          { attrs: { 'aria-live': 'assertive', 'aria-atomic': 'true' } },
          // make sure consecutive moves are different so that they get re-read
          renderSan(step.san, step.uci, style) + (ctrl.ply % 2 === 0 ? '' : ' '),
        ),
        ctrl.isPlaying() &&
          h('div.move-input', [
            h('h2', 'Move form'),
            h(
              'form#move-form',
              {
                hook: onInsert(el => {
                  const $form = $(el as HTMLFormElement),
                    $input = $form.find('.move').val('');
                  nvui.submitMove = createSubmitHandler(ctrl, notify.set, moveStyle.get, $input);
                  $form.on('submit', (ev: SubmitEvent) => {
                    ev.preventDefault();
                    nvui.submitMove?.();
                  });
                }),
              },
              [
                h('label', [
                  d.player.color === d.game.player ? i18n.site.yourTurn : i18n.site.waiting,
                  h('input.move.mousetrap', {
                    attrs: {
                      name: 'move',
                      type: 'text',
                      autocomplete: 'off',
                      autofocus: true,
                    },
                  }),
                ]),
              ],
            ),
          ]),
        clocks.some(c => !!c) &&
          h('div.clocks', [
            h('h2', 'Your clock'),
            h('div.botc', clocks[0]),
            h('h2', 'Opponent clock'),
            h('div.topc', clocks[1]),
          ]),
        notify.render(),
        h('h2', 'Actions'),
        ...(ctrl.data.player.spectator
          ? renderTableWatch(ctrl)
          : playable(ctrl.data)
            ? renderTablePlay(ctrl)
            : renderTableEnd(ctrl)),
        h('h2', i18n.site.board),
        h(
          'div.board',
          {
            hook: onInsert(el => {
              const $board = $(el);
              const $buttons = $board.find('button');
              $buttons.on(
                'click',
                selectionHandler(() => ctrl.data.opponent.color, selectSound),
              );
              $buttons.on('keydown', (e: KeyboardEvent) => {
                if (e.shiftKey && e.key.match(/^[ad]$/i)) nextOrPrev(ctrl)(e);
                else if (['o', 'l', 't'].includes(e.key)) boardCommandsHandler()(e);
                else if (e.key.startsWith('Arrow')) arrowKeyHandler(ctrl.data.player.color, borderSound)(e);
                else if (e.key === 'c')
                  lastCapturedCommandHandler(
                    () => ctrl.data.steps.map(step => step.fen),
                    pieceStyle.get(),
                    prefixStyle.get(),
                  )();
                else if (e.code.match(/^Digit([1-8])$/)) positionJumpHandler()(e);
                else if (e.key.match(/^[kqrbnp]$/i)) pieceJumpingHandler(selectSound, errorSound)(e);
                else if (e.key.toLowerCase() === 'm')
                  possibleMovesHandler(
                    ctrl.data.player.color,
                    ctrl.chessground,
                    ctrl.data.game.variant.key,
                    ctrl.data.steps,
                  )(e);
              });
            }),
          },
          renderBoard(
            ctrl.chessground.state.pieces,
            ctrl.data.game.variant.key === 'racingKings' ? 'white' : ctrl.data.player.color,
            pieceStyle.get(),
            prefixStyle.get(),
            positionStyle.get(),
            boardStyle.get(),
          ),
        ),
        h('div.boardstatus', { attrs: { 'aria-live': 'polite', 'aria-atomic': 'true' } }, ''),
        h('h2', 'Settings'),
        h('label', ['Move notation', renderSetting(moveStyle, ctrl.redraw)]),
        h('h3', 'Board settings'),
        h('label', ['Piece style', renderSetting(pieceStyle, ctrl.redraw)]),
        h('label', ['Piece prefix style', renderSetting(prefixStyle, ctrl.redraw)]),
        h('label', ['Show position', renderSetting(positionStyle, ctrl.redraw)]),
        h('label', ['Board layout', renderSetting(boardStyle, ctrl.redraw)]),
        h('h2', `${i18n.keyboardMove.keyboardInputCommands}`),
        h(
          'p',
          [
            'Type these commands in the move input.',
            ...inputCommands
              .filter(c => !c.invalid?.(ctrl))
              .map(cmd => `${cmd.cmd}${cmd.alt ? ` or ${cmd.alt}` : ''}: ${cmd.help}`),
          ].reduce(addBreaks, []),
        ),
        ...boardCommands(),
        h('h2', 'Promotion'),
        h('p', [
          'Standard PGN notation selects the piece to promote to. Example: a8=n promotes to a knight.',
          h('br'),
          'Omission results in promotion to queen',
        ]),
      ]);
    },
  };
}

function createSubmitHandler(
  ctrl: RoundController,
  notify: (txt: string) => void,
  style: () => MoveStyle,
  $input: Cash,
) {
  return (submitStoredPremove = false) => {
    const nvui = ctrl.nvui!;

    if (submitStoredPremove && nvui.premoveInput === '') return;
    if (!submitStoredPremove && $input.val() === '') {
      if (nvui.premoveInput !== '') {
        // if this is not a premove submission, the input is empty, and we have a stored premove, clear it
        nvui.premoveInput = '';
        notify('Cleared premove');
      } else notify('Invalid move');
    }

    const input = submitStoredPremove
      ? nvui.premoveInput
      : castlingFlavours(($input.val() as string).trim().toLowerCase());
    if (!input) return;

    // commands may be submitted with or without a leading /
    const command = isInputCommand(input) || isInputCommand(input.slice(1));
    if (command) command.cb(notify, ctrl, style(), input);
    else {
      const uciOrDrop = inputToLegalUci(input, plyStep(ctrl.data, ctrl.ply).fen, ctrl.chessground);
      if (uciOrDrop && typeof uciOrDrop !== 'string' && !ctrl.crazyValid(uciOrDrop.role, uciOrDrop.key))
        notify(`Invalid input: ${input}`);
      else if (uciOrDrop) sendMove(uciOrDrop, ctrl, !!nvui.premoveInput);
      else if (ctrl.data.player.color !== ctrl.data.game.player) {
        // if it is not the user's turn, store this input as a premove
        nvui.premoveInput = input;
        notify(`Will attempt to premove: ${input}. Enter to cancel`);
      } else notify(`Invalid move: ${input}`);
    }
    $input.val('');
  };
}

type Command =
  | 'clock'
  | 'last'
  | 'abort'
  | 'resign'
  | 'draw'
  | 'takeback'
  | 'p'
  | 's'
  | 'opponent'
  | 'pocket';

type InputCommand = {
  cmd: Command;
  help: string;
  cb: (notify: (txt: string) => void, ctrl: RoundController, style: MoveStyle, input: string) => void;
  alt?: string;
  invalid?: (ctrl: RoundController) => boolean;
};

const inputCommands: InputCommand[] = [
  {
    cmd: 'clock',
    help: i18n.keyboardMove.readOutClocks,
    cb: notify => notify($('.nvui .botc').text() + ', ' + $('.nvui .topc').text()),
    alt: 'c',
  },
  {
    cmd: 'last',
    help: 'Read last move.',
    cb: notify => notify($('.lastMove').text()),
    alt: 'l',
  },
  { cmd: 'abort', help: i18n.site.abortGame, cb: () => $('.nvui button.abort').trigger('click') },
  { cmd: 'resign', help: i18n.site.resign, cb: () => $('.nvui button.resign').trigger('click') },
  {
    cmd: 'draw',
    help: i18n.keyboardMove.offerOrAcceptDraw,
    cb: () => $('.nvui button.draw-yes').trigger('click'),
  },
  {
    cmd: 'takeback',
    help: i18n.site.proposeATakeback,
    cb: () => $('.nvui button.takeback-yes').trigger('click'),
  },
  {
    cmd: 'p',
    help: commands.piece.help,
    cb: (notify, ctrl, style, input) =>
      notify(
        commands.piece.apply(input, ctrl.chessground.state.pieces, style) ??
          `Bad input: ${input}. Exptected format: ${commands.piece.help}`,
      ),
  },
  {
    cmd: 's',
    help: commands.scan.help,
    cb: (notify, ctrl, style, input) =>
      notify(
        commands.scan.apply(input, ctrl.chessground.state.pieces, style) ??
          `Bad input: ${input}. Exptected format: ${commands.scan.help}`,
      ),
  },
  {
    cmd: 'opponent',
    help: i18n.keyboardMove.readOutOpponentName,
    cb: (notify, ctrl) => notify(playerText(ctrl)),
    alt: 'o',
  },
  {
    cmd: 'pocket',
    help: 'Read out pockets for white or black. Example: "pocket black"',
    cb: (notify, ctrl, _, input) => {
      const pockets = ctrl.data?.crazyhouse?.pockets;
      const color = input.split(' ')?.[1]?.trim();
      return notify(
        pockets
          ? color
            ? pocketsStr(color === 'white' ? pockets[0] : pockets[1]) || i18n.site.none
            : 'Expected format: pocket [white|black]'
          : 'Command only available in crazyhouse',
      );
    },
    invalid: ctrl => ctrl.data.game.variant.key !== 'crazyhouse',
  },
];

const isInputCommand = (input: string) => {
  const firstWordLowerCase = input.split(' ')[0].toLowerCase();
  return inputCommands.find(c => c.cmd === firstWordLowerCase || c?.alt === firstWordLowerCase);
};

const sendMove = (uciOrDrop: string | DropMove, ctrl: RoundController, premove: boolean): void =>
  typeof uciOrDrop === 'string'
    ? ctrl.socket.send('move', { u: uciOrDrop }, { ackable: true })
    : ctrl.sendNewPiece(uciOrDrop.role, uciOrDrop.key, premove);

function anyClock(ctrl: RoundController, position: Position): VNode | undefined {
  const d = ctrl.data,
    player = ctrl.playerAt(position);
  return (
    (ctrl.clock && renderClock(ctrl, player, position)) ||
    (d.correspondence && renderCorresClock(ctrl.corresClock!, player.color, position, d.game.player))
  );
}

const renderMoves = (steps: Step[], style: MoveStyle) =>
  steps.reduce<(string | VNode)[]>((res, s) => {
    const turn = s.ply & 1 ? `${plyToTurn(s.ply)}` : '';
    const san = `${renderSan(s.san, s.uci, style)}, `;
    return res.concat(`${turn} ${san}`).concat(s.ply % 2 === 0 ? h('br') : []);
  }, []);

function playerHtml(ctrl: RoundController, player: Player) {
  if (player.ai) return i18n.site.aiNameLevelAiLevel('Stockfish', player.ai);
  const perf = ctrl.data.game.perf,
    user = player.user,
    rating = user?.perfs[perf]?.rating,
    rd = player.ratingDiff,
    ratingDiff = rd ? (rd > 0 ? '+' + rd : rd < 0 ? '−' + -rd : '') : '';
  return user
    ? h('span', [
        h(
          'a',
          { attrs: { href: '/@/' + user.username } },
          user.title ? `${user.title} ${user.username}` : user.username,
        ),
        rating ? ` ${rating}` : ``,
        ' ' + ratingDiff,
      ])
    : i18n.site.anonymous;
}

function playerText(ctrl: RoundController) {
  const player = ctrl.data.opponent;
  if (player.ai) return i18n.site.aiNameLevelAiLevel('Stockfish', player.ai);
  const user = player.user,
    rating = player?.rating ?? user?.perfs[ctrl.data.game.perf]?.rating ?? i18n.site.unknown;
  return !user ? i18n.site.anonymous : `${user.title || ''} ${user.username}. ${i18n.site.rating} ${rating}`;
}

function gameText(ctrl: RoundController) {
  const d = ctrl.data;
  return [
    d.game.status.name === 'started'
      ? ctrl.isPlaying()
        ? i18n.site[ctrl.data.player.color === 'white' ? 'youPlayTheWhitePieces' : 'youPlayTheBlackPieces']
        : 'Spectating.'
      : i18n.site.gameOver,
    i18n.site[ctrl.data.game.rated ? 'rated' : 'casual'],
    d.clock ? `${d.clock.initial / 60} + ${d.clock.increment}` : '',
    d.game.perf,
    i18n.site.gameVsX(playerText(ctrl)),
  ].join(' ');
}

function doAndRedraw(ctrl: RoundController, f: (ctrl: RoundController) => void) {
  f(ctrl);
  ctrl.redraw();
}

function nextOrPrev(ctrl: RoundController) {
  return (e: KeyboardEvent) => {
    if (e.key === 'A') doAndRedraw(ctrl, prev);
    else if (e.key === 'D') doAndRedraw(ctrl, next);
  };
}
