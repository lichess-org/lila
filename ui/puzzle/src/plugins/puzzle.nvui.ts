import { h, VNode } from 'snabbdom';
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
  Style,
  styleSetting,
} from 'nvui/chess';
import { makeConfig } from '../view/chessground';
import { renderSetting } from 'nvui/setting';
import { Notify } from 'nvui/notify';
import { commands } from 'nvui/command';
import * as control from '../control';
import { bind, onInsert } from 'common/snabbdom';
import { Api } from 'chessground/api';
import throttle from 'common/throttle';
import PuzzleCtrl from '../ctrl';

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
        site.makeChessground(document.createElement('div'), {
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
          !ctrl.streak ? userBox(ctrl) : null,
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
                $input[0]!.focus();
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
                const steps = () => ctrl.tree.getNodeList(ctrl.path);
                const uciSteps = () => steps().filter(hasUci);
                const fenSteps = () => steps().map(step => step.fen);
                const opponentColor = ctrl.pov === 'white' ? 'black' : 'white';
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

function hasUci(step: Tree.Node): step is StepWithUci {
  return step.uci !== undefined;
}

function lastMove(ctrl: PuzzleCtrl, style: Style): string {
  const node = ctrl.node;
  if (node.ply === 0) return 'Initial position';
  // make sure consecutive moves are different so that they get re-read
  return renderSan(node.san || '', node.uci, style) + (node.ply % 2 === 0 ? '' : ' ');
}

function onSubmit(
  ctrl: PuzzleCtrl,
  notify: (txt: string) => void,
  style: () => Style,
  $input: Cash,
  ground: Api,
): () => false {
  return () => {
    let input = castlingFlavours(($input.val() as string).trim());
    if (isShortCommand(input)) input = '/' + input;
    if (input[0] === '/') onCommand(ctrl, notify, input.slice(1), style());
    else {
      const uci = inputToLegalUci(input, ctrl.node.fen, ground);
      if (uci) {
        ctrl.playUci(uci);
        switch (ctrl.lastFeedback) {
          case 'fail':
            notify(ctrl.trans.noarg('notTheMove'));
            break;
          case 'good':
            notify(ctrl.trans.noarg('bestMove'));
            break;
          case 'win':
            notify(ctrl.trans.noarg('puzzleSuccess'));
        }
      } else {
        notify([`Invalid move: ${input}`, ...browseHint(ctrl)].join('. '));
      }
    }
    $input.val('');
    return false;
  };
}

function isYourMove(ctrl: PuzzleCtrl) {
  return ctrl.node.children.length === 0 || ctrl.node.children[0].puzzle === 'fail';
}

function browseHint(ctrl: PuzzleCtrl): string[] {
  if (ctrl.mode !== 'view' && !isYourMove(ctrl)) return ['You browsed away from the latest position.'];
  else return [];
}

const shortCommands = ['l', 'last', 'p', 's', 'v'];

function isShortCommand(input: string): boolean {
  return shortCommands.includes(input.split(' ')[0].toLowerCase());
}

function onCommand(ctrl: PuzzleCtrl, notify: (txt: string) => void, c: string, style: Style): void {
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
      control.next(ctrl);
      ctrl.redraw();
    } else if (isInSolution(node)) {
      notify('Puzzle complete!');
    } else {
      ctrl.viewSolution();
    }
  } else {
    ctrl.viewSolution();
  }
}

function isInSolution(node?: Tree.Node): boolean {
  return !!node && (node.puzzle === 'good' || node.puzzle === 'win');
}

function nextNode(node?: Tree.Node): Tree.Node | undefined {
  if (node?.children?.length) return node.children[0];
  else return;
}

function renderStreak(ctrl: PuzzleCtrl): VNode[] {
  if (!ctrl.streak) return [];
  return [h('h2', 'Puzzle streak'), h('p', ctrl.streak.data.index || ctrl.trans.noarg('streakDescription'))];
}

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
  return `Replaying ${ctrl.trans.noarg(ctrl.data.angle.key)} puzzles: ${i} of ${replay.of}`;
}

function playActions(ctrl: PuzzleCtrl): VNode {
  if (ctrl.streak)
    return button(
      ctrl.trans.noarg('skip'),
      ctrl.skip,
      ctrl.trans.noarg('streakSkipExplanation'),
      !ctrl.streak.data.skip,
    );
  else return h('div.actions_play', button('View the solution', ctrl.viewSolution));
}

function afterActions(ctrl: PuzzleCtrl): VNode {
  const win = ctrl.lastFeedback === 'win';
  return h(
    'div.actions_after',
    ctrl.streak && !win
      ? anchor(ctrl.trans.noarg('newStreak'), '/streak')
      : [...renderVote(ctrl), button('Continue training', ctrl.nextPuzzle)],
  );
}

const renderVoteTutorial = (ctrl: PuzzleCtrl): VNode[] =>
  ctrl.session.isNew() && ctrl.data.user?.provisional
    ? [h('p', ctrl.trans.noarg('didYouLikeThisPuzzle')), h('p', ctrl.trans.noarg('voteToLoadNextOne'))]
    : [];

function renderVote(ctrl: PuzzleCtrl): VNode[] {
  if (!ctrl.data.user || ctrl.autoNexting()) return [];
  return [
    ...renderVoteTutorial(ctrl),
    button('Thumbs up', () => ctrl.vote(true), undefined, ctrl.voteDisabled),
    button('Thumbs down', () => ctrl.vote(false), undefined, ctrl.voteDisabled),
  ];
}

function anchor(text: string, href: string): VNode {
  return h('a', { attrs: { href } }, text);
}

function button(text: string, action: (e: Event) => void, title?: string, disabled?: boolean): VNode {
  return h(
    'button',
    { hook: bind('click', action), attrs: { ...(title ? { title } : {}), disabled: !!disabled } },
    text,
  );
}
