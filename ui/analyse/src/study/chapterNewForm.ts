import { h, VNode } from 'snabbdom';
import { defined, prop, Prop } from 'common/common';
import { storedProp, StoredProp } from 'common/storage';
import { bind, bindSubmit, onInsert } from 'common/snabbdom';
import spinner from 'common/spinner';
import * as xhr from 'common/xhr';
import { option } from '../util';
import { variants as xhrVariants, importNotation } from './studyXhr';
import * as modal from '../modal';
import { chapter as chapterTour } from './studyTour';
import { StudyChapterMeta } from './interfaces';
import { Redraw } from '../interfaces';
import AnalyseCtrl from '../ctrl';
import { toBlackWhite } from 'shogiops/util';

export const modeChoices = [
  ['normal', 'normalAnalysis'],
  ['practice', 'practiceWithComputer'],
  ['conceal', 'hideNextMoves'],
  ['gamebook', 'interactiveLesson'],
];

export const fieldValue = (e: Event, id: string) =>
  ((e.target as HTMLElement).querySelector('#chapter-' + id) as HTMLInputElement)?.value;

export interface StudyChapterNewFormCtrl {
  root: AnalyseCtrl;
  vm: {
    variants: Variant[];
    open: boolean;
    initial: Prop<boolean>;
    tab: StoredProp<string>;
    editor: any;
    editorSfen: Prop<Sfen | null>;
  };
  open(): void;
  openInitial(): void;
  close(): void;
  toggle(): void;
  submit(d: any): void;
  chapters: Prop<StudyChapterMeta[]>;
  startTour(): void;
  redraw: Redraw;
}

export function ctrl(
  send: SocketSend,
  chapters: Prop<StudyChapterMeta[]>,
  setTab: () => void,
  root: AnalyseCtrl
): StudyChapterNewFormCtrl {
  const vm = {
    variants: [],
    open: false,
    initial: prop(false),
    tab: storedProp('study.form.tab', 'init'),
    editor: null,
    editorSfen: prop(null),
  };

  function loadVariants() {
    if (!vm.variants.length)
      xhrVariants().then(function (vs) {
        vm.variants = vs;
        root.redraw();
      });
  }

  function open() {
    vm.open = true;
    loadVariants();
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
    startTour: () =>
      chapterTour(tab => {
        vm.tab(tab);
        root.redraw();
      }),
    redraw: root.redraw,
  };
}

export function view(ctrl: StudyChapterNewFormCtrl): VNode {
  const trans = ctrl.root.trans;
  const activeTab = ctrl.vm.tab();
  const makeTab = function (key: string, name: string, title: string) {
    return h(
      'span.' + key,
      {
        class: { active: activeTab === key },
        attrs: { title },
        hook: bind('click', () => ctrl.vm.tab(key), ctrl.root.redraw),
      },
      name
    );
  };
  const currentChapter = ctrl.root.study!.data.chapter;
  const notVariantTab = activeTab === 'game' || activeTab === 'notation' || activeTab === 'edit';
  const mode = currentChapter.practice
    ? 'practice'
    : defined(currentChapter.conceal)
    ? 'conceal'
    : currentChapter.gamebook
    ? 'gamebook'
    : 'normal';
  const noarg = trans.noarg;
  let isDefaultName = true;

  return modal.modal({
    class: 'chapter-new',
    onClose() {
      ctrl.close();
      ctrl.redraw();
    },
    content: [
      activeTab === 'edit'
        ? null
        : h('h2', [
            noarg('newChapter'),
            h('i.help', {
              attrs: { 'data-icon': '' },
              hook: bind('click', ctrl.startTour),
            }),
          ]),
      h(
        'form.form3',
        {
          hook: bindSubmit(e => {
            const o: any = {
              sfen: fieldValue(e, 'sfen') || (ctrl.vm.tab() === 'edit' ? ctrl.vm.editorSfen() : null),
              isDefaultName: isDefaultName,
            };
            'name game variant notation orientation mode'.split(' ').forEach(field => {
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
              noarg('name')
            ),
            h('input#chapter-name.form-control', {
              attrs: {
                minlength: 2,
                maxlength: 80,
              },
              hook: onInsert<HTMLInputElement>(el => {
                if (!el.value) {
                  el.value = trans('chapterX', ctrl.vm.initial() ? 1 : ctrl.chapters().length + 1);
                  el.onchange = function () {
                    isDefaultName = false;
                  };
                  el.select();
                  el.focus();
                }
              }),
            }),
          ]),
          h('div.tabs-horiz', [
            makeTab('init', noarg('empty'), noarg('startFromInitialPosition')),
            makeTab('edit', noarg('editor'), noarg('startFromCustomPosition')),
            makeTab('game', 'URL', noarg('loadAGameByUrl')),
            makeTab('sfen', 'SFEN', noarg('loadAPositionFromSfen')),
            makeTab('notation', 'KIF/CSA', noarg('loadAGameFromKifCsa')),
          ]),
          activeTab === 'edit'
            ? h(
                'div.board-editor-wrap',
                {
                  hook: {
                    insert(vnode) {
                      Promise.all([
                        window.lishogi.loadScript(
                          'compiled/lishogi.editor' + ($('body').data('dev') ? '' : '.min') + '.js'
                        ),
                        xhr.json(
                          xhr.url('/editor.json', {
                            sfen: ctrl.root.node.sfen,
                          })
                        ),
                      ]).then(([_, data]) => {
                        data.embed = true;
                        data.options = {
                          orientation: currentChapter.setup.orientation,
                          onChange: ctrl.vm.editorSfen,
                        };
                        ctrl.vm.editor = window['LishogiEditor'](vnode.elm as HTMLElement, data);
                        ctrl.vm.editorSfen(ctrl.vm.editor.getSfen());
                      });
                    },
                    destroy: _ => {
                      ctrl.vm.editor = null;
                    },
                  },
                },
                [spinner()]
              )
            : null,
          activeTab === 'game'
            ? h('div.form-group', [
                h(
                  'label.form-label',
                  {
                    attrs: { for: 'chapter-game' },
                  },
                  trans('loadAGameFromXOrY', 'lishogi.org')
                ),
                h('textarea#chapter-game.form-control', {
                  attrs: { placeholder: noarg('urlOfTheGame') },
                }),
              ])
            : null,
          activeTab === 'sfen'
            ? h('div.form-group', [
                h('input#chapter-sfen.form-control', {
                  attrs: {
                    value: ctrl.root.node.sfen,
                    placeholder: noarg('loadAPositionFromSfen'),
                  },
                }),
              ])
            : null,
          activeTab === 'notation'
            ? h('div.form-groupabel', [
                h('textarea#chapter-notation.form-control', {
                  attrs: {
                    placeholder: trans.noarg('pasteTheKifCsaStringHere'),
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
                          reader.onload = function () {
                            const res = reader.result as string;
                            if (encoding === 'UTF-8' && res.match(/�/)) {
                              console.log(
                                "UTF-8 didn't work, trying shift-jis, if you still have problems with your import, try converting the file to a different encoding"
                              );
                              readFile(file, 'shift-jis');
                            } else {
                              (document.getElementById('chapter-notation') as HTMLTextAreaElement).value = res;
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
                noarg('Variant')
              ),
              h(
                'select#chapter-variant.form-control',
                {
                  attrs: { disabled: notVariantTab },
                },
                notVariantTab
                  ? [h('option', noarg('automatic'))]
                  : ctrl.vm.variants.map(v => option(v.key, currentChapter.setup.variant.key, v.name))
              ),
            ]),
            h('div.form-group.form-half', [
              h(
                'label.form-label',
                {
                  attrs: { for: 'chapter-orientation' },
                },
                noarg('orientation')
              ),
              h(
                'select#chapter-orientation.form-control',
                {
                  hook: bind('change', e => {
                    ctrl.vm.editor && ctrl.vm.editor.setOrientation((e.target as HTMLInputElement).value);
                  }),
                },
                ['sente', 'gote'].map(function (color) {
                  return option(color, currentChapter.setup.orientation, noarg(toBlackWhite(color)));
                })
              ),
            ]),
          ]),
          h('div.form-group', [
            h(
              'label.form-label',
              {
                attrs: { for: 'chapter-mode' },
              },
              noarg('analysisMode')
            ),
            h(
              'select#chapter-mode.form-control',
              modeChoices.map(c => option(c[0], mode, noarg(c[1])))
            ),
          ]),
          modal.button(noarg('createChapter')),
        ]
      ),
    ],
  });
}
