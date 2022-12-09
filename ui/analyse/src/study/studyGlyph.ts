import { prop, Prop } from 'common';
import { bind } from 'common/snabbdom';
import throttle from 'common/throttle';
import { spinnerVdom as spinner } from 'common/spinner';
import { h, VNode } from 'snabbdom';
import AnalyseCtrl from '../ctrl';
import * as xhr from './studyXhr';

interface AllGlyphs {
  move: Tree.Glyph[];
  observation: Tree.Glyph[];
  position: Tree.Glyph[];
}

export interface GlyphCtrl {
  root: AnalyseCtrl;
  all: Prop<AllGlyphs | null>;
  loadGlyphs(): void;
  toggleGlyph(id: Tree.GlyphId): void;
}

function renderGlyph(ctrl: GlyphCtrl, node: Tree.Node) {
  return (glyph: Tree.Glyph) =>
    h(
      'button',
      {
        hook: bind('click', () => ctrl.toggleGlyph(glyph.id)),
        attrs: { 'data-symbol': glyph.symbol, type: 'button' },
        class: {
          active: !!node.glyphs && !!node.glyphs.find(g => g.id === glyph.id),
        },
      },
      [glyph.name]
    );
}

export function ctrl(root: AnalyseCtrl): GlyphCtrl {
  const all = prop<AllGlyphs | null>(null);

  function loadGlyphs() {
    if (!all())
      xhr.glyphs().then(gs => {
        all(gs);
        root.redraw();
      });
  }

  const toggleGlyph = throttle(500, (id: Tree.GlyphId) => {
    root.study!.makeChange(
      'toggleGlyph',
      root.study!.withPosition({
        id,
      })
    );
    root.redraw();
  });

  return { root, all, loadGlyphs, toggleGlyph };
}

export function viewDisabled(why: string): VNode {
  return h('div.study__glyphs', [h('div.study__message', why)]);
}

export function view(ctrl: GlyphCtrl): VNode {
  const all = ctrl.all(),
    node = ctrl.root.node;

  return h(
    'div.study__glyphs' + (all ? '' : '.empty'),
    {
      hook: { insert: ctrl.loadGlyphs },
    },
    all
      ? [
          h('div.move', all.move.map(renderGlyph(ctrl, node))),
          h('div.position', all.position.map(renderGlyph(ctrl, node))),
          h('div.observation', all.observation.map(renderGlyph(ctrl, node))),
        ]
      : [h('div.study__message', spinner())]
  );
}
