import * as chapterForm from './chapterNewForm';
import { bind, bindSubmit, onInsert } from 'common/snabbdom';
import spinner from 'common/spinner';
import { option, emptyRedButton } from '../util';
import { ChapterMode, EditChapterData, Orientation, StudyChapterConfig, StudyChapterMeta } from './interfaces';
import { defined, prop, Prop } from 'common';
import { h, VNode } from 'snabbdom';
import { modalButton } from './chapterNewForm';
import { Redraw } from '../interfaces';
import { snabModal } from 'common/modal';
import { StudySocketSend } from '../socket';

export interface StudyChapterEditFormCtrl {
  current: Prop<StudyChapterMeta | StudyChapterConfig | null>;
  open(data: StudyChapterMeta): void;
  toggle(data: StudyChapterMeta): void;
  submit(data: Omit<EditChapterData, 'id'>): void;
  delete(id: string): void;
  clearAnnotations(id: string): void;
  isEditing(id: string): boolean;
  redraw: Redraw;
  trans: Trans;
}

export function ctrl(
  send: StudySocketSend,
  chapterConfig: (id: string) => Promise<StudyChapterConfig>,
  trans: Trans,
  redraw: Redraw
): StudyChapterEditFormCtrl {
  const current = prop<StudyChapterMeta | StudyChapterConfig | null>(null);

  function open(data: StudyChapterMeta) {
    current({
      id: data.id,
      name: data.name,
    });
    chapterConfig(data.id).then(d => {
      current(d!);
      redraw();
    });
  }

  function isEditing(id: string) {
    const c = current();
    return c ? c.id === id : false;
  }

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
        send('editChapter', { id: c.id, ...data });
        current(null);
      }
    },
    delete(id) {
      send('deleteChapter', id);
      current(null);
    },
    clearAnnotations(id) {
      send('clearAnnotations', id);
      current(null);
    },
    isEditing,
    trans,
    redraw,
  };
}

export function view(ctrl: StudyChapterEditFormCtrl): VNode | undefined {
  const data = ctrl.current();
  return data
    ? snabModal({
        class: 'edit-' + data.id, // full redraw when changing chapter
        onClose() {
          ctrl.current(null);
          ctrl.redraw();
        },
        content: [
          h('h2', ctrl.trans.noarg('editChapter')),
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
                h(
                  'label.form-label',
                  {
                    attrs: { for: 'chapter-name' },
                  },
                  ctrl.trans.noarg('name')
                ),
                h('input#chapter-name.form-control', {
                  attrs: {
                    minlength: 2,
                    maxlength: 80,
                  },
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
            ]
          ),
          h('div.destructive', [
            h(
              emptyRedButton,
              {
                hook: bind(
                  'click',
                  _ => {
                    if (confirm(ctrl.trans.noarg('clearAllCommentsInThisChapter'))) ctrl.clearAnnotations(data.id);
                  },
                  ctrl.redraw
                ),
                attrs: { type: 'button' },
              },
              ctrl.trans.noarg('clearAnnotations')
            ),
            h(
              emptyRedButton,
              {
                hook: bind(
                  'click',
                  _ => {
                    if (confirm(ctrl.trans.noarg('deleteThisChapter'))) ctrl.delete(data.id);
                  },
                  ctrl.redraw
                ),
                attrs: { type: 'button' },
              },
              ctrl.trans.noarg('deleteChapter')
            ),
          ]),
        ],
      })
    : undefined;
}

function isLoaded(data: StudyChapterMeta | StudyChapterConfig): data is StudyChapterConfig {
  return 'orientation' in data;
}

function viewLoaded(ctrl: StudyChapterEditFormCtrl, data: StudyChapterConfig): VNode[] {
  const mode = data.practice ? 'practice' : defined(data.conceal) ? 'conceal' : data.gamebook ? 'gamebook' : 'normal';
  return [
    h('div.form-split', [
      h('div.form-group.form-half', [
        h(
          'label.form-label',
          {
            attrs: { for: 'chapter-orientation' },
          },
          ctrl.trans.noarg('orientation')
        ),
        h(
          'select#chapter-orientation.form-control',
          ['white', 'black'].map(function (color) {
            return option(color, data.orientation, ctrl.trans.noarg(color));
          })
        ),
      ]),
      h('div.form-group.form-half', [
        h(
          'label.form-label',
          {
            attrs: { for: 'chapter-mode' },
          },
          ctrl.trans.noarg('analysisMode')
        ),
        h(
          'select#chapter-mode.form-control',
          chapterForm.modeChoices.map(c => {
            return option(c[0], mode, ctrl.trans.noarg(c[1]));
          })
        ),
      ]),
    ]),
    h('div.form-group', [
      h(
        'label.form-label',
        {
          attrs: { for: 'chapter-description' },
        },
        ctrl.trans.noarg('pinnedChapterComment')
      ),
      h(
        'select#chapter-description.form-control',
        [
          ['', ctrl.trans.noarg('noPinnedComment')],
          ['1', ctrl.trans.noarg('rightUnderTheBoard')],
        ].map(v => option(v[0], data.description ? '1' : '', v[1]))
      ),
    ]),
    modalButton(ctrl.trans.noarg('saveChapter')),
  ];
}
