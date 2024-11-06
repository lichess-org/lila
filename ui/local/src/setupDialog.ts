import { handOfCards, HandOfCards } from './handOfCards';
import * as co from 'chessops';
import * as licon from 'common/licon';
import { domDialog, Dialog } from 'common/dialog';
import { defined } from 'common';
import { pubsub } from 'common/pubsub';
import { domIdToUid, uidToDomId, type BotCtrl } from './botCtrl';
import { rangeTicks } from './gameView';
import type { LocalSetup } from './types';
import { env } from './localEnv';

export function showSetupDialog(botCtrl: BotCtrl, setup: LocalSetup = {}): void {
  pubsub.after('local.images.ready').then(() => new SetupDialog(botCtrl, setup).show());
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
  ) {
    this.setup = { ...setup };
  }

  show() {
    if (window.screen.width < 1260) return;

    domDialog({
      class: 'game-setup base-view setup-view',
      css: [{ hashed: 'local.setup' }],
      htmlText: `<div class="with-cards">
        <div class="vs">
          <div class="player" data-color="black">
            <img class="z-remove" src="${site.asset.flairSrc('symbols.cancel')}">
            <div class="placard none" data-color="black">Human Player</div>
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
      onClose: () => localStorage.setItem('local.setup', JSON.stringify(this.setup)),
      noCloseButton: env.game !== undefined,
      noClickAway: env.game !== undefined,
    }).then(dlg => {
      this.dialog = dlg;
      this.view = dlg.view.querySelector('.with-cards')!;
      this.setPlayerColor(this.setup.black ? 'white' : this.setup.white ? 'black' : 'white');
      const cardData = this.botCtrl
        .sorted('classical')
        .map(b => this.botCtrl.card(b))
        .filter(defined);
      this.hand = handOfCards({
        getCardData: () => cardData,
        getDrops: () => [
          { el: this.view.querySelector('.player')!, selected: uidToDomId(this.setup[this.botColor]) },
        ],
        view: this.view,
        select: this.dropSelect,
        orientation: 'bottom',
      });
      window.addEventListener('resize', this.hand.resize);
      dlg.showModal();
      this.select(this.setup[this.botColor]);
      this.hand.resize();
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
    }`; // i18n
    this.setup[this.botColor] = this.uid;
    this.setup[this.playerColor] = undefined;
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
    const bot = this.botCtrl.get(selection);
    const placard = this.view.querySelector('.placard') as HTMLElement;
    placard.textContent = bot?.description ?? '';
    placard.classList.toggle('none', !bot);
    this.dialog.view.querySelector(`img.z-remove`)?.classList.toggle('show', !!bot);
    this.dialog.view.querySelector('.random')!.classList.toggle('disabled', !bot);
    this.setup[this.botColor] = this.uid = bot?.uid;
    if (!bot) this.hand.redraw();
  }

  private updateClock = () => {
    for (const type of ['initial', 'increment'] as const) {
      const selectEl = this.dialog.view.querySelector<HTMLSelectElement>(`[data-type="${type}"]`);
      this.setup[type] = Number(selectEl?.value);
    }
  };

  private fight = () => {
    this.updateClock();
    this.setup.go = true;
    if (env.game) {
      env.game.reset(this.setup);
      this.dialog.close(this.uid);
      env.redraw();
      return;
    }
    localStorage.setItem('local.setup', JSON.stringify(this.setup));
    window.location.href = `/local`;
  };

  private switch = () => {
    this.setPlayerColor(co.opposite(this.playerColor));
    this.hand.redraw();
  };

  private random = () => {
    if (Math.random() < 0.5) {
      this.switch();
      setTimeout(this.fight, 300);
    } else this.fight();
  };

  private get botColor() {
    return co.opposite(this.playerColor);
  }
}
