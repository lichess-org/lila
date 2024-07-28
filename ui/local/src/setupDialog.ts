import { isTouchDevice } from 'common/device';
import { HandOfCards } from './handOfCards';
//import { clamp } from 'common';
import * as co from 'chessops';
import * as licon from 'common/licon';
import { domDialog, Dialog } from 'common/dialog';
import { defined } from 'common';
import { domIdToUid, uidToDomId, type BotCtrl } from './botCtrl';
import { rangeTicks } from './gameView';
import type { Libots, BotInfo, LocalSetup } from './types';

export class SetupDialog {
  view: HTMLElement;
  playerColor: Color = 'white';
  setup: LocalSetup = {};
  hand: HandOfCards;
  bots: Libots;
  selected?: BotInfo;
  dialog: Dialog;

  constructor(
    readonly botCtrl: BotCtrl,
    setup: LocalSetup = {},
    readonly noClose = false,
  ) {
    this.bots = botCtrl.bots;
    this.setup = { ...setup };
    this.selected = this.bots[setup.black ?? ''];
    const player = (color: 'white' | 'black') =>
      `<div class="player" data-color="${color}"><img class="z-remove" src="${site.asset.flairSrc(
        'symbols.cancel',
      )}"><div class="placard data-color="${color}"">Player</div></div>`;
    this.view = $as<HTMLElement>(`<div class="with-cards">
      <div class="vs">
        ${player('black')}
        <div class="you" data-color="${this.playerColor}">
          <p class="text">You play the ${this.playerColor} pieces</p>
        </div>
      </div>
    </div>`);
    const cardData = [...Object.values(this.bots).map(b => botCtrl.card(b))].filter(defined);
    this.hand = new HandOfCards({
      view: () => this.view,
      drops: () => [
        { el: this.view.querySelector('.player')!, selected: uidToDomId(this.setup[this.botColor]) },
      ],
      cardData: () => cardData,
      select: this.dropSelect,
    });
    this.show();
  }

  private get botColor() {
    return co.opposite(this.playerColor);
  }

  private show() {
    domDialog({
      class: 'game-setup base-view setup-view', // with-cards',
      css: [{ hashed: 'local.setup' }],
      htmlText: `<div class="chin">
            <div class="clock">
              <label>Clock<select data-type="initial">${this.timeOptions('initial')}</select></label>
              <label>Increment<select data-type="increment">${this.timeOptions('increment')}</select></label>
            </div>
            <div class="actions">
              <button class="button button-empty switch disabled" data-icon="${licon.Switch}"></button>
              <button class="button button-empty random disabled" data-icon="${licon.DieSix}"></button>
              <button class="button button-empty fight" data-icon="${licon.Swords}"></button>
            </div>
            <label>Dev UI<input type="checkbox" id="bot-dev" checked></label>
          </div>`,
      append: [{ node: this.view, where: '.chin', how: 'before' }],
      actions: [
        { selector: '.fight', listener: this.fight },
        { selector: '.switch', listener: this.switch },
        { selector: '.random', listener: this.random },
        { selector: '[data-type]', event: 'input', listener: this.updateClock },
        { selector: 'img.z-remove', listener: () => this.select() },
      ],
      noCloseButton: this.noClose,
      noClickAway: this.noClose,
    }).then(dlg => {
      this.dialog = dlg;
      this.select(this.setup.black);
      window.addEventListener('resize', this.hand.resize);
      dlg.showModal();
      setTimeout(this.hand.resize);
    });
  }

  private timeOptions(type: 'initial' | 'increment') {
    const val = this.setup[type] ?? 0;
    return rangeTicks[type]
      .map(([secs, label]) => `<option value="${secs}"${secs === val ? ' selected' : ''}>${label}</option>`)
      .join('');
  }

  private dropSelect = (target: HTMLElement, domId?: string) => {
    this.select(domIdToUid(domId));
  };

  private select(selection?: string) {
    const bot = (this.selected = selection ? this.bots[selection] : undefined);
    this.view.querySelector(`.placard`)!.textContent = bot?.description ?? 'Player';
    this.setup[this.botColor] = bot?.uid;
    if (!bot) this.hand.redraw();
    this.dialog.view.querySelector(`img.z-remove`)?.classList.toggle('show', !!bot);
    this.dialog.view.querySelector('.switch')!.classList.toggle('disabled', !bot);
    this.dialog.view.querySelector('.random')!.classList.toggle('disabled', !bot);
  }

  private updateClock = () => {
    for (const type of ['initial', 'increment'] as const) {
      this.setup[type] = Number($as<HTMLSelectElement>(`.chin [data-type="${type}"]`).value);
    }
  };

  private fight = () => {
    this.updateClock();
    this.setup.go = true;
    localStorage.setItem('local.setup', JSON.stringify(this.setup));
    window.location.href = `/local?devUi=${$as<HTMLInputElement>('#bot-dev').checked}`;
  };

  private switch = () => {
    this.playerColor = co.opposite(this.playerColor);
    this.view.querySelectorAll<HTMLElement>('[data-color]').forEach(el => {
      el.dataset.color = co.opposite(el.dataset.color as Color);
    });
    this.setup[this.playerColor] = undefined;
    this.setup[this.botColor] = this.selected?.uid;
    this.hand.redraw();
  };

  private random = () => {
    if (Math.random() < 0.5) this.switch();
    this.fight();
  };
}
