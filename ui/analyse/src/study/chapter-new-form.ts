import { loadCompiledScript } from 'common/assets';
import { type Prop, defined, prop } from 'common/common';
import * as modal from 'common/modal';
import { bind, bindSubmit, onInsert } from 'common/snabbdom';
import spinner from 'common/spinner';
import { type StoredProp, storedProp } from 'common/storage';
import { i18n, i18nFormat } from 'i18n';
import { i18nVariant } from 'i18n/variant';
import { colorName } from 'shogi/color-name';
import { RULES } from 'shogiops/constants';
import { type VNode, h } from 'snabbdom';
import type AnalyseCtrl from '../ctrl';
import { option } from '../util';
import type { StudyChapterMeta } from './interfaces';
import { importNotation } from './study-xhr';

export const modeChoices: [string, string][] = [
  ['normal', i18n('study:normalAnalysis')],
  ['practice', i18n('practiceWithComputer')],
  ['conceal', i18n('study:hideNextMoves')],
  ['gamebook', i18n('study:interactiveLesson')],
];

export const fieldValue = (e: Event, id: string): string =>
  ((e.target as HTMLElement).querySelector(`#chapter-${id}`) as HTMLInputElement)?.value;

export interface StudyChapterNewFormCtrl {
  root: AnalyseCtrl;
  vm: {
    open: boolean;
    initial: Prop<boolean>;
    tab: StoredProp<string>;
    editor: any;
    editorSfen: Prop<Sfen | null>;
    editorVariant: Prop<VariantKey | null>;
    editorOrientation: Prop<Color | null>;
  };
  open(): void;
  openInitial(): void;
  close(): void;
  toggle(): void;
  submit(d: any): void;
  chapters: Prop<StudyChapterMeta[]>;
  redraw: Redraw;
}

export function ctrl(
  send: Socket.Send,
  chapters: Prop<StudyChapterMeta[]>,
  setTab: () => void,
  root: AnalyseCtrl,
): StudyChapterNewFormCtrl {
  const vm = {
    open: false,
    initial: prop(false),
    tab: storedProp('study.form.tab', 'init'),
    editor: null,
    editorSfen: prop(null),
    editorVariant: prop(null),
    editorOrientation: prop(null),
  };

  function open() {
    vm.open = true;
    vm.initial(false);
  }
  function close() {
    vm.open = false;
  }

  return {
    vm,
    open,
    root,
    openInitial() {
      open();
      vm.initial(true);
    },
    close,
    toggle() {
      if (vm.open) close();
      else open();
    },
    submit(d) {
      const study = root.study!;
      d.initial = vm.initial();
      d.sticky = study.vm.mode.sticky;
      if (!d.notation) send('addChapter', d);
      else importNotation(study.data.id, d);
      close();
      setTab();
    },
    chapters,
    redraw: root.redraw,
  };
}

export function view(ctrl: StudyChapterNewFormCtrl): VNode {
  const activeTab = ctrl.vm.tab();
  const makeTab = (key: string, name: string, title: string) =>
    h(
      `span.${key}`,
      {
        class: { active: activeTab === key },
        attrs: { title },
        hook: bind('click', () => ctrl.vm.tab(key), ctrl.root.redraw),
      },
      name,
    );
  const currentChapter = ctrl.root.study!.data.chapter;
  const notVariantTab = activeTab === 'game' || activeTab === 'notation' || activeTab === 'edit';
  const notOrientationTab = activeTab === 'edit';
  const mode = currentChapter.practice
    ? 'practice'
    : defined(currentChapter.conceal)
      ? 'conceal'
      : currentChapter.gamebook
        ? 'gamebook'
        : 'normal';
  let isDefaultName = true;

  return modal.modal({
    class: 'study__modal.chapter-new',
    onClose() {
      ctrl.close();
      ctrl.redraw();
    },
    content: [
      activeTab === 'edit' ? null : h('h2', [i18n('study:newChapter')]),
      h(
        'form.form3',
        {
          hook: bindSubmit(e => {
            const o: any = {
              sfen:
                fieldValue(e, 'sfen') || (ctrl.vm.tab() === 'edit' ? ctrl.vm.editorSfen() : null),
              variant:
                (ctrl.vm.tab() === 'edit' && ctrl.vm.editorVariant()) || fieldValue(e, 'variant'),
              orientation:
                (ctrl.vm.tab() === 'edit' && ctrl.vm.editorOrientation()) ||
                fieldValue(e, 'orientation'),
              isDefaultName: isDefaultName,
            };
            'name game notation mode'.split(' ').forEach(field => {
              o[field] = fieldValue(e, field);
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
                  el.value = i18nFormat(
                    'study:chapterX',
                    ctrl.vm.initial() ? 1 : ctrl.chapters().length + 1,
                  );
                  el.onchange = () => {
                    isDefaultName = false;
                  };
                  el.select();
                  el.focus();
                }
              }),
            }),
          ]),
          h('div.tabs-horiz', [
            makeTab('init', i18n('study:empty'), i18n('study:startFromInitialPosition')),
            makeTab('edit', i18n('study:editor'), i18n('study:startFromCustomPosition')),
            makeTab('game', 'URL', i18n('study:loadAGameByUrl')),
            makeTab('sfen', 'SFEN', i18n('study:loadAPositionFromSfen')),
            makeTab('notation', 'KIF/CSA', 'KIF/CSA'),
          ]),
          activeTab === 'edit'
            ? h(
                'div.board-editor-wrap',
                {
                  hook: {
                    insert(vnode) {
                      Promise.all([
                        loadCompiledScript('lishogi.editor'),
                        window.lishogi.xhr.json('GET', '/editor.json', {
                          url: { sfen: ctrl.root.node.sfen },
                        }),
                      ]).then(([_, data]) => {
                        data.embed = true;
                        data.options = {
                          orientation: currentChapter.setup.orientation,
                          onChange: (sfen, variant, orientation) => {
                            ctrl.vm.editorSfen(sfen);
                            ctrl.vm.editorVariant(variant);
                            ctrl.vm.editorOrientation(orientation);
                          },
                        };
                        ctrl.vm.editor = window.lishogi.modules.editor?.({
                          element: vnode.elm as HTMLElement,
                          ...data,
                        });
                        ctrl.vm.editorSfen(ctrl.vm.editor.getSfen());
                      });
                    },
                    destroy: _ => {
                      ctrl.vm.editor = null;
                    },
                  },
                },
                [spinner()],
              )
            : null,
          activeTab === 'game'
            ? h('div.form-group', [
                h(
                  'label.form-label',
                  {
                    attrs: { for: 'chapter-game' },
                  },
                  i18nFormat('study:loadAGameFromX', 'lishogi.org'),
                ),
                h('textarea#chapter-game.form-control', {
                  attrs: { placeholder: i18n('study:urlOfTheGame') },
                }),
              ])
            : null,
          activeTab === 'sfen'
            ? h('div.form-group', [
                h('input#chapter-sfen.form-control', {
                  attrs: {
                    value: ctrl.root.node.sfen,
                    placeholder: i18n('study:loadAPositionFromSfen'),
                  },
                }),
              ])
            : null,
          activeTab === 'notation'
            ? h('div.form-groupabel', [
                h('textarea#chapter-notation.form-control', {
                  attrs: {
                    placeholder: i18n('pasteTheKifCsaStringHere'),
                  },
                }),
                window.FileReader
                  ? h('input#chapter-notation-file.form-control', {
                      attrs: {
                        type: 'file',
                        accept: '.kif, .kifu, .csa',
                      },
                      hook: bind('change', e => {
                        function readFile(file: File, encoding: string) {
                          if (!file) return;
                          const reader = new FileReader();
                          reader.onload = () => {
                            const res = reader.result as string;
                            if (encoding === 'UTF-8' && res.match(/�/)) {
                              console.log(
                                "UTF-8 didn't work, trying shift-jis, if you still have problems with your import, try converting the file to a different encoding",
                              );
                              readFile(file, 'shift-jis');
                            } else {
                              (
                                document.getElementById('chapter-notation') as HTMLTextAreaElement
                              ).value = res;
                            }
                          };
                          reader.readAsText(file, encoding);
                        }
                        const file = (e.target as HTMLInputElement).files![0];
                        readFile(file, 'UTF-8');
                      }),
                    })
                  : null,
              ])
            : null,
          h('div.form-split', [
            h('div.form-group.form-half', [
              h(
                'label.form-label',
                {
                  attrs: { for: 'chapter-variant' },
                },
                i18n('variant'),
              ),
              h(
                'select#chapter-variant.form-control',
                {
                  attrs: { disabled: notVariantTab },
                },
                notVariantTab
                  ? [h('option', i18n('study:automatic'))]
                  : RULES.map(r => option(r, currentChapter.setup.variant.key, i18nVariant(r))),
              ),
            ]),
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
                {
                  attrs: { disabled: notOrientationTab },
                },
                notOrientationTab
                  ? [h('option', i18n('study:automatic'))]
                  : ['sente', 'gote'].map((color: Color) =>
                      option(color, currentChapter.setup.orientation, colorName(color, false)),
                    ),
              ),
            ]),
          ]),
          h('div.form-group', [
            h(
              'label.form-label',
              {
                attrs: { for: 'chapter-mode' },
              },
              i18n('study:analysisMode'),
            ),
            h(
              'select#chapter-mode.form-control',
              modeChoices.map(c => option(c[0], mode, c[1])),
            ),
          ]),
          modal.button(i18n('study:createChapter')),
        ],
      ),
    ],
  });
}
