import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode'
import { StudyCtrl } from './interfaces';
import { bind, enrichText, innerHTML, onInsert } from '../util';

export type Save = (string) => void;

export class ChapterDescriptionCtrl {

  edit: boolean = false;
  text?: string;

  constructor(text: string | undefined, readonly doSave: Save, readonly redraw: () => void) {
    this.text = text;
  }

  save(t: string) {
    this.text = t;
    this.doSave(t);
    this.redraw();
  }

  set(t: string | undefined) {
    this.text = t ? t : undefined;
  }
}

export const title = 'Chapter pinned comment';

export function view(study: StudyCtrl): VNode | undefined {
  const desc = study.desc,
    contrib = study.members.canContribute() && !study.gamebookPlay();
  if (desc.edit) return edit(desc, study.data.chapter.id);
  const isEmpty = desc.text === '-';
  if (!desc.text || (isEmpty && !contrib)) return;
  return h('div.chapter-desc' + (isEmpty ? '.empty' : ''), [
    contrib && !isEmpty ? h('div.contrib', [
      h('span', title),
      isEmpty ? null : h('a', {
        attrs: {
          'data-icon': 'm',
          title: 'Edit'
        },
        hook: bind('click', _ => { desc.edit = true; }, desc.redraw)
      }),
      h('a', {
        attrs: {
          'data-icon': 'q',
          title: 'Delete'
        },
        hook: bind('click', () => {
          if (confirm('Delete permanent description?')) desc.save('');
        })
      })
    ]) : null,
    isEmpty ? h('a.text.button', {
      hook: bind('click', _ => { desc.edit = true; }, desc.redraw)
    }, title) : h('div.text', {
      hook: innerHTML(desc.text, text => enrichText(text, true))
    })
  ]);
}

function edit(ctrl: ChapterDescriptionCtrl, chapterId: string): VNode {
  return h('div.chapter-desc-form', [
    h('div.title', [
      title,
      h('button.button.button-empty.button-red', {
        attrs: {
          'data-icon': 'L',
          title: 'Close'
        },
        hook: bind('click', () => ctrl.edit = false, ctrl.redraw)
      })
    ]),
    h('form.form3', [
      h('div.form-group', [
        h('textarea#form-control.desc-text.' + chapterId, {
          hook: onInsert<HTMLInputElement>(el => {
            el.value = ctrl.text === '-' ? '' : (ctrl.text || '');
            el.onkeyup = el.onpaste = () => {
              ctrl.save(el.value.trim());
            };
            el.focus();
          })
        })
      ])
    ])
  ]);
}
