import { h, VNode } from 'snabbdom';
import { fixCrazySan } from 'chess';
import { defined } from 'common';
import { view as cevalView, renderEval as normalizeEval } from 'ceval';
import { plyToTurn } from '../util';

export interface Ctx {
  withDots?: boolean;
  showEval: boolean;
  showGlyphs?: boolean;
}

export const renderGlyph = (glyph: Tree.Glyph): VNode =>
  h('glyph', { attrs: { title: glyph.name } }, glyph.symbol);

const renderEval = (e: string): VNode => h('eval', e.replace('-', '−'));

export const renderIndexText = (ply: Ply, withDots?: boolean): string =>
  plyToTurn(ply) + (withDots ? (ply % 2 === 1 ? '.' : '...') : '');

export const renderIndex = (ply: Ply, withDots?: boolean): VNode =>
  h(`index.sbhint${ply}`, renderIndexText(ply, withDots));

export function renderMove(ctx: Ctx, node: Tree.Node): VNode[] {
  const ev = cevalView.getBestEval({ client: node.ceval, server: node.eval });
  const nodes = [h('san', fixCrazySan(node.san!))];
  if (node.glyphs && ctx.showGlyphs) node.glyphs.forEach(g => nodes.push(renderGlyph(g)));
  if (node.shapes?.length) nodes.push(h('shapes'));
  if (ev && ctx.showEval) {
    if (defined(ev.cp)) nodes.push(renderEval(normalizeEval(ev.cp)));
    else if (defined(ev.mate)) nodes.push(renderEval('#' + ev.mate));
  }
  return nodes;
}

export const renderIndexAndMove = (ctx: Ctx, node: Tree.Node): VNode[] | undefined =>
  node.san ? [renderIndex(node.ply, ctx.withDots), ...renderMove(ctx, node)] : undefined;
