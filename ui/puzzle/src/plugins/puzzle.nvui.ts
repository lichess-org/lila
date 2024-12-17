import { h, type VNode } from 'snabbdom';
import { puzzleBox, renderDifficultyForm, userBox } from '../view/side';
import theme from '../view/theme';
import {
  arrowKeyHandler,
  boardCommandsHandler,
  boardSetting,
  castlingFlavours,
  inputToLegalUci,
  lastCapturedCommandHandler,
  pieceJumpingHandler,
  pieceSetting,
  positionJumpHandler,
  positionSetting,
  possibleMovesHandler,
  prefixSetting,
  renderBoard,
  renderMainline,
  renderPieces,
  renderSan,
  selectionHandler,
  styleSetting,
  type MoveStyle,
} from 'nvui/chess';
import { makeConfig } from '../view/chessground';
import { renderSetting } from 'nvui/setting';
import { Notify } from 'nvui/notify';
import { commands } from 'nvui/command';
import { next as controlNext } from '../control';
import { bind, onInsert } from 'common/snabbdom';
import { throttle } from 'common/timing';
import type PuzzleCtrl from '../ctrl';
import { Chessground as makeChessground } from 'chessground';
import { defined } from 'common';
import { opposite } from 'chessops';

const throttled = (sound: string) => throttle(100, () => site.sound.play(sound));
const selectSound = throttled('select');
const borderSound = throttled('outOfBound');
const errorSound = throttled('error');

export function initModule() {
  const notify = new Notify(),
    moveStyle = styleSetting(),
    prefixStyle = prefixSetting(),
    pieceStyle = pieceSetting(),
    positionStyle = positionSetting(),
    boardStyle = boardSetting();
  return {
    render(ctrl: PuzzleCtrl): VNode {
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
          h('h1', `Puzzle: ${ctrl.pov} to play.`),
          h('h2', 'Puzzle info'),
          puzzleBox(ctrl),
          theme(ctrl),
          !ctrl.streak && userBox(ctrl),
          h('h2', 'Moves'),
          h(
            'p.moves',
            { attrs: { role: 'log', 'aria-live': 'off' } },
            renderMainline(ctrl.mainline, ctrl.path, moveStyle.get()),
          ),
          h('h2', 'Pieces'),
          h('div.pieces', renderPieces(ground.state.pieces, moveStyle.get())),
          h('h2', 'Puzzle status'),
          h(
            'div.status',
            { attrs: { role: 'status', 'aria-live': 'polite', 'aria-atomic': 'true' } },
            renderStatus(ctrl),
          ),
          h('div.replay', renderReplay(ctrl)),
          ...(ctrl.streak ? renderStreak(ctrl) : []),
          h('h2', 'Last move'),
          h(
            'p.lastMove',
            { attrs: { 'aria-live': 'assertive', 'aria-atomic': 'true' } },
            lastMove(ctrl, moveStyle.get()),
          ),
          h('h2', 'Move form'),
          h(
            'form',
            {
              hook: onInsert(el => {
                const $form = $(el),
                  $input = $form.find('.move').val('');
                $input[0]?.focus();
                $form.on('submit', onSubmit(ctrl, notify.set, moveStyle.get, $input, ground));
              }),
            },
            [
              h('label', [
                ctrl.mode === 'view' ? 'Command input' : `Find the best move for ${ctrl.pov}.`,
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
              hook: onInsert(el => {
                const $board = $(el);
                const $buttons = $board.find('button');
                const steps = ctrl.tree.getNodeList(ctrl.path);
                const uciSteps = () => steps.filter((s): s is StepWithUci => defined(s.uci));
                const fenSteps = () => steps.map(step => step.fen);
                const opponentColor = opposite(ctrl.pov);
                $board.on(
                  'click',
                  selectionHandler(() => opponentColor, selectSound),
                );
                $board.on('keydown', arrowKeyHandler(ctrl.pov, borderSound));
                $board.on('keypress', boardCommandsHandler());
                $buttons.on(
                  'keypress',
                  lastCapturedCommandHandler(fenSteps, pieceStyle.get(), prefixStyle.get()),
                );
                $buttons.on(
                  'keypress',
                  possibleMovesHandler(
                    ctrl.pov,
                    () => ground.state.turnColor,
                    ground.getFen,
                    () => ground.state.pieces,
                    'standard',
                    () => ground.state.movable.dests,
                    uciSteps,
                  ),
                );
                $buttons.on('keypress', positionJumpHandler());
                $buttons.on('keypress', pieceJumpingHandler(selectSound, errorSound));
              }),
            },
            renderBoard(
              ground.state.pieces,
              ctrl.pov,
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
          ...(!ctrl.data.replay && !ctrl.streak
            ? [h('h3', 'Puzzle Settings'), renderDifficultyForm(ctrl)]
            : []),
          h('h2', 'Keyboard shortcuts'),
          h('p', [
            'Left and right arrow keys or k and j: Navigate to the previous or next move.',
            h('br'),
            'Up and down arrow keys, or 0 and $, or home and end: Jump to the first or last move.',
          ]),
          h('h2', 'Commands'),
          h('p', [
            'Type these commands in the move input.',
            h('br'),
            'v: View the solution.',
            h('br'),
            'l: Read last move.',
            h('br'),
            commands.piece.help,
            h('br'),
            commands.scan.help,
            h('br'),
          ]),
          h('h2', 'Board mode commands'),
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
        ]),
      );
    },
  };
}

interface StepWithUci extends Tree.Node {
  uci: Uci;
}

function lastMove(ctrl: PuzzleCtrl, style: MoveStyle): string {
  const node = ctrl.node;
  return node.ply === 0
    ? 'Initial position'
    : // make sure consecutive moves are different so that they get re-read
      renderSan(node.san || '', node.uci, style) + (node.ply % 2 === 0 ? '' : ' ');
}

function onSubmit(
  ctrl: PuzzleCtrl,
  notify: (txt: string) => void,
  style: () => MoveStyle,
  $input: Cash,
  ground: CgApi,
): (ev: SubmitEvent) => void {
  return (ev: SubmitEvent) => {
    ev.preventDefault();
    let input = castlingFlavours(($input.val() as string).trim());
    if (isShortCommand(input)) input = '/' + input;
    if (input[0] === '/') onCommand(ctrl, notify, input.slice(1), style());
    else {
      const uci = inputToLegalUci(input, ctrl.node.fen, ground);
      if (uci) {
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
  ctrl.mode !== 'view' && !isYourMove(ctrl) ? ['You browsed away from the latest position.'] : [];

const shortCommands = ['l', 'last', 'p', 's', 'v'];

const isShortCommand = (input: string): boolean => shortCommands.includes(input.split(' ')[0].toLowerCase());

function onCommand(ctrl: PuzzleCtrl, notify: (txt: string) => void, c: string, style: MoveStyle): void {
  const lowered = c.toLowerCase();
  const pieces = ctrl.ground().state.pieces;
  if (lowered === 'l' || lowered === 'last') notify($('.lastMove').text());
  else if (lowered === 'v') viewOrAdvanceSolution(ctrl, notify);
  else
    notify(
      commands.piece.apply(c, pieces, style) ||
        commands.scan.apply(c, pieces, style) ||
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
    } else if (isInSolution(node)) notify('Puzzle complete!');
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
  else if (ctrl.streak) return `GAME OVER. Your streak: ${ctrl.streak.data.index}`;
  else if (ctrl.lastFeedback === 'win') return 'Puzzle solved!';
  else return 'Puzzle complete.';
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
    : h('div.actions_play', button('View the solution', ctrl.viewSolution));

const afterActions = (ctrl: PuzzleCtrl): VNode =>
  h(
    'div.actions_after',
    ctrl.streak && ctrl.lastFeedback === 'win'
      ? anchor(i18n.puzzle.newStreak, '/streak')
      : [...renderVote(ctrl), button('Continue training', ctrl.nextPuzzle)],
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
        button('Thumbs up', () => ctrl.vote(true), undefined, ctrl.voteDisabled),
        button('Thumbs down', () => ctrl.vote(false), undefined, ctrl.voteDisabled),
      ];

const anchor = (text: string, href: string): VNode => h('a', { attrs: { href } }, text);

const button = (text: string, action: (e: Event) => void, title?: string, disabled?: boolean): VNode =>
  h(
    'button',
    { hook: bind('click', action), attrs: { ...(title ? { title } : {}), disabled: !!disabled } },
    text,
  );
