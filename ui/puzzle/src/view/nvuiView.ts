import { Chessground as makeChessground } from '@lichess-org/chessground';
import type { Api } from '@lichess-org/chessground/api';
import { makeSquare, opposite } from 'chessops';

import { throttle } from 'lib/async';
import * as nv from 'lib/nvui/chess';
import { commands, boardCommands, addBreaks } from 'lib/nvui/command';
import { scanDirectionsHandler } from 'lib/nvui/directionScan';
import { renderSetting } from 'lib/nvui/setting';
import type { TreeNode } from 'lib/tree/types';
import { type VNode, bind, onInsert, requiresI18n, hl, type LooseVNodes } from 'lib/view';

import { nextCorrectMove } from '@/moveTree';

import { next as controlNext, prev } from '../control';
import type PuzzleCtrl from '../ctrl';
import type { PuzzleNvuiContext } from '../puzzle.nvui';
import { makeConfig } from '../view/chessground';
import { puzzleBox, renderDifficultyForm, userBox } from '../view/side';
import theme from '../view/theme';

const throttled = (sound: string) => throttle(100, () => site.sound.play(sound));
const selectSound = throttled('select');
const borderSound = throttled('outOfBound');
const errorSound = throttled('error');

export function renderNvui(ctx: PuzzleNvuiContext): VNode {
  const {
    ctrl,
    notify,
    moveStyle,
    pieceStyle,
    prefixStyle,
    positionStyle,
    boardStyle,
    deviceType,
    pageStyle,
  } = ctx;
  notify.redraw = ctrl.redraw;
  const ground =
    ctrl.ground() ||
    makeChessground(document.createElement('div'), {
      ...makeConfig(ctrl),
      animation: { enabled: false },
      drawable: { enabled: false },
      coordinates: false,
    });
  ctrl.ground(ground);
  const boardFirst = deviceType.get() === 'touchscreen' && pageStyle.get() === 'board-actions';

  if (boardFirst) {
    pieceStyle.set('name');
    prefixStyle.set('name');
    boardStyle.set('plain');
  }

  return hl(
    `main.puzzle.puzzle--nvui.puzzle-${ctrl.data.replay ? 'replay' : 'play'}${
      ctrl.streak ? '.puzzle--streak' : ''
    }`,
    hl('div.nvui', [
      boardFirst && hl('h2', 'Board'),
      boardFirst &&
        hl(
          'div.board',
          {
            hook: {
              insert: el => boardEventsHook(ctx, ground, el.elm as HTMLElement),
              update: (_, vnode) => boardEventsHook(ctx, ground, vnode.elm as HTMLElement),
            },
          },

          nv.renderBoard(
            ground.state.pieces,
            ctrl.flipped() ? opposite(ctrl.pov) : ctrl.pov,
            pieceStyle.get(),
            prefixStyle.get(),
            positionStyle.get(),
            boardStyle.get(),
          ),
        ),
      boardFirst && renderTouchDeviceCommands(ctx),
      hl('h2', 'Puzzle info'),
      puzzleBox(ctrl),
      theme(ctrl),
      ctrl.streak ? undefined : userBox(ctrl),
      hl('h2', 'Moves'),
      hl(
        'p.moves',
        { attrs: { role: 'log', 'aria-live': 'off' } },
        nv.renderMainline(ctrl.mainline, ctrl.path, moveStyle.get()),
      ),
      hl('h2', 'Pieces'),
      nv.renderPieces(ground.state.pieces, moveStyle.get()),
      hl('h2', 'Puzzle status'),
      hl(
        'div.status',
        { attrs: { role: 'status', 'aria-live': 'polite', 'aria-atomic': 'true' } },
        renderStatus(ctrl),
      ),
      ctrl.data.replay && hl('div.replay', renderReplay(ctrl)),
      ctrl.streak && renderStreak(ctrl),
      hl('h2', 'Last move'),
      hl(
        'p.lastMove',
        { attrs: { 'aria-live': 'assertive', 'aria-atomic': 'true' } },
        lastMove(ctrl, moveStyle.get()),
      ),
      hl('h2', 'Move form'),
      hl(
        'form#move-form',
        {
          hook: onInsert(el => {
            const $form = $(el),
              $input = $form.find('.move').val('');
            $form.on('submit', onSubmit(ctrl, notify.set, moveStyle.get, $input, ground));
          }),
        },
        [
          hl('label', [
            ctrl.mode === 'view'
              ? 'Command input'
              : i18n.puzzle[ctrl.pov === 'white' ? 'findTheBestMoveForWhite' : 'findTheBestMoveForBlack'],
            hl('input.move.mousetrap', {
              attrs: { name: 'move', type: 'text', autocomplete: 'off', autofocus: true },
            }),
          ]),
        ],
      ),
      notify.render(),
      hl('h2', 'Actions'),
      ctrl.mode === 'view' ? afterActions(ctrl) : playActions({ ctrl, notify } as PuzzleNvuiContext),
      !boardFirst && hl('h2', 'Board'),
      !boardFirst &&
        hl(
          'div.board',
          {
            hook: {
              insert: el => boardEventsHook(ctx, ground, el.elm as HTMLElement),
              update: (_, vnode) => boardEventsHook(ctx, ground, vnode.elm as HTMLElement),
            },
          },

          nv.renderBoard(
            ground.state.pieces,
            ctrl.flipped() ? opposite(ctrl.pov) : ctrl.pov,
            pieceStyle.get(),
            prefixStyle.get(),
            positionStyle.get(),
            boardStyle.get(),
          ),
        ),
      hl('div.boardstatus', { attrs: { 'aria-live': 'polite', 'aria-atomic': 'true' } }, ''),
      hl('h2', i18n.site.advancedSettings),
      hl('label', ['Move notation', renderSetting(moveStyle, ctrl.redraw)]),
      hl('h3', 'Board settings'),
      hl('label', ['Piece style', renderSetting(pieceStyle, ctrl.redraw)]),
      hl('label', ['Piece prefix style', renderSetting(prefixStyle, ctrl.redraw)]),
      hl('label', ['Show position', renderSetting(positionStyle, ctrl.redraw)]),
      hl('label', ['Board layout', renderSetting(boardStyle, ctrl.redraw)]),
      ...(!ctrl.data.replay && !ctrl.streak ? [hl('h3', 'Puzzle Settings'), renderDifficultyForm(ctrl)] : []),
      hl('h2', i18n.site.keyboardShortcuts),
      hl('p', [
        `Left and right arrow keys: ${i18n.site.keyMoveBackwardOrForward}`,
        hl('br'),
        `Up and down arrow keys, or 0 and $, or home and end: ${i18n.site.keyGoToStartOrEnd}`,
      ]),
      hl('h2', 'Commands'),
      hl(
        'p',
        [
          'Type these commands in the move input.',
          `v: ${i18n.site.viewTheSolution}`,
          'l: Read last move.',
          commands().piece.help,
          commands().scan.help,
        ].reduce(addBreaks, []),
      ),
      ...boardCommands(),
      hl('h2', 'Promotion'),
      hl('p', [
        'Standard PGN notation selects the piece to promote to. Example: a8=n promotes to a knight.',
        hl('br'),
        'Omission results in promotion to queen',
      ]),
    ]),
  );
}

function renderTouchDeviceCommands(ctx: PuzzleNvuiContext): LooseVNodes {
  const { notify, ctrl } = ctx;
  return hl('div.actions', [
    hl(
      'button',
      {
        hook: bind('click', () => {
          const hint = nextCorrectMove(ctrl);
          if (hint) {
            notify.set(makeSquare(hint.from));
          }
        }),
      },
      i18n.site.getAHint,
    ),
    hl(
      'button',
      {
        hook: bind('click', () => ctrl.viewSolution()),
      },
      i18n.site.viewTheSolution,
    ),
  ]);
}

function boardEventsHook(ctx: PuzzleNvuiContext, ground: Api, el: HTMLElement): void {
  const { ctrl, moveStyle, pieceStyle, prefixStyle, notify } = ctx;
  const $board = $(el);
  // Remove old handlers before rebinding (important on re-render)
  $board.off('.nvui');
  const steps = ctrl.tree.getNodeList(ctrl.path);
  const fenSteps = () => steps.map(step => step.fen);

  $board.on('blur', 'button', e => nv.leaveSquareHandler($board.find('button'))(e));
  $board.on('click', 'button', e => nv.selectionHandler(() => opposite(ctrl.pov))(e));
  $board.on('keydown', 'button', (e: KeyboardEvent) => {
    if (e.shiftKey && e.key.match(/^[ad]$/i)) nextOrPrev(ctrl)(e);
    else if (e.key.match(/^x$/i))
      scanDirectionsHandler(
        ctrl.flipped() ? opposite(ctrl.pov) : ctrl.pov,
        ground.state.pieces,
        moveStyle.get(),
      )(e);
    else if (e.key.toLowerCase() === 'f') {
      notify.set('Flipping the board');
      setTimeout(() => ctrl.flip(), 1000);
    } else if (['o'].includes(e.key)) nv.boardCommandsHandler()(e);
    else if (e.key.startsWith('Arrow'))
      nv.arrowKeyHandler(ctrl.flipped() ? opposite(ctrl.pov) : ctrl.pov, borderSound)(e);
    else if (e.code.match(/^Digit([1-8])$/)) nv.positionJumpHandler()(e);
    else if (e.key.match(/^[kqrbnp]$/i)) nv.pieceJumpingHandler(selectSound, errorSound)(e);
    else if (e.key.toLowerCase() === 'm') nv.possibleMovesHandler(ctrl.pov, ground, 'standard', steps)(e);
    else if (e.key === 'c') nv.lastCapturedCommandHandler(fenSteps, pieceStyle.get(), prefixStyle.get())();
    else if (e.key === 'i') {
      e.preventDefault();
      $('input.move').get(0)?.focus();
    }
  });
}

function lastMove(ctrl: PuzzleCtrl, style: nv.MoveStyle): string {
  const node = ctrl.node;
  return node.ply === 0
    ? 'Initial position'
    : // make sure consecutive moves are different so that they get re-read
      nv.renderSan(node.san || '', node.uci, style) + (node.ply % 2 === 0 ? '' : '\u00A0');
}

function onSubmit(
  ctrl: PuzzleCtrl,
  notify: (txt: string) => void,
  style: () => nv.MoveStyle,
  $input: Cash,
  ground: CgApi,
): (ev: SubmitEvent) => void {
  return (ev: SubmitEvent) => {
    ev.preventDefault();
    let input = nv.castlingFlavours(($input.val() as string).trim());
    if (isShortCommand(input)) input = '/' + input;
    if (input[0] === '/') onCommand(ctrl, notify, input.slice(1), style());
    else {
      const uci = nv.inputToMove(input, ctrl.node.fen, ground);
      if (uci && typeof uci === 'string') {
        ctrl.playUci(uci);
        const fback = ctrl.lastFeedback;
        if (fback === 'fail') notify(i18n.puzzle.notTheMove);
        else if (fback === 'good') notify(i18n.puzzle.bestMove);
        else if (fback === 'win') notify(i18n.puzzle.puzzleSuccess);
      } else notify([`Invalid move: ${input}`, ...browseHint(ctrl)].join('. '));
    }
    $input.val('');
  };
}

const isYourMove = (ctrl: PuzzleCtrl): boolean =>
  ctrl.node.children.length === 0 || ctrl.node.children[0].puzzle === 'fail';

const browseHint = (ctrl: PuzzleCtrl): string[] =>
  ctrl.mode !== 'view' && !isYourMove(ctrl) ? [i18n.site.youBrowsedAway] : [];

const shortCommands = ['b', 'l', 'last', 'p', 's', 'v'];

const isShortCommand = (input: string): boolean => shortCommands.includes(input.split(' ')[0].toLowerCase());

function onCommand(ctrl: PuzzleCtrl, notify: (txt: string) => void, c: string, style: nv.MoveStyle): void {
  const lowered = c.toLowerCase();
  const pieces = ctrl.ground().state.pieces;
  if (lowered === 'l' || lowered === 'last') notify($('.lastMove').text());
  else if (lowered === 'v') viewOrAdvanceSolution(ctrl, notify);
  else if (lowered.charAt(0) === 'b') commands().board.apply(c, pieces, style);
  else
    notify(
      commands().piece.apply(c, pieces, style) ||
        commands().scan.apply(c, pieces, style) ||
        `Invalid command: ${c}`,
    );
}

function viewOrAdvanceSolution(ctrl: PuzzleCtrl, notify: (txt: string) => void): void {
  if (ctrl.mode === 'view') {
    const node = ctrl.node,
      next = nextNode(node),
      nextNext = nextNode(next);
    if (isInSolution(next) || (isInSolution(node) && isInSolution(nextNext))) {
      controlNext(ctrl);
      ctrl.redraw();
    } else if (isInSolution(node)) notify(i18n.puzzle.puzzleComplete);
    else ctrl.viewSolution();
  } else ctrl.viewSolution();
}

const isInSolution = (node?: TreeNode): boolean =>
  !!node && (node.puzzle === 'good' || node.puzzle === 'win');

const nextNode = (node?: TreeNode): TreeNode | undefined =>
  node?.children?.length ? node.children[0] : undefined;

const renderStreak = (ctrl: PuzzleCtrl): VNode[] =>
  !ctrl.streak
    ? []
    : [hl('h2', 'Puzzle streak'), hl('p', ctrl.streak.data.index || i18n.puzzle.streakDescription)];

function renderStatus(ctrl: PuzzleCtrl): string {
  if (ctrl.mode !== 'view') return 'Solving';
  else if (ctrl.streak) return `GAME OVER. ${i18n.puzzle.yourStreakX(ctrl.streak.data.index)}`;
  else if (ctrl.lastFeedback === 'win') return i18n.puzzle.puzzleSuccess;
  else return i18n.puzzle.puzzleComplete;
}

function renderReplay(ctrl: PuzzleCtrl): string {
  const replay = ctrl.data.replay;
  if (!replay) return '';
  const i = replay.i + (ctrl.mode === 'play' ? 0 : 1);
  const text = i18n.puzzleTheme[ctrl.data.angle.key];
  return `Replaying ${text} puzzles: ${i} of ${replay.of}`;
}

const playActions = (ctx: PuzzleNvuiContext): VNode => {
  const { ctrl, notify } = ctx;
  return ctrl.streak
    ? requiresI18n('storm', ctx.ctrl.redraw, cat =>
        button(cat.skip, ctrl.skip, i18n.puzzle.streakSkipExplanation, !ctrl.streak?.data.skip),
      )
    : hl('div.actions_play', [
        button(i18n.site.getAHint, () => {
          const hint = nextCorrectMove(ctrl);
          if (hint) {
            notify.set(makeSquare(hint.from));
          }
        }),
        button(i18n.site.viewTheSolution, ctrl.viewSolution),
      ]);
};

const afterActions = (ctrl: PuzzleCtrl): VNode =>
  hl(
    'div.actions_after',
    ctrl.streak && ctrl.lastFeedback === 'win'
      ? hl('a', { attrs: { href: '/streak' } }, i18n.puzzle.newStreak)
      : [...renderVote(ctrl), button(i18n.puzzle.continueTraining, ctrl.nextPuzzle)],
  );

const renderVoteTutorial = (ctrl: PuzzleCtrl): VNode[] =>
  ctrl.session.isNew() && ctrl.data.user?.provisional
    ? [hl('p', i18n.puzzle.didYouLikeThisPuzzle), hl('p', i18n.puzzle.voteToLoadNextOne)]
    : [];

const renderVote = (ctrl: PuzzleCtrl): VNode[] =>
  !ctrl.data.user || ctrl.autoNexting()
    ? []
    : [
        ...renderVoteTutorial(ctrl),
        button(i18n.puzzle.upVote, () => ctrl.vote(true), undefined, ctrl.voteDisabled),
        button(i18n.puzzle.downVote, () => ctrl.vote(false), undefined, ctrl.voteDisabled),
      ];

const button = (text: string, action: (e: Event) => void, title?: string, disabled?: boolean): VNode =>
  hl(
    'button',
    { hook: bind('click', action), attrs: { ...(title ? { title } : {}), disabled: !!disabled } },
    text,
  );

function nextOrPrev(ctrl: PuzzleCtrl) {
  return (e: KeyboardEvent) => {
    if (e.key === 'A') doAndRedraw(ctrl, prev);
    else if (e.key === 'D') doAndRedraw(ctrl, controlNext);
  };
}

const doAndRedraw = (ctrl: PuzzleCtrl, fn: (ctrl: PuzzleCtrl) => void): void => {
  fn(ctrl);
  ctrl.redraw();
};
