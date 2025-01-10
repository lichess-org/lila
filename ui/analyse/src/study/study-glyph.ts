import { bind } from 'common/snabbdom';
import throttle from 'common/throttle';
import { allGlyphs } from 'shogi/glyphs';
import { type VNode, h } from 'snabbdom';
import type AnalyseCtrl from '../ctrl';

export interface GlyphCtrl {
  root: AnalyseCtrl;
  toggleGlyph(id: Tree.GlyphId): void;
  redraw(): void;
}

function renderGlyph(ctrl: GlyphCtrl, node: Tree.Node) {
  return (glyph: Tree.Glyph) =>
    h(
      'button',
      {
        hook: bind('click', _ => ctrl.toggleGlyph(glyph.id), ctrl.redraw),
        attrs: { 'data-symbol': glyph.symbol, type: 'button' },
        class: {
          active: !!node.glyphs && !!node.glyphs.find(g => g.id === glyph.id),
        },
      },
      [glyph.name],
    );
}

export function ctrl(root: AnalyseCtrl): GlyphCtrl {
  const toggleGlyph = throttle(500, (id: string) => {
    root.study!.makeChange(
      'toggleGlyph',
      root.study!.withPosition({
        id,
      }),
    );
  });

  return {
    root,
    toggleGlyph,
    redraw: root.redraw,
  };
}

export function viewDisabled(why: string): VNode {
  return h('div.study__glyphs', [h('div.study__message', why)]);
}

export function view(ctrl: GlyphCtrl): VNode {
  const node = ctrl.root.node;

  return h('div.study__glyphs', [
    h('div.move', allGlyphs.move.map(renderGlyph(ctrl, node))),
    h('div.position', allGlyphs.position.map(renderGlyph(ctrl, node))),
    h('div.observation', allGlyphs.observation.map(renderGlyph(ctrl, node))),
  ]);
}
