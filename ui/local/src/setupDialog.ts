import { handOfCards, HandOfCards } from './handOfCards';
import * as co from 'chessops';
import * as licon from 'common/licon';
import * as data from 'common/data';
import { domDialog, Dialog } from 'common/dialog';
import { fen960 } from 'chess';
import { myUsername } from 'common';
import { pubsub } from 'common/pubsub';
import { definedMap } from 'common/algo';
import { domIdToUid, uidToDomId, type BotCtrl } from './botCtrl';
import { rangeTicks } from './gameView';
import type { LocalSetup } from './types';
import { env } from './localEnv';
import type { LichessEditor } from 'editor';
import { json } from 'common/xhr';
import { Chessground as makeChessground } from 'chessground';

export function showSetupDialog(setup: LocalSetup = {}): void {
  pubsub.after('local.images.ready').then(() => new SetupDialog(setup).show());
}

class SetupDialog {
  view: HTMLElement;
  editorEl: HTMLElement;
  editor: LichessEditor;
  playerColor: Color = 'white';
  setup: LocalSetup = {};
  hand: HandOfCards;
  uid?: string;
  dialog: Dialog;

  constructor(setup: LocalSetup) {
    this.setup = { ...setup };
  }

  async show() {
    const dlg = await domDialog({
      class: 'game-setup base-view setup-view',
      css: [{ hashed: 'local.setup' }],
      htmlText: $html`
        <div class="main-content">
          <div class="with-cards snap-pane">
            <div class="vs">
              <div class="player" data-color="black">
                <img class="z-remove" src="${site.asset.flairSrc('symbols.cancel')}">
                <div class="placard none" data-color="black">Human Player</div>
              </div>
            </div>
          </div>
          <div class="from-position is2d snap-pane">
            <div class="editor"></div>
          </div>
        </div>
        <div class="chin">
          <div class="params">
            <span>
              <input class="fen" type="text" spellcheck="false" placeholder="${co.fen.INITIAL_FEN}"
                     value="${this.setup.setupFen ?? ''}">
            </span>
            <span>
              <select data-type="initial">${this.timeOptions('initial')}</select>
              +
              <select data-type="increment">${this.timeOptions('increment')}</select>
            </span>
          </div>
          <div class="actions">
            <button class="button button-metal black"><i></i></button>
            <button class="button button-metal random"><i></i></button>
            <button class="button button-metal white"><i></i></button>
          </div>
        </div>`,
      modal: true,
      focus: '.white',
      actions: [
        {
          selector: '.fen',
          event: 'focus',
          listener: () => {
            const content = dlg.viewEl.querySelector<HTMLElement>('.main-content')!;
            content.scrollLeft = content.scrollWidth;
          },
        },
        { selector: '.white', listener: () => this.fight('white') },
        { selector: '.black', listener: () => this.fight('black') },
        { selector: '.random', listener: () => this.fight() },
        { selector: '[data-type]', event: 'input', listener: this.updateClock },
        { selector: 'img.z-remove', listener: () => this.select() },
      ],
      onClose: () => {
        localStorage.setItem('local.setup', JSON.stringify(this.setup));
        window.removeEventListener('resize', this.hand.resize);
      },
      noCloseButton: env.game !== undefined,
      noClickAway: env.game !== undefined,
    });
    this.dialog = dlg;
    this.view = dlg.viewEl.querySelector('.with-cards')!;
    this.editorEl = dlg.viewEl.querySelector('.editor')!;
    const cardData = definedMap(env.bot.sorted('classical'), b => env.bot.card(b));
    this.hand = handOfCards({
      getCardData: () => cardData,
      getDrops: () => [
        { el: this.view.querySelector('.player')!, selected: uidToDomId(this.setup[this.botColor]) },
      ],
      viewEl: this.view,
      select: this.dropSelect,
      orientation: 'bottom',
    });
    window.addEventListener('resize', this.hand.resize);
    dlg.viewEl.querySelector<HTMLElement>('.main-content')?.addEventListener(
      'wheel',
      e => {
        if (Math.abs(e.deltaY) > Math.abs(e.deltaX)) {
          e.preventDefault();
        } else {
          dlg.viewEl.querySelector<HTMLElement>('.main-content')!.scrollTop = 0;
        }
      },
      { passive: false },
    );
    json('/editor.json').then(async data => {
      data.el = this.editorEl;
      data.fen = this.setup.setupFen ?? co.fen.INITIAL_FEN;
      data.embed = true;
      data.options = {
        inlineCastling: true,
        orientation: 'white',
        onChange: (fen: string) => {
          console.log('hayo ' + fen);
          this.setup.setupFen = fen;
          dlg.viewEl.querySelector<HTMLInputElement>('.fen')!.value = fen;
        },
        coordinates: false,
        bindHotkeys: false,
      };
      this.editor = await site.asset.loadEsm<LichessEditor>('editor', { init: data });
      this.editor.setRules(co.compat.lichessRules('chess960'));
    });

    dlg.show();
    this.select(this.setup[this.botColor]);
    this.hand.resize();
  }

  private timeOptions(type: 'initial' | 'increment') {
    const defaults = { initial: 300, increment: 0 };
    const val = this.setup[type] ?? defaults[type];
    return rangeTicks[type]
      .map(([secs, label]) => `<option value="${secs}"${secs === val ? ' selected' : ''}>${label}</option>`)
      .join('');
  }

  private dropSelect = (target: HTMLElement, domId?: string) => {
    this.select(domIdToUid(domId));
  };

  private select(selection?: string) {
    const bot = env.bot.get(selection);
    const placard = this.view.querySelector('.placard') as HTMLElement;
    placard.textContent = bot?.description ?? '';
    placard.classList.toggle('none', !bot?.description);
    this.dialog.viewEl.querySelector(`img.z-remove`)?.classList.toggle('show', !!bot);
    this.dialog.viewEl.querySelectorAll('.button-empty').forEach(x => x.classList.toggle('disabled', !bot));
    this.setup[this.botColor] = this.uid = bot?.uid;
    if (!bot) this.hand.redraw();
  }

  private updateClock = () => {
    for (const type of ['initial', 'increment'] as const) {
      const selectEl = this.dialog.viewEl.querySelector<HTMLSelectElement>(`[data-type="${type}"]`);
      this.setup[type] = Number(selectEl?.value);
    }
  };

  private fight = (asColor: Color = Math.random() < 0.5 ? 'white' : 'black') => {
    this.updateClock();
    this.setup.white = this.setup.black = undefined;
    if (asColor === 'black') this.setup.white = this.uid;
    else this.setup.black = this.uid;
    if (env.game) {
      console.log(this.setup);
      env.game.load(this.setup);
      this.dialog.close(this.uid);
      env.redraw();
      return;
    }
    const fragParams = ['go=true'];
    localStorage.setItem('local.setup', JSON.stringify(this.setup));
    for (const [key, val] of Object.entries(this.setup)) {
      if (key && val) fragParams.push(`${key}=${encodeURIComponent(val)}`);
    }
    site.redirect(`/local${fragParams.length ? `#${fragParams.join('&')}` : ''}`);
  };

  private get botColor() {
    return 'black' as const;
  }
}
