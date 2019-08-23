import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode'
import { defined, prop, Prop } from 'common';
import { storedProp, StoredProp } from 'common/storage';
import { bind, bindSubmit, spinner, option, onInsert } from '../util';
import { variants as xhrVariants, importPdn } from './studyXhr';
import * as modal from '../modal';
import { chapter as chapterTour } from './studyTour';
import { StudyChapterMeta } from './interfaces';
import { Redraw } from '../interfaces';
import AnalyseCtrl from '../ctrl';

export const modeChoices = [
  ['normal', 'normalAnalysis'],
  //['practice', 'practiceWithComputer'],
  ['conceal', 'hideNextMoves'],
  ['gamebook', 'interactiveLesson']
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
  multiPdnMax: number;
  redraw: Redraw;
}

export function ctrl(send: SocketSend, chapters: Prop<StudyChapterMeta[]>, setTab: () => void, root: AnalyseCtrl): StudyChapterNewFormCtrl {

  const multiPdnMax = 20;

  const vm = {
    variants: [],
    open: false,
    initial: prop(false),
    tab: storedProp('study.form.tab', 'init'),
    editor: null,
    editorFen: prop(null)
  };

  function loadVariants() {
    if (!vm.variants.length) xhrVariants().then(function (vs) {
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
      const study = root.study!;
      d.initial = vm.initial();
      d.sticky = study.vm.mode.sticky;
      if (!d.pdn) send("addChapter", d);
      else importPdn(study.data.id, d, study.sri);
      close();
      setTab();
    },
    chapters,
    startTour: () => chapterTour(tab => {
      vm.tab(tab);
      root.redraw();
    }),
    multiPdnMax,
    redraw: root.redraw
  }
}

export function view(ctrl: StudyChapterNewFormCtrl): VNode {

  const trans = ctrl.root.trans;
  const activeTab = ctrl.vm.tab();
  const makeTab = function(key: string, name: string, title: string) {
    return h('span.' + key, {
      class: { active: activeTab === key },
      attrs: { title },
      hook: bind('click', () => ctrl.vm.tab(key), ctrl.root.redraw)
    }, name);
  };
  const gameOrPdn = activeTab === 'game' || activeTab === 'pdn';
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
        trans.noarg('newChapter'),
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
          'name game variant pdn orientation mode'.split(' ').forEach(field => {
            o[field] = fieldValue(e, field);
          });
          ctrl.submit(o);
        }, ctrl.redraw)
      }, [
        h('div.form-group', [
          h('label.form-label', {
            attrs: {for: 'chapter-name' }
          }, trans.noarg('name')),
          h('input#chapter-name.form-control', {
            attrs: {
              minlength: 2,
              maxlength: 80
            },
            hook: onInsert<HTMLInputElement>(el => {
              if (!el.value) {
                el.value = trans('chapterX', (ctrl.vm.initial() ? 1 : (ctrl.chapters().length + 1)));
                el.select();
                el.focus();
              }
            })
          })
        ]),
        h('div.tabs-horiz', [
          makeTab('init', trans.noarg('empty'), trans.noarg('startFromInitialPosition')),
          makeTab('edit', trans.noarg('editor'), trans.noarg('startFromCustomPosition')),
          makeTab('game', 'URL', trans.noarg('loadAGameByUrl')),
          makeTab('fen', 'FEN', trans.noarg('loadAPositionFromFen')),
          makeTab('pdn', 'PDN', trans.noarg('loadAGameFromPdn'))
        ]),
        activeTab === 'edit' ? h('div.board-editor-wrap.is2d', {
          hook: {
            insert: vnode => {
              $.when(
                window.lidraughts.loadScript('compiled/lidraughts.editor.min.js'),
                $.get('/editor.json', {
                  fen: ctrl.root.node.fen,
                  variant: currentChapter.setup.variant.key
                })
              ).then(function (_, b) {
                const data = b[0];
                data.embed = true;
                data.options = {
                  inlineCastling: true,
                  onChange: ctrl.vm.editorFen
                };
                ctrl.vm.editor = window['LidraughtsEditor'](vnode.elm as HTMLElement, data);
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
          }, 'Load a game from lidraughts.org'),
          h('input#chapter-game.form-control', {
            attrs: { placeholder: trans.noarg('urlOfTheGame') }
          })
        ]) : null,
        activeTab === 'fen' ? h('div.form-group', [
          h('input#chapter-fen.form-control', {
            attrs: {
              value: ctrl.root.node.fen,
              placeholder: trans.noarg('loadAPositionFromFen')
            }
          })
        ]) : null,
        activeTab === 'pdn' ? h('div.form-group', [
          h('textarea#chapter-pdn.form-control', {
            attrs: { placeholder: trans.plural('pasteYourPdnTextHereUpToNbGames', ctrl.multiPdnMax) }
          }),
          window.FileReader ? h('input#chapter-pdn-file.form-control', {
            attrs: {
              type: 'file',
              accept: '.pdn'
            },
            hook: bind('change', e => {
              const file = (e.target as HTMLInputElement).files![0];
              if (!file) return;
              const reader = new FileReader();
              reader.onload = function() {
                (document.getElementById('chapter-pdn') as HTMLTextAreaElement).value = reader.result as string;
              };
              reader.readAsText(file);
            })
          }) : null
        ]) : null,
        h('div.form-split', [
          h('div.form-group.form-half', [
            h('label.form-label', {
              attrs: { 'for': 'chapter-variant' }
            }, trans.noarg('variant')),
            h('select#chapter-variant.form-control', {
              attrs: { disabled: gameOrPdn }
            }, gameOrPdn ? [
              h('option', trans.noarg('automatic'))
            ] :
            ctrl.vm.variants.map(v => option(v.key, currentChapter.setup.variant.key, v.name)))
          ]),
          h('div.form-group.form-half', [
            h('label.form-label', {
              attrs: { 'for': 'chapter-orientation' }
            }, trans.noarg('orientation')),
            h('select#chapter-orientation.form-control', {
              hook: bind('change', e => {
                ctrl.vm.editor && ctrl.vm.editor.setOrientation((e.target as HTMLInputElement).value);
              })
            }, ['white', 'black'].map(function(color) {
              return option(color, currentChapter.setup.orientation, trans.noarg(color));
            }))
          ])
        ]),
        h('div.form-group', [
          h('label.form-label', {
            attrs: { 'for': 'chapter-mode' }
          }, trans.noarg('analysisMode')),
          h('select#chapter-mode.form-control', modeChoices.map(c => option(c[0], mode, trans.noarg(c[1]))))
        ]),
        modal.button(trans.noarg('createChapter'))
      ])
    ]
  });
}
