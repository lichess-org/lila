import { parseFen } from 'chessops/fen';
import { defined, prop, Prop, toggle } from 'common';
import { snabDialog } from 'common/dialog';
import * as licon from 'common/licon';
import { bind, bindSubmit, onInsert, looseH as h, dataIcon } from 'common/snabbdom';
import { storedProp } from 'common/storage';
import * as xhr from 'common/xhr';
import { VNode } from 'snabbdom';
import AnalyseCtrl from '../ctrl';
import { StudySocketSend } from '../socket';
import { spinnerVdom as spinner } from 'common/spinner';
import { option } from '../view/util';
import { ChapterData, ChapterMode, ChapterTab, Orientation, StudyTour } from './interfaces';
import { importPgn, variants as xhrVariants } from './studyXhr';
import { StudyChapters } from './studyChapters';
import { FEN } from 'chessground/types';
import type { LichessEditor } from 'editor';
import { pubsub } from 'common/pubsub';

export const modeChoices = [
  ['normal', i18n.study.normalAnalysis],
  ['practice', i18n.site.practiceWithComputer],
  ['conceal', i18n.study.hideNextMoves],
  ['gamebook', i18n.study.interactiveLesson],
];

export const fieldValue = (e: Event, id: string) =>
  ((e.target as HTMLElement).querySelector('#chapter-' + id) as HTMLInputElement)?.value;

export class StudyChapterNewForm {
  readonly multiPgnMax = 32;
  variants: Variant[] = [];
  isOpen = toggle(false);
  initial = toggle(false);
  tab = storedProp<ChapterTab>(
    'analyse.study.form.tab',
    'init',
    str => str as ChapterTab,
    v => v,
  );
  editor: LichessEditor | null = null;
  editorFen: Prop<FEN | null> = prop(null);
  isDefaultName = toggle(true);

  constructor(
    private readonly send: StudySocketSend,
    readonly chapters: StudyChapters,
    readonly setTab: () => void,
    readonly root: AnalyseCtrl,
  ) {
    pubsub.on('analysis.closeAll', () => this.isOpen(false));
  }

  open = () => {
    pubsub.emit('analysis.closeAll');
    this.isOpen(true);
    this.loadVariants();
    this.initial(false);
  };

  toggle = () => (this.isOpen() ? this.isOpen(false) : this.open());

  loadVariants = () => {
    if (!this.variants.length)
      xhrVariants().then(vs => {
        this.variants = vs;
        this.redraw();
      });
  };

  openInitial = () => {
    this.open();
    this.initial(true);
  };
  submit = (d: Omit<ChapterData, 'initial'>) => {
    const study = this.root.study!;
    const dd = { ...d, sticky: study.vm.mode.sticky, initial: this.initial() };
    if (!dd.pgn) this.send('addChapter', dd);
    else importPgn(study.data.id, dd);
    this.isOpen(false);
    this.setTab();
  };
  startTour = async () => {
    const [tour] = await Promise.all([
      site.asset.loadEsm<StudyTour>('analyse.study.tour'),
      site.asset.loadCssPath('bits.shepherd'),
    ]);

    tour.chapter(tab => {
      this.tab(tab);
      this.redraw();
    });
  };
  redraw = this.root.redraw;
}

export function view(ctrl: StudyChapterNewForm): VNode {
  const study = ctrl.root.study!;
  const activeTab = ctrl.tab();
  const makeTab = (key: ChapterTab, name: string, title: string) =>
    h(
      'span.' + key,
      {
        class: { active: activeTab === key },
        attrs: { role: 'tab', title, tabindex: '0' },
        hook: onInsert(el => {
          const select = (e: Event) => {
            ctrl.tab(key);
            ctrl.root.redraw();
            e.preventDefault();
          };
          el.addEventListener('click', select);
          el.addEventListener('keydown', e => {
            if (e.key === 'Enter' || e.key === ' ') select(e);
          });
        }),
      },
      name,
    );
  const gameOrPgn = activeTab === 'game' || activeTab === 'pgn';
  const currentChapter = study.data.chapter;
  const mode = currentChapter.practice
    ? 'practice'
    : defined(currentChapter.conceal)
      ? 'conceal'
      : currentChapter.gamebook
        ? 'gamebook'
        : 'normal';

  return snabDialog({
    class: 'chapter-new',
    onClose() {
      ctrl.isOpen(false);
      ctrl.redraw();
    },
    noClickAway: true,
    onInsert: dlg => dlg.show(),
    vnodes: [
      activeTab !== 'edit' &&
        h('h2', [
          i18n.study.newChapter,
          h('i.help', { attrs: { 'data-icon': licon.InfoCircle }, hook: bind('click', ctrl.startTour) }),
        ]),
      h(
        'form.form3',
        {
          hook: bindSubmit(
            e =>
              ctrl.submit({
                name: fieldValue(e, 'name'),
                game: fieldValue(e, 'game'),
                variant: fieldValue(e, 'variant') as VariantKey,
                pgn: fieldValue(e, 'pgn'),
                orientation: fieldValue(e, 'orientation') as Orientation,
                mode: fieldValue(e, 'mode') as ChapterMode,
                fen: fieldValue(e, 'fen') || (ctrl.tab() === 'edit' ? ctrl.editorFen() : null),
                isDefaultName: ctrl.isDefaultName(),
              }),
            ctrl.redraw,
          ),
        },
        [
          h('div.form-group', [
            h('label.form-label', { attrs: { for: 'chapter-name' } }, i18n.site.name),
            h('input#chapter-name.form-control', {
              attrs: { minlength: 2, maxlength: 80 },
              hook: onInsert<HTMLInputElement>(el => {
                if (!el.value) {
                  el.value = i18n.study.chapterX(ctrl.initial() ? 1 : ctrl.chapters.size() + 1);
                  el.onchange = () => ctrl.isDefaultName(false);
                  el.select();
                }
                el.addEventListener('focus', () => el.select());
                // set initial modal focus
                setTimeout(() => el.focus());
              }),
            }),
          ]),
          h('div.tabs-horiz', { attrs: { role: 'tablist' } }, [
            makeTab('init', i18n.study.empty, i18n.study.startFromInitialPosition),
            makeTab('edit', i18n.study.editor, i18n.study.startFromCustomPosition),
            makeTab('game', 'URL', i18n.study.loadAGameByUrl),
            makeTab('fen', 'FEN', i18n.study.loadAPositionFromFen),
            makeTab('pgn', 'PGN', i18n.study.loadAGameFromPgn),
          ]),
          activeTab === 'edit' &&
            h(
              'div.board-editor-wrap',
              {
                hook: {
                  insert(vnode) {
                    xhr.json('/editor.json').then(async data => {
                      data.el = vnode.elm;
                      data.fen = ctrl.root.node.fen;
                      data.embed = true;
                      data.options = {
                        inlineCastling: true,
                        orientation: currentChapter.setup.orientation,
                        onChange: ctrl.editorFen,
                        coordinates: true,
                      };
                      ctrl.editor = await site.asset.loadEsm<LichessEditor>('editor', { init: data });
                      ctrl.editorFen(ctrl.editor.getFen());
                    });
                  },
                  destroy: () => (ctrl.editor = null),
                },
              },
              [spinner()],
            ),
          activeTab === 'game' &&
            h('div.form-group', [
              h(
                'label.form-label',
                { attrs: { for: 'chapter-game' } },
                i18n.study.loadAGameFromXOrY('lichess.org', 'chessgames.com'),
              ),
              h('textarea#chapter-game.form-control', {
                attrs: { placeholder: i18n.study.urlOfTheGame },
                hook: onInsert((el: HTMLTextAreaElement) => {
                  el.addEventListener('change', () => el.reportValidity());
                  el.addEventListener('input', () => {
                    const ok = el.value
                      .trim()
                      .split('\n')
                      .every(line =>
                        line
                          .trim()
                          .match(
                            new RegExp(
                              `^((.*${location.host}/\\w{8,12}.*)|\\w{8}|\\w{12}|(.*chessgames\\.com/.*[?&]gid=\\d+.*)|)$`,
                            ),
                          ),
                      );
                    el.setCustomValidity(ok ? '' : 'Invalid game ID(s) or URL(s)');
                  });
                }),
              }),
            ]),
          activeTab === 'fen' &&
            h('div.form-group', [
              h('input#chapter-fen.form-control', {
                attrs: {
                  value: ctrl.root.node.fen,
                  placeholder: i18n.study.loadAPositionFromFen,
                  spellcheck: 'false',
                },
                hook: onInsert((el: HTMLInputElement) => {
                  el.addEventListener('change', () => el.reportValidity());
                  el.addEventListener('input', _ => {
                    if (parseFen(el.value.trim()).isOk) {
                      el.setCustomValidity('');
                      ctrl.root.node.fen = el.value;
                    } else el.setCustomValidity('Invalid FEN');
                  });
                }),
              }),
              h(
                'a.preview-in-editor',
                {
                  hook: bind('click', () => ctrl.tab('edit'), ctrl.root.redraw),
                },
                [h('i.text', { attrs: dataIcon(licon.Eye) }), i18n.study.editor],
              ),
            ]),
          activeTab === 'pgn' &&
            h('div.form-group', [
              h('textarea#chapter-pgn.form-control', {
                attrs: {
                  placeholder: i18n.study.pasteYourPgnTextHereUpToNbGames(ctrl.multiPgnMax),
                },
              }),
              h(
                'button.button.button-empty.import-from__chapter',
                {
                  attrs: { type: 'button' },
                  hook: bind(
                    'click',
                    () => {
                      xhr
                        .text(`/study/${study.data.id}/${study.vm.chapterId}.pgn`)
                        .then(pgnData => $('#chapter-pgn').val(pgnData));
                      return false;
                    },
                    undefined,
                    false,
                  ),
                },
                i18n.study.importFromChapterX(study.currentChapter().name),
              ),
              window.FileReader &&
                h('input#chapter-pgn-file.form-control', {
                  attrs: { type: 'file', accept: '.pgn' },
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
                }),
            ]),
          h('div.form-split', [
            h('div.form-group.form-half', [
              h('label.form-label', { attrs: { for: 'chapter-variant' } }, i18n.site.variant),
              h(
                'select#chapter-variant.form-control',
                { attrs: { disabled: gameOrPgn } },
                gameOrPgn
                  ? [h('option', { attrs: { value: 'standard' } }, i18n.study.automatic)]
                  : ctrl.variants.map(v => option(v.key, currentChapter.setup.variant.key, v.name)),
              ),
            ]),
            h('div.form-group.form-half', [
              h('label.form-label', { attrs: { for: 'chapter-orientation' } }, i18n.study.orientation),
              h(
                'select#chapter-orientation.form-control',
                {
                  hook: bind('change', e =>
                    ctrl.editor?.setOrientation((e.target as HTMLInputElement).value as Color),
                  ),
                },
                [activeTab === 'pgn' && i18n.study.automatic, i18n.site.white, i18n.site.black].map(
                  c => c && option(c, currentChapter.setup.orientation, c),
                ),
              ),
            ]),
          ]),
          h('div.form-group', [
            h('label.form-label', { attrs: { for: 'chapter-mode' } }, i18n.study.analysisMode),
            h(
              'select#chapter-mode.form-control',
              modeChoices.map(c => option(c[0], mode, c[1])),
            ),
          ]),
          h(
            'div.form-actions.single',
            h('button.button', { attrs: { type: 'submit' } }, i18n.study.createChapter),
          ),
        ],
      ),
    ],
  });
}
