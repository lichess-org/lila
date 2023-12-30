import { h } from 'snabbdom';
import { MaybeVNodes, onInsert } from 'common/snabbdom';
import { SetupCtrl } from '../ctrl';
import { fenInput } from './components/fenInput';
import { timePickerAndSliders } from './components/timePickerAndSliders';
import { colorButtons } from './components/colorButtons';
import type { Libot, Libots } from 'libot';
import { spinnerVdom } from 'common/spinner';
//import { ratingView } from './components/ratingView';

export default function localContent(ctrl: SetupCtrl): MaybeVNodes {
  return [
    h('h2', 'Select Opponent'),
    h(
      'div#bot-view',
      {
        key: 'bot-view',
        hook: onInsert(el => new BotDeck(el as HTMLDivElement, bot => console.log(bot))),
      },
      [spinnerVdom()],
    ),
    fenInput(ctrl),
    timePickerAndSliders(ctrl, true),
    colorButtons(ctrl),
  ];
}

class BotDeck {
  bots: Libots;
  cards: HTMLDivElement[] = [];
  userMidX: number;
  userMidY: number;
  startAngle = 0;
  startMag = 0;
  dragMag = 0;
  dragAngle: number = 0;
  rect: DOMRect;
  selectedCard: HTMLDivElement | null = null;

  constructor(
    readonly view: HTMLDivElement,
    readonly select: (bot: Libot) => void,
  ) {
    lichess.asset.loadEsm<Libots>('libot', { init: { stubs: true } }).then(bots => {
      this.view.innerHTML = '';
      this.bots = bots;
      for (const bot of this.bots.sort()) this.createCard(bot);
      this.animate();
    });
  }

  createCard(bot: Libot) {
    const card = document.createElement('div');
    card.id = bot.uid;
    card.classList.add('card');
    const img = document.createElement('img');
    img.src = bot.imageUrl;
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
    this.rect = this.view.getBoundingClientRect();
    const radius = this.view.offsetWidth;
    const containerHeight = this.view.offsetHeight;

    this.userMidX = radius / 2;
    this.userMidY = containerHeight + Math.sqrt(3) * this.userMidX;

    const beginAngle = -Math.PI / 8;
    const visibleCards = this.cards.length;
    const hovered = $as<HTMLElement>($('.card.pull'));
    const hoveredIndex = this.cards.findIndex(x => x == hovered);
    this.cards.forEach((card, cardIndex) => {
      const angleNudge = !hovered || cardIndex <= hoveredIndex ? 0 : (Math.PI * 0.5) / visibleCards;
      const angle =
        beginAngle + angleNudge + this.dragAngle + ((Math.PI / 4) * (cardIndex + 0.5)) / visibleCards;
      this.transform(card, angle);
    });
  }

  transform(card: HTMLDivElement, angle: number) {
    const hovered = card.classList.contains('pull');
    const mag = 15 + this.view.offsetWidth + (hovered ? 40 + this.dragMag - this.startMag : 0);
    const x = this.userMidX + mag * Math.sin(angle) - 64;
    const y = this.userMidY - mag * Math.cos(angle);
    if (hovered) angle -= Math.PI / 8;
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
  }

  mouseLeave(e: MouseEvent) {
    $(e.target as HTMLElement).removeClass('pull');
  }

  startDrag(e: PointerEvent): void {
    e.preventDefault();

    this.startAngle = this.getAngle([e.clientX, e.clientY]) - this.dragAngle;
    this.dragMag = this.startMag = this.getMag([e.clientX, e.clientY]);

    this.selectedCard = e.currentTarget as HTMLDivElement;

    this.selectedCard.setPointerCapture(e.pointerId);
  }

  duringDrag(e: PointerEvent): void {
    e.preventDefault();
    if (!this.selectedCard) return;

    const newAngle = this.getAngle([e.clientX, e.clientY]);

    this.dragMag = this.getMag([e.clientX, e.clientY]);
    this.dragAngle = newAngle - this.startAngle;
    this.placeCards();
  }

  endDrag(e: PointerEvent): void {
    if (this.selectedCard) {
      this.selectedCard.releasePointerCapture(e.pointerId);
      if (this.dragMag - this.startMag > 20) {
        console.log(this.selectedCard);
        this.select(this.bots.bots[this.selectedCard!.id.slice(1)]);
      }
    }
    this.startMag = this.dragMag = this.startAngle = this.dragAngle = 0;
    this.selectedCard = null;
    this.placeCards();
  }

  animate() {
    requestAnimationFrame(() => this.animate());
    this.placeCards(); // TODO - only do this if the cards have moved
  }
}
