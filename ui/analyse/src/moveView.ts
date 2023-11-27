import { view as cevalView, renderEval as normalizeEval } from 'ceval';
import { defined } from 'common/common';
import { notationsWithColor } from 'common/notation';
import { VNode, h } from 'snabbdom';
import { renderTime } from './clocks';

export interface Ctx {
  variant: VariantKey;
  withDots?: boolean;
  showEval: boolean;
  showGlyphs?: boolean;
  offset?: number;
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
  return h(`index.sbhint${ply}`, renderIndexText(ply, offset, withDots));
}

export function renderMove(ctx: Ctx, node: Tree.Node, moveTime?: number): VNode[] {
  const ev: any = cevalView.getBestEval({ client: node.ceval, server: node.eval }) || {},
    colorIcon = notationsWithColor() ? '.color-icon.' + (node.ply % 2 ? 'sente' : 'gote') : '';

  return [h('move-notation' + colorIcon, node.notation)]
    .concat(node.glyphs && ctx.showGlyphs ? renderGlyphs(node.glyphs) : [])
    .concat(
      ctx.showEval
        ? defined(ev.cp)
          ? [renderEval(normalizeEval(ev.cp))]
          : defined(ev.mate)
            ? [renderEval('#' + ev.mate)]
            : []
        : []
    )
    .concat(defined(moveTime) ? [h('movetime', renderTime(moveTime, false))] : []);
}

export function renderIndexAndMove(ctx: Ctx, node: Tree.Node, moveTime?: number): VNode[] | undefined {
  if (!node.usi) return; // initial position
  return [renderIndex(node.ply, ctx.offset, ctx.withDots), ...renderMove(ctx, node, moveTime)];
}
