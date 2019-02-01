import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode'
import * as xhr from './studyXhr';
import { prop, Prop } from 'common';
import throttle from 'common/throttle';
import { bind, spinner } from '../util';
import AnalyseCtrl from '../ctrl';

interface AllGlyphs {
  move: Tree.Glyph[];
  observation: Tree.Glyph[];
  position: Tree.Glyph[];
}

export interface GlyphCtrl {
  root: AnalyseCtrl;
  all: Prop<AllGlyphs>;
  loadGlyphs(): void;
  toggleGlyph(id: Tree.GlyphId): void;
  redraw(): void;
}

function renderGlyph(ctrl: GlyphCtrl, node: Tree.Node) {
  return function(glyph) {
    return h('a', {
      hook: bind('click', _ => {
        ctrl.toggleGlyph(glyph.id);
        return false;
      }, ctrl.redraw),
      class: {
        active: !!node.glyphs && !!node.glyphs.find(g => g.id === glyph.id)
      }
    }, [
      h('i', {
        attrs: { 'data-symbol': glyph.symbol }
      }),
      glyph.name
    ]);
  };
}

export function ctrl(root: AnalyseCtrl) {

  const all = prop<any | null>(null);

  function loadGlyphs() {
    if (!all()) xhr.glyphs().then(gs => {
      all(gs);
      root.redraw();
    });
  };

  const toggleGlyph = throttle(500, (id: string) => {
    root.study!.makeChange('toggleGlyph', root.study!.withPosition({
      id
    }));
  });

  return {
    root,
    all,
    loadGlyphs,
    toggleGlyph,
    redraw: root.redraw
  };
}

export function viewDisabled(why: string): VNode {
  return h('div.study_glyph_form', [
    h('div.message', h('span', why))
  ]);
}

export function view(ctrl: GlyphCtrl): VNode {

  const all = ctrl.all(), node = ctrl.root.node;

  return h('div.study_glyph_form.underboard_form', {
    hook: { insert: ctrl.loadGlyphs }
  }, [
    all ? h('div.glyph_form', [
      h('div.move', all.move.map(renderGlyph(ctrl, node))),
      h('div.position', all.position.map(renderGlyph(ctrl, node))),
      h('div.observation', all.observation.map(renderGlyph(ctrl, node)))
    ]) : h('div.message', spinner())
  ]);
}
