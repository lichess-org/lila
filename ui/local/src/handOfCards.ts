import { isTouchDevice } from 'common/device';
import { clamp } from 'common';

const BASE_CARD_SIZE = 192; // at ---scale-factor: 1

type DomId = string; // must not contain any dom selector chars

export interface CardData {
  imageUrl?: string;
  label: string;
  domId: DomId;
}

export interface HandOwner {
  view: () => HTMLElement;
  drops: () => { el: HTMLElement; selected?: DomId }[];
  cardData: () => Iterable<CardData>;
  select: (drop: HTMLElement, domId?: DomId) => void;
  deck?: () => HTMLElement;
  autoResize?: boolean;
}

export class HandOfCards {
  cards: HTMLElement[] = [];
  userMidX: number;
  userMidY: number;
  //startAngle = 0;
  startMag = 0;
  dragMag = 0;
  dragAngle: number = 0;
  frame: number = 0;
  killAnimation = 0;
  scaleFactor = 1;
  pointerDownTime?: number;
  touchDragShape?: TouchDragShape;
  dragCard: HTMLElement | null = null;
  rect: DOMRect;

  constructor(readonly owner: HandOwner) {
    this.updateCards();
    this.view.addEventListener('mousemove', this.mouseMove);
    if (owner.autoResize) window.addEventListener('resize', this.resize);
  }

  resize: () => void = () => {
    const newRect = this.view.getBoundingClientRect();
    if (this.rect && newRect.width === this.rect.width && newRect.height === this.rect.height) return;
    this.scaleFactor = 0.8 + 0.3 * clamp((newRect.width - 400) / 200, { min: 0, max: 1 });
    this.view.style.setProperty('---scale-factor', String(this.scaleFactor));
    const h2 = BASE_CARD_SIZE * this.scaleFactor - (1 - Math.sqrt(3 / 4)) * this.fanRadius;
    this.rect = newRect;
    this.userMidX = this.view.offsetWidth / 2;
    this.userMidY = this.view.offsetHeight + Math.sqrt(3 / 4) * this.fanRadius - h2;
    this.redraw();
  };

  updateCards(): void {
    const data = [...this.owner.cardData()].reverse();
    const deletes = this.cards.filter(x => !data.some(y => y.domId === x.id));
    for (const cd of data) {
      const card = this.cards.find(c => c.id === cd.domId);
      if (!card) {
        const newCard = this.createCard(cd);
        this.cards.push(newCard);
        this.view.append(newCard);
      } else {
        const label = card.querySelector('label') as HTMLElement;
        if (cd.label !== label.textContent) label.textContent = cd.label;
        const img = card.querySelector('img') as HTMLImageElement;
        if (cd.imageUrl !== img.src) img.src = cd.imageUrl ?? '';
      }
    }
    for (const card of deletes) {
      this.cards.splice(this.cards.indexOf(card), 1);
      card.remove();
    }
    this.redraw();
  }

  redraw(andKeepAnimatingFor = 300): void {
    if (this.frame === 0) this.animate();
    clearTimeout(this.killAnimation);
    this.killAnimation = setTimeout(() => {
      cancelAnimationFrame(this.frame);
      this.frame = 0;
    }, andKeepAnimatingFor);
  }

  private get drops() {
    return this.owner.drops();
  }
  private get view() {
    return this.owner.view();
  }
  private get select() {
    return this.owner.select;
  }
  private get selected() {
    return this.drops.map(x => this.cards.find(y => y.id === x.selected));
  }
  private get deck() {
    return this.owner.deck?.();
  }
  private get fanout() {
    return !this.deck || this.deck.classList.contains('fanout');
  }
  private get fanRadius() {
    return this.view.offsetWidth;
  }

  private createCard(c: CardData) {
    const card = $as<HTMLElement>(`<div id="${c.domId}" class="card">
      <img src="${c.imageUrl}">
      <label>${c.label}</label>
    </div>`);
    card.addEventListener('pointerdown', this.pointerDown);
    card.addEventListener('pointermove', this.pointerMove);
    card.addEventListener('pointerup', this.pointerUp);
    card.addEventListener('mouseenter', this.mouseEnterCard);
    card.addEventListener('mouseleave', this.mouseLeaveCard);
    card.addEventListener('dragstart', e => e.preventDefault());
    return card;
  }

  private placeCards() {
    const visibleCards = this.cards.length; //Math.min(this.view.offsetWidth / 50, this.cards.length);
    const hovered = $as<HTMLElement>($('.card.pull'));
    const hoveredIndex = this.cards.findIndex(x => x == hovered);
    const deck = this.cards.filter(x => x !== this.dragCard);
    const selected = deck.filter(x => !this.selectedTransform(x));
    const arcSlice = Math.PI / 5;

    if (!this.fanout)
      for (const [i, card] of deck.filter(x => !selected.includes(x)).entries()) {
        card.style.backgroundColor = '';
        this.deckTransform(card, deck.length - selected.length - i);
      }
    else
      for (const [i, card] of this.cards.entries()) {
        if (this.selected.includes(card) || card === this.dragCard) continue;
        card.style.backgroundColor = '';
        const pull =
          !hovered || i <= hoveredIndex ? 0 : -(Math.PI / 2) /* * this.scaleFactor*/ / visibleCards;
        const fanout = ((Math.PI / 5) * (this.cards.length - i - 0.5)) / visibleCards;
        this.fanoutTransform(card, fanout - arcSlice / 2 + pull);
      }
  }

  private deckTransform(card: HTMLElement, i: number) {
    const dindex = this.drops.findIndex(x => x.selected === card.id);
    if (dindex >= 0 || !this.deck) return false;
    const to = this.deck;
    const x = to.offsetLeft - card.offsetLeft + (to.offsetWidth - card.offsetWidth) / 2 + i;
    const y = to.offsetTop - card.offsetTop + (to.offsetHeight - card.offsetHeight) / 2 + i;
    card.style.transform = `translate(${x}px, ${y}px) rotate(-5deg)`;
    return true;
  }

  private fanoutTransform(card: HTMLElement, angle: number) {
    const hovered = card.classList.contains('pull');
    const mag =
      15 + this.view.offsetWidth + (hovered ? 40 * this.scaleFactor + this.dragMag - this.startMag : 0);
    const x = this.userMidX + mag * Math.sin(angle) - (BASE_CARD_SIZE * this.scaleFactor) / 2;
    const y = this.userMidY - mag * Math.cos(angle);
    if (hovered) angle += Math.PI / 12;
    card.style.transform = `translate(${x}px, ${y}px) rotate(${angle}rad)`;
  }

  private selectedTransform(card: HTMLElement) {
    const dindex = this.drops.findIndex(x => x.selected === card.id);
    card.classList.toggle('selected', dindex >= 0);
    if (dindex < 0) return true;
    const to = this.drops[dindex].el;
    const scale = to.offsetHeight / card.offsetHeight;
    const x = to.offsetLeft - card.offsetLeft + (to.offsetWidth - card.offsetWidth) / 2;
    const y = to.offsetTop - card.offsetTop + (to.offsetHeight - card.offsetHeight) / 2;
    card.style.transform = `translate(${x}px, ${y}px) scale(${scale})`;
    card.style.backgroundColor = window.getComputedStyle(to).backgroundColor;
    return false;
  }

  private clientToOrigin(client: [number, number]): [number, number] {
    const originX = client[0] - (this.rect.left + window.scrollX) - this.userMidX;
    const originY = this.rect.top + window.scrollY + this.userMidY - client[1];
    return [originX, originY];
  }

  private clientToView(client: [number, number]): [number, number] {
    const viewX = client[0] - (this.rect.left + window.scrollX);
    const viewY = client[1] - (this.rect.top + window.scrollY);
    return [viewX, viewY];
  }

  private originToClient(origin: [number, number]): [number, number] {
    const clientX = this.rect.left + window.scrollX + this.userMidX + origin[0];
    const clientY = this.rect.top + window.scrollY + this.userMidY - origin[1];
    return [clientX, clientY];
  }

  private getAngle(client: [number, number]): number {
    const translated = this.clientToOrigin(client);
    return Math.atan2(translated[0], translated[1]);
  }

  private getMag(client: [number, number]): number {
    const userPt = this.clientToOrigin(client);
    return Math.sqrt(userPt[0] * userPt[0] + userPt[1] * userPt[1]);
  }

  private mouseMove = (e: MouseEvent) => {
    if (this.dragCard || !this.deck) return;
    let fanout = this.fanout;
    if (document.elementFromPoint(e.clientX, e.clientY) === this.deck) fanout = true;
    else if (
      e.clientX < this.rect.left ||
      e.clientX > this.rect.right ||
      e.clientY < this.rect.bottom - BASE_CARD_SIZE * this.scaleFactor * 1.5 ||
      e.clientY > this.rect.bottom
    )
      fanout = false;
    if (fanout === this.fanout) return;
    this.deck.classList.toggle('fanout');
    this.redraw();
  };

  private mouseEnterCard = (e: MouseEvent) => {
    $(e.target as HTMLElement).addClass('pull');
    this.redraw();
  };

  private mouseLeaveCard = (e: MouseEvent) => {
    $(e.target as HTMLElement).removeClass('pull');
    this.redraw();
  };

  private pointerDown = (e: PointerEvent) => {
    const card = e.currentTarget as HTMLElement;
    this.pointerDownTime = Date.now();
    if (e.pointerType === 'touch') {
      this.view.classList.add('no-capture');
      this.touchDragShape = new TouchDragShape(e, this.cards, card, this.rect.width / this.cards.length);
      this.select(this.drops[0].el, card.id);
      //this.dragCard = card;
      //this.select(this.drops[0].el, card.id);
      this.redraw();
      return;
    }
    this.dragCard?.releasePointerCapture(e.pointerId);
    this.dragCard = card;
    this.dragCard.setPointerCapture(e.pointerId);
    this.redraw();
  };

  private pointerMove = (e: PointerEvent) => {
    if (e.pointerType === 'touch') {
      if (this.touchDragShape?.update(e)) {
        this.select(this.drops[0].el, this.cards[this.touchDragShape.currentIndex].id);
        this.redraw();
        return;
      }
    }
    if (!this.pointerDownTime || !this.dragCard) return;
    e.preventDefault();
    this.dragCard.classList.add('dragging');
    const viewPt = this.clientToView([e.clientX, e.clientY]);
    const viewX = viewPt[0] - (BASE_CARD_SIZE * this.scaleFactor) / 2;
    const viewY = viewPt[1] - (BASE_CARD_SIZE * this.scaleFactor) / 2;
    const newAngle = this.getAngle([e.clientX, e.clientY]);
    this.dragCard.style.transform = `translate(${viewX}px, ${viewY}px) rotate(${newAngle}rad)`;
    for (const drop of this.drops) drop.el?.classList.remove('drag-over');
    this.dropTarget(e)?.classList.add('drag-over');
    this.redraw();
  };

  private pointerUp = (e: PointerEvent) => {
    if (e.pointerType === 'touch') {
      this.view.classList.remove('no-capture');
      this.touchDragShape = undefined;
      return;
    }
    for (const drop of this.drops) drop.el?.classList.remove('drag-over');
    this.view.querySelectorAll('.dragging')?.forEach(x => x.classList.remove('dragging'));
    if (!this.dragCard) return;
    this.dragCard.classList.remove('pull');
    this.dragCard.releasePointerCapture(e.pointerId);
    const target =
      this.dropTarget(e) ||
      (this.pointerDownTime &&
        Date.now() - this.pointerDownTime < 500 &&
        this.drops[this.drops.length - 1].el);
    if (target) this.select(target, this.dragCard.id);
    this.dragCard = null;
    this.redraw();
    this.pointerDownTime = undefined;
  };

  private dropTarget(e: PointerEvent): HTMLElement | undefined {
    for (const drop of this.drops) {
      const r = drop.el.getBoundingClientRect();
      if (e.clientX >= r.left && e.clientX <= r.right && e.clientY >= r.top && e.clientY <= r.bottom)
        return drop.el;
    }
    return undefined;
  }

  private animate = () => {
    if (document.contains(this.view)) this.placeCards();
    this.frame = requestAnimationFrame(this.animate);
  };
}

class TouchDragShape {
  start: [number, number];
  current: [number, number];
  initialIndex: number;
  constructor(
    e: PointerEvent,
    readonly cards: HTMLElement[],
    readonly startCard: HTMLElement,
    readonly touchRadius = 25,
  ) {
    this.start = [e.clientX, e.clientY];
    this.current = [e.clientX, e.clientY];
    this.initialIndex = this.cards.indexOf(startCard);
  }
  update(e: PointerEvent): boolean {
    this.current = [e.clientX, e.clientY];
    return this.initialIndex !== this.currentIndex;
  }
  get deltaX(): number {
    return this.current[0] - this.start[0];
  }
  get deltaY(): number {
    return this.current[1] - this.start[1];
  }
  get currentIndex(): number {
    return clamp(this.initialIndex - Math.round(this.deltaX / this.touchRadius), {
      min: 0,
      max: this.cards.length - 1,
    });
  }
}
