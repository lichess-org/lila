import { h, VNode } from 'snabbdom';
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
  submit(data: any): void;
  delete(id: string): void;
  clearAnnotations(id: string): void;
  isEditing(id: string): boolean;
  redraw: Redraw;
  trans: Trans;
}

export function ctrl(
  send: SocketSend,
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
        data.id = c.id;
        send('editChapter', data);
        current(null);
      }
      redraw();
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
    ? modal.modal({
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
                const o: any = {};
                'name mode orientation description'.split(' ').forEach(field => {
                  o[field] = chapterForm.fieldValue(e, field);
                });
                ctrl.submit(o);
              }),
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
                hook: bind('click', _ => {
                  if (confirm(ctrl.trans.noarg('clearAllCommentsInThisChapter'))) ctrl.clearAnnotations(data.id);
                }),
              },
              ctrl.trans.noarg('clearAnnotations')
            ),
            h(
              emptyRedButton,
              {
                hook: bind('click', _ => {
                  if (confirm(ctrl.trans.noarg('deleteThisChapter'))) ctrl.delete(data.id);
                }),
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
    modal.button(ctrl.trans.noarg('saveChapter')),
  ];
}
