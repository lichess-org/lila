import { h } from 'snabbdom';
import { MaybeVNodes, onInsert } from 'common/snabbdom';
import { SetupCtrl } from '../ctrl';
import { fenInput } from './components/fenInput';
import { timePickerAndSliders } from './components/timePickerAndSliders';
import { colorButtons } from './components/colorButtons';
import { ratingView } from './components/ratingView';
import { localBots, type BotInfo } from 'libot';

let deck: BotDeck;

export default function localContent(ctrl: SetupCtrl): MaybeVNodes {
  return [
    h('h2', 'Select Opponent'),
    //h('div#bot-view', [
    h('div#bot-view', {
      key: 'bot-view',
      hook: onInsert(el => {
        deck = new BotDeck(el as HTMLDivElement);
      }),
    }),
    fenInput(ctrl),
    timePickerAndSliders(ctrl, true),
    colorButtons(ctrl),
    //]),
  ];
}

class BotDeck {
  constructor(readonly view: HTMLDivElement) {
    this.botInfos.forEach(bot => this.createCard(bot));
    this.animate();
  }
  botInfos = Object.values(localBots);
  cards: HTMLDivElement[] = [];
  userMidX: number;
  userMidY: number;
  startAngle = 0;
  startMag = 0;
  handRotation: number = 0;
  selectedCard: HTMLDivElement | null = null;

  createCard(bot: BotInfo) {
    const card = document.createElement('div');
    card.classList.add('card');
    const img = document.createElement('img');
    img.src = bot.image;
    const label = document.createElement('label');
    label.innerText = bot.name;
    card.appendChild(label);
    card.appendChild(img);
    card.addEventListener('pointerdown', e => this.startDrag(e));
    card.addEventListener('pointermove', e => this.duringDrag(e));
    card.addEventListener('pointerup', e => this.endDrag(e));
    card.addEventListener('mouseenter', e => this.mouseEnter(e));
    card.addEventListener('mouseleave', e => this.mouseLeave(e));
    this.cards.push(card);
    this.view.appendChild(card);
    return card;
  }

  placeCards() {
    const radius = this.view.offsetWidth;
    const containerHeight = this.view.offsetHeight;

    this.userMidX = radius / 2;
    this.userMidY = containerHeight + Math.sqrt(3) * this.userMidX;

    const beginAngle = -Math.PI / 8;
    const visibleCards = this.cards.length;
    const hovered = $as<HTMLElement>($('.card.pull'));
    const hoveredIndex = this.cards.findIndex(x => x == hovered);
    this.cards.forEach((card, cardIndex) => {
      const angleNudge =
        !hovered || cardIndex == hoveredIndex
          ? 0
          : cardIndex < hoveredIndex
          ? 0 //(-Math.PI * 0.25) / visibleCards
          : (Math.PI * 0.5) / visibleCards;
      let angle =
        beginAngle + angleNudge + this.handRotation + ((Math.PI / 4) * (cardIndex + 0.5)) / visibleCards;
      const mag = 15 + radius + ($(card).hasClass('pull') ? 40 : 0);
      const x = this.userMidX + mag * Math.sin(angle) - 64;
      const y = this.userMidY - mag * Math.cos(angle);
      if (cardIndex === hoveredIndex) angle -= Math.PI / 8;
      card.style.transform = `translate(${x}px, ${y}px) rotate(${angle}rad)`;
    });
  }

  clientToOrigin(client: [number, number]): [number, number] {
    const originX = client[0] - this.view.offsetLeft - this.userMidX;
    const originY = this.view.offsetTop + this.userMidY - client[1];
    return [originX, originY];
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
  }

  mouseLeave(e: MouseEvent) {
    $(e.target as HTMLElement).removeClass('pull');
  }

  startDrag(e: PointerEvent): void {
    e.preventDefault();

    this.startAngle = this.getAngle([e.clientX, e.clientY]) - this.handRotation;
    this.startMag = this.getMag([e.clientX, e.clientY]);

    this.selectedCard = e.currentTarget as HTMLDivElement;

    this.selectedCard.setPointerCapture(e.pointerId);
  }

  duringDrag(e: PointerEvent): void {
    e.preventDefault();
    if (!this.selectedCard) return;

    const newAngle = this.getAngle([e.clientX, e.clientY]);

    this.handRotation = newAngle - this.startAngle;
    this.placeCards();
  }

  endDrag(e: PointerEvent): void {
    if (this.selectedCard) {
      this.selectedCard.releasePointerCapture(e.pointerId);
    }
    this.selectedCard = null;
  }

  animate() {
    requestAnimationFrame(() => this.animate());
    this.placeCards();
  }
}
/*function botInfo(ctrl: SetupCtrl, bot: BotInfo) {
  ctrl;
  return h('div', [h('img', { attrs: { src: bot.image } }), h('div', [h('h2', bot.name), h('p', bot.description)])]);
}*/
