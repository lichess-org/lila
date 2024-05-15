import type { Libot, Libots } from '../bots/libot';
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
  //dragging?: string;
  rect: DOMRect;
  selectedCard: HTMLDivElement | null = null;

  constructor(readonly bots: Libots) {
    //this.view.innerHTML = '';
    this.view = $as<HTMLDivElement>(`<div class="local-view">
    <div class="vs">
      <div class="player white">White</div>
      <div class="actions"></div>
      <div class="player black">Black</div>
    </div>
    </div>`);
    for (const bot of this.bots.sort()) {
      const card = this.createCard(bot);
      this.cards.push(card);
      this.view.appendChild(card);
    }
    this.show();
    this.white = this.view.querySelector('.white')!;
    this.black = this.view.querySelector('.black')!;
    this.animate();
    this.resetTimer();
  }

  createCard(bot: Libot) {
    const card = document.createElement('div');
    card.id = bot.uid;
    card.classList.add('card');
    const img = document.createElement('img');
    img.src = bot.imageUrl;
    const label = document.createElement('label');
    label.innerText = bot.name;
    //card.setAttribute('draggable', 'true');
    card.appendChild(label);
    card.appendChild(img);
    card.addEventListener('pointerdown', e => this.startDrag(e));
    card.addEventListener('pointermove', e => this.duringDrag(e));
    card.addEventListener('pointerup', e => this.endDrag(e));
    card.addEventListener('mouseenter', e => this.mouseEnter(e));
    card.addEventListener('mouseleave', e => this.mouseLeave(e));
    card.addEventListener('dragstart', e => e.preventDefault());
    return card;
  }

  placeCards() {
    this.rect = this.view.getBoundingClientRect();
    const radius = this.view.offsetWidth;
    const containerHeight = this.view.offsetHeight;

    this.userMidX = radius / 2;
    this.userMidY = containerHeight + Math.sqrt(3) * this.userMidX;

    console.log('placeCards');
    const beginAngle = -Math.PI / 8;
    const visibleCards = this.cards.length;
    const hovered = $as<HTMLElement>($('.card.pull'));
    const hoveredIndex = this.cards.findIndex(x => x == hovered);
    for (let cardIndex = this.cards.length - 1; cardIndex >= 0; cardIndex--) {
      const card = this.cards[cardIndex];
      const angleNudge = !hovered || cardIndex <= hoveredIndex ? 0 : (Math.PI * 0.5) / visibleCards;
      const angle =
        beginAngle + angleNudge + this.dragAngle + ((Math.PI / 4) * (cardIndex + 0.5)) / visibleCards;
      this.transform(card, angle);
    }
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
    this.resetTimer();
  }

  mouseLeave(e: MouseEvent) {
    $(e.target as HTMLElement).removeClass('pull');
    this.resetTimer();
  }

  startDrag(e: PointerEvent): void {
    this.startAngle = this.getAngle([e.clientX, e.clientY]) - this.dragAngle;
    this.dragMag = this.startMag = this.getMag([e.clientX, e.clientY]);

    this.selectedCard = e.currentTarget as HTMLDivElement;
    this.selectedCard.setPointerCapture(e.pointerId);
    this.selectedCard.style.transition = 'none';
    this.resetTimer();
  }

  duringDrag(e: PointerEvent): void {
    e.preventDefault();
    if (!this.selectedCard) return;
    $('.player').removeClass('drag-over');
    this.dropTarget(e)?.classList.add('drag-over');
    const newAngle = this.getAngle([e.clientX, e.clientY]);

    this.dragMag = this.getMag([e.clientX, e.clientY]);
    this.dragAngle = newAngle - this.startAngle;
    this.placeCards();
    this.resetTimer();
  }

  endDrag(e: PointerEvent): void {
    $('.player').removeClass('drag-over');

    if (this.selectedCard) {
      this.selectedCard.style.transition = '';
      this.selectedCard.releasePointerCapture(e.pointerId);
      const target = this.dropTarget(e);
      if (target) {
        target.style.backgroundImage = `url(${this.selectedCard.querySelector('img')!.src})`;
        target.innerText = this.selectedCard.querySelector('label')!.innerText;
      }
      /*if (this.dragMag - this.startMag > 20) {
        console.log(this.selectedCard);
        //this.select(this.bots.bots[this.selectedCard!.id.slice(1)]);

        // do sommeeat
      }*/
    }
    this.startMag = this.dragMag = this.startAngle = this.dragAngle = 0;
    this.selectedCard = null;
    this.placeCards();
    this.resetTimer();
  }

  dropTarget(e: PointerEvent): HTMLDivElement | undefined {
    for (const player of [this.white, this.black]) {
      const rect = player.getBoundingClientRect();
      if (
        e.clientX >= rect.left &&
        e.clientX <= rect.right &&
        e.clientY >= rect.top &&
        e.clientY <= rect.bottom
      ) {
        return player;
      }
    }
    return undefined;
  }

  animate = () => {
    if (document.querySelector('.game-setup.local-setup')) this.placeCards();
    this.frame = requestAnimationFrame(this.animate);
  };

  resetTimer() {
    if (this.frame === 0) this.animate();
    clearTimeout(this.killAnimation);
    this.killAnimation = setTimeout(() => {
      console.log('killing');
      cancelAnimationFrame(this.frame);
      this.frame = 0;
    }, 300);
  }

  show() {
    site.dialog.dom({
      class: 'game-setup.local-setup',
      css: [{ hashed: 'local.setup' }],
      htmlText: `
      <h2>Select Players</h2>
      <div class="content"></div>
      <div class="chin"></div>
      `,
      append: [{ selector: '.content', node: this.view }],
      show: 'modal',
    });
  }
}
