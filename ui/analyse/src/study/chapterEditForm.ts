import { fieldValue, modeChoices } from './chapterNewForm';
import { bind, bindSubmit, onInsert, spinnerVdom as spinner, snabDialog, confirm } from 'lib/view';
import { option, emptyRedButton } from '../view/util';
import type {
  ChapterMode,
  EditChapterData,
  Orientation,
  StudyChapterConfig,
  ChapterPreview,
} from './interfaces';
import { defined, prop } from 'lib';
import { h, type VNode } from 'snabbdom';
import type { StudySocketSend } from '../socket';
import { COLORS } from 'chessops';

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
        noClickAway: true,
        vnodes: [
          h('h2', i18n.study.editChapter),
          h(
            'form.form3',
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
              h('div.form-group', [
                h('label.form-label', { attrs: { for: 'chapter-name' } }, i18n.site.name),
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
        : 'normal';
  return [
    h('div.form-split', [
      h('div.form-group.form-half', [
        h('label.form-label', { attrs: { for: 'chapter-orientation' } }, i18n.study.orientation),
        h(
          'select#chapter-orientation.form-control',
          COLORS.map(color => option(color, data.orientation, i18n.site[color])),
        ),
      ]),
      h('div.form-group.form-half' + (ctrl.isBroadcast ? '.none' : ''), [
        h('label.form-label', { attrs: { for: 'chapter-mode' } }, i18n.study.analysisMode),
        h(
          'select#chapter-mode.form-control',
          modeChoices.map(c => option(c[0], mode, c[1])),
        ),
      ]),
    ]),
    h('div.form-group' + (ctrl.isBroadcast ? '.none' : ''), [
      h('label.form-label', { attrs: { for: 'chapter-description' } }, i18n.study.pinnedChapterComment),
      h(
        'select#chapter-description.form-control',
        [
          ['', i18n.study.noPinnedComment],
          ['1', i18n.study.rightUnderTheBoard],
        ].map(v => option(v[0], data.description ? '1' : '', v[1])),
      ),
    ]),
    h('div.form-actions-secondary.destructive', [
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
    h('div.form-actions', [
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
      h('button.button', { attrs: { type: 'submit' } }, i18n.study.saveChapter),
    ]),
  ];
}
