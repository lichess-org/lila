import type { Libot, Libots } from './interfaces';
import { HandOfCards } from './handOfCards';
import { defined } from 'common';
import { isTouchDevice } from 'common/device';
//import { clamp } from 'common';
import * as licon from 'common/licon';
//import { ratingView } from './components/ratingView';

export class EditBotDialog {
  view: HTMLElement;
  botEl: HTMLElement;
  hand: HandOfCards;
  constructor(
    readonly bots: Libots,
    uid?: string,
  ) {
    this.view = $as<HTMLElement>(`<div class="local-view">
        <div class="player"><img class="remove" src="${site.asset.flairSrc(
          'symbols.cancel',
        )}"><div class="placard">Player</div></div>
    `);
    this.botEl = this.view.querySelector('.player')!;
    this.hand = new HandOfCards(
      this.view,
      [this.botEl],
      Object.values(this.bots)
        .map(b => b.card)
        .filter(defined),
      this.select,
    );
    this.show();
  }

  show() {
    site.dialog
      .dom({
        class: 'game-setup.local-setup',
        css: [{ hashed: 'local.setup' }],
        append: [{ node: this.view }],
        action: [],
      })
      .then(dlg => {
        dlg.showModal();
      });
  }
  select = (target: HTMLElement, domId?: string) => {};
}
