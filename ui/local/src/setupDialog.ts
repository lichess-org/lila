import { handOfCards, HandOfCards } from './handOfCards';
//import { clamp } from 'common';
import * as co from 'chessops';
import * as licon from 'common/licon';
import { domDialog, Dialog } from 'common/dialog';
import { defined } from 'common';
import { domIdToUid, uidToDomId, type BotCtrl } from './botCtrl';
import { type GameCtrl } from './gameCtrl';
import { rangeTicks } from './gameView';
import type { Libots, BotInfo, LocalSetup } from './types';

export function showSetupDialog(botCtrl: BotCtrl, setup: LocalSetup = {}, gameCtrl?: GameCtrl): void {
  new SetupDialog(botCtrl, setup, gameCtrl);
}

class SetupDialog {
  view: HTMLElement;
  playerColor: Color = 'white';
  setup: LocalSetup = {};
  hand: HandOfCards;
  uid?: string;
  dialog: Dialog;

  constructor(
    readonly botCtrl: BotCtrl,
    setup: LocalSetup,
    readonly gameCtrl: GameCtrl | undefined,
  ) {
    this.setup = { ...setup };
    this.show();
  }

  private get botColor() {
    return co.opposite(this.playerColor);
  }

  private show() {
    domDialog({
      class: 'game-setup base-view setup-view',
      css: [{ hashed: 'local.setup' }],
      htmlText: `<div class="with-cards">
        <div class="vs">
          <div class="player" data-color="black">
            <img class="z-remove" src="${site.asset.flairSrc('symbols.cancel')}">
            <div class="placard" data-color="black">Human Player</div>
          </div>
          vs
          <div class="switch" data-color="white">
            You play as White
          </div>
        </div>
      </div>
      <div class="chin">
        <div class="clock">
          <label>Clock<select data-type="initial">${this.timeOptions('initial')}</select></label>
          <label>Increment<select data-type="increment">${this.timeOptions('increment')}</select></label>
        </div>
        <div class="actions">
          <button class="button button-empty random disabled" data-icon="${licon.DieSix}"></button>
          <button class="button button-empty fight" data-icon="${licon.Swords}"></button>
        </div>
      </div>`,
      actions: [
        { selector: '.fight', listener: this.fight },
        { selector: '.switch', listener: this.switch },
        { selector: '.random', listener: this.random },
        { selector: '[data-type]', event: 'input', listener: this.updateClock },
        { selector: 'img.z-remove', listener: () => this.select() },
      ],
      noCloseButton: this.gameCtrl !== undefined,
      noClickAway: this.gameCtrl !== undefined,
    }).then(dlg => {
      this.dialog = dlg;
      this.view = dlg.view.querySelector('.with-cards')!;
      this.setPlayerColor(this.setup.black ? 'white' : this.setup.white ? 'black' : 'white');
      const cardData = [...Object.values(this.botCtrl.bots).map(b => this.botCtrl.card(b))].filter(defined);
      this.hand = handOfCards({
        getView: () => this.view,
        getDrops: () => [
          { el: this.view.querySelector('.player')!, selected: uidToDomId(this.setup[this.botColor]) },
        ],
        getCardData: () => cardData,
        select: this.dropSelect,
        orientation: 'bottom',
      });
      window.addEventListener('resize', this.hand.resize);
      dlg.showModal();
      setTimeout(() => {
        this.select(this.setup[this.botColor]);
        this.hand.resize();
      });
    });
  }

  private setPlayerColor(color: Color) {
    if (this.playerColor === color) return;
    this.playerColor = color;
    this.view.querySelectorAll<HTMLElement>('[data-color]').forEach(el => {
      el.dataset.color = co.opposite(el.dataset.color as Color);
    });
    this.view.querySelector<HTMLElement>('.switch')!.textContent = `You play as ${
      this.playerColor[0].toUpperCase() + this.playerColor.slice(1)
    }`;
    this.setup[this.botColor] = this.uid;
    this.setup[this.playerColor] = undefined;
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
    this.uid = selection;
    const bot = selection ? this.botCtrl.bots[selection] : undefined;
    this.view.querySelector(`.placard`)!.textContent = bot?.description ?? 'Human Player';
    this.setup[this.botColor] = bot?.uid;
    if (!bot) this.hand.redraw();
    this.dialog.view.querySelector(`img.z-remove`)?.classList.toggle('show', !!bot);
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
    if (this.gameCtrl) this.gameCtrl.reset(this.setup);
    else window.location.href = `/local`;
    this.dialog.close(this.uid);
    this.gameCtrl?.round?.redraw();
  };

  private switch = () => {
    this.setPlayerColor(co.opposite(this.playerColor));
    this.hand.redraw();
  };

  private random = () => {
    if (Math.random() < 0.5) this.switch();
    this.fight();
  };
}
