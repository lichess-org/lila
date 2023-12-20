import { VNode } from 'snabbdom';
import * as licon from 'common/licon';
import { bind, onInsert, looseH as h } from 'common/snabbdom';
import { richHTML } from 'common/richText';
import StudyCtrl from './studyCtrl';

export type Save = (t: string) => void;

export class DescriptionCtrl {
  edit = false;

  constructor(
    public text: string | undefined,
    readonly doSave: Save,
    readonly redraw: () => void,
  ) {}

  save(t: string) {
    this.text = t;
    this.doSave(t);
    this.redraw();
  }

  set(t: string | undefined) {
    this.text = t ? t : undefined;
  }
}

export const descTitle = (chapter: boolean) => `Pinned ${chapter ? 'chapter' : 'study'} comment`;

export function view(study: StudyCtrl, chapter: boolean): VNode | undefined {
  const desc = chapter ? study.chapterDesc : study.studyDesc,
    contrib = study.members.canContribute() && !study.gamebookPlay;
  if (desc.edit) return edit(desc, chapter ? study.data.chapter.id : study.data.id, chapter);
  const isEmpty = desc.text === '-';
  if (!desc.text || (isEmpty && !contrib)) return;
  return h(`div.study-desc${chapter ? '.chapter-desc' : ''}${isEmpty ? '.empty' : ''}`, [
    contrib &&
      !isEmpty &&
      h('div.contrib', [
        h('span', descTitle(chapter)),
        !isEmpty &&
          h('a', {
            attrs: { 'data-icon': licon.Pencil, title: 'Edit' },
            hook: bind('click', () => (desc.edit = true), desc.redraw),
          }),
        h('a', {
          attrs: { 'data-icon': licon.Trash, title: 'Delete' },
          hook: bind('click', () => {
            if (confirm('Delete permanent description?')) desc.save('');
          }),
        }),
      ]),
    isEmpty
      ? h('a.text.button', { hook: bind('click', () => (desc.edit = true), desc.redraw) }, descTitle(chapter))
      : h('div.text', { hook: richHTML(desc.text) }),
  ]);
}

const edit = (ctrl: DescriptionCtrl, id: string, chapter: boolean): VNode =>
  h('div.study-desc-form', [
    h('div.title', [
      descTitle(chapter),
      h('button.button.button-empty.button-green', {
        attrs: { 'data-icon': licon.Checkmark, title: 'Save and close' },
        hook: bind('click', () => (ctrl.edit = false), ctrl.redraw),
      }),
    ]),
    h('form.form3', [
      h('div.form-group', [
        h('textarea#form-control.desc-text.' + id, {
          hook: onInsert<HTMLInputElement>(el => {
            el.value = ctrl.text === '-' ? '' : ctrl.text || '';
            el.oninput = () => ctrl.save(el.value.trim());
            el.focus();
          }),
        }),
      ]),
    ]),
  ]);
