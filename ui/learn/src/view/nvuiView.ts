import { h, type VNode } from 'snabbdom';
import { Chessground as makeChessground } from '@lichess-org/chessground';
import type { Api as CgApi } from '@lichess-org/chessground/api';
import { throttle } from 'lib/async';
import { bind, onInsert } from 'lib/view';
import * as nv from 'lib/nvui/chess';
import { renderSetting } from 'lib/nvui/setting';
import { commands, boardCommands, addBreaks } from 'lib/nvui/command';
import type { LearnNvuiContext } from '../learn.nvui';
import type { LearnCtrl } from '../ctrl';
import type { RunCtrl } from '../run/runCtrl';
import type { LevelCtrl } from '../levelCtrl';
import { categs } from '../stage/list';
import { hashHref } from '../hashRouting';

const throttled = (sound: string) => throttle(100, () => site.sound.play(sound));
const selectSound = throttled('select');
const errorSound = throttled('error');

export function renderNvui(ctx: LearnNvuiContext): VNode {
  const { ctrl } = ctx;
  ctx.notify.redraw = ctrl.redraw;
  return h('main.learn.learn--nvui', [
    h('div.nvui', ctrl.inStage() ? renderStage(ctx) : renderMap(ctrl)),
  ]);
}

function renderMap(ctrl: LearnCtrl): VNode[] {
  return [
    h('h1', `${i18n.learn.learnChess} ${i18n.learn.byPlaying}`),
    ...categs.map(categ =>
      h('section', [
        h('h2', categ.name),
        h(
          'ul',
          categ.stages.map(stage => {
            const [done, total] = ctrl.stageProgress(stage);
            const status = ctrl.isStageIdComplete(stage.id) ? 'complete' : `${done} / ${total}`;
            return h('li', [
              h('a', { attrs: { href: hashHref(stage.id) } }, `${stage.title} — ${stage.subtitle}`),
              ` (${status})`,
            ]);
          }),
        ),
      ]),
    ),
  ];
}

function renderStage(ctx: LearnNvuiContext): VNode[] {
  const { ctrl, notify, moveStyle, pieceStyle, prefixStyle, positionStyle, boardStyle } = ctx;
  const runCtrl = ctrl.runCtrl;
  const stage = runCtrl.stage;
  const levelCtrl = runCtrl.levelCtrl;

  // The visual view builds chessground via a snabbdom hook. We need a ground for renderBoard
  // even though no visual board is shown — mirror puzzle's offscreen ground.
  const ground =
    runCtrl.chessground ??
    makeChessground(document.createElement('div'), {
      fen: levelCtrl.chess.fen(),
      orientation: levelCtrl.blueprint.color,
      coordinates: false,
      animation: { enabled: false },
      drawable: { enabled: false },
    });
  if (!runCtrl.chessground) runCtrl.setChessground(ground);

  return [
    h('h1', `${stage.title} — ${stage.subtitle}`),
    h('h2', 'Goal'),
    h(
      'p.goal',
      { attrs: { role: 'status', 'aria-live': 'polite', 'aria-atomic': 'true' } },
      levelCtrl.blueprint.goal,
    ),
    h('h2', i18n.nvui.pieces),
    nv.renderPieces(ground.state.pieces, moveStyle.get()),
    h('h2', i18n.nvui.gameStatus),
    h(
      'div.status',
      { attrs: { role: 'status', 'aria-live': 'assertive', 'aria-atomic': 'true' } },
      renderStatus(levelCtrl),
    ),
    h('h2', i18n.nvui.lastMove),
    h(
      'p.lastMove',
      { attrs: { 'aria-live': 'assertive', 'aria-atomic': 'true' } },
      describeLastMove(ground, moveStyle.get()),
    ),
    h('h2', i18n.nvui.inputForm),
    h(
      'form#move-form',
      {
        hook: onInsert(el => {
          const $form = $(el);
          const $input = $form.find('.move').val('');
          $form.on('submit', onSubmit(runCtrl, notify.set, $input, ground));
        }),
      },
      [
        h('label', [
          'Your move',
          h('input.move.mousetrap', {
            attrs: { name: 'move', type: 'text', autocomplete: 'off', autofocus: true },
          }),
        ]),
      ],
    ),
    notify.render(),
    h('h2', i18n.nvui.actions),
    h('div.actions', [
      button(i18n.learn.retry, () => {
        runCtrl.restart();
      }),
      runCtrl.getNext()
        ? button(i18n.learn.next, () => {
            const next = runCtrl.getNext();
            if (next) location.hash = hashHref(next.id);
          })
        : null,
      button(i18n.learn.backToMenu, () => {
        location.hash = '';
      }),
    ]),
    h('h2', 'Board'),
    nv.renderBoard(
      ground.state.pieces,
      levelCtrl.blueprint.color,
      pieceStyle.get(),
      prefixStyle.get(),
      positionStyle.get(),
      boardStyle.get(),
    ),
    h('h2', i18n.site.advancedSettings),
    h('label', ['Move notation', renderSetting(moveStyle, ctrl.redraw)]),
    h('h3', 'Board settings'),
    h('label', ['Piece style', renderSetting(pieceStyle, ctrl.redraw)]),
    h('label', ['Piece prefix style', renderSetting(prefixStyle, ctrl.redraw)]),
    h('label', ['Show position', renderSetting(positionStyle, ctrl.redraw)]),
    h('label', ['Board layout', renderSetting(boardStyle, ctrl.redraw)]),
    h('h2', i18n.nvui.boardCommandList),
    h(
      'p',
      ['Type these commands in the move input.', commands().piece.help, commands().scan.help].reduce(
        addBreaks,
        [],
      ),
    ),
    ...boardCommands(),
  ];
}

function renderStatus(levelCtrl: LevelCtrl): string {
  if (levelCtrl.vm.failed) return i18n.learn.puzzleFailed;
  if (levelCtrl.vm.completed) return 'Completed';
  return `${levelCtrl.vm.nbMoves} / ${levelCtrl.blueprint.nbMoves} moves played`;
}

function describeLastMove(ground: CgApi, style: nv.MoveStyle): string {
  const last = ground.state.lastMove;
  if (!last || last.length < 2) return i18n.nvui.gameStart;
  return `${nv.renderKey(last[0] as Key, style)} ${nv.renderKey(last[1] as Key, style)}`;
}

function onSubmit(
  runCtrl: RunCtrl,
  notify: (txt: string) => void,
  $input: Cash,
  ground: CgApi,
): (ev: SubmitEvent) => void {
  return (ev: SubmitEvent) => {
    ev.preventDefault();
    const raw = ($input.val() as string).trim();
    const input = nv.castlingFlavours(raw);
    if (!input) return;
    const uci = nv.inputToMove(input, runCtrl.levelCtrl.chess.fen(), ground);
    if (typeof uci === 'string' && uci.length >= 4) {
      const orig = uci.slice(0, 2) as Key;
      const dest = uci.slice(2, 4) as Key;
      // Drive the move through chessground so all existing event hooks (capture detection,
      // scenarios, completion) fire exactly as they do for sighted users.
      ground.move(orig, dest);
    } else {
      notify(`Invalid move: ${raw}`);
      errorSound();
    }
    $input.val('');
    selectSound();
  };
}

const button = (text: string, action: (e: Event) => void): VNode =>
  h('button', { hook: bind('click', action) }, text);
