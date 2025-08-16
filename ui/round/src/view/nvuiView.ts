import type { RoundNvuiContext } from '../round.nvui';
import type RoundController from '../ctrl';
import { type LooseVNodes, type VNode, bind, hl, noTrans, onInsert } from 'lib/snabbdom';
import { renderClock } from 'lib/game/clock/clockView';
import { type Player, type TopOrBottom, playable } from 'lib/game/game';
import { renderTableWatch, renderTablePlay, renderTableEnd } from './table';
import { scanDirectionsHandler } from 'lib/nvui/directionScan';
import { commands, boardCommands } from 'lib/nvui/command';
import { plyToTurn } from 'lib/game/chess';
import { renderSetting } from 'lib/nvui/setting';
import * as nv from 'lib/nvui/chess';
import { Chessground as makeChessground } from '@lichess-org/chessground';
import { makeConfig as makeCgConfig } from '../ground';
import renderCorresClock from '../corresClock/corresClockView';
import { renderResult } from './replay';
import { plyStep } from '../util';
import type { Step } from '../interfaces';
import { next, prev } from '../keyboard';
import { opposite } from 'chessops';

const selectSound = () => site.sound.play('select');
const borderSound = () => site.sound.play('outOfBound');
const errorSound = () => site.sound.play('error');

export function renderNvui(ctx: RoundNvuiContext): VNode {
  const {
    ctrl,
    notify,
    moveStyle,
    pieceStyle,
    prefixStyle,
    positionStyle,
    boardStyle,
    pageStyle,
    deviceType,
  } = ctx;
  notify.redraw = ctrl.redraw;
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
  if (deviceType.get() === 'touchscreen' && pageStyle.get() === 'board-actions') {
    pieceStyle.set('name');
    prefixStyle.set('name');
    boardStyle.set('plain');
    return hl('div.nvui', { hook: onInsert(_ => setTimeout(() => notify.set(gameText(ctrl)), 2000)) }, [
      pageStyle.get() === 'actions-board'
        ? [ctrl.isPlaying() && inputForm(ctx), renderActions(ctx), renderBoard(ctx)]
        : [
            renderBoard(ctx),
            renderTouchDeviceCommands(ctx),
            renderActions(ctx),
            ctrl.isPlaying() && inputForm(ctx),
          ],
      gameInfo(ctx),
      hl('h2', i18n.site.advancedSettings),
      hl('label', [noTrans('Move notation'), renderSetting(moveStyle, ctrl.redraw)]),
      hl('label', [noTrans('Page layout'), renderSetting(pageStyle, ctrl.redraw)]),
      hl('label', [noTrans('Show position'), renderSetting(positionStyle, ctrl.redraw)]),
      hl('h2', i18n.keyboardMove.keyboardInputCommands),
      hl('p', [
        i18n.nvui.inputFormCommandList,
        hl('br'),
        i18n.nvui.movePiece,
        hl('br'),
        i18n.nvui.promotion,
        hl('br'),
        inputCommands
          .filter(c => !c.invalid?.(ctrl))
          .flatMap(cmd => [`${cmd.cmd}${cmd.alt ? ` / ${cmd.alt}` : ''}: `, cmd.help, hl('br')]),
      ]),
    ]);
  } else
    return hl('div.nvui', { hook: onInsert(_ => setTimeout(() => notify.set(gameText(ctrl)), 2000)) }, [
      gameInfo(ctx),
      ctrl.isPlaying() && inputForm(ctx),
      pageStyle.get() === 'actions-board'
        ? [renderActions(ctx), renderBoard(ctx)]
        : [renderBoard(ctx), renderActions(ctx)],
      hl('h2', i18n.site.advancedSettings),
      hl('label', [noTrans('Move notation'), renderSetting(moveStyle, ctrl.redraw)]),
      hl('label', [noTrans('Page layout'), renderSetting(pageStyle, ctrl.redraw)]),
      hl('h3', noTrans('Board settings')),
      hl('label', [noTrans('Piece style'), renderSetting(pieceStyle, ctrl.redraw)]),
      hl('label', [noTrans('Piece prefix style'), renderSetting(prefixStyle, ctrl.redraw)]),
      hl('label', [noTrans('Show position'), renderSetting(positionStyle, ctrl.redraw)]),
      hl('label', [noTrans('Board layout'), renderSetting(boardStyle, ctrl.redraw)]),
      hl('h2', i18n.keyboardMove.keyboardInputCommands),
      hl('p', [
        i18n.nvui.inputFormCommandList,
        hl('br'),
        i18n.nvui.movePiece,
        hl('br'),
        i18n.nvui.promotion,
        hl('br'),
        inputCommands
          .filter(c => !c.invalid?.(ctrl))
          .flatMap(cmd => [`${cmd.cmd}${cmd.alt ? ` / ${cmd.alt}` : ''}: `, cmd.help, hl('br')]),
      ]),
      boardCommands(),
    ]);
}

function inputForm(ctx: RoundNvuiContext): LooseVNodes {
  const { ctrl, notify, moveStyle } = ctx;
  const d = ctrl.data,
    nvui = ctrl.nvui!;
  return hl('div.move-input', [
    hl('h2', i18n.nvui.inputForm),
    hl(
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
        hl('label', [
          d.player.color === d.game.player ? i18n.site.yourTurn : i18n.site.waiting,
          hl('input.move.mousetrap', {
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
  ]);
}

function gameInfo(ctx: RoundNvuiContext): LooseVNodes {
  const { ctrl, notify, moveStyle } = ctx;
  const d = ctrl.data,
    step = plyStep(d, ctrl.ply),
    style = moveStyle.get(),
    pockets = step.crazy?.pockets,
    clocks = [anyClock(ctrl, 'bottom'), anyClock(ctrl, 'top')];

  return [
    hl('h1', gameText(ctrl)),
    hl('h2', i18n.nvui.gameInfo),
    ['white', 'black'].map((color: Color) =>
      hl('p', [i18n.site[color], ':', playerHtml(ctrl, ctrl.playerByColor(color))]),
    ),
    hl('p', [i18n.site[d.game.rated ? 'rated' : 'casual'] + ' ' + transGamePerf(d.game.perf)]),
    d.clock ? hl('p', [i18n.site.clock, `${d.clock.initial / 60} + ${d.clock.increment}`]) : null,
    hl('h2', i18n.nvui.moveList),
    hl('p.moves', { attrs: { role: 'log', 'aria-live': 'off' } }, renderMoves(d.steps.slice(1), style)),
    hl('h2', i18n.nvui.pieces),
    nv.renderPieces(ctrl.chessground.state.pieces, style),
    pockets && hl('h2', i18n.nvui.pockets),
    pockets && nv.renderPockets(pockets),
    hl('h2', i18n.nvui.gameStatus),
    hl('div.status', { attrs: { role: 'status', 'aria-live': 'assertive', 'aria-atomic': 'true' } }, [
      ctrl.data.game.status.name === 'started' ? i18n.site.playingRightNow : renderResult(ctrl),
    ]),
    hl('h2', i18n.nvui.lastMove),
    hl(
      'p.lastMove',
      { attrs: { 'aria-live': 'assertive', 'aria-atomic': 'true' } },
      // make sure consecutive moves are different so that they get re-read
      nv.renderSan(step.san, step.uci, style) + (ctrl.ply % 2 === 0 ? '' : ' '),
    ),
    clocks.some(c => !!c) &&
      hl('div.clocks', [
        hl('h2', i18n.nvui.yourClock),
        hl('div.botc', clocks[0]),
        hl('h2', i18n.nvui.opponentClock),
        hl('div.topc', clocks[1]),
      ]),
    notify.render(),
  ];
}

function renderActions({ ctrl }: RoundNvuiContext): LooseVNodes {
  return [
    hl('h2', i18n.nvui.actions),
    ctrl.data.player.spectator
      ? renderTableWatch(ctrl)
      : playable(ctrl.data)
        ? renderTablePlay(ctrl)
        : renderTableEnd(ctrl),
  ];
}

function renderTouchDeviceCommands(ctx: RoundNvuiContext): LooseVNodes {
  const { notify, ctrl } = ctx;
  return [
    hl('div.actions', [
      hl('button', { hook: bind('click', () => notify.set($('.lastMove').text())) }, 'last move'),
      hl(
        'button',
        {
          hook: bind('click', () => {
            if ($('.nvui .botc').text().trim() != '')
              notify.set($('.nvui .botc').text() + ' - ' + $('.nvui .topc').text());
            else notify.set('not available');
          }),
        },
        'clocks',
      ),
      hl(
        'button',
        {
          hook: bind('click', () => {
            if (ctrl.isPlaying()) {
              $('input.move').val('');
              $('#move-form').trigger('submit');
            } else notify.set('not available');
          }),
        },
        'cancel premove',
      ),
      hl(
        'button',
        {
          hook: bind('click', () => {
            flipBoard(ctx);
          }),
        },
        'flip the board',
      ),
    ]),
  ];
}

function renderBoard(ctx: RoundNvuiContext): LooseVNodes {
  const { ctrl, prefixStyle, pieceStyle, positionStyle, boardStyle } = ctx;

  return [
    hl('h2', i18n.site.board),
    hl(
      'div.board',
      { hook: { insert: el => boardEventsHook(ctx, el.elm as HTMLElement) } },
      nv.renderBoard(
        ctrl.chessground.state.pieces,
        ctrl.data.game.variant.key === 'racingKings'
          ? 'white'
          : ctrl.flip
            ? opposite(ctrl.data.player.color)
            : ctrl.data.player.color,
        pieceStyle.get(),
        prefixStyle.get(),
        positionStyle.get(),
        boardStyle.get(),
      ),
    ),
    hl('div.boardstatus', { attrs: { 'aria-live': 'polite', 'aria-atomic': 'true' } }, ''),
  ];
}

function flipBoard(ctx: RoundNvuiContext): void {
  const { ctrl, notify } = ctx;
  if (ctrl.data.game.variant.key !== 'racingKings') {
    notify.set('Flipping the board');
    setTimeout(() => {
      ctrl.flip = !ctrl.flip;
      ctrl.redraw();
    }, 1000);
  }
}

function boardEventsHook(ctx: RoundNvuiContext, el: HTMLElement): void {
  const { ctrl, prefixStyle, pieceStyle, moveStyle, deviceType } = ctx;

  const $board = $(el);
  const $buttons = $board.find('button');
  $buttons.on('blur', nv.leaveSquareHandler($buttons));
  $buttons.on(
    'click',
    nv.selectionHandler(
      () => ctrl.data.opponent.color,
      deviceType.get() === 'touchscreen',
      ctrl.data.game.variant.key === 'antichess',
    ),
  );
  $buttons.on('keydown', (e: KeyboardEvent) => {
    if (e.shiftKey && e.key.match(/^[ad]$/i)) nextOrPrev(ctrl)(e);
    else if (e.key.match(/^x$/i))
      scanDirectionsHandler(
        ctrl.flip ? opposite(ctrl.data.player.color) : ctrl.data.player.color,
        ctrl.chessground.state.pieces,
        moveStyle.get(),
      )(e);
    else if (e.key.toLowerCase() === 'f') {
      flipBoard(ctx);
    } else if (['o', 'l', 't'].includes(e.key)) nv.boardCommandsHandler()(e);
    else if (e.key.startsWith('Arrow'))
      nv.arrowKeyHandler(
        ctrl.flip ? opposite(ctrl.data.player.color) : ctrl.data.player.color,
        borderSound,
      )(e);
    else if (e.key === 'c')
      nv.lastCapturedCommandHandler(
        () => ctrl.data.steps.map(step => step.fen),
        pieceStyle.get(),
        prefixStyle.get(),
      )();
    else if (e.code.match(/^Digit([1-8])$/)) nv.positionJumpHandler()(e);
    else if (e.key.match(/^[kqrbnp]$/i))
      nv.pieceJumpingHandler(selectSound, errorSound, ctrl.data.game.variant.key === 'antichess')(e);
    else if (e.key.toLowerCase() === 'm')
      nv.possibleMovesHandler(
        ctrl.data.player.color,
        ctrl.chessground,
        ctrl.data.game.variant.key,
        ctrl.data.steps,
      )(e);
    else if (e.key === 'i') {
      e.preventDefault();
      $('input.move').get(0)?.focus();
    }
  });
}

function createSubmitHandler(
  ctrl: RoundController,
  notify: (txt: string) => void,
  style: () => nv.MoveStyle,
  $input: Cash,
) {
  return (submitStoredPremove = false) => {
    const nvui = ctrl.nvui!;

    if (submitStoredPremove && nvui.premoveInput === '') return;
    if (!submitStoredPremove && $input.val() === '') {
      if (nvui.premoveInput !== '') {
        // if this is not a premove submission, the input is empty, and we have a stored premove, clear it
        nvui.premoveInput = '';
        notify(i18n.nvui.premoveCancelled);
      } else notify(i18n.nvui.invalidMove);
    }

    const input = submitStoredPremove
      ? nvui.premoveInput
      : nv.castlingFlavours(($input.val() as string).trim());
    if (!input) return;

    // commands may be submitted with or without a leading /
    const command = isInputCommand(input) || isInputCommand(input.slice(1));
    if (command) command.cb(notify, ctrl, style(), input);
    else {
      const move = nv.inputToMove(input, plyStep(ctrl.data, ctrl.ply).fen, ctrl.chessground);
      const isDrop = (u: undefined | string | nv.DropMove) => !!(u && typeof u !== 'string');
      const isOpponentsTurn = ctrl.data.player.color !== ctrl.data.game.player;
      const isInvalidDrop = (d: nv.DropMove) =>
        !ctrl.crazyValid(d.role, d.key) || (!isOpponentsTurn && ctrl.chessground.state.pieces.has(d.key));

      if (isOpponentsTurn) {
        // if it is not the user's turn, store this input as a premove
        nvui.premoveInput = input;
        notify(i18n.nvui.premoveRecorded(input));
      } else if (isDrop(move) && isInvalidDrop(move)) notify(`Invalid drop: ${input}`);
      else if (move) sendMove(move, ctrl, !!nvui.premoveInput);
      else notify(`${i18n.nvui.invalidMove}: ${input}`);
    }
    $input.val('');
  };
}

type Command =
  | 'board'
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
  help: string | VNode;
  cb: (notify: (txt: string) => void, ctrl: RoundController, style: nv.MoveStyle, input: string) => void;
  alt?: string;
  invalid?: (ctrl: RoundController) => boolean;
};

const inputCommands: InputCommand[] = [
  {
    cmd: 'board',
    help: i18n.nvui.goToBoard,
    cb: (notify, ctrl, style, input) => {
      notify(commands().board.apply(input, ctrl.chessground.state.pieces, style) || '');
    },
    alt: 'b',
  },
  {
    cmd: 'clock',
    help: i18n.keyboardMove.readOutClocks,
    cb: notify => notify($('.nvui .botc').text() + ' - ' + $('.nvui .topc').text()),
    alt: 'c',
  },
  {
    cmd: 'last',
    help: i18n.nvui.announceLastMove,
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
    help: commands().piece.help,
    cb: (notify, ctrl, style, input) =>
      notify(
        commands().piece.apply(input, ctrl.chessground.state.pieces, style) ??
          `Bad input: ${input}. Expected format: ${commands().piece.help}`,
      ),
  },
  {
    cmd: 's',
    help: commands().scan.help,
    cb: (notify, ctrl, style, input) =>
      notify(
        commands().scan.apply(input, ctrl.chessground.state.pieces, style) ??
          `Bad input: ${input}. Expected format: ${commands().scan.help}`,
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
            ? nv.pocketsStr(color === 'white' ? pockets[0] : pockets[1]) || i18n.site.none
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

const sendMove = (uciOrDrop: string | nv.DropMove, ctrl: RoundController, premove: boolean): void =>
  typeof uciOrDrop === 'string'
    ? ctrl.socket.send('move', { u: uciOrDrop }, { ackable: true })
    : ctrl.sendNewPiece(uciOrDrop.role, uciOrDrop.key, premove);

function anyClock(ctrl: RoundController, position: TopOrBottom): VNode | undefined {
  const d = ctrl.data,
    player = ctrl.playerAt(position);
  return (
    (ctrl.clock && renderClock(ctrl.clock, player.color, position, _ => [])) ||
    (d.correspondence && renderCorresClock(ctrl.corresClock!, player.color, position, d.game.player))
  );
}

const renderMoves = (steps: Step[], style: nv.MoveStyle) =>
  steps.reduce<(string | VNode)[]>((res, s) => {
    const turn = s.ply & 1 ? `${plyToTurn(s.ply)}.` : '';
    const san = `${nv.renderSan(s.san, s.uci, style)}, `;
    return res.concat(`${turn} ${san}`).concat(s.ply % 2 === 0 ? hl('br') : []);
  }, []);

function playerHtml(ctrl: RoundController, player: Player) {
  if (player.ai) return i18n.site.aiNameLevelAiLevel('Stockfish', player.ai);
  const perf = ctrl.data.game.perf,
    user = player.user,
    rating = user?.perfs[perf]?.rating,
    rd = player.ratingDiff,
    ratingDiff = rd ? (rd > 0 ? '+' + rd : rd < 0 ? '−' + -rd : '') : '';
  return user
    ? hl('span', [
        hl(
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
    transGamePerf(d.game.perf),
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

const transGamePerf = (perf: string): string => (i18n.site[perf as keyof typeof i18n.site] as string) || perf;
