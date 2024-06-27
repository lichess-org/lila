import * as chapterForm from './chapterNewForm';
import { bind, bindSubmit, onInsert } from 'common/snabbdom';
import { spinnerVdom as spinner } from 'common/spinner';
import { option, emptyRedButton } from '../view/util';
import { ChapterMode, EditChapterData, Orientation, StudyChapterConfig, ChapterPreview } from './interfaces';
import { defined, prop } from 'common';
import { snabDialog } from 'common/dialog';
import { h, VNode } from 'snabbdom';
import { Redraw } from '../interfaces';
import { StudySocketSend } from '../socket';

export class StudyChapterEditForm {
  current = prop<ChapterPreview | StudyChapterConfig | null>(null);

  constructor(
    private readonly send: StudySocketSend,
    private readonly chapterConfig: (id: string) => Promise<StudyChapterConfig>,
    readonly trans: Trans,
    readonly redraw: Redraw,
  ) {}

  open = (data: ChapterPreview) => {
    this.current(data);
    this.chapterConfig(data.id).then(d => {
      this.current(d);
      this.redraw();
    });
  };

  isEditing = (id: string) => this.current()?.id === id;

  toggle = (data: ChapterPreview) => {
    if (this.isEditing(data.id)) this.current(null);
    else this.open(data);
  };
  submit = (data: Omit<EditChapterData, 'id'>) => {
    const c = this.current();
    if (c) {
      this.send('editChapter', { id: c.id, ...data });
      this.current(null);
    }
  };
  delete = (id: string) => {
    this.send('deleteChapter', id);
    this.current(null);
  };
  clearAnnotations = (id: string) => {
    this.send('clearAnnotations', id);
    this.current(null);
  };
  clearVariations = (id: string) => {
    this.send('clearVariations', id);
    this.current(null);
  };
}

export function view(ctrl: StudyChapterEditForm): VNode | undefined {
  const data = ctrl.current(),
    noarg = ctrl.trans.noarg;
  return data
    ? snabDialog({
        class: 'edit-' + data.id, // full redraw when changing chapter
        onClose() {
          ctrl.current(null);
          ctrl.redraw();
        },
        vnodes: [
          h('h2', noarg('editChapter')),
          h(
            'form.form3',
            {
              hook: bindSubmit(e => {
                ctrl.submit({
                  name: chapterForm.fieldValue(e, 'name'),
                  mode: chapterForm.fieldValue(e, 'mode') as ChapterMode,
                  orientation: chapterForm.fieldValue(e, 'orientation') as Orientation,
                  description: chapterForm.fieldValue(e, 'description'),
                });
              }, ctrl.redraw),
            },
            [
              h('div.form-group', [
                h('label.form-label', { attrs: { for: 'chapter-name' } }, noarg('name')),
                h('input#chapter-name.form-control', {
                  attrs: { minlength: 2, maxlength: 80 },
                  hook: onInsert<HTMLInputElement>(el => {
                    if (!el.value) {
                      el.value = data.name;
                      el.select();
                      el.focus();
                    }
                  }),
                }),
              ]),
              ...(isLoaded(data) ? viewLoaded(ctrl, data) : [spinner()]),
            ],
          ),
        ],
      })
    : undefined;
}

const isLoaded = (data: ChapterPreview | StudyChapterConfig): data is StudyChapterConfig =>
  'orientation' in data;

function viewLoaded(ctrl: StudyChapterEditForm, data: StudyChapterConfig): VNode[] {
  const mode = data.practice
      ? 'practice'
      : defined(data.conceal)
      ? 'conceal'
      : data.gamebook
      ? 'gamebook'
      : 'normal',
    noarg = ctrl.trans.noarg;
  return [
    h('div.form-split', [
      h('div.form-group.form-half', [
        h('label.form-label', { attrs: { for: 'chapter-orientation' } }, noarg('orientation')),
        h(
          'select#chapter-orientation.form-control',
          ['white', 'black'].map(color => option(color, data.orientation, noarg(color))),
        ),
      ]),
      h('div.form-group.form-half', [
        h('label.form-label', { attrs: { for: 'chapter-mode' } }, noarg('analysisMode')),
        h(
          'select#chapter-mode.form-control',
          chapterForm.modeChoices.map(c => option(c[0], mode, noarg(c[1]))),
        ),
      ]),
    ]),
    h('div.form-group', [
      h('label.form-label', { attrs: { for: 'chapter-description' } }, noarg('pinnedChapterComment')),
      h(
        'select#chapter-description.form-control',
        [
          ['', noarg('noPinnedComment')],
          ['1', noarg('rightUnderTheBoard')],
        ].map(v => option(v[0], data.description ? '1' : '', v[1])),
      ),
    ]),
    h('div.form-actions-secondary.destructive', [
      h(
        emptyRedButton,
        {
          hook: bind(
            'click',
            () => {
              if (confirm(noarg('clearAllCommentsInThisChapter'))) ctrl.clearAnnotations(data.id);
            },
            ctrl.redraw,
          ),
          attrs: { type: 'button', title: noarg('clearAllCommentsInThisChapter') },
        },
        noarg('clearAnnotations'),
      ),
      h(
        emptyRedButton,
        {
          hook: bind(
            'click',
            () => {
              if (confirm(noarg('clearVariations'))) ctrl.clearVariations(data.id);
            },
            ctrl.redraw,
          ),
          attrs: { type: 'button' },
        },
        noarg('clearVariations'),
      ),
    ]),
    h('div.form-actions', [
      h(
        emptyRedButton,
        {
          hook: bind(
            'click',
            () => {
              if (confirm(noarg('deleteThisChapter'))) ctrl.delete(data.id);
            },
            ctrl.redraw,
          ),
          attrs: { type: 'button', title: noarg('deleteThisChapter') },
        },
        noarg('deleteChapter'),
      ),
      h('button.button', { attrs: { type: 'submit' } }, noarg('saveChapter')),
    ]),
  ];
}
