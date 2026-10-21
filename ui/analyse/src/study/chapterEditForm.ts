import { COLORS } from 'chessops';

import { defined, prop } from 'lib';
import {
  bind,
  bindSubmit,
  onInsert,
  spinnerVdom as spinner,
  snabDialog,
  confirm,
  div,
  label,
  select,
  button,
  h2,
  form,
  input,
  snabH as h,
  type VNode,
} from 'lib/view';

import type { StudySocketSend } from '../socket';
import { option, emptyRedButton } from '../view/util';
import { fieldValue, modeChoices } from './chapterNewForm';
import type {
  ChapterMode,
  EditChapterData,
  Orientation,
  StudyChapterConfig,
  ChapterPreview,
} from './interfaces';

export class StudyChapterEditForm {
  current = prop<ChapterPreview | StudyChapterConfig | null>(null);

  constructor(
    private readonly send: StudySocketSend,
    private readonly chapterConfig: (id: string) => Promise<StudyChapterConfig>,
    readonly isBroadcast: boolean,
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
  const data = ctrl.current();
  return data
    ? snabDialog({
        class: 'edit-' + data.id, // full redraw when changing chapter
        onClose() {
          ctrl.current(null);
          ctrl.redraw();
        },
        modal: true,
        vnodes: [
          h2(i18n.study.editChapter),
          form(
            '.form3',
            {
              hook: bindSubmit(e => {
                ctrl.submit({
                  name: fieldValue(e, 'name'),
                  mode: fieldValue(e, 'mode') as ChapterMode,
                  orientation: fieldValue(e, 'orientation') as Orientation,
                  description: fieldValue(e, 'description'),
                });
              }, ctrl.redraw),
            },
            [
              div('.form-group', [
                label('.form-label', { for: 'chapter-name' }, i18n.site.name),
                input('text')('#chapter-name.form-control', {
                  minlength: 2,
                  maxlength: 80,
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
        : 'normal';
  return [
    div('.form-split', [
      div('.form-group.form-half', [
        label('.form-label', { for: 'chapter-orientation' }, i18n.study.orientation),
        select(
          '#chapter-orientation.form-control',
          COLORS.map(color => option(color, data.orientation, i18n.site[color])),
        ),
      ]),
      div('.form-group.form-half', { class: { none: ctrl.isBroadcast } }, [
        label('.form-label', { for: 'chapter-mode' }, i18n.study.analysisMode),
        select(
          '#chapter-mode.form-control',
          modeChoices.map(c => option(c[0], mode, c[1])),
        ),
      ]),
    ]),
    div('.form-group', [
      label('.form-label', { for: 'chapter-description' }, i18n.study.pinnedChapterComment),
      select(
        '#chapter-description.form-control',
        [
          ['', i18n.study.noPinnedComment],
          ['1', i18n.study.rightUnderTheBoard],
        ].map(v => option(v[0], data.description ? '1' : '', v[1])),
      ),
    ]),
    div('.form-actions-secondary.destructive', [
      h(
        emptyRedButton,
        {
          hook: bind(
            'click',
            async () => {
              if (await confirm(i18n.study.clearAllCommentsInThisChapter)) ctrl.clearAnnotations(data.id);
            },
            ctrl.redraw,
          ),
          attrs: { type: 'button', title: i18n.study.clearAllCommentsInThisChapter },
        },
        i18n.study.clearAnnotations,
      ),
      h(
        emptyRedButton,
        {
          hook: bind(
            'click',
            async () => {
              if (await confirm(i18n.study.clearVariations)) ctrl.clearVariations(data.id);
            },
            ctrl.redraw,
          ),
          attrs: { type: 'button' },
        },
        i18n.study.clearVariations,
      ),
    ]),
    div('.form-actions', [
      h(
        emptyRedButton,
        {
          hook: bind(
            'click',
            async () => {
              if (await confirm(i18n.study.deleteThisChapter)) ctrl.delete(data.id);
            },
            ctrl.redraw,
          ),
          attrs: { type: 'button', title: i18n.study.deleteThisChapter },
        },
        i18n.study.deleteChapter,
      ),
      button('.button', { type: 'submit' }, i18n.study.saveChapter),
    ]),
  ];
}
