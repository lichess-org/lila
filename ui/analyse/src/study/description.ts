import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode'
import { StudyCtrl } from './interfaces';
import { bind, enrichText, innerHTML } from '../util';

export type Save = (string) => void;

export class DescriptionCtrl {

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

export function descTitle(chapter: boolean) {
  return `${chapter ? 'Chapter' : 'Study'} pinned comment`;
}

export function view(study: StudyCtrl, chapter: boolean): VNode | undefined {
  const desc = study.desc,
    contrib = study.members.canContribute() && !study.gamebookPlay();
  if (desc.edit) return edit(desc, chapter ? study.data.chapter.id : study.data.id, chapter);
  const isEmpty = desc.text === '-';
  if (!desc.text || (isEmpty && !contrib)) return;
  return h('div.study_desc', [
    contrib && !isEmpty ? h('div.contrib', [
      h('span', descTitle(chapter)),
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
    isEmpty ? h('a.text.empty.button', {
      hook: bind('click', _ => { desc.edit = true; }, desc.redraw)
    }, descTitle(chapter)) : h('div.text', {
      hook: innerHTML(desc.text, text => enrichText(text, true))
    })
  ]);
}

function edit(ctrl: DescriptionCtrl, id: string, chapter: boolean): VNode {
  return h('div.study_desc_form.underboard_form', {
    hook: {
      insert: _ => window.lidraughts.loadCss('stylesheets/material.form.css')
    }
  }, [
    h('p.title', [
      h('button.button.frameless.close', {
        attrs: {
          'data-icon': 'L',
          title: 'Close'
        },
        hook: bind('click', () => ctrl.edit = false, ctrl.redraw)
      }),
      descTitle(true),
    ]),
    h('form.material.form', [
      h('div.form-group', [
        h('textarea#desc-text.' + id, {
          hook: {
            insert(vnode: VNode) {
              const el = vnode.elm as HTMLInputElement;
              el.value = ctrl.text === '-' ? '' : (ctrl.text || '');
              el.onkeyup = el.onpaste = () => {
                ctrl.save(el.value.trim());
              };
              el.focus();
            }
          }
        }),
        h('i.bar')
      ])
    ])
  ]);
}
