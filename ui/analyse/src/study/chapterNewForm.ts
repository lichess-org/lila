import { parseFen } from 'chessops/fen';
import { defined, prop, type Prop, toggle } from 'lib';
import { type Dialog, snabDialog } from 'lib/view/dialog';
import { alert } from 'lib/view/dialogs';
import * as licon from 'lib/licon';
import { bind, bindSubmit, onInsert, hl, dataIcon, type VNode } from 'lib/snabbdom';
import { storedProp } from 'lib/storage';
import { json as xhrJson, text as xhrText } from 'lib/xhr';
import type AnalyseCtrl from '../ctrl';
import type { StudySocketSend } from '../socket';
import { spinnerVdom as spinner } from 'lib/view/controls';
import { option } from '../view/util';
import type { ChapterData, ChapterMode, ChapterTab, Orientation, StudyTour } from './interfaces';
import { importPgn, variants as xhrVariants } from './studyXhr';
import type { StudyChapters } from './studyChapters';
import type { LichessEditor } from 'editor';
import { pubsub } from 'lib/pubsub';
import { lichessRules } from 'chessops/compat';

export const modeChoices = [
  ['normal', i18n.study.normalAnalysis],
  ['practice', i18n.site.practiceWithComputer],
  ['conceal', i18n.study.hideNextMoves],
  ['gamebook', i18n.study.interactiveLesson],
];

export const fieldValue = (e: Event, id: string) =>
  ((e.target as HTMLElement).querySelector('#chapter-' + id) as HTMLInputElement)?.value;

export class StudyChapterNewForm {
  readonly multiPgnMax = 64;
  variants: Variant[] = [];
  dialog: Dialog | undefined;
  isOpen = toggle(false, val => {
    if (!val) this.dialog?.close();
  });
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
  orientation: Color | 'automatic';

  constructor(
    private readonly send: StudySocketSend,
    readonly chapters: StudyChapters,
    readonly isBroadcast: boolean,
    readonly setChaptersTab: () => void,
    readonly root: AnalyseCtrl,
  ) {
    pubsub.on('analysis.closeAll', () => this.isOpen(false));
    this.orientation = root.bottomColor();
  }

  open = () => {
    pubsub.emit('analysis.closeAll');
    this.orientation = this.root.bottomColor();
    this.isOpen(true);
    this.loadVariants();
    this.initial(false);
  };

  toggle = () => (this.isOpen() ? this.isOpen(false) : this.open());

  setTab = (key: ChapterTab) => {
    this.tab(key);
    if (key !== 'pgn' && this.orientation === 'automatic') this.orientation = 'white';
    this.root.redraw();
  };

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
    else
      importPgn(study.data.id, dd).catch(e => {
        if (e.message === 'Too many requests') alert('Limit of 1000 pgn imports every 24 hours');
        throw e;
      });
    this.isOpen(false);
    this.setChaptersTab();
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
    hl(
      'span.' + key,
      {
        class: { active: activeTab === key },
        attrs: { role: 'tab', title, tabindex: '0' },
        hook: onInsert(el => {
          const select = (e: Event) => {
            ctrl.setTab(key);
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
      ctrl.dialog = undefined;
      ctrl.isOpen(false);
      ctrl.redraw();
    },
    modal: true,
    noClickAway: true,
    onInsert: dlg => {
      ctrl.dialog = dlg;
      dlg.show();
    },
    vnodes: [
      activeTab !== 'edit' &&
        hl('h2', [
          i18n.study.newChapter,
          hl('i.help', { attrs: { 'data-icon': licon.InfoCircle }, hook: bind('click', ctrl.startTour) }),
        ]),
      hl(
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
          hl('div.form-group', [
            hl('label.form-label', { attrs: { for: 'chapter-name' } }, i18n.site.name),
            hl('input#chapter-name.form-control', {
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
          hl('div.tabs-horiz', { attrs: { role: 'tablist' } }, [
            makeTab('init', i18n.study.empty, i18n.study.startFromInitialPosition),
            makeTab('edit', i18n.study.editor, i18n.study.startFromCustomPosition),
            makeTab('game', 'URL', i18n.study.loadAGameByUrl),
            makeTab('fen', 'FEN', i18n.study.loadAPositionFromFen),
            makeTab('pgn', 'PGN', i18n.study.loadAGameFromPgn),
          ]),
          activeTab === 'edit' &&
            hl(
              'div.board-editor-wrap',
              {
                hook: {
                  insert(vnode) {
                    xhrJson('/editor.json').then(async data => {
                      data.el = vnode.elm;
                      data.fen = ctrl.root.node.fen;
                      data.embed = true;
                      data.options = {
                        inlineCastling: true,
                        orientation: ctrl.orientation,
                        onChange: ctrl.editorFen,
                        coordinates: true,
                        bindHotkeys: false,
                      };
                      ctrl.editor = await site.asset.loadEsm<LichessEditor>('editor', { init: data });
                      ctrl.editorFen(ctrl.editor.getFen());
                      ctrl.editor.setRules(lichessRules(currentChapter.setup.variant.key));
                    });
                  },
                  destroy: () => (ctrl.editor = null),
                },
              },
              [spinner()],
            ),
          activeTab === 'game' &&
            hl('div.form-group', [
              hl(
                'label.form-label',
                { attrs: { for: 'chapter-game' } },
                i18n.study.loadAGameFromXOrY('lichess.org', 'chessgames.com'),
              ),
              hl('textarea#chapter-game.form-control', {
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
            hl('div.form-group', [
              hl('input#chapter-fen.form-control', {
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
              hl(
                'a.preview-in-editor',
                {
                  hook: bind('click', () => ctrl.tab('edit'), ctrl.root.redraw),
                },
                [hl('i.text', { attrs: dataIcon(licon.Eye) }), i18n.study.editor],
              ),
            ]),
          activeTab === 'pgn' &&
            hl('div.form-group', [
              hl('textarea#chapter-pgn.form-control', {
                attrs: {
                  placeholder: i18n.study.pasteYourPgnTextHereUpToNbGames(ctrl.multiPgnMax),
                },
              }),
              hl(
                'button.button.button-empty.import-from__chapter',
                {
                  attrs: { type: 'button' },
                  hook: bind(
                    'click',
                    () => {
                      xhrText(`/study/${study.data.id}/${study.vm.chapterId}.pgn`).then(pgnData =>
                        $('#chapter-pgn').val(pgnData),
                      );
                      return false;
                    },
                    undefined,
                    false,
                  ),
                },
                i18n.study.importFromChapterX(study.currentChapter().name),
              ),
              window.FileReader &&
                hl('input#chapter-pgn-file.form-control', {
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
          hl('div.form-split', [
            hl('div.form-group.form-half', [
              hl('label.form-label', { attrs: { for: 'chapter-variant' } }, i18n.site.variant),
              hl(
                'select#chapter-variant.form-control',
                {
                  attrs: { disabled: gameOrPgn },
                  hook: bind('change', e => {
                    ctrl.editor?.setRules(lichessRules((e.target as HTMLSelectElement).value as VariantKey));
                  }),
                },
                gameOrPgn
                  ? [hl('option', { attrs: { value: 'standard' } }, i18n.study.automatic)]
                  : ctrl.variants.map(v => option(v.key, currentChapter.setup.variant.key, v.name)),
              ),
            ]),
            hl('div.form-group.form-half', [
              hl('label.form-label', { attrs: { for: 'chapter-orientation' } }, i18n.study.orientation),
              hl(
                'select#chapter-orientation.form-control',
                {
                  hook: bind('change', e => {
                    ctrl.orientation = (e.target as HTMLInputElement).value as Color;
                    ctrl.editor?.setOrientation(ctrl.orientation);
                  }),
                },
                [
                  ...(activeTab === 'pgn' ? [['automatic', i18n.study.automatic]] : []),
                  ['white', i18n.site.white],
                  ['black', i18n.site.black],
                ].map(([value, name]) => value && option(value, ctrl.orientation, name, { key: value })),
              ),
            ]),
          ]),
          hl('div.form-group' + (ctrl.isBroadcast ? '.none' : ''), [
            hl('label.form-label', { attrs: { for: 'chapter-mode' } }, i18n.study.analysisMode),
            hl(
              'select#chapter-mode.form-control',
              modeChoices.map(c => option(c[0], mode, c[1])),
            ),
          ]),
          hl(
            'div.form-actions.single',
            hl('button.button', { attrs: { type: 'submit' } }, i18n.study.createChapter),
          ),
        ],
      ),
    ],
  });
}
