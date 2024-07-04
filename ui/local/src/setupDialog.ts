import type { Libot, Libots, LocalSetup } from './types';
import { isTouchDevice } from 'common/device';
import { HandOfCards } from './handOfCards';
//import { clamp } from 'common';
import * as licon from 'common/licon';
import { domDialog, Dialog } from 'common/dialog';
import { defined } from 'common';
import { domIdToUid, uidToDomId, BotCtrl } from './botCtrl';
//import { ratingView } from './components/ratingView';

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
    const player = (color: 'white' | 'black') =>
      `<div class="player ${color}"><img class="remove" src="${site.asset.flairSrc(
        'symbols.cancel',
      )}"><div class="placard ${color}">Player</div></div>`;
    this.view = $as<HTMLElement>(`<div class="with-hand-of-cards">
      <div class="vs">
        ${player('white')}
        <div class="actions">
          <button class="button button-empty switch" data-icon="${licon.Switch}"></button>
          <button class="button button-empty random" data-icon="${licon.DieSix}"></button>
          <button class="button button-empty fight" data-icon="${licon.Swords}"></button>
        </div>
        ${player('black')}
      </div>
    </div>`);
    this.white = this.view.querySelector('.white')!;
    this.black = this.view.querySelector('.black')!;
    const cardData = [...Object.values(this.bots).map(b => botCtrl.card(b))].filter(defined);
    //const drops = ;
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

  show() {
    domDialog({
      class: 'game-setup.local-setup',
      css: [{ hashed: 'local.setup' }],
      htmlText: `<div class="chin">
            <input type="checkbox" id="bot-dev" checked>
            <label for="bot-dev">Dev UI</label>
          </div>`,
      append: [{ node: this.view, where: '.chin', how: 'before' }],
      actions: [
        { selector: '.fight', listener: this.fight },
        { selector: '.switch', listener: this.switch },
        { selector: '.random', listener: this.random },
        { selector: '.white > img.remove', listener: () => this.select('white') },
        { selector: '.black > img.remove', listener: () => this.select('black') },
      ],
      noCloseButton: this.noClose,
      noClickAway: this.noClose,
    }).then(dlg => {
      if (this.setup.white) this.select('white', this.setup.white);
      if (this.setup.black) this.select('black', this.setup.black);
      dlg.showModal();
      this.hand.resize();
    });
  }

  dropSelect = (target: HTMLElement, domId?: string) => {
    const color = target.classList.contains('white') ? 'white' : 'black';
    console.log(domId, domIdToUid(domId));
    this.select(color, domIdToUid(domId));
  };

  select(color: 'white' | 'black', selection?: string) {
    const bot = selection ? this.bots[selection] : undefined;
    this.view.querySelector(`.${color} .placard`)!.textContent = bot ? bot.description : 'Player';
    this.setup[color] = bot?.uid;
    if (!bot) this.hand.redraw();
    this[color].querySelector(`img.remove`)?.classList.toggle('show', !!bot);
  }

  fight = () => {
    this.setup.time = 'unlimited';
    this.setup.go = true;
    const form = $as<HTMLFormElement>(
      `<form method="POST" action="/local?devUi=${$as<HTMLInputElement>('#bot-dev').checked}">`,
    );
    for (const [k, v] of Object.entries(this.setup)) {
      form.appendChild($as<HTMLInputElement>(`<input name="${k}" type="hidden" value="${v ?? ''}">`));
    }
    document.body.appendChild(form);
    form.submit();
    form.remove();
  };

  switch = () => {
    const newBlack = this.setup.white;
    this.select('white', this.setup.black);
    this.select('black', newBlack);
    this.hand.redraw();
  };

  random = () => {
    if (Math.random() < 0.5) this.switch();
    this.fight();
  };
}
