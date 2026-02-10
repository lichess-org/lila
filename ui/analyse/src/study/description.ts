import * as licon from 'lib/licon';
import { type VNode, bind, onInsert, hl, confirm } from 'lib/view';
import { richHTML } from 'lib/richText';
import type StudyCtrl from './studyCtrl';

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
  return hl(`div.study-desc${chapter ? '.chapter-desc' : ''}${isEmpty ? '.empty' : ''}`, [
    contrib &&
      !isEmpty &&
      hl('div.contrib', [
        hl('span', descTitle(chapter)),
        !isEmpty &&
          hl('a', {
            attrs: { 'data-icon': licon.Pencil, title: 'Edit' },
            hook: bind('click', () => (desc.edit = true), desc.redraw),
          }),
        hl('a', {
          attrs: { 'data-icon': licon.Trash, title: 'Delete' },
          hook: bind('click', async () => {
            if (await confirm('Delete permanent description?')) desc.save('');
          }),
        }),
      ]),
    isEmpty
      ? hl(
          'a.text.button',
          { hook: bind('click', () => (desc.edit = true), desc.redraw) },
          descTitle(chapter),
        )
      : hl('div.text', { hook: richHTML(desc.text) }),
  ]);
}

const edit = (ctrl: DescriptionCtrl, id: string, chapter: boolean): VNode =>
  hl('div.study-desc-form', [
    hl('div.title', [
      descTitle(chapter),
      hl('button.button.button-empty.button-green', {
        attrs: { 'data-icon': licon.Checkmark, title: 'Save and close' },
        hook: bind('click', () => (ctrl.edit = false), ctrl.redraw),
      }),
    ]),
    hl('form.form3', [
      hl('div.form-group', [
        hl('textarea#form-control.desc-text.' + id, {
          hook: onInsert<HTMLInputElement>(el => {
            el.value = ctrl.text === '-' ? '' : ctrl.text || '';
            el.oninput = () => ctrl.save(el.value.trim());
            el.focus();
          }),
        }),
      ]),
    ]),
  ]);
