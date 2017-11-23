import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode'
import * as xhr from './studyXhr';
import { prop, throttle } from 'common';
import { bind, nodeFullName, spinner } from '../util';
import AnalyseCtrl from '../ctrl';

function renderGlyph(ctrl, node) {
  return function(glyph) {
    return h('a', {
      hook: bind('click', _ => {
        ctrl.toggleGlyph(glyph.id);
        return false;
      }, ctrl.redraw),
      class: {
        active: (node.glyphs && node.glyphs.find(function(g) {
          return g.id === glyph.id;
        }))
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
  const isOpen = prop(false),
  all = prop<any | null>(null);

  function loadGlyphs() {
    if (!all()) xhr.glyphs().then(function(gs) {
      all(gs);
      root.redraw();
    });
  };

  const toggleGlyph = throttle(500, function(id) {
    root.study!.makeChange('toggleGlyph', root.study!.withPosition({
      id
    }));
  });

  function open() {
    loadGlyphs();
    isOpen(true);
  };

  return {
    root,
    all,
    open,
    isOpen,
    toggle() {
      if (isOpen()) isOpen(false);
      else open();
    },
    toggleGlyph,
    redraw: root.redraw
  };
}

export function view(ctrl): VNode | undefined {

  if (!ctrl.isOpen()) return;
  const all = ctrl.all();
  const node = ctrl.root.node;

  return h('div.study_glyph_form.underboard_form', [
    h('p.title', [
      h('button.button.frameless.close', {
        attrs: {
          'data-icon': 'L',
          title: 'Close'
        },
        hook: bind('click', () => ctrl.isOpen(false), ctrl.redraw)
      }),
      'Annotating position after ',
      h('strong', nodeFullName(node))
    ]),
    all ? h('div.glyph_form', [
      h('div.move', all.move.map(renderGlyph(ctrl, node))),
      h('div.position', all.position.map(renderGlyph(ctrl, node))),
      h('div.observation', all.observation.map(renderGlyph(ctrl, node)))
    ]) : spinner()
  ]);
}
