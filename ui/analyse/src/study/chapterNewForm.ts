import { COLORS } from 'chessops';
import { lichessRules } from 'chessops/compat';
import { makeBoardFen, makeFen, parseFen } from 'chessops/fen';
import { defaultPosition } from 'chessops/variant';
import type { LichessEditor } from 'editor';
import { chess960IdToFEN, randomPositionId } from 'editor/chess960';

import { defined, prop, type Prop, toggle } from 'lib';
import { licon } from 'lib/licon';
import { pubsub } from 'lib/pubsub';
import { storedProp } from 'lib/storage';
import {
  snabDialog,
  alert,
  bind,
  bindSubmit,
  onInsert,
  hl,
  dataIcon,
  spinnerVdom,
  type Dialog,
  icon,
  button,
  h2,
  div,
  label,
  input,
  textarea,
  select,
  option as opt,
} from 'lib/view';
import { json as xhrJson, text as xhrText } from 'lib/xhr';

import type AnalyseCtrl from '../ctrl';
import type { StudySocketSend } from '../socket';
import { option } from '../view/util';
import type {
  ChapterData,
  ChapterMode,
  ChapterTab,
  Orientation,
  StudyChapter,
  StudyTour,
} from './interfaces';
import type { StudyChapters } from './studyChapters';
import { importPgn, variants as xhrVariants } from './studyXhr';

export const modeChoices = [
  ['normal', i18n.study.normalAnalysis],
  ['practice', i18n.site.practiceWithComputer],
  ['conceal', i18n.study.hideNextMoves],
  ['gamebook', i18n.study.interactiveLesson],
];

export const fieldValue = (e: Event, id: string) =>
  ((e.target as HTMLElement).querySelector('#chapter-' + id) as HTMLInputElement)?.value;

const startFen = (variant: VariantKey): FEN => makeFen(defaultPosition(lichessRules(variant)).toSetup());

const boardFen = (fen: FEN): string | undefined =>
  parseFen(fen).unwrap(
    setup => makeBoardFen(setup.board),
    () => undefined,
  );

export class StudyChapterNewForm {
  readonly multiPgnMax = 64;
  variants: Variant[] = [];
  private dialog?: Dialog<StudyChapterNewForm>;
  initial = toggle(false);
  tab = storedProp<ChapterTab>('analyse.study.form.tab', 'init', str => str as ChapterTab);
  editor: LichessEditor | null = null;
  editorFen: Prop<FEN | null> = prop(null);
  isDefaultName = toggle(true);
  orientation: Color | 'automatic';
  chess960Position: Prop<number> = prop(518); // 518 = standard chess starting position
  selectedVariant: Prop<VariantKey> = prop('standard');

  constructor(
    private readonly send: StudySocketSend,
    readonly chapters: StudyChapters,
    readonly isBroadcast: boolean,
    readonly setChaptersTab: () => void,
    readonly root: AnalyseCtrl,
    private readonly currentChapter: () => StudyChapter,
  ) {
    pubsub.on('analysis.closeAll', () => this.dialog?.close());
    this.orientation = root.bottomColor();
  }

  isOpen = () => {
    return Boolean(this.dialog);
  };

  close = () => {
    this.dialog?.close();
  };

  setVariant = (v: VariantKey) => {
    this.selectedVariant(v);
    // racing kings boards always have white at the bottom, see AnalyseCtrl.bottomColor
    if (v === 'racingKings') this.orientation = 'white';
  };

  // an untouched starting position is replaced by the starting position of the new variant
  setEditorVariant = (from: VariantKey, to: VariantKey) => {
    if (!this.editor) return;
    const isStart = boardFen(this.editor.getFen()) === boardFen(startFen(from));
    this.editor.setVariant(to);
    if (isStart && from !== to) this.editor.setFen(startFen(to));
  };

  setTab = (key: ChapterTab) => {
    this.tab(key);
    if (key !== 'pgn' && this.orientation === 'automatic') this.orientation = 'white';
    this.redraw();
  };

  loadVariants = () => {
    if (!this.variants.length)
      xhrVariants().then(vs => {
        this.variants = vs;
        this.redraw();
      });
  };

  submit = (d: Omit<ChapterData, 'initial'>) => {
    const study = this.root.study!;
    const showRatings = study.data.showRatings ? undefined : false; // define only if false
    const dd = { ...d, sticky: study.vm.mode.sticky, showRatings, initial: this.initial() };
    if (!dd.pgn) this.send('addChapter', dd);
    else
      importPgn(study.data.id, dd).catch(e => {
        if (e.message === 'Too many requests') alert('Limit of 1000 pgn imports every 24 hours');
        if (e.message === 'Too many chapters')
          alert('You have reached the maximum number of chapters (64). Some of the games were not imported.');
        throw e;
      });
    this.setChaptersTab();
  };
  startTour = async () => {
    const [tour] = await Promise.all([
      site.asset.loadEsm<StudyTour>('analyse.study.tour'),
      site.asset.loadCss({ key: 'bits.shepherd' }),
    ]);

    if (this.root.reaper.disposed) return;
    tour.chapter(tab => {
      this.tab(tab);
      this.redraw();
    });
  };

  redraw = () => {
    this.root.redraw();
    this.dialog?.redraw();
  };

  open = async (initial = false) => {
    pubsub.emit('analysis.closeAll');
    this.orientation = this.root.bottomColor();
    this.loadVariants();
    this.initial(initial);
    this.isDefaultName(true);
    this.setVariant(this.currentChapter().setup.variant.key);
    this.chess960Position(518);
    const study = this.root.study!;
    const makeTab = (key: ChapterTab, name: string, title: string) => {
      return button(
        `.${key}`,
        {
          class: { active: this.tab() === key },
          type: 'button',
          role: 'tab',
          title,
          tabindex: '0',
          hook: onInsert(el => {
            el.addEventListener('click', (e: Event) => {
              this.setTab(key);
              e.preventDefault();
            });
          }),
        },
        name,
      );
    };
    const gameOrPgn = this.tab() === 'game' || this.tab() === 'pgn';
    const currentChapter = study.data.chapter;
    const mode = currentChapter.practice
      ? 'practice'
      : defined(currentChapter.conceal)
        ? 'conceal'
        : currentChapter.gamebook
          ? 'gamebook'
          : 'normal';

    this.dialog = await snabDialog<StudyChapterNewForm>({
      closeButton: true,
      class: 'chapter-new study-modal',
      show: true,
      ctx: this,
      modal: true,
      onClose: () => (this.dialog = undefined),
      render: (_, dlg) => [
        this.tab() !== 'edit' &&
          h2([
            i18n.study.newChapter,
            icon(licon.InfoCircle)('.help', { hook: bind('click', this.startTour) }),
          ]),
        hl(
          'form.form3',
          {
            hook: bindSubmit(e => {
              const tab = this.tab();
              this.submit({
                name: fieldValue(e, 'name'),
                game: fieldValue(e, 'game'),
                variant: fieldValue(e, 'variant') as VariantKey,
                pgn: fieldValue(e, 'pgn'),
                orientation: fieldValue(e, 'orientation') as Orientation,
                mode: fieldValue(e, 'mode') as ChapterMode,
                fen:
                  tab === 'init' && this.selectedVariant() === 'chess960'
                    ? chess960IdToFEN(this.chess960Position())
                    : fieldValue(e, 'fen') || (tab === 'edit' ? this.editorFen() : null),
                isDefaultName: this.isDefaultName(),
              });
              dlg.close();
            }, this.redraw),
          },
          [
            div('.form-group', [
              label('.form-label', { for: 'chapter-name' }, i18n.site.name),
              input('text')('#chapter-name.form-control', {
                minlength: 2,
                maxlength: 80,
                hook: onInsert<HTMLInputElement>(el => {
                  if (!el.value) {
                    el.value = i18n.study.chapterX(this.initial() ? 1 : this.chapters.size() + 1);
                    el.onchange = () => this.isDefaultName(false);
                    el.select();
                  }
                  el.addEventListener('focus', () => el.select());
                  // set initial modal focus
                  setTimeout(() => el.focus());
                }),
              }),
            ]),
            div('.tabs-horiz', { role: 'tablist', key: this.tab() }, [
              makeTab('init', i18n.study.empty, i18n.study.startFromInitialPosition),
              makeTab('edit', i18n.study.editor, i18n.study.startFromCustomPosition),
              makeTab('game', 'URL', i18n.study.loadAGameByUrl),
              makeTab('fen', 'FEN', i18n.study.loadAPositionFromFen),
              makeTab('pgn', 'PGN', i18n.study.loadAGameFromPgn),
            ]),
            this.tab() === 'edit' &&
              div(
                '.board-editor-wrap',
                {
                  class: {
                    is2d: document.getElementById('main-wrap')?.classList.contains('is2d') ?? false,
                    is3d: document.getElementById('main-wrap')?.classList.contains('is3d') ?? false,
                  },
                  hook: {
                    insert(vnode) {
                      xhrJson('/editor.json').then(async data => {
                        data.el = vnode.elm;
                        data.fen = dlg.ctx.root.node.fen;
                        data.embed = true;
                        data.options = {
                          inlineCastling: true,
                          orientation: dlg.ctx.orientation,
                          onChange: dlg.ctx.editorFen,
                          coordinates: true,
                          bindHotkeys: false,
                        };
                        dlg.ctx.editor = await site.asset.loadEsm<LichessEditor>('editor', { init: data });
                        dlg.ctx.editorFen(dlg.ctx.editor.getFen());
                        dlg.ctx.setEditorVariant(currentChapter.setup.variant.key, dlg.ctx.selectedVariant());
                      });
                    },
                  },
                },
                [spinnerVdom()],
              ),
            this.tab() === 'game' &&
              div('.form-group', [
                label('.form-label', { for: 'chapter-game' }, 'Load Lichess games'),
                textarea()('#chapter-game.form-control', {
                  placeholder: i18n.study.urlOfTheGame,
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
                                `^((.*${location.host}/game/\\w{8,12}.*)|\\w{8}|\\w{12}|(.*chessgames\\.com/.*[?&]gid=\\d+.*)|)$`,
                              ),
                            ),
                        );
                      el.setCustomValidity(ok ? '' : 'Invalid game ID(s) or URL(s)');
                    });
                  }),
                }),
              ]),
            this.tab() === 'fen' &&
              div('.form-group', [
                input('text')('#chapter-fen.form-control', {
                  value: this.root.node.fen,
                  placeholder: i18n.study.loadAPositionFromFen,
                  spellcheck: 'false',
                  hook: onInsert((el: HTMLInputElement) => {
                    el.addEventListener('change', () => el.reportValidity());
                    el.addEventListener('input', () => {
                      if (parseFen(el.value.trim()).isOk) {
                        el.setCustomValidity('');
                        dlg.ctx.root.node.fen = el.value;
                      } else el.setCustomValidity('Invalid FEN');
                    });
                  }),
                }),
                button(
                  '.button-link.preview-in-editor',
                  {
                    hook: bind('click', () => this.tab('edit'), this.redraw),
                  },
                  [icon(licon.Eye)('.text'), i18n.study.editor],
                ),
              ]),
            this.tab() === 'pgn' &&
              div('.form-group', [
                textarea()('#chapter-pgn.form-control', {
                  placeholder: i18n.study.pasteYourPgnTextHereUpToNbGames(this.multiPgnMax),
                }),
                button(
                  '.button-link.import-from__chapter',
                  {
                    type: 'button',
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
                  input('file')('#chapter-pgn-file.form-control', {
                    accept: '.pgn',
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
            div('.form-split', [
              div('.form-group.form-half', [
                label('.form-label', { for: 'chapter-variant' }, i18n.site.variant),
                select(
                  '#chapter-variant.form-control',
                  {
                    disabled: gameOrPgn,
                    hook: bind('change', e => {
                      const v = (e.target as HTMLSelectElement).value as VariantKey;
                      this.setEditorVariant(this.selectedVariant(), v);
                      this.setVariant(v);
                      if (v === 'racingKings') this.editor?.setOrientation('white');
                      if (v !== 'chess960') this.chess960Position(518);
                      this.redraw();
                    }),
                  },
                  gameOrPgn
                    ? [opt({ value: 'standard' }, i18n.study.automatic)]
                    : this.variants.map(v => option(v.key, currentChapter.setup.variant.key, v.name)),
                ),
              ]),
              div('.form-group.form-half', [
                label('.form-label', { for: 'chapter-orientation' }, i18n.study.orientation),
                select(
                  '#chapter-orientation.form-control',
                  {
                    disabled: !gameOrPgn && this.selectedVariant() === 'racingKings',
                    props: { value: this.orientation },
                    hook: bind(
                      'change',
                      e => {
                        this.orientation = (e.target as HTMLInputElement).value as Color;
                        this.editor?.setOrientation(this.orientation);
                      },
                      this.redraw,
                    ),
                  },
                  [...(this.tab() === 'pgn' ? ['automatic' as const] : []), ...COLORS].map(orientation =>
                    option(
                      orientation,
                      this.orientation,
                      orientation === 'automatic' ? i18n.study[orientation] : i18n.site[orientation],
                      {
                        key: orientation,
                      },
                    ),
                  ),
                ),
              ]),
            ]),
            this.tab() === 'init' &&
              this.selectedVariant() === 'chess960' &&
              div('.form-group.chess960-position', [
                label('.form-label', i18n.site.chess960StartPosition(this.chess960Position())),
                div('.chess960-position__inputs', [
                  input('number')('.form-control', {
                    min: 0,
                    max: 959,
                    value: this.chess960Position(),
                    hook: onInsert((el: HTMLInputElement) => {
                      el.addEventListener('input', () => {
                        const pos = parseInt(el.value);
                        if (!isNaN(pos) && pos >= 0 && pos <= 959) {
                          this.chess960Position(pos);
                          this.redraw();
                        }
                      });
                    }),
                  }),
                  button('.button.button-empty', {
                    type: 'button',
                    title: i18n.site.randomChess960Position,
                    ...dataIcon(licon.DieSix),
                    hook: bind('click', () => {
                      this.chess960Position(randomPositionId());
                      this.redraw();
                    }),
                  }),
                ]),
              ]),
            div('.form-group', { class: { none: this.isBroadcast } }, [
              label('.form-label', { for: 'chapter-mode' }, i18n.study.analysisMode),
              select(
                '#chapter-mode.form-control',
                modeChoices.map(c => option(c[0], mode, c[1])),
              ),
            ]),
            div('.form-actions.single', [button('.button', { type: 'submit' }, i18n.study.createChapter)]),
          ],
        ),
      ],
    });
  };
}
