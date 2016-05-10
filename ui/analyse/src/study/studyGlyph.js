var m = require('mithril');
var xhr = require('./studyXhr');
var throttle = require('../util').throttle;
var nodeFullName = require('../util').nodeFullName;
var partial = require('chessground').util.partial;

function renderGlyph(ctrl, node) {
  return function(glyph) {
    return m('a', {
      onclick: function() {
        ctrl.toggleGlyph(glyph.id);
        return false;
      },
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
  form: {
    ctrl: function(root) {
      var current = m.prop(null); // {chapterId, path, node}
      var dirty = m.prop(true);
      var all = m.prop(null);

      var loadGlyphs = function() {
        if (!all()) xhr.glyphs().then(function(gs) {
          all(gs);
          m.redraw();
        });
      };

      var toggleGlyph = function(id) {
        if (!current()) return;
        if (!dirty()) {
          dirty(true);
          m.redraw();
        }
        doToggleGlyph(id);
      };

      var doToggleGlyph = throttle(500, false, function(id) {
        root.study.contribute('toggleGlyph', {
          chapterId: current().chapterId,
          path: current().path,
          id: id
        });
      });

      var open = function(chapterId, path, node) {
        loadGlyphs();
        dirty(true);
        current({
          chapterId: chapterId,
          path: path,
          node: node
        });
        root.userJump(path);
      };

      var close = function() {
        current(null);
      };

      return {
        root: root,
        all: all,
        current: current,
        dirty: dirty,
        open: open,
        close: close,
        toggle: function(chapterId, path, node) {
          if (current()) close();
          else open(chapterId, path, node);
        },
        toggleGlyph: toggleGlyph
      };
    },
    view: function(ctrl) {

      var current = ctrl.current();
      if (!current) return;
      var all = ctrl.all();

      return m('div.study_glyph_form.underboard_form', [
        m('p.title', [
          m('button.button.frameless.close', {
            'data-icon': 'L',
            title: 'Close',
            onclick: ctrl.close
          }),
          'Annotating position after ',
          m('button.button', {
            class: ctrl.root.vm.path === current.path ? '' : 'active',
            onclick: partial(ctrl.root.userJump, current.path)
          }, nodeFullName(current.node)),
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
          m('div.move', all.move.map(renderGlyph(ctrl, current.node))),
          m('div.position', all.position.map(renderGlyph(ctrl, current.node))),
          m('div.observation', all.observation.map(renderGlyph(ctrl, current.node)))
        ]) : m.trust(lichess.spinnerHtml)
      ]);
    }
  }
};
