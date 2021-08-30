import { h } from 'snabbdom';
import { VNode } from 'snabbdom/vnode';
import { defined } from 'common';
import { view as cevalView, renderEval as normalizeEval } from 'ceval';
import { notationStyle } from 'common/notation';
import { renderTime } from './clocks';

export interface Ctx {
  notation: number;
  withDots?: boolean;
  showEval: boolean;
  showGlyphs?: boolean;
  offset?: number; // mainly to show handicaps starting at move 1
}

export function plyToTurn(ply: Ply): number {
  return Math.floor((ply - 1) / 2) + 1;
}

export function renderGlyphs(glyphs: Tree.Glyph[]): VNode[] {
  return glyphs.map(glyph =>
    h(
      'glyph',
      {
        attrs: { title: glyph.name },
      },
      glyph.symbol
    )
  );
}

function renderEval(e): VNode {
  return h('eval', e);
}

export function renderIndexText(ply: Ply, offset?: number, withDots?: boolean): string {
  return ply - ((offset ?? 0) % 2) + (withDots ? '.' : '');
}

export function renderIndex(ply: Ply, offset?: number, withDots?: boolean): VNode {
  return h('index', renderIndexText(ply, offset, withDots));
}

export function renderMove(ctx: Ctx, node: Tree.Node, moveTime?: number): VNode[] {
  const ev: any = cevalView.getBestEval({ client: node.ceval, server: node.eval }) || {};
  return [
    h(
      'san',
      notationStyle(ctx.notation)({
        san: node.san!,
        uci: node.uci!,
        fen: node.fen,
      })
    ),
  ]
    .concat(node.glyphs && ctx.showGlyphs ? renderGlyphs(node.glyphs) : [])
    .concat(defined(moveTime) ? [h('movetime', renderTime(moveTime, false))] : [])
    .concat(
      ctx.showEval
        ? defined(ev.cp)
          ? [renderEval(normalizeEval(ev.cp))]
          : defined(ev.mate)
          ? [renderEval('#' + ev.mate)]
          : []
        : []
    );
}

export function renderIndexAndMove(ctx: Ctx, node: Tree.Node, moveTime?: number): VNode[] | undefined {
  if (!node.san) return; // initial position
  return [renderIndex(node.ply, ctx.offset, ctx.withDots), ...renderMove(ctx, node, moveTime)];
}
