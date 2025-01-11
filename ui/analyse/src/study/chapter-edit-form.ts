import { type Prop, defined, prop } from 'common/common';
import * as modal from 'common/modal';
import { bind, bindSubmit, onInsert } from 'common/snabbdom';
import spinner from 'common/spinner';
import { i18n } from 'i18n';
import { colorName } from 'shogi/color-name';
import { type VNode, h } from 'snabbdom';
import type { Redraw } from '../interfaces';
import { emptyRedButton, option } from '../util';
import * as chapterForm from './chapter-new-form';
import type { StudyChapterConfig, StudyChapterMeta } from './interfaces';

interface StudyChapterEditFormCtrl {
  current: Prop<StudyChapterMeta | StudyChapterConfig | null>;
  open(data: StudyChapterMeta): void;
  toggle(data: StudyChapterMeta): void;
  submit(data: any): void;
  delete(id: string): void;
  clearAnnotations(id: string): void;
  //unbindFromGame(id: string): void;
  isEditing(id: string): boolean;
  redraw: Redraw;
}

export function ctrl(
  send: Socket.Send,
  chapterConfig: (id: string) => Promise<StudyChapterConfig>,
  redraw: Redraw,
): StudyChapterEditFormCtrl {
  const current = prop<StudyChapterMeta | StudyChapterConfig | null>(null);

  function open(data: StudyChapterMeta) {
    current({
      id: data.id,
      name: data.name,
      variant: data.variant,
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
    },
    delete(id) {
      send('deleteChapter', id);
      current(null);
    },
    clearAnnotations(id) {
      send('clearAnnotations', id);
      current(null);
    },
    //unbindFromGame(id) {
    //  send('unbindFromGame', id);
    //  current(null);
    //},
    isEditing,
    redraw,
  };
}

export function view(ctrl: StudyChapterEditFormCtrl): VNode | undefined {
  const data = ctrl.current();
  return data
    ? modal.modal({
        class: `study__modal.edit-${data.id}`, // full redraw when changing chapter
        onClose() {
          ctrl.current(null);
          ctrl.redraw();
        },
        content: [
          h('h2', i18n('study:editChapter')),
          h(
            'form.form3',
            {
              hook: bindSubmit(e => {
                const o: any = {};
                'name mode orientation description'.split(' ').forEach(field => {
                  o[field] = chapterForm.fieldValue(e, field);
                });
                ctrl.submit(o);
              }, ctrl.redraw),
            },
            [
              h('div.form-group', [
                h(
                  'label.form-label',
                  {
                    attrs: { for: 'chapter-name' },
                  },
                  i18n('name'),
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
              ...(isLoaded(data) ? viewLoaded(data) : [spinner()]),
            ],
          ),
          h('div.destructive', [
            h(
              emptyRedButton,
              {
                hook: bind(
                  'click',
                  _ => {
                    if (confirm(i18n('study:clearAllCommentsInThisChapter')))
                      ctrl.clearAnnotations(data.id);
                  },
                  ctrl.redraw,
                ),
                attrs: { type: 'button' },
              },
              i18n('study:clearAnnotations'),
            ),
            h(
              emptyRedButton,
              {
                hook: bind(
                  'click',
                  _ => {
                    if (confirm(i18n('study:deleteThisChapter'))) ctrl.delete(data.id);
                  },
                  ctrl.redraw,
                ),
                attrs: { type: 'button' },
              },
              i18n('study:deleteChapter'),
            ),
            // !ctrl.chapter().gameLength
            //  ? h(
            //      emptyRedButton,
            //      {
            //        hook: bind(
            //          'click',
            //          _ => {
            //            if (
            //              confirm(
            //                `This isn't reversible. Chapter will not longer be directly linked to the game. You will be able to change the mainline, but you can't share server analysis with the game.`
            //              )
            //            )
            //              ctrl.unbindFromGame(data.id);
            //          },
            //          ctrl.redraw
            //        ),
            //        attrs: { type: 'button' },
            //      },
            //      'Unbind from game'
            //    )
            //  : null,
          ]),
        ],
      })
    : undefined;
}

function isLoaded(data: StudyChapterMeta | StudyChapterConfig): data is StudyChapterConfig {
  return Object.prototype.hasOwnProperty.call(data, 'orientation');
}

function viewLoaded(data: StudyChapterConfig): VNode[] {
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
        h(
          'label.form-label',
          {
            attrs: { for: 'chapter-orientation' },
          },
          i18n('study:orientation'),
        ),
        h(
          'select#chapter-orientation.form-control',
          ['sente', 'gote'].map((color: Color) =>
            option(color, data.orientation, colorName(color, false)),
          ),
        ),
      ]),
      h('div.form-group.form-half', [
        h(
          'label.form-label',
          {
            attrs: { for: 'chapter-mode' },
          },
          i18n('study:analysisMode'),
        ),
        h(
          'select#chapter-mode.form-control',
          chapterForm.modeChoices.map(c => {
            return option(c[0], mode, c[1]);
          }),
        ),
      ]),
    ]),
    h('div.form-group', [
      h(
        'label.form-label',
        {
          attrs: { for: 'chapter-description' },
        },
        i18n('study:pinnedChapterComment'),
      ),
      h(
        'select#chapter-description.form-control',
        [
          ['', i18n('study:noPinnedComment')],
          ['1', i18n('study:rightUnderTheBoard')],
        ].map(v => option(v[0], data.description ? '1' : '', v[1])),
      ),
    ]),
    modal.button(i18n('study:saveChapter')),
  ];
}
