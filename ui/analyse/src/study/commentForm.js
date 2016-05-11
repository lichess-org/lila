var m = require('mithril');
var partial = require('chessground').util.partial;
var nodeFullName = require('../util').nodeFullName;
var throttle = require('../util').throttle;

module.exports = {
  ctrl: function(root) {

    var current = m.prop(null); // {chapterId, path, node}
    var dirty = m.prop(true);
    var focus = m.prop(false);
    var opening = m.prop(false);

    var submit = function(text) {
      if (!current()) return;
      if (!dirty()) {
        dirty(true);
        m.redraw();
      }
      doSubmit(text);
    };

    var doSubmit = throttle(500, false, function(text) {
      root.study.contribute('setComment', {
        chapterId: current().chapterId,
        path: current().path,
        text: text
      });
    });

    var open = function(chapterId, path, node) {
      dirty(true);
      opening(true);
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

    var onSetPath = function(path, node) {
      var cur = current();
      if (cur && cur.path !== path && !focus()) {
        cur.path = path;
        cur.node = node;
        current(cur);
        dirty(true);
      }
      m.redraw();
    };

    return {
      root: root,
      current: current,
      dirty: dirty,
      focus: focus,
      opening: opening,
      open: open,
      close: close,
      toggle: function(chapterId, path, node) {
        if (current()) close();
        else open(chapterId, path, node);
      },
      submit: submit,
      delete: function(chapterId, path, userId) {
        root.study.contribute('setComment', {
          chapterId: chapterId,
          path: path,
          text: '',
          by: userId
        });
      },
      onSetPath: onSetPath
    };
  },
  view: function(ctrl) {

    var current = ctrl.current();
    if (!current) return;

    return m('div.study_comment_form.underboard_form', {
      config: function(el, isUpdate) {
        if (!isUpdate) lichess.loadCss('/assets/stylesheets/material.form.css');
      }
    }, [
      m('p.title', [
        m('button.button.frameless.close', {
          'data-icon': 'L',
          title: 'Close',
          onclick: ctrl.close
        }),
        'Commenting position after ',
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
      m('form.material.form', [
        m('div.form-group', [
          m('textarea#comment-text', {
            config: function(el, isUpdate, ctx) {
              if (isUpdate && ctx.path === current.path) return;
              ctx.path = current.path;
              var mine = (current.node.comments || []).find(function(c) {
                return c.by.toLowerCase() === ctrl.root.userId;
              });
              el.value = mine ? mine.text : '';
              if (ctrl.opening() || ctrl.focus()) el.focus();
              ctrl.opening(false);
              if (!ctx.trap) {
                ctx.trap = Mousetrap(el);
                ctx.trap.bind(['ctrl+enter', 'command+enter'], ctrl.close);
                ctx.trap.stopCallback = function() {
                  return false;
                };
                ctx.onunload = ctx.trap.reset;
              }
            },
            onkeyup: function(e) {
              ctrl.submit(e.target.value);
            },
            onfocus: partial(ctrl.focus, true),
            onblur: partial(ctrl.focus, false)
          }),
          m('i.bar')
        ])
      ])
    ]);
  }
};
