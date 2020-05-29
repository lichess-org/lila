import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode'
import { renderEval as normalizeEval } from 'draughts';
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
  return [h('san', node.alg || node.san!)]
    .concat((node.glyphs && ctx.showGlyphs) ? renderGlyphs(node.glyphs) : [])
    .concat(ctx.showEval ? (
      defined(ev.cp) ? [renderEval(normalizeEval(ev.cp))] : (
        defined(ev.win) ? [renderEval('#' + ev.win)] : []
      )
    ) : []);
}

export function renderIndexAndMove(ctx: Ctx, node): VNode[] | undefined {
  if (!node.san) return; // initial position
  return [renderIndex(node.displayPly ? node.displayPly : node.ply, ctx.withDots), ...renderMove(ctx, node)];
}
