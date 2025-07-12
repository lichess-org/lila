import { handOfCards, HandOfCards } from './handOfCards';
import * as co from 'chessops';
import { domDialog, Dialog } from 'lib/view/dialog';
import { fen960 } from 'lib/game/chess';
import { pubsub } from 'lib/pubsub';
import { definedMap, clamp } from 'lib/algo';
import { domIdToUid, uidToDomId } from './devBotCtrl';
import { rangeTicks } from './devUtil';
import type { LocalSetup } from 'lib/bot/types';
import { env } from './devEnv';
import * as licon from 'lib/licon';
import type { LichessEditor } from 'editor';
import { json } from 'lib/xhr';
import { Janitor } from 'lib/event';

export function showSetupDialog(setup: LocalSetup = {}): void {
  pubsub.after('botdev.images.ready').then(() => new SetupDialog(setup).show());
}

class SetupDialog {
  view: HTMLElement;
  editorEl: HTMLElement;
  mainContentEl: HTMLElement;
  editor: LichessEditor;
  playerColor: Color = 'white';
  setup: LocalSetup = {};
  hand: HandOfCards;
  uid?: string;
  dialog: Dialog;
  janitor = new Janitor();

  constructor(setup: LocalSetup) {
    this.setup = { ...setup };
  }

  async show() {
    const dlg = await domDialog({
      class: 'game-setup base-view setup-view',
      css: [{ hashed: 'bot-dev.setup' }],
      htmlText: $html`
        <div class="main-content">
          <div class="with-cards snap-pane">
            <div class="vs">
              <div class="player" data-color="black">
                <img class="z-remove" src="${site.asset.flairSrc('symbols.cancel')}">
                <div class="placard none" data-color="black">Human Player</div>
              </div>
            </div>
            <button class="button button-empty go-to-board" data-icon="${licon.GreaterThan}"></button>
          </div>
          <div class="from-position is2d snap-pane">
            <div class="editor"></div>
            <div class="resets">
              <button class="button button-metal standard">Standard</button>
              <button class="button button-metal chess960">Chess960</button>
            </div>
            <button class="button button-empty go-to-opponent" data-icon="${licon.LessThan}"></button>
          </div>
        </div>
        <div class="chin">
          <div class="params">
            <input class="fen" type="text" spellcheck="false" placeholder="${co.fen.INITIAL_FEN}"
                    value="${this.setup.setupFen ?? ''}">
            <span>
              Clock:
              <select data-type="initial">${this.timeOptions('initial')}</select>
              +
              <select data-type="increment">${this.timeOptions('increment')}</select>
            </span>
          </div>
          <div class="actions">
            <button class="button button-empty black"><i></i></button>
            <button class="button button-empty random"><i></i></button>
            <button class="button button-empty white"><i></i></button>
          </div>
        </div>`,
      modal: true,
      focus: '.white',
      actions: [
        { selector: '.fen', event: 'focus', listener: this.focusFen },
        { selector: '.fen', event: 'input', listener: this.inputFen },
        { selector: '.standard', listener: this.clickStandard },
        { selector: '.chess960', listener: this.clickChess960 },
        { selector: '.main-content', event: 'wheel', listener: this.wheel },
        { selector: '.go-to-board', listener: this.goToBoard },
        { selector: '.go-to-opponent', listener: this.goToOpponent },
        { selector: '.white', listener: () => this.fight('white') },
        { selector: '.black', listener: () => this.fight('black') },
        { selector: '.random', listener: () => this.fight() },
        { selector: '[data-type]', event: 'input', listener: this.updateClock },
        { selector: 'img.z-remove', listener: () => this.select() },
      ],
      onClose: () => {
        localStorage.setItem('botdev.setup', JSON.stringify(this.setup));
        this.janitor.cleanup();
      },
      noCloseButton: true, //env.game === undefined,
      noClickAway: env.game === undefined,
    });
    this.dialog = dlg;
    this.mainContentEl = dlg.view.querySelector('.main-content')!;
    this.initCards();
    this.initEditor();
    this.goToOpponent();

    dlg.show();

    this.select(this.setup[this.botColor] ?? env.bot.firstUid);
    this.hand.resize();
  }

  private initCards() {
    this.view = this.dialog.view.querySelector('.with-cards')!;
    const cardData = definedMap(env.bot.sorted('classical'), b => env.bot.card(b));
    this.hand = handOfCards({
      getCardData: () => cardData,
      getDrops: () => [
        { el: this.view.querySelector('.player')!, selected: uidToDomId(this.setup[this.botColor]) },
      ],
      viewEl: this.view,
      select: this.dropSelect,
      orientation: 'bottom',
      peek: clamp(0.5 + (window.innerHeight - 320) / 960, { min: 0.5, max: 1.0 }),
    });
    this.janitor.addListener(window, 'resize', () => {
      this.hand.resize();
      this.snapToPane();
    });
  }

  private initEditor() {
    this.editorEl = this.dialog.view.querySelector('.editor')!;
    json('/editor.json').then(async data => {
      data.el = this.editorEl;
      data.fen = this.setup.setupFen ?? co.fen.INITIAL_FEN;
      data.embed = true;
      data.options = {
        inlineCastling: true,
        orientation: 'white',
        onChange: (fen: string) => {
          this.setup.setupFen = fen;
          this.dialog.view.querySelector<HTMLInputElement>('.fen')!.value = fen;
        },
        coordinates: false,
        bindHotkeys: false,
      };
      this.editor = await site.asset.loadEsm<LichessEditor>('editor', { init: data });
      this.editor.setRules(co.compat.lichessRules('chess960'));
    });
  }

  private timeOptions(type: 'initial' | 'increment') {
    const defaults = { initial: 300, increment: 0 };
    const val = this.setup[type] ?? defaults[type];
    return rangeTicks[type]
      .map(([secs, label]) => `<option value="${secs}"${secs === val ? ' selected' : ''}>${label}</option>`)
      .join('');
  }

  private dropSelect = (_: HTMLElement, domId?: string) => {
    this.select(domIdToUid(domId));
  };

  private select(selection?: string) {
    const bot = env.bot.info(selection);
    const placard = this.view.querySelector('.placard') as HTMLElement;
    placard.textContent = bot?.description ?? '';
    placard.classList.toggle('none', !bot?.description);
    this.dialog.view.querySelector(`img.z-remove`)?.classList.toggle('show', !!bot);
    this.setup[this.botColor] = this.uid = bot?.uid;
    if (!bot) this.hand.redraw();
  }

  private snapToPane() {
    if (this.mainContentEl.scrollLeft > 0) this.mainContentEl.scrollLeft = this.mainContentEl.scrollWidth;
  }

  private updateClock = () => {
    for (const type of ['initial', 'increment'] as const) {
      const selectEl = this.dialog.view.querySelector<HTMLSelectElement>(`[data-type="${type}"]`);
      this.setup[type] = Number(selectEl?.value);
    }
  };

  private fight = (asColor: Color = Math.random() < 0.5 ? 'white' : 'black') => {
    this.updateClock();
    this.setup.white = this.setup.black = undefined;
    if (asColor === 'black') this.setup.white = this.uid;
    else this.setup.black = this.uid;
    if (env.game) {
      env.game.load(this.setup);
      this.dialog.close(this.uid);
      env.redraw();
      return;
    }
    const fragParams = [];
    for (const [key, val] of Object.entries(this.setup)) {
      if (key && val) fragParams.push(`${key}=${encodeURIComponent(val)}`);
    }
    site.redirect(`/bots/dev${fragParams.length ? `#${fragParams.join('&')}` : ''}`);
  };

  focusFen = () => {
    this.mainContentEl.scrollLeft = this.mainContentEl.scrollWidth;
  };

  inputFen = (e: InputEvent) => {
    if (!(e.target instanceof HTMLInputElement)) return;
    //const value = e.target.value;
    this.editor.setFen(e.target.value);
  };

  clickStandard = () => {
    this.editor.setRules(co.compat.lichessRules('standard'));
    const input = this.dialog.view.querySelector<HTMLInputElement>('.fen')!;
    input.value = '';
    this.editor.setFen(co.fen.INITIAL_FEN);
  };

  clickChess960 = () => {
    this.editor.setRules(co.compat.lichessRules('chess960'));
    const input = this.dialog.view.querySelector<HTMLInputElement>('.fen')!;
    input.value = fen960();
    this.editor.setFen(input.value);
  };

  wheel = (e: WheelEvent) => {
    if (Math.abs(e.deltaY) > Math.abs(e.deltaX)) e.preventDefault();
    else this.mainContentEl.scrollTop = 0;
  };

  goToBoard = () => {
    this.mainContentEl.scrollLeft = this.mainContentEl.scrollWidth;
  };

  goToOpponent = () => {
    if (this.mainContentEl.scrollLeft === 0) return;
    this.mainContentEl.scrollLeft = 0;
  };

  private get botColor() {
    return 'black' as const;
  }
}
