import { Chessground as makeChessground } from '@lichess-org/chessground';
import type { Api } from '@lichess-org/chessground/api';
import type { DrawShape } from '@lichess-org/chessground/draw';
import { opposite, type SquareName } from 'chessops';

import { throttle } from 'lib/async';
import * as nv from 'lib/nvui/chess';
import { commands, addBreaks } from 'lib/nvui/command';
import { renderSetting } from 'lib/nvui/setting';
import { type VNode, bind, onInsert, hl } from 'lib/view';

import type { LearnCtrl } from '../ctrl';
import { hashHref, hashNavigate } from '../hashRouting';
import type { LearnNvuiContext } from '../learn.nvui';
import type { LevelCtrl } from '../levelCtrl';
import type { RunCtrl } from '../run/runCtrl';
import { categs } from '../stage/list';
import type { PromotionRole } from '../util';

const promotionByChar: Record<string, PromotionRole> = {
  q: 'queen',
  r: 'rook',
  b: 'bishop',
  n: 'knight',
};

const throttled = (sound: string) => throttle(100, () => site.sound.play(sound));
const selectSound = throttled('select');
const borderSound = throttled('outOfBound');
const errorSound = throttled('error');

export function renderNvui(ctx: LearnNvuiContext): VNode {
  const { ctrl } = ctx;
  ctx.notify.redraw = ctrl.redraw;
  return hl('main.learn.learn--nvui', [hl('div.nvui', ctrl.inStage() ? renderStage(ctx) : renderMap(ctrl))]);
}

function renderMap(ctrl: LearnCtrl): VNode[] {
  return [
    hl('h1', `${i18n.learn.learnChess} ${i18n.learn.byPlaying}`),
    ...categs.map(categ =>
      hl('section', [
        hl('h2', categ.name),
        hl(
          'ul',
          categ.stages.map(stage => {
            const [done, total] = ctrl.stageProgress(stage);
            const status = ctrl.isStageIdComplete(stage.id) ? 'complete' : `${done} / ${total}`;
            return hl('li', [
              hl('a', { attrs: { href: hashHref(stage.id) } }, `${stage.title} — ${stage.subtitle}`),
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

  if (runCtrl.stageCompleted()) return renderStageComplete(runCtrl);
  if (runCtrl.stageStarting()) return renderStageIntro(runCtrl, notify);

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
    hl(
      'h1',
      {
        key: `${stage.id}-${levelCtrl.blueprint.id}`,
        hook: onInsert(() => setTimeout(() => notify.set(levelCtrl.blueprint.goal), 100)),
      },
      `${stage.title} — ${stage.subtitle}`,
    ),
    hl('h2', 'Goal'),
    hl(
      'p.goal',
      { attrs: { role: 'status', 'aria-live': 'polite', 'aria-atomic': 'true' } },
      levelCtrl.blueprint.goal,
    ),
    ...renderHints(levelCtrl, moveStyle.get()),
    hl('h2', i18n.nvui.pieces),
    nv.renderPieces(ground.state.pieces, moveStyle.get(), levelCtrl.blueprint.color),
    ...renderStars(levelCtrl, moveStyle.get()),
    hl('h2', i18n.nvui.gameStatus),
    hl('div.status', { attrs: { 'aria-live': 'assertive', 'aria-atomic': 'true' } }, renderStatus(levelCtrl)),
    hl('h2', i18n.nvui.lastMove),
    hl(
      'p.lastMove',
      { attrs: { 'aria-live': 'assertive', 'aria-atomic': 'true' } },
      describeLastMove(ground, moveStyle.get()),
    ),
    hl('h2', i18n.nvui.inputForm),
    hl(
      'form#move-form',
      {
        hook: onInsert(el => {
          const $form = $(el);
          const $input = $form.find('.move').val('');
          $form.on('submit', onSubmit(runCtrl, notify.set, moveStyle.get, $input, ground));
        }),
      },
      [
        hl('label', [
          'Your move',
          hl('input.move.mousetrap', {
            attrs: { name: 'move', type: 'text', autocomplete: 'off', autofocus: true },
          }),
        ]),
      ],
    ),
    notify.render(),
    hl('h2', i18n.nvui.actions),
    hl('div.actions', [
      button(i18n.learn.retry, runCtrl.restart),
      runCtrl.getNext() ? button(i18n.learn.next, () => hashNavigate(runCtrl.getNext().id)) : null,
      button(i18n.learn.backToMenu, () => hashNavigate()),
    ]),
    hl('h2', 'Board'),
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
        levelCtrl.blueprint.color,
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
    hl('h2', 'Commands'),
    hl(
      'p',
      [
        'Type these commands in the move input.',
        'g: Read goal',
        'v: Read hints',
        's: Read stars',
        commands().piece.help,
        commands().scan.help,
      ].reduce(addBreaks, []),
    ),
    hl('h2', i18n.nvui.boardCommandList),
    hl('p', [
      `i: ${i18n.nvui.goToInputForm}`,
      ...[
        `g: Read goal`,
        `v: Read hints`,
        `s: Read stars`,
        `o: ${i18n.nvui.announceCurrentSquare}`,
        `m: ${i18n.nvui.announcePossibleMoves}`,
        `arrow keys: ${i18n.nvui.moveWithArrows}`,
        `k-q-r-b-n-p: ${i18n.nvui.moveToPieceByType}`,
        `1 to 8: ${i18n.nvui.moveToRank}`,
      ].reduce(addBreaks, []),
    ]),
  ];
}

function renderStageIntro(runCtrl: RunCtrl, notify: LearnNvuiContext['notify']): VNode[] {
  const { stage } = runCtrl;
  return [
    hl(
      'h1',
      { hook: onInsert(() => setTimeout(() => notify.set(stage.intro), 100)) },
      i18n.learn.stageX(stage.id),
    ),
    hl('p', stage.intro),
    hl('div.actions', [button(i18n.learn.letsGo, runCtrl.hideStartingPane)]),
  ];
}

function renderStageComplete(runCtrl: RunCtrl): VNode[] {
  const stage = runCtrl.stage;
  const next = runCtrl.getNext();
  return [
    hl(
      'h1',
      { attrs: { 'aria-live': 'assertive', 'aria-atomic': 'true' } },
      i18n.learn.stageXComplete(stage.id),
    ),
    hl('p', stage.complete),
    hl('div.actions', [
      next ? button(i18n.learn.nextX(next.title), () => hashNavigate(next.id)) : null,
      button(i18n.learn.backToMenu, () => hashNavigate()),
    ]),
  ];
}

function describeHintsText(levelCtrl: LevelCtrl, style: nv.MoveStyle): string {
  const shapes = levelCtrl.blueprint.shapes;
  if (!shapes?.length) return '';
  return shapes
    .map(shape => {
      const orig = nv.renderKey(shape.orig, style);
      if (!shape.dest) return orig;
      const dest = nv.renderKey(shape.dest, style);
      if (shape.brush === 'red') return `Threat: ${orig} to ${dest}`;
      if (shape.brush === 'blue') return `Cover: ${orig} to ${dest}`;
      return `${orig} to ${dest}`;
    })
    .join(', ');
}

function describeStarsText(levelCtrl: LevelCtrl, style: nv.MoveStyle): string {
  if (!levelCtrl.isAppleLevel()) return '';
  const keys = levelCtrl.items.appleKeys();
  const text = keys.length ? keys.map(k => nv.renderKey(k as Key, style)).join(', ') : i18n.site.none;
  return `${keys.length} remaining: ${text}`;
}

function renderHints(levelCtrl: LevelCtrl, style: nv.MoveStyle): VNode[] {
  const text = describeHintsText(levelCtrl, style);
  if (!text) return [];
  return [hl('h2', 'Hints'), hl('p', text)];
}

function renderStars(levelCtrl: LevelCtrl, style: nv.MoveStyle): VNode[] {
  const text = describeStarsText(levelCtrl, style);
  if (!text) return [];
  return [
    hl('h2', 'Stars'),
    hl('p.apples', { attrs: { 'aria-live': 'polite', 'aria-atomic': 'true' } }, text),
  ];
}

function renderStatus(levelCtrl: LevelCtrl): string {
  if (levelCtrl.vm.failed) return i18n.learn.puzzleFailed;
  if (levelCtrl.vm.completed) return 'Completed';
  return `${levelCtrl.vm.nbMoves} / ${levelCtrl.blueprint.nbMoves} moves played`;
}

function describeLastMove(ground: Api, style: nv.MoveStyle): string {
  const last = ground.state.lastMove;
  if (!last || last.length < 2) return i18n.nvui.gameStart;
  return `${nv.renderKey(last[0], style)} ${nv.renderKey(last[1], style)}`;
}

function onSubmit(
  runCtrl: RunCtrl,
  notify: (txt: string) => void,
  moveStyle: () => nv.MoveStyle,
  $input: Cash,
  ground: Api,
): (ev: SubmitEvent) => void {
  return (ev: SubmitEvent) => {
    ev.preventDefault();
    const raw = ($input.val() as string).trim();
    const input = nv.castlingFlavours(raw);
    if (!input) return;
    if (input.toLowerCase() === 'g') {
      notify(runCtrl.levelCtrl.blueprint.goal);
      $input.val('');
      return;
    }
    if (input.toLowerCase() === 'v') {
      notify(describeHintsText(runCtrl.levelCtrl, moveStyle()) || 'No hints');
      $input.val('');
      return;
    }
    if (input.toLowerCase() === 's') {
      notify(describeStarsText(runCtrl.levelCtrl, moveStyle()) || 'No stars');
      $input.val('');
      return;
    }
    const uci = nv.inputToMove(input, runCtrl.levelCtrl.chess.fen(), ground);
    if (typeof uci === 'string' && uci.length >= 4) {
      const orig = uci.slice(0, 2) as Key;
      const dest = uci.slice(2, 4) as Key;
      const promotionChar = uci.length === 5 ? uci[4] : undefined;
      // route through ground.move so capture detection, scenarios and completion hooks fire
      ground.move(orig, dest);
      if (promotionChar && runCtrl.levelCtrl.promotionCtrl.promoting) {
        // ground.move parked the move in promotionCtrl; resolve from input instead of the visual modal
        const role = promotionByChar[promotionChar] ?? 'queen';
        runCtrl.levelCtrl.promotionCtrl.finish(role);
      }
    } else {
      notify(`Invalid move: ${raw}`);
      errorSound();
    }
    $input.val('');
    selectSound();
  };
}

const button = (text: string, action: (e: Event) => void): VNode =>
  hl('button', { hook: bind('click', action) }, text);

function boardEventsHook(ctx: LearnNvuiContext, ground: Api, el: HTMLElement): void {
  const pov = ctx.ctrl.runCtrl.levelCtrl.blueprint.color;
  const opponentColor = () => opposite(pov);
  const fen = () => ctx.ctrl.runCtrl.levelCtrl.chess.fen();
  const $board = $(el);
  $board.off('.nvui');

  $board.on('blur.nvui', 'button', e => nv.leaveSquareHandler($board.find('button'))(e));
  $board.on('click.nvui', 'button', e => nv.selectionHandler(opponentColor)(e));
  $board.on('focus.nvui', 'button', (e: FocusEvent) => {
    const btn = e.currentTarget as HTMLButtonElement;
    const file = btn.getAttribute('file');
    const rank = btn.getAttribute('rank');
    if (file && rank) {
      const lc = ctx.ctrl.runCtrl.levelCtrl;
      const isStar = lc.isAppleLevel() && lc.items.appleKeys().includes(`${file}${rank}` as SquareName);
      $('.boardstatus').text(isStar ? 'star' : '');
    }
  });
  $board.on('keydown.nvui', 'button', (e: KeyboardEvent) => {
    if (e.key.startsWith('Arrow')) nv.arrowKeyHandler(pov, borderSound)(e);
    else if (e.key.match(/^[kqrbnp]$/i)) nv.pieceJumpingHandler(selectSound, errorSound)(e);
    else if (e.code.match(/^Digit([1-8])$/)) nv.positionJumpHandler()(e);
    else if (e.key === 'o') nv.boardCommandsHandler()(e);
    else if (e.key.toLowerCase() === 'm')
      nv.possibleMovesHandler(pov, ground, 'standard', [{ fen: fen() }])(e);
    else if (e.key.toLowerCase() === 'g') {
      e.preventDefault();
      ctx.notify.set(ctx.ctrl.runCtrl.levelCtrl.blueprint.goal);
    } else if (e.key.toLowerCase() === 'v') {
      e.preventDefault();
      ctx.notify.set(describeHintsText(ctx.ctrl.runCtrl.levelCtrl, ctx.moveStyle.get()) || 'No hints');
    } else if (e.key.toLowerCase() === 's') {
      e.preventDefault();
      ctx.notify.set(describeStarsText(ctx.ctrl.runCtrl.levelCtrl, ctx.moveStyle.get()) || 'No stars');
    } else if (e.key === 'i') {
      e.preventDefault();
      $('input.move').get(0)?.focus();
    }
  });
}
