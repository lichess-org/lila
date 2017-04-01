var m = require('mithril');
var dialog = require('./dialog');
var chapterForm = require('./chapterNewForm');

module.exports = {
  ctrl: function(send, chapterConfig) {

    var current = m.prop(null);

    var open = function(data) {
      if (current()) m.redraw.strategy('all');
      current({
        id: data.id,
        name: data.name
      });
      chapterConfig(data.id).then(function(d) {
        current(d);
        m.redraw();
      });
    };

    var isEditing = function(id) {
      return current() ? current().id === id : false;
    };

    return {
      open: open,
      toggle: function(data) {
        if (isEditing(data.id)) current(null);
        else open(data);
      },
      current: current,
      submit: function(data) {
        if (!current()) return;
        data.id = current().id;
        send("editChapter", data)
        current(null);
      },
      delete: function(id) {
        send("deleteChapter", id);
        current(null);
      },
      isEditing: isEditing
    }
  },
  view: function(ctrl) {

    var data = ctrl.current();
    if (!data) return;

    var isLoaded = !!data.orientation;
    var mode = data.practice ? 'practice' : (data.conceal !== null ? 'conceal' : 'normal');

    return dialog.form({
      onClose: lichess.partial(ctrl.current, null),
      content: [
        m('h2', 'Edit chapter'),
        m('form.material.form', {
          onsubmit: function(e) {
            ctrl.submit({
              name: chapterForm.fieldValue(e, 'name'),
              mode: chapterForm.fieldValue(e, 'mode'),
              orientation: chapterForm.fieldValue(e, 'orientation')
            });
            e.stopPropagation();
            return false;
          }
        }, [
          m('div.form-group', [
            m('input#chapter-name', {
              required: true,
              minlength: 2,
              maxlength: 80,
              config: function(el, isUpdate) {
                if (!isUpdate && !el.value) {
                  el.value = data.name;
                  el.select();
                  el.focus();
                }
              }
            }),
            m('label.control-label[for=chapter-name]', 'Name'),
            m('i.bar')
          ]),
          isLoaded ? [
            m('div.form-group', [
              m('select#chapter-orientation', ['White', 'Black'].map(function(color) {
                var v = color.toLowerCase();
                return m('option', {
                  value: v,
                  selected: v == data.orientation
                }, color)
              })),
              m('label.control-label[for=chapter-orientation]', 'Orientation'),
              m('i.bar')
            ]),
            m('div.form-group', [
              m('select#chapter-mode', chapterForm.modeChoices.map(function(c) {
                return m('option', {
                  value: c[0],
                  selected: c[0] === mode,
                }, c[1])
              })),
              m('label.control-label[for=chapter-mode]', 'Analysis mode'),
              m('i.bar')
            ]),
            dialog.button('Save chapter')
          ] : m.trust(lichess.spinnerHtml)
        ]),
        m('div.destructive',
          m('button.button.frameless', {
            onclick: function() {
              if (confirm('Delete this chapter? There is no going back!'))
                ctrl.delete(data.id);
            }
          }, 'Delete chapter'))
      ]
    });
  }
};
