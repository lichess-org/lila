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
  supportedVariant,
  inputToLegalUci,
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
        variantNope =
          !supportedVariant(d.game.variant.key) &&
          'Sorry, the variant ' + d.game.variant.key + ' is not supported in blind mode.';
      if (!ctrl.chessground) {
        ctrl.setChessground(
          makeChessground(document.createElement('div'), {
            ...makeCgConfig(ctrl),
            animation: { enabled: false },
            drawable: { enabled: false },
            coordinates: false,
          }),
        );
        if (variantNope) setTimeout(() => notify.set(variantNope), 3000);
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
        ctrl.isPlaying() && h('h2', 'Move form'),
        ctrl.isPlaying() &&
          h(
            'form',
            {
              hook: onInsert(el => {
                const $form = $(el as HTMLFormElement),
                  $input = $form.find('.move').val('');
                $input[0]!.focus();
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
                    disabled: !!variantNope,
                    title: variantNope,
                  },
                }),
              ]),
            ],
          ),

        h('h2', 'Your clock'),
        h('div.botc', anyClock(ctrl, 'bottom')),
        h('h2', 'Opponent clock'),
        h('div.topc', anyClock(ctrl, 'top')),
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
              $board.on('keydown', jumpOrCommand(ctrl));
              const $buttons = $board.find('button');
              $buttons.on(
                'click',
                selectionHandler(() => ctrl.data.opponent.color, selectSound),
              );
              $buttons.on('keydown', arrowKeyHandler(ctrl.data.player.color, borderSound));
              $buttons.on(
                'keydown',
                lastCapturedCommandHandler(
                  () => ctrl.data.steps.map(step => step.fen),
                  pieceStyle.get(),
                  prefixStyle.get(),
                ),
              );
              $buttons.on(
                'keydown',
                possibleMovesHandler(
                  ctrl.data.player.color,
                  () => ctrl.chessground.state.turnColor,
                  ctrl.chessground.getFen,
                  () => ctrl.chessground.state.pieces,
                  ctrl.data.game.variant.key,
                  () => ctrl.chessground.state.movable.dests,
                  () => ctrl.data.steps,
                ),
              );
              $buttons.on('keydown', positionJumpHandler());
              $buttons.on('keydown', pieceJumpingHandler(selectSound, errorSound));
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
            `c: ${i18n.keyboardMove.readOutClocks}`,
            'l: Read last move.',
            `o: ${i18n.keyboardMove.readOutOpponentName}`,
            commands.piece.help,
            commands.scan.help,
            `abort: ${i18n.site.abortGame}`,
            `resign: ${i18n.site.resign}`,
            `draw: ${i18n.keyboardMove.offerOrAcceptDraw}`,
            `takeback: ${i18n.site.proposeATakeback}`,
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

    let input = submitStoredPremove ? nvui.premoveInput : castlingFlavours(($input.val() as string).trim());
    if (!input) return;

    // commands may be submitted with or without a leading /
    if (isShortCommand(input)) input = '/' + input;
    if (input[0] === '/') {
      onCommand(ctrl, notify, input.slice(1), style());
      $input.val('');
    } else {
      const uci = inputToLegalUci(input, plyStep(ctrl.data, ctrl.ply).fen, ctrl.chessground);
      if (uci) ctrl.socket.send('move', { u: uci }, { ackable: true });
      else if (ctrl.data.player.color !== ctrl.data.game.player) {
        // if it is not the user's turn, store this input as a premove
        nvui.premoveInput = input;
        notify(`Will attempt to premove: ${input}. Enter to cancel`);
      } else notify(`Invalid move: ${input}`);
      $input.val('');
    }
  };
}

const shortCommands = [
  'c',
  'clock',
  'l',
  'last',
  'abort',
  'resign',
  'draw',
  'takeback',
  'p',
  's',
  'o',
  'opponent',
];

function isShortCommand(input: string): boolean {
  return shortCommands.includes(input.split(' ')[0].toLowerCase());
}

function onCommand(ctrl: RoundController, notify: (txt: string) => void, c: string, style: MoveStyle) {
  const lowered = c.toLowerCase();
  if (lowered === 'c' || lowered === 'clock')
    notify($('.nvui .botc').text() + ', ' + $('.nvui .topc').text());
  else if (lowered === 'l' || lowered === 'last') notify($('.lastMove').text());
  else if (lowered === 'abort') $('.nvui button.abort').trigger('click');
  else if (lowered === 'resign') $('.nvui button.resign').trigger('click');
  else if (lowered === 'draw') $('.nvui button.draw-yes').trigger('click');
  else if (lowered === 'takeback') $('.nvui button.takeback-yes').trigger('click');
  else if (lowered === 'o' || lowered === 'opponent') notify(playerText(ctrl, ctrl.data.opponent));
  else {
    const pieces = ctrl.chessground.state.pieces;
    notify(
      commands.piece.apply(c, pieces, style) ||
        commands.scan.apply(c, pieces, style) ||
        `Invalid command: ${c}`,
    );
  }
}

function anyClock(ctrl: RoundController, position: Position) {
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

function playerText(ctrl: RoundController, player: Player) {
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
        ? ctrl.data.player.color === 'white'
          ? i18n.site.youPlayTheWhitePieces
          : i18n.site.youPlayTheBlackPieces
        : 'Spectating.'
      : i18n.site.gameOver,
    d.game.rated ? i18n.site.rated : i18n.site.casual,
    d.clock ? `${d.clock.initial / 60} + ${d.clock.increment}` : '',
    d.game.perf,
    i18n.site.gameVsX(playerText(ctrl, ctrl.data.opponent)),
  ].join(' ');
}

function doAndRedraw(ctrl: RoundController, f: (ctrl: RoundController) => void) {
  f(ctrl);
  ctrl.redraw();
}

function jumpOrCommand(ctrl: RoundController) {
  return (e: KeyboardEvent) => {
    if (e.shiftKey) {
      if (e.key === 'A') doAndRedraw(ctrl, prev);
      else if (e.key === 'D') doAndRedraw(ctrl, next);
    } else boardCommandsHandler()(e);
  };
}
