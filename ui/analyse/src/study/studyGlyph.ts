import { prop } from 'common';
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

const renderGlyph = (ctrl: GlyphForm, node: Tree.Node) => (glyph: Tree.Glyph) =>
  h(
    'button',
    {
      hook: bind('click', () => ctrl.toggleGlyph(glyph.id)),
      attrs: { 'data-symbol': glyph.symbol, type: 'button' },
      class: { active: !!node.glyphs && !!node.glyphs.find(g => g.id === glyph.id) },
    },
    [glyph.name],
  );

export class GlyphForm {
  all = prop<AllGlyphs | null>(null);

  constructor(readonly root: AnalyseCtrl) {}

  loadGlyphs = () => {
    if (!this.all())
      xhr.glyphs().then(gs => {
        this.all(gs);
        this.root.redraw();
      });
  };

  toggleGlyph = throttle(500, (id: Tree.GlyphId) => {
    this.root.study!.makeChange('toggleGlyph', this.root.study!.withPosition({ id }));
    this.root.redraw();
  });
}

export const viewDisabled = (why: string): VNode => h('div.study__glyphs', [h('div.study__message', why)]);

export function view(ctrl: GlyphForm): VNode {
  const all = ctrl.all(),
    node = ctrl.root.node;

  return h(
    'div.study__glyphs' + (all ? '' : '.empty'),
    { hook: { insert: ctrl.loadGlyphs } },
    all
      ? [
          h('div.move', all.move.map(renderGlyph(ctrl, node))),
          h('div.position', all.position.map(renderGlyph(ctrl, node))),
          h('div.observation', all.observation.map(renderGlyph(ctrl, node))),
        ]
      : [h('div.study__message', spinner())],
  );
}
