import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode'
import { defined, prop, Prop } from 'common';
import { storedProp, StoredProp } from 'common/storage';
import { bind, bindSubmit, spinner, option, onInsert } from '../util';
import { variants as xhrVariants, importPgn } from './studyXhr';
import * as modal from '../modal';
import { chapter as chapterTour } from './studyTour';
import { StudyChapterMeta } from './interfaces';
import { Redraw } from '../interfaces';
import { descTitle } from './description';
import AnalyseCtrl from '../ctrl';

export const modeChoices = [
  ['normal', 'Normal analysis'],
  ['practice', 'Practice with computer'],
  ['conceal', 'Hide next moves'],
  ['gamebook', 'Interactive lesson']
];

export function fieldValue(e: Event, id: string) {
  const el = (e.target as HTMLElement).querySelector('#chapter-' + id);
  return el ? (el as HTMLInputElement).value : null;
};

export interface StudyChapterNewFormCtrl {
  root: AnalyseCtrl;
  vm: {
    variants: Variant[];
    open: boolean;
    initial: Prop<boolean>;
    tab: StoredProp<string>;
    editor: any;
    editorFen: Prop<Fen | null>;
  };
  open(): void;
  openInitial(): void;
  close(): void;
  toggle(): void;
  submit(d: any): void;
  chapters: Prop<StudyChapterMeta[]>;
  startTour(): void;
  multiPgnMax: number;
  redraw: Redraw;
}

export function ctrl(send: SocketSend, chapters: Prop<StudyChapterMeta[]>, setTab: () => void, root: AnalyseCtrl): StudyChapterNewFormCtrl {

  const multiPgnMax = 20;

  const vm = {
    variants: [],
    open: false,
    initial: prop(false),
    tab: storedProp('study.form.tab', 'init'),
    editor: null,
    editorFen: prop(null)
  };

  function loadVariants() {
    if (!vm.variants.length) xhrVariants().then(function(vs) {
      vm.variants = vs;
      root.redraw();
    });
  };

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
      d.initial = vm.initial();
      d.sticky = root.study!.vm.mode.sticky;
      if (!d.pgn) send("addChapter", d);
      else importPgn(root.study!.data.id, d);
      close();
      setTab();
    },
    chapters,
    startTour: () => chapterTour(tab => {
      vm.tab(tab);
      root.redraw();
    }),
    multiPgnMax,
    redraw: root.redraw
  }
}

export function view(ctrl: StudyChapterNewFormCtrl): VNode {

  const activeTab = ctrl.vm.tab();
  const makeTab = function(key: string, name: string, title: string) {
    return h('span.' + key, {
      class: { active: activeTab === key },
      attrs: { title },
      hook: bind('click', () => ctrl.vm.tab(key), ctrl.root.redraw)
    }, name);
  };
  const gameOrPgn = activeTab === 'game' || activeTab === 'pgn';
  const currentChapter = ctrl.root.study!.data.chapter;
  const mode = currentChapter.practice ? 'practice' : (defined(currentChapter.conceal) ? 'conceal' : (currentChapter.gamebook ? 'gamebook' : 'normal'));

  return modal.modal({
    class: 'chapter-new',
    onClose() {
      ctrl.close();
      ctrl.redraw();
    },
    content: [
      activeTab === 'edit' ? null : h('h2', [
        'New chapter',
        h('i.help', {
          attrs: { 'data-icon': '' },
          hook: bind('click', ctrl.startTour)
        })
      ]),
      h('form.form3', {
        hook: bindSubmit(e => {
          const o: any = {
            fen: fieldValue(e, 'fen') || (ctrl.vm.tab() === 'edit' ? ctrl.vm.editorFen() : null)
          };
          'name game variant pgn orientation mode'.split(' ').forEach(field => {
            o[field] = fieldValue(e, field);
          });
          ctrl.submit(o);
        }, ctrl.redraw)
      }, [
        h('div.form-group', [
          h('label.form-label', {
            attrs: {for: 'chapter-name' }
          }, 'Name'),
          h('input#chapter-name.form-control', {
            attrs: {
              minlength: 2,
              maxlength: 80
            },
            hook: onInsert<HTMLInputElement>(el => {
                if (!el.value) {
                  el.value = 'Chapter ' + (ctrl.vm.initial() ? 1 : (ctrl.chapters().length + 1));
                  el.select();
                  el.focus();
                }
            })
          })
        ]),
        h('div.tabs-horiz', [
          makeTab('init', 'Empty', 'Start from initial position'),
          makeTab('edit', 'Editor', 'Start from custom position'),
          makeTab('game', 'URL', 'Load a game URL'),
          makeTab('fen', 'FEN', 'Load a FEN position'),
          makeTab('pgn', 'PGN', 'Load a PGN game')
        ]),
        activeTab === 'edit' ? h('div.board-editor-wrap', {
          hook: {
            insert: vnode => {
              $.when(
                window.lichess.loadScript('compiled/lichess.editor.min.js'),
                $.get('/editor.json', {
                  fen: ctrl.root.node.fen
                })
              ).then(function(_, b) {
                const data = b[0];
                data.embed = true;
                data.options = {
                  inlineCastling: true,
                  onChange: ctrl.vm.editorFen
                };
                ctrl.vm.editor = window['LichessEditor'](vnode.elm as HTMLElement, data);
                ctrl.vm.editorFen(ctrl.vm.editor.getFen());
              });
            },
            destroy: _ => {
              ctrl.vm.editor = null;
            }
          }
        }, [spinner()]) : null,
        activeTab === 'game' ? h('div.form-group', [
          h('label.form-label', {
            attrs: { 'for': 'chapter-game' }
          }, 'Load a game from lichess.org or chessgames.com'),
          h('input#chapter-game.form-control', {
            attrs: { placeholder: 'URL of the game' }
          })
        ]) : null,
        activeTab === 'fen' ? h('div.form-group', [
          h('input#chapter-fen.form-control', {
            attrs: {
              value: ctrl.root.node.fen,
              placeholder: 'Initial FEN position'
            }
          })
        ]) : null,
        activeTab === 'pgn' ? h('div.form-groupabel', [
          h('textarea#chapter-pgn.form-control', {
            attrs: { placeholder: 'Paste your PGN text here, up to ' + ctrl.multiPgnMax + ' games' }
          }),
          window.FileReader ? h('input#chapter-pgn-file.form-control', {
            attrs: {
              type: 'file',
              accept: '.pgn'
            },
            hook: bind('change', e => {
              const file = (e.target as HTMLInputElement).files![0];
              if (!file) return;
              const reader = new FileReader();
              reader.onload = function() {
                (document.getElementById('chapter-pgn') as HTMLTextAreaElement).value = reader.result as string;
              };
              reader.readAsText(file);
            })
          }) : null
        ]) : null,
        h('div.form-split', [
          h('div.form-group.form-half', [
            h('label.form-label', {
              attrs: { 'for': 'chapter-variant' }
            }, 'Variant'),
            h('select#chapter-variant.form-control', {
              attrs: { disabled: gameOrPgn }
            }, gameOrPgn ? [
              h('option', 'Automatic')
            ] :
            ctrl.vm.variants.map(v => option(v.key, currentChapter.setup.variant.key, v.name)))
          ]),
          h('div.form-group.form-half', [
            h('label.form-label', {
              attrs: { 'for': 'chapter-orientation' }
            }, 'Orientation'),
            h('select#chapter-orientation.form-control', {
              hook: bind('change', e => {
                ctrl.vm.editor && ctrl.vm.editor.setOrientation((e.target as HTMLInputElement).value);
              })
            }, ['White', 'Black'].map(function(color) {
              const c = color.toLowerCase();
              return option(c, currentChapter.setup.orientation, color);
            }))
          ])
        ]),
        h('div.form-group', [
          h('label.form-label', {
            attrs: { 'for': 'chapter-mode' }
          }, 'Analysis mode'),
          h('select#chapter-mode.form-control', modeChoices.map(c => option(c[0], mode, c[1])))
        ]),
        modal.button('Create chapter')
      ])
    ]
  });
}

export function descriptionGroup(desc?: string) {
  return h('div.form-group', [
    h('label.form-label', {
      attrs: { for: 'chapter-description' }
    }, descTitle(true)),
    h('select#chapter-description.form-control', [
      ['', 'None'],
      ['1', 'Right under the board']
    ].map(v => option(v[0], desc ? '1' : '', v[1])))
  ]);
}
