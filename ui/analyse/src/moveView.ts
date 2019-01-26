import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode'
import { fixCrazySan, renderEval as normalizeEval } from 'chess';
import { defined } from 'common';
import { view as cevalView } from 'ceval';

export interface Ctx {
  withDots?: boolean;
  showEval: boolean;
  showGlyphs?: boolean;
}

export function plyToTurn(ply: Ply): number {
  return Math.floor((ply - 1) / 2) + 1;
}

export function renderGlyphs(glyphs): VNode[] {
  return glyphs.map(glyph => h('glyph', {
    attrs: { title: glyph.name }
  }, glyph.symbol));
}

function renderEval(e): VNode {
  return h('eval', e);
}

export function renderIndexText(ply: Ply, withDots?: boolean): string {
  return plyToTurn(ply) + (withDots ? (ply % 2 === 1 ? '.' : '...') : '');
}

export function renderIndex(ply: Ply, withDots?: boolean): VNode {
  return h('index', renderIndexText(ply, withDots));
}

export function renderMove(ctx: Ctx, node: Tree.Node): VNode[] {
  const ev: any = cevalView.getBestEval({client: node.ceval, server: node.eval}) || {};
  return [h('san', fixCrazySan(node.san!))]
    .concat((node.glyphs && ctx.showGlyphs) ? renderGlyphs(node.glyphs) : [])
    .concat(ctx.showEval ? (
      defined(ev.cp) ? [renderEval(normalizeEval(ev.cp))] : (
        defined(ev.mate) ? [renderEval('#' + ev.mate)] : []
      )
    ) : []);
}

export function renderIndexAndMove(ctx: Ctx, node): VNode[] {
  return node.uci ?
  [renderIndex(node.ply, ctx.withDots)].concat(renderMove(ctx, node)) :
  [h('span.init', 'Initial position')];
}
