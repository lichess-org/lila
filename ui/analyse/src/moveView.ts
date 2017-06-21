import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode'
import { fixCrazySan, renderEval as normalizeEval } from 'chess';
import { defined } from 'common';
import { view as cevalView } from 'ceval';

function plyToTurn(ply) {
  return Math.floor((ply - 1) / 2) + 1;
}

export function renderGlyphs(glyphs): VNode[] {
  return glyphs.map(function(glyph) {
    return h('glyph', {
      attrs: { title: glyph.name }
    }, glyph.symbol);
  });
}

function renderEval(e): VNode {
  return h('eval', e);
}

export function renderIndex(ply, withDots): VNode {
  return h('index', plyToTurn(ply) + (withDots ? (ply % 2 === 1 ? '.' : '...') : ''));
}

export function renderMove(ctx, node): VNode[] {
  const ev: any = cevalView.getBestEval({client: node.ceval, server: node.eval}) || {};
  return [h('san', fixCrazySan(node.san))]
    .concat((node.glyphs && ctx.showGlyphs) ? renderGlyphs(node.glyphs) : [])
    .concat(ctx.showEval ? (
      defined(ev.cp) ? [renderEval(normalizeEval(ev.cp))] : (
        defined(ev.mate) ? [renderEval('#' + ev.mate)] : []
      )
    ) : []);
}

export function renderIndexAndMove(ctx, node): VNode[] {
  return node.uci ?
  [renderIndex(node.ply, ctx.withDots)].concat(renderMove(ctx, node)) :
  [h('span.init', 'Initial position')];
}
