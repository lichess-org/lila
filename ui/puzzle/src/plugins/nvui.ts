import { h, VNode } from 'snabbdom';
import { Controller, Redraw } from '../interfaces';
import { puzzleBox, userBox } from '../view/side';
import theme from '../view/theme';
import { castlingFlavours, inputToLegalUci, renderPieces, renderSan, Style, styleSetting } from 'nvui/chess';
import { Chessground } from 'chessground';
import { makeConfig } from '../view/chessground';
import { renderSetting } from 'nvui/setting';
import { Notify } from 'nvui/notify';
import { commands } from 'nvui/command';
import * as control from '../control';
import { bind, onInsert } from '../util';
import { Api } from 'chessground/api';

lichess.PuzzleNVUI = function (redraw: Redraw) {
  const notify = new Notify(redraw),
    moveStyle = styleSetting();

  return {
    render(ctrl: Controller): VNode {
      const ground = ctrl.ground() || createGround(ctrl),
        pieces = ground.state.pieces;

      return h(
        `main.puzzle.puzzle-${ctrl.getData().replay ? 'replay' : 'play'}${ctrl.streak ? '.puzzle--streak' : ''}`,
        [
          h('div.nvui', [
            h('h1', `You play the ${ctrl.vm.pov} pieces. ${ctrl.difficulty || ''} puzzle`),
            h('h2', 'Puzzle info'),
            puzzleBox(ctrl),
            theme(ctrl),
            !ctrl.streak ? userBox(ctrl) : null,
            h('h2', 'Pieces'),
            h('div.pieces', renderPieces(pieces, moveStyle.get())),
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
            ctrl.streak ? renderStreak(ctrl) : null,
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
                  ctrl.vm.pov === 'white' ? 'Find the best move for white.' : 'Find the best move for black.',
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
            h('h2', 'Settings'),
            h('label', ['Move notation', renderSetting(moveStyle, ctrl.redraw)]),
            h('h2', 'Keyboard shortcuts'),
            h('p', [
              'Left and right arrow keys or j and k: Navigate to the previous or the next move.',
              h('br'),
              'Up and down arrow keys or 0 and $: Navigate to the first or the last move.',
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
            h('h2', 'Promotion'),
            h('p', [
              'Standard PGN notation selects the piece to promote to. Example: a8=n promotes to a knight.',
              h('br'),
              'Omission results in promotion to queen',
            ]),
          ]),
        ]
      );
    },
  };
};

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

function renderStatus(ctrl: Controller) {
  if (ctrl.vm.mode !== 'view') return 'Your move.';
  else if (ctrl.streak) return `GAME OVER. Your streak: ${ctrl.streak.data.index}`;
  else if (ctrl.vm.lastFeedback === 'win') return 'Success!';
  else return 'Puzzle complete.';
}

function playActions(ctrl: Controller): VNode {
  if (ctrl.streak) return button(ctrl.trans.noarg('skip'), ctrl.skip, ctrl.trans.noarg('streakSkipExplanation'));
  else return h('div.actions_play', button('View the solution', ctrl.viewSolution));
}

function afterActions(ctrl: Controller): VNode {
  const win = ctrl.vm.lastFeedback === 'win';
  if (ctrl.streak && !win) return anchor(ctrl.trans.noarg('newStreak'), '/streak');
  else return h('div.actions_after', button('Continue training', ctrl.nextPuzzle));
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

function button(text: string, action: (e: Event) => void, title?: string): VNode {
  return h(
    'button',
    {
      hook: bind('click', action),
      attrs: title ? { title } : undefined,
    },
    text
  );
}
