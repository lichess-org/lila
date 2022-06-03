import { parseFen } from 'chessops/fen';
import { defined, prop, Prop } from 'common';
import { snabModal } from 'common/modal';
import { bind, bindSubmit, onInsert } from 'common/snabbdom';
import { storedProp, StoredProp } from 'common/storage';
import * as xhr from 'common/xhr';
import { h, VNode } from 'snabbdom';
import AnalyseCtrl from '../ctrl';
import { Redraw } from '../interfaces';
import { StudySocketSend } from '../socket';
import { spinnerVdom as spinner } from 'common/spinner';
import { option } from '../util';
import { ChapterData, ChapterMode, Orientation, StudyChapterMeta } from './interfaces';
import { chapter as chapterTour } from './studyTour';
import { importPgn, variants as xhrVariants } from './studyXhr';

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
    editor: LichessEditor | null;
    editorFen: Prop<Fen | null>;
    isDefaultName: boolean;
  };
  open(): void;
  openInitial(): void;
  close(): void;
  toggle(): void;
  submit(d: Omit<ChapterData, 'initial'>): void;
  chapters: Prop<StudyChapterMeta[]>;
  startTour(): void;
  multiPgnMax: number;
  redraw: Redraw;
}

export function ctrl(
  send: StudySocketSend,
  chapters: Prop<StudyChapterMeta[]>,
  setTab: () => void,
  root: AnalyseCtrl
): StudyChapterNewFormCtrl {
  const multiPgnMax = 32;

  const vm = {
    variants: [],
    open: false,
    initial: prop(false),
    tab: storedProp('study.form.tab', 'init'),
    editor: null,
    editorFen: prop(null),
    isDefaultName: true,
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
      const dd = {
        ...d,
        sticky: study.vm.mode.sticky,
        initial: vm.initial(),
      };
      if (!dd.pgn) send('addChapter', dd);
      else importPgn(study.data.id, dd);
      close();
      setTab();
    },
    chapters,
    startTour: () =>
      chapterTour(tab => {
        vm.tab(tab);
        root.redraw();
      }),
    multiPgnMax,
    redraw: root.redraw,
  };
}

export function view(ctrl: StudyChapterNewFormCtrl): VNode {
  const trans = ctrl.root.trans,
    study = ctrl.root.study!;
  const activeTab = ctrl.vm.tab();
  const makeTab = function (key: string, name: string, title: string) {
    return h(
      'span.' + key,
      {
        class: { active: activeTab === key },
        attrs: { role: 'tab', title },
        hook: bind('click', () => ctrl.vm.tab(key), ctrl.root.redraw),
      },
      name
    );
  };
  const gameOrPgn = activeTab === 'game' || activeTab === 'pgn';
  const currentChapter = study.data.chapter;
  const mode = currentChapter.practice
    ? 'practice'
    : defined(currentChapter.conceal)
    ? 'conceal'
    : currentChapter.gamebook
    ? 'gamebook'
    : 'normal';
  const noarg = trans.noarg;

  return snabModal({
    class: 'chapter-new',
    onClose() {
      ctrl.close();
      ctrl.redraw();
    },
    noClickAway: true,
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
            ctrl.submit({
              name: fieldValue(e, 'name'),
              game: fieldValue(e, 'game'),
              variant: fieldValue(e, 'variant') as VariantKey,
              pgn: fieldValue(e, 'pgn'),
              orientation: fieldValue(e, 'orientation') as Orientation,
              mode: fieldValue(e, 'mode') as ChapterMode,
              fen: fieldValue(e, 'fen') || (ctrl.vm.tab() === 'edit' ? ctrl.vm.editorFen() : null),
              isDefaultName: ctrl.vm.isDefaultName,
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
                    ctrl.vm.isDefaultName = false;
                  };
                  el.select();
                  el.focus();
                }
              }),
            }),
          ]),
          h('div.tabs-horiz', { attrs: { role: 'tablist' } }, [
            makeTab('init', noarg('empty'), noarg('startFromInitialPosition')),
            makeTab('edit', noarg('editor'), noarg('startFromCustomPosition')),
            makeTab('game', 'URL', noarg('loadAGameByUrl')),
            makeTab('fen', 'FEN', noarg('loadAPositionFromFen')),
            makeTab('pgn', 'PGN', noarg('loadAGameFromPgn')),
          ]),
          activeTab === 'edit'
            ? h(
                'div.board-editor-wrap',
                {
                  hook: {
                    insert(vnode) {
                      Promise.all([lichess.loadModule('editor'), xhr.json('/editor.json')]).then(
                        ([_, data]: [unknown, Editor.Config]) => {
                          data.fen = ctrl.root.node.fen;
                          data.embed = true;
                          data.options = {
                            inlineCastling: true,
                            orientation: currentChapter.setup.orientation,
                            onChange: ctrl.vm.editorFen,
                          };
                          ctrl.vm.editor = window.LichessEditor!(vnode.elm as HTMLElement, data);
                          ctrl.vm.editorFen(ctrl.vm.editor.getFen());
                        }
                      );
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
                  trans('loadAGameFromXOrY', 'lichess.org', 'chessgames.com')
                ),
                h('textarea#chapter-game.form-control', {
                  attrs: {
                    placeholder: noarg('urlOfTheGame'),
                  },
                  hook: onInsert((el: HTMLTextAreaElement) => {
                    el.addEventListener('change', () => el.reportValidity());
                    el.addEventListener('input', _ => {
                      const ok = el.value
                        .trim()
                        .split('\n')
                        .every(line =>
                          line
                            .trim()
                            .match(
                              new RegExp(
                                `^((.*${location.host}/\\w{8,12}.*)|\\w{8}|\\w{12}|(.*chessgames\\.com/.*[?&]gid=\\d+.*)|)$`
                              )
                            )
                        );
                      el.setCustomValidity(ok ? '' : 'Invalid game ID(s) or URL(s)');
                    });
                  }),
                }),
              ])
            : null,
          activeTab === 'fen'
            ? h('div.form-group', [
                h('input#chapter-fen.form-control', {
                  attrs: {
                    value: ctrl.root.node.fen,
                    placeholder: noarg('loadAPositionFromFen'),
                  },
                  hook: onInsert((el: HTMLInputElement) => {
                    el.addEventListener('change', () => el.reportValidity());
                    el.addEventListener('input', _ =>
                      el.setCustomValidity(parseFen(el.value.trim()).isOk ? '' : 'Invalid FEN')
                    );
                  }),
                }),
              ])
            : null,
          activeTab === 'pgn'
            ? h('div.form-group', [
                h('textarea#chapter-pgn.form-control', {
                  attrs: { placeholder: trans.plural('pasteYourPgnTextHereUpToNbGames', ctrl.multiPgnMax) },
                }),
                h(
                  'a.button.button-empty',
                  {
                    hook: bind('click', () => {
                      xhr
                        .text(`/study/${study.data.id}/${currentChapter.id}.pgn`)
                        .then(pgnData => $('#chapter-pgn').val(pgnData));
                    }),
                  },
                  trans('importFromChapterX', study.currentChapter().name)
                ),
                window.FileReader
                  ? h('input#chapter-pgn-file.form-control', {
                      attrs: {
                        type: 'file',
                        accept: '.pgn',
                      },
                      hook: bind('change', e => {
                        const file = (e.target as HTMLInputElement).files![0];
                        if (!file) return;
                        const reader = new FileReader();
                        reader.onload = function () {
                          (document.getElementById('chapter-pgn') as HTMLTextAreaElement).value =
                            reader.result as string;
                        };
                        reader.readAsText(file);
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
                  attrs: { disabled: gameOrPgn },
                },
                gameOrPgn
                  ? [
                      h(
                        'option',
                        {
                          attrs: { value: 'standard' },
                        },
                        noarg('automatic')
                      ),
                    ]
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
                    ctrl.vm.editor && ctrl.vm.editor.setOrientation((e.target as HTMLInputElement).value as Color);
                  }),
                },
                [...(activeTab === 'pgn' ? ['automatic'] : []), 'white', 'black'].map(c =>
                  option(c, currentChapter.setup.orientation, noarg(c))
                )
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
          h(
            'div.form-actions.single',
            h(
              'button.button',
              {
                attrs: { type: 'submit' },
              },
              noarg('createChapter')
            )
          ),
        ]
      ),
    ],
  });
}
