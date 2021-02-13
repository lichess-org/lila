import { h } from 'snabbdom';
import { VNode } from 'snabbdom/vnode';
import { fixCrazySan } from 'chess';
import { defined } from 'common';
import { view as cevalView, renderEval as normalizeEval } from 'ceval';

export interface Ctx {
  withDots?: boolean;
  showEval: boolean;
  showGlyphs?: boolean;
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
  return h('eval', e.replace('-', '−'));
}

export function renderIndexText(ply: Ply, withDots?: boolean): string {
  return plyToTurn(ply) + (withDots ? (ply % 2 === 1 ? '.' : '...') : '');
}

export function renderIndex(ply: Ply, withDots?: boolean): VNode {
  return h('index', renderIndexText(ply, withDots));
}

export function renderMove(ctx: Ctx, node: Tree.Node): VNode[] {
  const ev: any = cevalView.getBestEval({ client: node.ceval, server: node.eval }) || {};
  return [h('san', fixCrazySan(node.san!))]
    .concat(node.glyphs && ctx.showGlyphs ? renderGlyphs(node.glyphs) : [])
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

export function renderIndexAndMove(ctx: Ctx, node: Tree.Node): VNode[] | undefined {
  if (!node.san) return; // initial position
  return [renderIndex(node.ply, ctx.withDots), ...renderMove(ctx, node)];
}
