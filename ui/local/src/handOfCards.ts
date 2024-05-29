import { isTouchDevice } from 'common/device';

export interface CardData {
  readonly imageUrl: string;
  readonly label: string;
  readonly domId: string;
}

export class HandOfCards {
  cards: HTMLElement[] = [];
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
  selectedCard: HTMLElement | null = null;

  constructor(
    readonly view: HTMLElement,
    readonly drops: HTMLElement[],
    readonly cardData: Iterable<CardData>,
    readonly select: (target: HTMLElement, domId?: string) => void,
  ) {
    for (const c of cardData) this.cards.push(this.createCard(c));
    this.cards.reverse().forEach(card => this.view.appendChild(card));
    window.addEventListener('resize', this.resize);
  }

  createCard(c: CardData) {
    const card = $as<HTMLElement>(`<div id="${c.domId}" class="card">
      <img src="${c.imageUrl}">
      <label>${c.label}</label>
    </div>`);
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
      window.getComputedStyle(document.documentElement).getPropertyValue('---scale-factor'),
    );
    if (isNaN(this.scaleFactor)) this.scaleFactor = 1;
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

  transform(card: HTMLElement, angle: number) {
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
    this.selectedCard = e.currentTarget as HTMLElement;
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
    for (const drop of this.drops) drop.classList.remove('drag-over');
    this.dropTarget(e)?.classList.add('drag-over');
    const newAngle = this.getAngle([e.clientX, e.clientY]);

    this.dragMag = this.getMag([e.clientX, e.clientY]);
    this.dragAngle = newAngle - this.startAngle;
    this.placeCards();
    this.resetIdleTimer();
  }

  endDrag(e: PointerEvent): void {
    for (const drop of this.drops) drop.classList.remove('drag-over');
    $('.card').removeClass('pull');
    this.view.classList.remove('dragging');
    if (this.selectedCard) {
      this.selectedCard.style.transition = '';
      this.selectedCard.releasePointerCapture(e.pointerId);
      const target = this.dropTarget(e);
      if (target) this.select(target, this.selectedCard.id);
    }
    this.startMag = this.dragMag = this.startAngle = /*this.dragAngle =*/ 0;
    this.selectedCard = null;
    this.placeCards();
    this.resetIdleTimer();
  }

  dropTarget(e: PointerEvent): HTMLElement | undefined {
    for (const drop of this.drops) {
      const r = drop.getBoundingClientRect();
      if (e.clientX >= r.left && e.clientX <= r.right && e.clientY >= r.top && e.clientY <= r.bottom)
        return drop;
    }
    return undefined;
  }

  animate = () => {
    if (document.contains(this.view)) this.placeCards();
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

  get fanRadius() {
    return this.view.offsetWidth;
  }
}
