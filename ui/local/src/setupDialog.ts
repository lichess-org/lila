import { isTouchDevice } from 'common/device';
import { HandOfCards } from './handOfCards';
//import { clamp } from 'common';
import * as licon from 'common/licon';
import { domDialog } from 'common/dialog';
import { defined } from 'common';
import { domIdToUid, uidToDomId, type BotCtrl } from './botCtrl';
import { rangeTicks } from './gameView';
import type { Libots, LocalSetup } from './types';

export class SetupDialog {
  view: HTMLElement;
  white: HTMLElement;
  black: HTMLElement;
  setup: LocalSetup = {};
  hand: HandOfCards;
  bots: Libots;

  constructor(
    readonly botCtrl: BotCtrl,
    setup: LocalSetup = {},
    readonly noClose = false,
  ) {
    this.bots = botCtrl.bots;
    this.setup = { ...setup };
    console.log('setupDialog', this.setup);
    const player = (color: 'white' | 'black') =>
      `<div class="player ${color}"><img class="z-remove" src="${site.asset.flairSrc(
        'symbols.cancel',
      )}"><div class="placard ${color}">Player</div></div>`;
    this.view = $as<HTMLElement>(`<div class="with-cards">
      <div class="vs">
        ${player('white')}
        <div class="actions">
          <button class="button button-empty switch disabled" data-icon="${licon.Switch}"></button>
          <button class="button button-empty random disabled" data-icon="${licon.DieSix}"></button>
          <button class="button button-empty fight" data-icon="${licon.Swords}"></button>
        </div>
        ${player('black')}
      </div>
    </div>`);
    this.white = this.view.querySelector('.white')!;
    this.black = this.view.querySelector('.black')!;
    const cardData = [...Object.values(this.bots).map(b => botCtrl.card(b))].filter(defined);
    this.hand = new HandOfCards({
      view: () => this.view,
      drops: () => [
        { el: this.white, selected: uidToDomId(this.setup.white) },
        { el: this.black, selected: uidToDomId(this.setup.black) },
      ],
      cardData: () => cardData,
      select: this.dropSelect,
    });
    this.show();
  }

  private show() {
    domDialog({
      class: 'game-setup base-view setup-view', // with-cards',
      css: [{ hashed: 'local.setup' }],
      htmlText: `<div class="chin">
            <label>Clock<select data-type="initial">${this.timeOptions('initial')}</select></label>
            <label>Increment<select data-type="increment">${this.timeOptions('increment')}</select></label>
            <label>Dev UI<input type="checkbox" id="bot-dev" checked></label>
          </div>`,
      append: [{ node: this.view, where: '.chin', how: 'before' }],
      actions: [
        { selector: '.fight', listener: this.fight },
        { selector: '.switch', listener: this.switch },
        { selector: '.random', listener: this.random },
        { selector: '[data-type]', event: 'input', listener: this.updateClock },
        { selector: '.white > img.z-remove', listener: () => this.select('white') },
        { selector: '.black > img.z-remove', listener: () => this.select('black') },
      ],
      noCloseButton: this.noClose,
      noClickAway: this.noClose,
    }).then(dlg => {
      if (this.setup.white) this.select('white', this.setup.white);
      if (this.setup.black) this.select('black', this.setup.black);
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
    const color = target.classList.contains('white') ? 'white' : 'black';
    console.log(domId, domIdToUid(domId));
    this.select(color, domIdToUid(domId));
  };

  private select(color: 'white' | 'black', selection?: string) {
    const bot = selection ? this.bots[selection] : undefined;
    this.view.querySelector(`.${color} .placard`)!.textContent = bot ? bot.description : 'Player';
    this.setup[color] = bot?.uid;
    if (!bot) this.hand.redraw();
    this[color].querySelector(`img.z-remove`)?.classList.toggle('show', !!bot);
    this.view.querySelector('.switch')!.classList.toggle('disabled', !this.setup.white && !this.setup.black);
    this.view.querySelector('.random')!.classList.toggle('disabled', !this.setup.white && !this.setup.black);
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
    const newBlack = this.setup.white;
    this.select('white', this.setup.black);
    this.select('black', newBlack);
    this.hand.redraw();
  };

  private random = () => {
    if (Math.random() < 0.5) this.switch();
    this.fight();
  };
}
