import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode'
import { prop, storedProp, Prop } from 'common';
import { bind, bindSubmit, spinner, option } from '../util';
import { variants as xhrVariants, importPgn } from './studyXhr';
import * as dialog from './dialog';
import { chapter as chapterTour } from './studyTour';
import { StudyChapterMeta } from './interfaces';
import { title as descTitle } from './chapterDescription';
import AnalyseCtrl from '../ctrl';

export const modeChoices = [
  ['normal', "Normal analysis"],
  ['practice', "Practice with computer"],
  ['conceal', "Hide next moves"],
  ['gamebook', "Interactive lesson"]
];

export function fieldValue(e: Event, id: string) {
  const el = (e.target as HTMLElement).querySelector('#chapter-' + id);
  return el ? (el as HTMLInputElement).value : null;
};

export function ctrl(send: SocketSend, chapters: Prop<StudyChapterMeta[]>, setTab: () => void, root: AnalyseCtrl) {

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

export function view(ctrl): VNode {

  const activeTab = ctrl.vm.tab();
  const makeTab = function(key: string, name: string, title: string) {
    return h('a.hint--top.' + key, {
      class: { active: activeTab === key },
      attrs: { 'data-hint': title },
      hook: bind('click', () => ctrl.vm.tab(key), ctrl.root.redraw)
    }, name);
  };
  const gameOrPgn = activeTab === 'game' || activeTab === 'pgn';
  const currentChapterSetup = ctrl.root.study.data.chapter.setup;

  return dialog.form({
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
      h('form.chapter_form.material.form', {
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
          h('input#chapter-name', {
            attrs: {
              minlength: 2,
              maxlength: 80
            },
            hook: {
              insert: vnode => {
                const el = vnode.elm as HTMLInputElement;
                if (!el.value) {
                  el.value = 'Chapter ' + (ctrl.vm.initial() ? 1 : (ctrl.chapters().length + 1));
                  el.select();
                  el.focus();
                }
              }
            }
          }),
          h('label.control-label', {
            attrs: {for: 'chapter-name' }
          }, 'Name'),
          h('i.bar')
        ]),
        h('div.study_tabs', [
          makeTab('init', 'Init', 'Start from initial position'),
          makeTab('edit', 'Edit', 'Start from custom position'),
          makeTab('game', 'URL', 'Load a game URL'),
          makeTab('fen', 'FEN', 'Load a FEN position'),
          makeTab('pgn', 'PGN', 'Load a PGN game')
        ]),
        activeTab === 'edit' ? h('div.editor_wrap.is2d', {
          hook: {
            insert: vnode => {
              $.when(
                window.lichess.loadScript('/assets/compiled/lichess.editor.min.js'),
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
          h('input#chapter-game', {
            attrs: { placeholder: 'URL of the game' }
          }),
          h('label.control-label', {
            attrs: { 'for': 'chapter-game' }
          }, 'Load a game from lichess.org or chessgames.com'),
          h('i.bar')
        ]) : null,
        activeTab === 'fen' ? h('div.form-group.no-label', [
          h('input#chapter-fen', {
            attrs: {
              value: ctrl.root.node.fen,
              placeholder: 'Initial FEN position'
            }
          }),
          h('i.bar')
        ]) : null,
        activeTab === 'pgn' ? h('div.form-group.no-label', [
          h('textarea#chapter-pgn', {
            attrs: { placeholder: 'Paste your PGN here, up to ' + ctrl.multiPgnMax + ' games' }
          }),
          h('i.bar')
        ]) : null,
        h('div', [
          h('div.form-group.half.little-margin-bottom', [
            h('select#chapter-variant', {
              attrs: { disabled: gameOrPgn }
            }, gameOrPgn ? [
              h('option', 'Automatic')
            ] :
            ctrl.vm.variants.map(v => option(v.key, currentChapterSetup.variant.key, v.name))),
            h('label.control-label', {
              attrs: { 'for': 'chapter-variant' }
            }, 'Variant'),
            h('i.bar')
          ]),
          h('div.form-group.half.little-margin-bottom', [
            h('select#chapter-orientation', {
              hook: bind('change', e => {
                ctrl.vm.editor && ctrl.vm.editor.setOrientation((e.target as HTMLInputElement).value);
              })
            }, ['White', 'Black'].map(function(color) {
              const c = color.toLowerCase();
              return option(c, currentChapterSetup.orientation, color);
            })),
            h('label.control-label', {
              attrs: { 'for': 'chapter-orientation' }
            }, 'Orientation'),
            h('i.bar')
          ])
        ]),
        h('div.form-group.little-margin-bottom', [
          h('select#chapter-mode', modeChoices.map(c => option(c[0], '', c[1]))),
          h('label.control-label', {
            attrs: { 'for': 'chapter-mode' }
          }, 'Analysis mode'),
          h('i.bar')
        ]),
        dialog.button('Create chapter')
      ])
    ]
  });
}

export function descriptionGroup(desc?: string) {
  return h('div.form-group', [
    h('select#chapter-description', [
      ['', 'None'],
      ['1', 'Right under the board']
    ].map(v => option(v[0], desc ? '1' : '', v[1]))),
    h('label.control-label', {
      attrs: { for: 'chapter-description' }
    }, descTitle),
    h('i.bar')
  ]);
}
