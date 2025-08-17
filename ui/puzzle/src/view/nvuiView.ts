import { h, type VNode } from 'snabbdom';
import { puzzleBox, renderDifficultyForm, userBox } from '../view/side';
import theme from '../view/theme';
import * as nv from 'lib/nvui/chess';
import { makeConfig } from '../view/chessground';
import { renderSetting } from 'lib/nvui/setting';
import type { PuzzleNvuiContext } from '../puzzle.nvui';
import { commands, boardCommands, addBreaks } from 'lib/nvui/command';
import { next as controlNext, prev } from '../control';
import { bind, onInsert } from 'lib/snabbdom';
import { throttle } from 'lib/async';
import type PuzzleCtrl from '../ctrl';
import { Chessground as makeChessground } from '@lichess-org/chessground';
import { opposite } from 'chessops';
import { scanDirectionsHandler } from 'lib/nvui/directionScan';
import { Api } from '@lichess-org/chessground/api';

const throttled = (sound: string) => throttle(100, () => site.sound.play(sound));
const selectSound = throttled('select');
const borderSound = throttled('outOfBound');
const errorSound = throttled('error');

export function renderNvui({
  ctrl,
  notify,
  moveStyle,
  pieceStyle,
  prefixStyle,
  positionStyle,
  boardStyle,
}: PuzzleNvuiContext): VNode {
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

  return h(
    `main.puzzle.puzzle--nvui.puzzle-${ctrl.data.replay ? 'replay' : 'play'}${
      ctrl.streak ? '.puzzle--streak' : ''
    }`,
    h('div.nvui', [
      h('h2', 'Puzzle info'),
      puzzleBox(ctrl),
      theme(ctrl),
      !ctrl.streak && userBox(ctrl),
      h('h2', 'Moves'),
      h(
        'p.moves',
        { attrs: { role: 'log', 'aria-live': 'off' } },
        nv.renderMainline(ctrl.mainline, ctrl.path, moveStyle.get()),
      ),
      h('h2', 'Pieces'),
      h('div.pieces', nv.renderPieces(ground.state.pieces, moveStyle.get())),
      h('h2', 'Puzzle status'),
      h(
        'div.status',
        { attrs: { role: 'status', 'aria-live': 'polite', 'aria-atomic': 'true' } },
        renderStatus(ctrl),
      ),
      ctrl.data.replay && h('div.replay', renderReplay(ctrl)),
      ctrl.streak && renderStreak(ctrl),
      h('h2', 'Last move'),
      h(
        'p.lastMove',
        { attrs: { 'aria-live': 'assertive', 'aria-atomic': 'true' } },
        lastMove(ctrl, moveStyle.get()),
      ),
      h('h2', 'Move form'),
      h(
        'form#move-form',
        {
          hook: onInsert(el => {
            const $form = $(el),
              $input = $form.find('.move').val('');
            $form.on('submit', onSubmit(ctrl, notify.set, moveStyle.get, $input, ground));
          }),
        },
        [
          h('label', [
            ctrl.mode === 'view'
              ? 'Command input'
              : `${i18n.puzzle[ctrl.pov === 'white' ? 'findTheBestMoveForWhite' : 'findTheBestMoveForBlack']}`,
            h('input.move.mousetrap', {
              attrs: { name: 'move', type: 'text', autocomplete: 'off', autofocus: true },
            }),
          ]),
        ],
      ),
      notify.render(),
      h('h2', 'Actions'),
      ctrl.mode === 'view' ? afterActions(ctrl) : playActions(ctrl),
      h('h2', 'Board'),
      h(
        'div.board',
        {
          hook: {
            insert: el =>
              boardEventsHook(
                {
                  ctrl,
                  notify,
                  moveStyle,
                  pieceStyle,
                  prefixStyle,
                  positionStyle,
                  boardStyle,
                },
                ground,
                el.elm as HTMLElement,
              ),
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
      h('div.boardstatus', { attrs: { 'aria-live': 'polite', 'aria-atomic': 'true' } }, ''),
      h('h2', i18n.site.advancedSettings),
      h('label', ['Move notation', renderSetting(moveStyle, ctrl.redraw)]),
      h('h3', 'Board settings'),
      h('label', ['Piece style', renderSetting(pieceStyle, ctrl.redraw)]),
      h('label', ['Piece prefix style', renderSetting(prefixStyle, ctrl.redraw)]),
      h('label', ['Show position', renderSetting(positionStyle, ctrl.redraw)]),
      h('label', ['Board layout', renderSetting(boardStyle, ctrl.redraw)]),
      ...(!ctrl.data.replay && !ctrl.streak ? [h('h3', 'Puzzle Settings'), renderDifficultyForm(ctrl)] : []),
      h('h2', i18n.site.keyboardShortcuts),
      h('p', [
        `Left and right arrow keys: ${i18n.site.keyMoveBackwardOrForward}`,
        h('br'),
        `Up and down arrow keys, or 0 and $, or home and end: ${i18n.site.keyGoToStartOrEnd}`,
      ]),
      h('h2', 'Commands'),
      h(
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
      h('h2', 'Promotion'),
      h('p', [
        'Standard PGN notation selects the piece to promote to. Example: a8=n promotes to a knight.',
        h('br'),
        'Omission results in promotion to queen',
      ]),
    ]),
  );
}

function boardEventsHook(ctx: PuzzleNvuiContext, ground: Api, el: HTMLElement): void {
  const { ctrl, moveStyle, pieceStyle, prefixStyle, notify } = ctx;
  const $board = $(el);
  const $buttons = $board.find('button');
  const steps = ctrl.tree.getNodeList(ctrl.path);
  const fenSteps = () => steps.map(step => step.fen);
  const opponentColor = opposite(ctrl.pov);

  $buttons.on('blur', nv.leaveSquareHandler($buttons));
  $buttons.on(
    'click',
    nv.selectionHandler(() => opponentColor),
  );
  $buttons.on('keydown', (e: KeyboardEvent) => {
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
      nv.renderSan(node.san || '', node.uci, style) + (node.ply % 2 === 0 ? '' : ' ');
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

const isInSolution = (node?: Tree.Node): boolean =>
  !!node && (node.puzzle === 'good' || node.puzzle === 'win');

const nextNode = (node?: Tree.Node): Tree.Node | undefined =>
  node?.children?.length ? node.children[0] : undefined;

const renderStreak = (ctrl: PuzzleCtrl): VNode[] =>
  !ctrl.streak
    ? []
    : [h('h2', 'Puzzle streak'), h('p', ctrl.streak.data.index || i18n.puzzle.streakDescription)];

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

const playActions = (ctrl: PuzzleCtrl): VNode =>
  ctrl.streak
    ? button(i18n.storm.skip, ctrl.skip, i18n.puzzle.streakSkipExplanation, !ctrl.streak.data.skip)
    : h('div.actions_play', button(i18n.site.viewTheSolution, ctrl.viewSolution));

const afterActions = (ctrl: PuzzleCtrl): VNode =>
  h(
    'div.actions_after',
    ctrl.streak && ctrl.lastFeedback === 'win'
      ? h('a', { attrs: { href: '/streak' } }, i18n.puzzle.newStreak)
      : [...renderVote(ctrl), button(i18n.puzzle.continueTraining, ctrl.nextPuzzle)],
  );

const renderVoteTutorial = (ctrl: PuzzleCtrl): VNode[] =>
  ctrl.session.isNew() && ctrl.data.user?.provisional
    ? [h('p', i18n.puzzle.didYouLikeThisPuzzle), h('p', i18n.puzzle.voteToLoadNextOne)]
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
  h(
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
