import type { Libot, Libots } from './bots/interfaces';
import type { GameSetup } from './interfaces';
import { isTouchDevice } from 'common/device';
//import { clamp } from 'common';
import * as licon from 'common/licon';
//import { ratingView } from './components/ratingView';

export class LocalDialog {
  cards: HTMLDivElement[] = [];
  view: HTMLDivElement;
  white: HTMLDivElement;
  black: HTMLDivElement;
  userMidX: number;
  userMidY: number;
  startAngle = 0;
  startMag = 0;
  dragMag = 0;
  dragAngle: number = 0;
  frame: number = 0;
  killAnimation = 0;
  scaleFactor = 1;
  rect: DOMRect;
  selectedCard: HTMLDivElement | null = null;
  setup: GameSetup = {};

  constructor(readonly bots: Libots) {
    this.view = $as<HTMLDivElement>(`<div class="local-view">
      <div class="vs">
        <div class="player white"><div class="placard white">Human</div></div>
        <div class="actions">
          <button class="button button-empty random" data-icon="${licon.DieSix}"></button>
          <button class="button button-empty switch" data-icon="${licon.Switch}"></button>
          <button class="button button-empty fight" data-icon="${licon.Swords}"></button>
        </div>
        <div class="player black"><div class="placard black">Human</div></div>
      </div>
    </div>`);
    Object.values(this.bots).forEach(bot => this.cards.push(this.createCard(bot)));
    this.cards.reverse().forEach(card => this.view.appendChild(card));
    this.show();
    this.white = this.view.querySelector('.white')!;
    this.black = this.view.querySelector('.black')!;
    window.addEventListener('resize', this.resize);
  }

  createCard(bot: Libot) {
    const card = $as<HTMLDivElement>(`<div id="${bot.uid.slice(1)}" class="card">
      <img src="${bot.imageUrl}"><label>${bot.name}</label></div>`);
    card.addEventListener('pointerdown', e => this.startDrag(e));
    card.addEventListener('pointermove', e => this.duringDrag(e));
    card.addEventListener('pointerup', e => this.endDrag(e));
    card.addEventListener('mouseenter', e => this.mouseEnter(e));
    card.addEventListener('mouseleave', e => this.mouseLeave(e));
    card.addEventListener('dragstart', e => e.preventDefault());
    return card;
  }

  resize = () => {
    const newRect = this.view.getBoundingClientRect();
    if (this.rect && newRect.width === this.rect.width && newRect.height === this.rect.height) return;
    this.scaleFactor = parseFloat(
      getComputedStyle(document.documentElement).getPropertyValue('---scale-factor'),
    );
    const h2 = 192 * this.scaleFactor - (1 - Math.sqrt(3 / 4)) * this.fanRadius;
    this.rect = newRect;
    this.userMidX = this.view.offsetWidth / 2;
    this.userMidY = this.view.offsetHeight + Math.sqrt(3 / 4) * this.fanRadius - h2;
    this.animate();
    this.resetIdleTimer();
  };

  placeCards() {
    const visibleCards = Math.min(this.view.offsetWidth / 50, this.cards.length);
    const hovered = $as<HTMLElement>($('.card.pull'));
    const hoveredIndex = this.cards.findIndex(x => x == hovered);
    for (const [i, card] of this.cards.entries()) {
      const pull = !hovered || i <= hoveredIndex ? 0 : (-(Math.PI / 2) * this.scaleFactor) / visibleCards;
      const fanout = ((Math.PI / 4) * (this.cards.length - i - 0.5)) / visibleCards;
      this.transform(card, -Math.PI / 8 + pull + this.dragAngle + fanout);
    }
  }

  transform(card: HTMLDivElement, angle: number) {
    const hovered = card.classList.contains('pull');
    const mag =
      15 + this.view.offsetWidth + (hovered ? 40 * this.scaleFactor + this.dragMag - this.startMag : 0);
    const x = this.userMidX + mag * Math.sin(angle) - 64;
    const y = this.userMidY - mag * Math.cos(angle);
    if (hovered) angle += Math.PI / 12;
    card.style.transform = `translate(${x}px, ${y}px) rotate(${angle}rad)`;
  }

  clientToOrigin(client: [number, number]): [number, number] {
    const originX = client[0] - (this.rect.left + window.scrollX) - this.userMidX;
    const originY = this.rect.top + window.scrollY + this.userMidY - client[1];
    return [originX, originY];
  }

  originToClient(origin: [number, number]): [number, number] {
    const clientX = this.rect.left + window.scrollX + this.userMidX + origin[0];
    const clientY = this.rect.top + window.scrollY + this.userMidY - origin[1];
    return [clientX, clientY];
  }

  getAngle(client: [number, number]): number {
    const translated = this.clientToOrigin(client);
    return Math.atan2(translated[0], translated[1]);
  }

  getMag(client: [number, number]): number {
    const userPt = this.clientToOrigin(client);
    return Math.sqrt(userPt[0] * userPt[0] + userPt[1] * userPt[1]);
  }

  mouseEnter(e: MouseEvent) {
    $(e.target as HTMLElement).addClass('pull');
    this.resetIdleTimer();
  }

  mouseLeave(e: MouseEvent) {
    $(e.target as HTMLElement).removeClass('pull');
    this.resetIdleTimer();
  }

  startDrag(e: PointerEvent): void {
    this.startAngle = this.getAngle([e.clientX, e.clientY]) - this.dragAngle;
    this.dragMag = this.startMag = this.getMag([e.clientX, e.clientY]);
    this.view.classList.add('dragging');
    this.selectedCard = e.currentTarget as HTMLDivElement;
    if (isTouchDevice()) {
      $('.card').removeClass('pull');
      this.selectedCard.classList.add('pull');
    }
    this.selectedCard.setPointerCapture(e.pointerId);
    this.selectedCard.style.transition = 'none';
    this.resetIdleTimer();
  }

  duringDrag(e: PointerEvent): void {
    e.preventDefault();
    if (!this.selectedCard) return;
    $('.player').removeClass('drag-over');
    this.dropTarget(e)?.el.classList.add('drag-over');
    const newAngle = this.getAngle([e.clientX, e.clientY]);

    this.dragMag = this.getMag([e.clientX, e.clientY]);
    this.dragAngle = newAngle - this.startAngle;
    this.placeCards();
    this.resetIdleTimer();
  }

  endDrag(e: PointerEvent): void {
    $('.player').removeClass('drag-over');
    $('.card').removeClass('pull');
    this.view.classList.remove('dragging');
    if (this.selectedCard) {
      this.selectedCard.style.transition = '';
      this.selectedCard.releasePointerCapture(e.pointerId);
      const target = this.dropTarget(e);
      if (target) {
        this.select(target.color as 'white' | 'black', this.selectedCard.id);
      }
      /*if (this.dragMag - this.startMag > 20) {
        console.log(this.selectedCard);
        //this.select(this.bots.bots[this.selectedCard!]);

        // do sommeeat
      }*/
    }
    this.startMag = this.dragMag = this.startAngle = /*this.dragAngle =*/ 0;
    this.selectedCard = null;
    this.placeCards();
    this.resetIdleTimer();
  }

  dropTarget(e: PointerEvent): { el: HTMLDivElement; color: string } | undefined {
    for (const player of [
      { el: this.white, color: 'white' },
      { el: this.black, color: 'black' },
    ]) {
      const r = player.el.getBoundingClientRect();
      if (e.clientX >= r.left && e.clientX <= r.right && e.clientY >= r.top && e.clientY <= r.bottom) {
        return player;
      }
    }
    return undefined;
  }

  animate = () => {
    if (document.querySelector('.game-setup.local-setup')) this.placeCards();
    this.frame = requestAnimationFrame(this.animate);
  };

  resetIdleTimer() {
    if (this.frame === 0) this.animate();
    clearTimeout(this.killAnimation);
    this.killAnimation = setTimeout(() => {
      cancelAnimationFrame(this.frame);
      this.frame = 0;
    }, 300);
  }

  show() {
    site.dialog
      .dom({
        class: 'game-setup.local-setup',
        css: [{ hashed: 'local.setup' }],
        htmlText: `<div class="chin"></div>`,
        append: [{ node: this.view, where: '.chin', how: 'before' }],
        action: [
          { selector: '.fight', action: dlg => this.fight(dlg) },
          { selector: '.switch', action: dlg => this.switch(dlg) },
          { selector: '.random', action: dlg => this.random(dlg) },
        ],
      })
      .then(dlg => {
        dlg.showModal();
        this.resize();
      });
  }

  select(color: 'white' | 'black', player?: string) {
    const bot = player ? this.bots[player] : undefined;
    const placard = this.view.querySelector(`.${color} .placard`);
    const card = this.selectedCard;
    if (bot && card) {
      this.view.style.setProperty(`---${color}-image-url`, `url(${card.querySelector('img')!.src})`);
      $(`.placard.${color}`).text(bot.description);
      placard!.textContent = bot.description;
    }
    this.setup[color] = bot?.uid;
  }

  fight = (dlg: Dialog) => {
    site.asset.loadEsm('local', { init: this.setup });
    dlg.close();
  };

  switch = (dlg: Dialog) => {
    const white = this.white.querySelector('.placard')!.textContent;
    const black = this.black.querySelector('.placard')!.textContent;
    this.white.querySelector('.placard')!.textContent = black;
    this.black.querySelector('.placard')!.textContent = white;
  };

  random = (dlg: Dialog) => {};

  get fanRadius() {
    return this.view.offsetWidth;
  }
}
