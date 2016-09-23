var m = require('mithril');
var xhr = require('./studyXhr');
var throttle = require('../util').throttle;
var nodeFullName = require('../util').nodeFullName;
var partial = require('chessground').util.partial;
var util = require('../util');

function renderGlyph(ctrl, node) {
  return function(glyph) {
    return m('a', {
      config: util.bindOnce('click', function() {
        ctrl.toggleGlyph(glyph.id);
        return false;
      }),
      class: (node.glyphs && node.glyphs.filter(function(g) {
        return g.id === glyph.id;
      })[0]) ? 'active' : ''
    }, [
      m('i', {
        'data-symbol': glyph.symbol,
      }),
      glyph.name
    ]);
  };
}

module.exports = {
  ctrl: function(root) {
    var isOpen = m.prop(false);
    var dirty = m.prop(true);
    var all = m.prop(null);

    var loadGlyphs = function() {
      if (!all()) xhr.glyphs().then(function(gs) {
        all(gs);
        m.redraw();
      });
    };

    var toggleGlyph = function(id) {
      if (!dirty()) {
        dirty(true);
        m.redraw();
      }
      doToggleGlyph(id);
    };

    var doToggleGlyph = throttle(500, false, function(id) {
      root.study.contribute('toggleGlyph', root.study.withPosition({
        id: id
      }));
    });

    var open = function() {
      loadGlyphs();
      dirty(true);
      isOpen(true);
    };

    return {
      root: root,
      all: all,
      open: open,
      isOpen: isOpen,
      dirty: dirty,
      toggle: function(chapterId, path, node) {
        if (isOpen()) isOpen(false);
        else open();
      },
      toggleGlyph: toggleGlyph
    };
  },
  view: function(ctrl) {

    if (!ctrl.isOpen()) return;
    var all = ctrl.all();
    var node = ctrl.root.vm.node;

    return m('div.study_glyph_form.underboard_form', [
      m('p.title', [
        m('button.button.frameless.close', {
          'data-icon': 'L',
          title: 'Close',
          config: util.bindOnce('click', partial(ctrl.isOpen, false))
        }),
        'Annotating position after ',
        m('strong', nodeFullName(node)),
        m('span.saved', {
          config: function(el, isUpdate, ctx) {
            if (ctrl.dirty())
              el.classList.remove('visible');
            else
              el.classList.add('visible');
          }
        }, 'Saved.')
      ]),
      all ? m('div.glyph_form', [
        m('div.move', all.move.map(renderGlyph(ctrl, node))),
        m('div.position', all.position.map(renderGlyph(ctrl, node))),
        m('div.observation', all.observation.map(renderGlyph(ctrl, node)))
      ]) : m.trust(lichess.spinnerHtml)
    ]);
  }
};
