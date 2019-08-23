import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode'
import { defined, prop, Prop } from 'common';
import { Redraw } from '../interfaces';
import { bind, bindSubmit, spinner, option, onInsert, emptyRedButton } from '../util';
import * as modal from '../modal';
import * as chapterForm from './chapterNewForm';
import { StudyChapterConfig, StudyChapterMeta } from './interfaces';

interface StudyChapterEditFormCtrl {
  current: Prop<StudyChapterMeta | StudyChapterConfig | null>;
  open(data: StudyChapterMeta): void;
  toggle(data: StudyChapterMeta): void;
  submit(data: StudyChapterConfig): void;
  delete(id: string): void;
  clearAnnotations(id: string): void;
  isEditing(id: string): boolean;
  redraw: Redraw;
}

export function ctrl(send: SocketSend, chapterConfig: (string) => JQueryPromise<StudyChapterConfig>, redraw: Redraw): StudyChapterEditFormCtrl {

  const current = prop<StudyChapterMeta | StudyChapterConfig | null>(null);

  function open(data: StudyChapterMeta) {
    current({
      id: data.id,
      name: data.name
    });
    chapterConfig(data.id).then(d => {
      current(d!);
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

export function view(ctrl: StudyChapterEditFormCtrl): VNode | undefined {
  const data = ctrl.current();
  return data ? modal.modal({
    class: 'edit-' + data.id, // full redraw when changing chapter
    onClose() {
      ctrl.current(null);
      ctrl.redraw();
    },
    content: [
      h('h2', 'Edit chapter'),
      h('form.form3', {
        hook: bindSubmit(e => {
          const o: any = {};
          'name mode orientation description'.split(' ').forEach(field => {
            o[field] = chapterForm.fieldValue(e, field);
          });
          ctrl.submit(o);
        })
      }, [
        h('div.form-group', [
          h('label.form-label', {
            attrs: { for: 'chapter-name' }
          }, 'Name'),
          h('input#chapter-name.form-control', {
            attrs: {
              minlength: 2,
              maxlength: 80
            },
            hook: onInsert<HTMLInputElement>(el => {
              if (!el.value) {
                el.value = data.name;
                el.select();
                el.focus();
              }
            })
          })
        ]),
        ...(isLoaded(data) ? viewLoaded(data) : [spinner()])
      ]),
      h('div.destructive', [
        h(emptyRedButton, {
          hook: bind('click', _ => {
            if (confirm('Clear all comments and shapes in this chapter?'))
              ctrl.clearAnnotations(data.id);
          })
        }, 'Clear annotations'),
        h(emptyRedButton, {
          hook: bind('click', _ => {
            if (confirm('Delete this chapter? There is no going back!'))
              ctrl.delete(data.id);
          })
        }, 'Delete chapter')
      ])
    ]
  }) : undefined;
}

function isLoaded(data: StudyChapterMeta | StudyChapterConfig): data is StudyChapterConfig {
  return !!data['orientation'];
}

function viewLoaded(data: StudyChapterConfig): VNode[] {
  const mode = data.practice ? 'practice' : (defined(data.conceal) ? 'conceal' : (data.gamebook ? 'gamebook' : 'normal'));
  return [
    h('div.form-split', [
      h('div.form-group.form-half', [
        h('label.form-label', {
          attrs: { for: 'chapter-orientation' }
        }, 'Orientation'),
        h('select#chapter-orientation.form-control', ['White', 'Black'].map(function(color) {
          const v = color.toLowerCase();
          return option(v, data.orientation, color);
        }))
      ]),
      h('div.form-group.form-half', [
        h('label.form-label', {
          attrs: { for: 'chapter-mode' }
        }, 'Analysis mode'),
        h('select#chapter-mode.form-control', chapterForm.modeChoices.map(c => {
          return option(c[0], mode, c[1]);
        }))
      ])
    ]),
    chapterForm.descriptionGroup(data.description),
    modal.button('Save chapter')
  ];
}
