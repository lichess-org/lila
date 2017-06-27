import { h } from 'snabbdom'
import { prop } from 'common';
import { bind, spinner } from '../util';
import * as dialog from './dialog';
import * as chapterForm from './chapterNewForm';

export function ctrl(send, chapterConfig) {

  const current = prop<any>(null);

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
}

export function view(ctrl) {

  var data = ctrl.current();
  if (!data) return;

  var isLoaded = !!data.orientation;
  var mode = data.practice ? 'practice' : (data.conceal !== null ? 'conceal' : 'normal');

  return dialog.form({
    onClose: () => ctrl.current(null),
    content: [
      h('h2', 'Edit chapter'),
      h('form.material.form', {
        hook: bind('submit', e => {
          ctrl.submit({
            name: chapterForm.fieldValue(e, 'name'),
            mode: chapterForm.fieldValue(e, 'mode'),
            orientation: chapterForm.fieldValue(e, 'orientation')
          });
          e.stopPropagation();
          return false;
        })
      }, [
        h('div.form-group', [
          h('input#chapter-name', {
            attrs: {
              required: true,
              minlength: 2,
              maxlength: 80
            },
            insert: vnode => {
              const el = vnode.elm as HTMLInputElement;
              if (!el.value) {
                el.value = data.name;
                el.select();
                el.focus();
              }
            }
          }),
          h('label.control-label', {
            attrs: { for: 'chapter-name' }
          }, 'Name'),
          h('i.bar')
        ])
      ].concat(
        isLoaded ? [
          h('div.form-group', [
            h('select#chapter-orientation', ['White', 'Black'].map(function(color) {
              const v = color.toLowerCase();
              return m('option', {
                attrs: {
                  value: v,
                  selected: v == data.orientation
                }
              }, color)
            })),
            h('label.control-label', {
              attrs: { for: 'chapter-orientation' }
            }, 'Orientation'),
            h('i.bar')
          ]),
          h('div.form-group', [
            h('select#chapter-mode', chapterForm.modeChoices.map(function(c) {
              return m('option', {
                attrs: {
                  value: c[0],
                  selected: c[0] === mode
                },
              }, c[1])
            })),
            h('label.control-label', {
              attrs: { for: 'chapter-mode' }
            }, 'Analysis mode'),
            h('i.bar')
          ]),
          dialog.button('Save chapter')
        ] : [spinner()]
      )),
      h('div.destructive',
        h('button.button.frameless', {
          hook: bind('click', _ => {
            if (confirm('Delete this chapter? There is no going back!'))
            ctrl.delete(data.id);
          })
        }, 'Delete chapter'))
    ]
  });
}
