import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode'
import { prop } from 'common';
import { bind, bindSubmit, spinner, option } from '../util';
import * as dialog from './dialog';
import * as chapterForm from './chapterNewForm';
import { StudyChapterMeta } from './interfaces';

export function ctrl(send: SocketSend, chapterConfig, redraw: () => void) {

  const current = prop<StudyChapterMeta | null>(null);

  function open(data) {
    current({
      id: data.id,
      name: data.name
    });
    chapterConfig(data.id).then(d => {
      current(d);
      redraw();
    });
  };

  function isEditing(id) {
    const c = current();
    return c ? c.id === id : false;
  };

  return {
    open,
    toggle(data) {
      if (isEditing(data.id)) current(null);
      else open(data);
    },
    current,
    submit(data) {
      const c = current();
      if (c) {
        data.id = c.id;
        send("editChapter", data)
        current(null);
      }
      redraw();
    },
    delete(id) {
      send("deleteChapter", id);
      current(null);
    },
    clearAnnotations(id) {
      send("clearAnnotations", id);
      current(null);
    },
    isEditing,
    redraw
  }
}

export function view(ctrl): VNode | undefined {

  const data = ctrl.current();
  if (!data) return;

  const isLoaded = !!data.orientation;
  const mode = data.practice ? 'practice' : (!isNaN(data.conceal) ? 'conceal' : (data.gamebook ? 'gamebook' : 'normal'));

  return dialog.form({
    class: 'edit-' + data.id, // full redraw when changing chapter
    onClose() {
      ctrl.current(null);
      ctrl.redraw();
    },
    content: [
      h('h2', 'Edit chapter'),
      h('form.material.form', {
        hook: bindSubmit(e => {
          const o: any = {};
          'name mode orientation description'.split(' ').forEach(field => {
            o[field] = chapterForm.fieldValue(e, field);
          });
          ctrl.submit(o);
        })
      }, [
        h('div.form-group', [
          h('input#chapter-name', {
            attrs: {
              minlength: 2,
              maxlength: 80
            },
            hook: {
              insert: vnode => {
                const el = vnode.elm as HTMLInputElement;
                if (!el.value) {
                  el.value = data.name;
                  el.select();
                  el.focus();
                }
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
          h('div.form-group.little-margin-bottom', [
            h('select#chapter-orientation', ['White', 'Black'].map(function(color) {
              const v = color.toLowerCase();
              return option(v, data.orientation, color);
            })),
            h('label.control-label', {
              attrs: { for: 'chapter-orientation' }
            }, 'Orientation'),
            h('i.bar')
          ]),
          h('div.form-group.little-margin-bottom', [
            h('select#chapter-mode', chapterForm.modeChoices.map(c => {
              return option(c[0], mode, c[1]);
            })),
            h('label.control-label', {
              attrs: { for: 'chapter-mode' }
            }, 'Analysis mode'),
            h('i.bar')
          ]),
          chapterForm.descriptionGroup(data.description),
          dialog.button('Save chapter')
        ] : [spinner()]
      )),
      h('div.destructive', [
        h('button.button.frameless', {
          hook: bind('click', _ => {
            if (confirm('Clear all comments and shapes in this chapter?'))
            ctrl.clearAnnotations(data.id);
          })
        }, 'Clear annotations'),
        h('button.button.frameless', {
          hook: bind('click', _ => {
            if (confirm('Delete this chapter? There is no going back!'))
            ctrl.delete(data.id);
          })
        }, 'Delete chapter')
      ])
    ]
  });
}
