import { h, VNode } from 'snabbdom';
import { Controller, Redraw } from '../interfaces';
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
import { Chessground } from 'chessground';
import { makeConfig } from '../view/chessground';
import { renderSetting } from 'nvui/setting';
import { Notify } from 'nvui/notify';
import { commands } from 'nvui/command';
import * as control from '../control';
import { bind, onInsert } from '../util';
import { Api } from 'chessground/api';
import throttle from 'common/throttle';

const throttled = (sound: string) => throttle(100, () => lichess.sound.play(sound));

const selectSound = throttled('select');
const wrapSound = throttled('wrapAround');
const borderSound = throttled('outOfBound');
const errorSound = throttled('error');

lichess.PuzzleNVUI = function (redraw: Redraw) {
  const notify = new Notify(redraw),
    moveStyle = styleSetting(),
    prefixStyle = prefixSetting(),
    pieceStyle = pieceSetting(),
    positionStyle = positionSetting(),
    boardStyle = boardSetting();

  return {
    render(ctrl: Controller): VNode {
      const ground = ctrl.ground() || createGround(ctrl);

      return h(
        `main.puzzle.puzzle-${ctrl.getData().replay ? 'replay' : 'play'}${ctrl.streak ? '.puzzle--streak' : ''}`,
        h('div.nvui', [
          h('h1', `Puzzle: ${ctrl.vm.pov} to play.`),
          h('h2', 'Puzzle info'),
          puzzleBox(ctrl),
          theme(ctrl),
          !ctrl.streak ? userBox(ctrl) : null,
          h('h2', 'Moves'),
          h(
            'p.moves',
            {
              attrs: {
                role: 'log',
                'aria-live': 'off',
              },
            },
            renderMainline(ctrl.vm.mainline, ctrl.vm.path, moveStyle.get())
          ),
          h('h2', 'Pieces'),
          h('div.pieces', renderPieces(ground.state.pieces, moveStyle.get())),
          h('h2', 'Puzzle status'),
          h(
            'div.status',
            {
              attrs: {
                role: 'status',
                'aria-live': 'polite',
                'aria-atomic': 'true',
              },
            },
            renderStatus(ctrl)
          ),
          h('div.replay', renderReplay(ctrl)),
          ...(ctrl.streak ? renderStreak(ctrl) : []),
          h('h2', 'Last move'),
          h(
            'p.lastMove',
            {
              attrs: {
                'aria-live': 'assertive',
                'aria-atomic': 'true',
              },
            },
            lastMove(ctrl, moveStyle.get())
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
                ctrl.vm.mode === 'view' ? 'Command input' : `Find the best move for ${ctrl.vm.pov}.`,
                h('input.move.mousetrap', {
                  attrs: {
                    name: 'move',
                    type: 'text',
                    autocomplete: 'off',
                    autofocus: true,
                  },
                }),
              ]),
            ]
          ),
          notify.render(),
          h('h2', 'Actions'),
          ctrl.vm.mode === 'view' ? afterActions(ctrl) : playActions(ctrl),
          h('h2', 'Board'),
          h(
            'div.board',
            {
              hook: onInsert(el => {
                const $board = $(el);
                const $buttons = $board.find('button');
                const steps = () => ctrl.getTree().getNodeList(ctrl.vm.path);
                const uciSteps = () => steps().filter(hasUci);
                const fenSteps = () => steps().map(step => step.fen);
                const opponentColor = ctrl.vm.pov === 'white' ? 'black' : 'white';
                $board.on(
                  'click',
                  selectionHandler(() => opponentColor, selectSound)
                );
                $board.on('keypress', arrowKeyHandler(ctrl.vm.pov, borderSound));
                $board.on('keypress', boardCommandsHandler());
                $buttons.on('keypress', lastCapturedCommandHandler(fenSteps, pieceStyle.get(), prefixStyle.get()));
                $buttons.on(
                  'keypress',
                  possibleMovesHandler(
                    ctrl.vm.pov,
                    () => ground.state.turnColor,
                    ground.getFen,
                    () => ground.state.pieces,
                    'standard',
                    () => ground.state.movable.dests,
                    uciSteps
                  )
                );
                $buttons.on('keypress', positionJumpHandler());
                $buttons.on('keypress', pieceJumpingHandler(wrapSound, errorSound));
              }),
            },
            renderBoard(
              ground.state.pieces,
              ctrl.vm.pov,
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
          h('h2', 'Settings'),
          h('label', ['Move notation', renderSetting(moveStyle, ctrl.redraw)]),
          h('h3', 'Board Settings'),
          h('label', ['Piece style', renderSetting(pieceStyle, ctrl.redraw)]),
          h('label', ['Piece prefix style', renderSetting(prefixStyle, ctrl.redraw)]),
          h('label', ['Show position', renderSetting(positionStyle, ctrl.redraw)]),
          h('label', ['Board layout', renderSetting(boardStyle, ctrl.redraw)]),
          ...(!ctrl.getData().replay && !ctrl.streak && ctrl.difficulty
            ? [h('h3', 'Puzzle Settings'), renderDifficultyForm(ctrl)]
            : []),
          h('h2', 'Keyboard shortcuts'),
          h('p', [
            'Left and right arrow keys or k and j: Navigate to the previous or next move.',
            h('br'),
            'Up and down arrow keys or 0 and $: Jump to the first or last move.',
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
        ])
      );
    },
  };
};

interface StepWithUci extends Tree.Node {
  uci: Uci;
}

function hasUci(step: Tree.Node): step is StepWithUci {
  return step.uci !== undefined;
}

function lastMove(ctrl: Controller, style: Style): string {
  const node = ctrl.vm.node;
  if (node.ply === 0) return 'Initial position';
  // make sure consecutive moves are different so that they get re-read
  return renderSan(node.san || '', node.uci, style) + (node.ply % 2 === 0 ? '' : ' ');
}

function createGround(ctrl: Controller): Api {
  const ground = Chessground(document.createElement('div'), {
    ...makeConfig(ctrl),
    animation: { enabled: false },
    drawable: { enabled: false },
    coordinates: false,
  });
  ctrl.ground(ground);
  return ground;
}

function onSubmit(
  ctrl: Controller,
  notify: (txt: string) => void,
  style: () => Style,
  $input: Cash,
  ground: Api
): () => false {
  return () => {
    let input = castlingFlavours(($input.val() as string).trim());
    if (isShortCommand(input)) input = '/' + input;
    if (input[0] === '/') onCommand(ctrl, notify, input.slice(1), style());
    else {
      const uci = inputToLegalUci(input, ctrl.vm.node.fen, ground);
      if (uci) {
        ctrl.playUci(uci);
        if (ctrl.vm.lastFeedback === 'fail') notify("That's not the move!");
        else if (ctrl.vm.lastFeedback === 'win') notify('Success!');
      } else {
        notify([`Invalid move: ${input}`, ...browseHint(ctrl)].join('. '));
      }
    }
    $input.val('');
    return false;
  };
}

function isYourMove(ctrl: Controller) {
  return ctrl.vm.node.children.length === 0 || ctrl.vm.node.children[0].puzzle === 'fail';
}

function browseHint(ctrl: Controller): string[] {
  if (ctrl.vm.mode !== 'view' && !isYourMove(ctrl)) return ['You browsed away from the latest position.'];
  else return [];
}

const shortCommands = ['l', 'last', 'p', 's', 'v'];

function isShortCommand(input: string): boolean {
  return shortCommands.includes(input.split(' ')[0].toLowerCase());
}

function onCommand(ctrl: Controller, notify: (txt: string) => void, c: string, style: Style): void {
  const lowered = c.toLowerCase();
  const pieces = ctrl.ground()!.state.pieces;
  if (lowered === 'l' || lowered === 'last') notify($('.lastMove').text());
  else if (lowered === 'v') viewOrAdvanceSolution(ctrl, notify);
  else
    notify(commands.piece.apply(c, pieces, style) || commands.scan.apply(c, pieces, style) || `Invalid command: ${c}`);
}

function viewOrAdvanceSolution(ctrl: Controller, notify: (txt: string) => void): void {
  if (ctrl.vm.mode === 'view') {
    const node = ctrl.vm.node,
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

function renderStreak(ctrl: Controller): VNode[] {
  if (!ctrl.streak) return [];
  return [h('h2', 'Puzzle streak'), h('p', ctrl.streak.data.index || ctrl.trans.noarg('streakDescription'))];
}

function renderStatus(ctrl: Controller): string {
  if (ctrl.vm.mode !== 'view') return 'Solving';
  else if (ctrl.streak) return `GAME OVER. Your streak: ${ctrl.streak.data.index}`;
  else if (ctrl.vm.lastFeedback === 'win') return 'Puzzle solved!';
  else return 'Puzzle complete.';
}

function renderReplay(ctrl: Controller): string {
  const replay = ctrl.getData().replay;
  if (!replay) return '';
  const i = replay.i + (ctrl.vm.mode === 'play' ? 0 : 1);
  return `Replaying ${ctrl.trans.noarg(ctrl.getData().theme.key)} puzzles: ${i} of ${replay.of}`;
}

function playActions(ctrl: Controller): VNode {
  if (ctrl.streak)
    return button(
      ctrl.trans.noarg('skip'),
      ctrl.skip,
      ctrl.trans.noarg('streakSkipExplanation'),
      !ctrl.streak.data.skip
    );
  else return h('div.actions_play', button('View the solution', ctrl.viewSolution));
}

function afterActions(ctrl: Controller): VNode {
  const win = ctrl.vm.lastFeedback === 'win';
  return h(
    'div.actions_after',
    ctrl.streak && !win
      ? anchor(ctrl.trans.noarg('newStreak'), '/streak')
      : [...renderVote(ctrl), button('Continue training', ctrl.nextPuzzle)]
  );
}

const renderVoteTutorial = (ctrl: Controller): VNode[] =>
  ctrl.session.isNew() && ctrl.getData().user?.provisional
    ? [h('p', ctrl.trans.noarg('didYouLikeThisPuzzle')), h('p', ctrl.trans.noarg('voteToLoadNextOne'))]
    : [];

function renderVote(ctrl: Controller): VNode[] {
  if (!ctrl.getData().user || ctrl.autoNexting()) return [];
  return [
    ...renderVoteTutorial(ctrl),
    button('Thumbs up', () => ctrl.vote(true), undefined, ctrl.vm.voteDisabled),
    button('Thumbs down', () => ctrl.vote(false), undefined, ctrl.vm.voteDisabled),
  ];
}

function anchor(text: string, href: string): VNode {
  return h(
    'a',
    {
      attrs: { href },
    },
    text
  );
}

function button(text: string, action: (e: Event) => void, title?: string, disabled?: boolean): VNode {
  return h(
    'button',
    {
      hook: bind('click', action),
      attrs: {
        ...(title ? { title } : {}),
        disabled: !!disabled,
      },
    },
    text
  );
}
