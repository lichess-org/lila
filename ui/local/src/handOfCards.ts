import { clamp } from 'common';
import { type EventJanitor, eventJanitor } from 'common/event';

type DomId = string; // no selector chars allowed

export interface CardData {
  imageUrl?: string;
  label: string;
  domId: DomId;
}

export type Drop = { el: HTMLElement; selected?: DomId };

export interface HandOwner {
  select: (drop: HTMLElement | undefined, domId?: DomId) => void;
  onRemove?: () => void;
  getView: () => HTMLElement;
  getDrops: () => Drop[];
  getCardData: () => Iterable<CardData>;
  getDeck?: () => HTMLElement;

  autoResize?: boolean; // default false
  transient?: boolean; // default false
  orientation?: 'bottom' | 'left'; // default bottom
}

export interface HandOfCards {
  remove(): void;
  update(): void;
  resize(): void;
  redraw(): void;
}

export function handOfCards(owner: HandOwner): HandOfCards {
  return new HandOfCardsImpl(owner);
}

class HandOfCardsImpl {
  cards: HTMLElement[] = [];
  originX: number;
  originY: number;
  killAnimation = 0;
  animFrame = 0;
  animTime = 300;
  scaleFactor = 1;
  pointerDownTime?: number;
  touchDragShape?: TouchDragShape;
  dragCard: HTMLElement | null = null;
  events: EventJanitor = eventJanitor();
  rect: DOMRect;

  constructor(readonly owner: HandOwner) {
    this.update();
    this.events.addListener(this.view, 'mousemove', this.mouseMove);
    if (owner.autoResize) this.events.addListener(window, 'resize', this.resize);
    //if (!this.deck || owner.transient)
    setTimeout(this.resize);
  }

  resize: () => void = () => {
    const newRect = this.view.getBoundingClientRect();
    if (this.rect && newRect.width === this.rect.width && newRect.height === this.rect.height) return;
    this.rect = newRect;
    this.scaleFactor = 0.8 + 0.3 * clamp((newRect.width - 720) / 360, { min: 0, max: 1 });
    this.view.style.setProperty('---scale-factor', String(this.scaleFactor));
    const h2 = this.cardSize - (1 - Math.sqrt(3 / 4)) * this.fanRadius;
    this.originX = this.isLeft ? -Math.sqrt(3 / 4) * this.fanRadius : this.view.offsetWidth / 2;
    this.originY = this.isLeft
      ? (this.view.offsetHeight - h2) / 2
      : this.view.offsetHeight + Math.sqrt(3 / 4) * this.fanRadius - h2;
    this.redraw();
  };

  update(): void {
    const data = [...this.owner.getCardData()].reverse();
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

  remove(): void {
    if (!this.cards.length) return;
    const cards = this.cards.slice();
    this.cards = [];
    this.events.removeAll();
    cards.forEach(x => (x.style.transform = `translate(${this.originX}px, ${this.originY}px)`));
    setTimeout(() => {
      cards.forEach(x => x.remove());
      this.owner.onRemove?.();
    }, this.animTime);
  }

  redraw(): void {
    if (this.animFrame === 0) this.animate();
    clearTimeout(this.killAnimation);
    this.killAnimation = setTimeout(() => {
      cancelAnimationFrame(this.animFrame);
      this.animFrame = 0;
    }, this.animTime);
  }

  private createCard(c: CardData) {
    const card = $as<HTMLElement>(`<div id="${c.domId}" class="card">
      <img src="${c.imageUrl}">
      <label>${c.label}</label>
    </div>`);
    this.events.addListener(card, 'pointerdown', this.pointerDown);
    this.events.addListener(card, 'pointermove', this.pointerMove);
    this.events.addListener(card, 'pointerup', this.pointerUp);
    this.events.addListener(card, 'mouseenter', this.mouseEnterCard);
    this.events.addListener(card, 'mouseleave', this.mouseLeaveCard);
    this.events.addListener(card, 'dragstart', this.dragStart);
    return card;
  }

  private placeCards() {
    const hovered = this.view.querySelector('.card.pull');
    const hoverIndex = this.cards.findIndex(x => x == hovered);
    const deck = this.cards.filter(x => x !== this.dragCard);
    const unplaced = deck.filter(x => !this.selectedTransform(x));

    for (const [i, card] of unplaced.entries()) {
      card.style.backgroundColor = '';
      if (this.fanout) this.fanoutTransform(card, hoverIndex);
      else if (this.owner.transient) card.style.transform = `translate(${this.originX}px, ${this.originY}px)`;
      else this.deckTransform(card, i);
    }
  }

  private deckTransform(card: HTMLElement, i: number) {
    const dindex = this.drops.findIndex(x => x.selected === card.id);
    if (dindex >= 0 || !this.deck || this.owner.transient) return false;
    const to = this.deck;
    const x = to.offsetLeft - card.offsetLeft + (to.offsetWidth - card.offsetWidth) / 2 + i;
    const y = to.offsetTop - card.offsetTop + (to.offsetHeight - card.offsetHeight) / 2 + i;
    card.style.transform = `translate(${x}px, ${y}px) rotate(-5deg)`;
    return true;
  }

  private fanoutTransform(card: HTMLElement, hoverIndex: number) {
    const fanArc = Math.PI / 5;
    const centerCard = -this.cardSize / 2;
    const index = this.cards.indexOf(card);
    const visibleCards = this.cards.length; // this will do for now, but if cards go up...
    const leftPull = hoverIndex === -1 || index <= hoverIndex ? 0 : -Math.PI / visibleCards / 2;
    const bottomPull =
      hoverIndex === -1
        ? 0
        : index <= hoverIndex
        ? (Math.PI * this.cardSize) / (this.fanRadius * visibleCards)
        : (-Math.PI * this.cardSize) / (this.fanRadius * visibleCards);
    const pull = this.isLeft ? leftPull : bottomPull;
    const angle = pull + fanArc * ((this.cards.length - index - 0.5) / visibleCards - 0.5);
    const isHovered = card.classList.contains('pull');
    const magLeft =
      this.fanRadius +
      (isHovered ? this.cardSize / 4 : 0) +
      (hoverIndex === -1 ? 0 : index > hoverIndex ? centerCard : 0);
    const magBottom = this.fanRadius + centerCard / (isHovered ? 2 : 1);
    const mag = this.isLeft ? magLeft : magBottom;
    const x = this.isLeft ? this.originX + mag * Math.cos(angle) : this.originX + mag * Math.sin(angle);
    const y = this.isLeft ? this.originY + mag * Math.sin(angle) : this.originY - mag * Math.cos(angle);
    const cardRotation =
      angle +
      (this.isBottom
        ? isHovered
          ? Math.PI / 12
          : 0
        : Math.PI / 6 + (hoverIndex === -1 ? 0 : index <= hoverIndex ? 0 : -Math.PI / 6));

    card.style.transform = `translate(${x + centerCard}px, ${y + centerCard}px) rotate(${cardRotation}rad)`;
  }

  private selectedTransform(card: HTMLElement) {
    if (this.owner.transient) return false;
    const dindex = this.drops.findIndex(x => x.selected === card.id);
    card.classList.toggle('selected', dindex >= 0);
    if (dindex < 0) return false;
    const to = this.drops[dindex].el;
    const scale = to.offsetHeight / card.offsetHeight;
    const x = to.offsetLeft - card.offsetLeft + (to.offsetWidth - card.offsetWidth) / 2;
    const y = to.offsetTop - card.offsetTop + (to.offsetHeight - card.offsetHeight) / 2;
    card.style.transform = `translate(${x}px, ${y}px) scale(${scale})`;
    card.style.backgroundColor = window.getComputedStyle(to).backgroundColor;
    return true;
  }

  private clientToOriginOffset(client: [number, number]): [number, number] {
    // origin is the midpoint of the fanout circle in viewport coords
    const ooX = client[0] - (this.rect.left + window.scrollX) - this.originX;
    const ooY = this.rect.top + window.scrollY + this.originY - client[1];
    return [ooX, ooY];
  }

  private clientToElementOffset(client: [number, number]): [number, number] {
    const elX = client[0] - (this.rect.left + window.scrollX);
    const elY = client[1] - (this.rect.top + window.scrollY);
    return [elX, elY];
  }

  private getAngle(client: [number, number]): number {
    const translated = this.clientToOriginOffset(client);
    return Math.atan2(translated[0], translated[1]);
  }

  private mouseMove = (e: MouseEvent) => {
    if (this.dragCard || (!this.deck && !this.owner.transient)) return;
    let fanout = this.fanout;
    const fanDepth = this.cardSize * 1.5;
    const hoverEl = document.elementFromPoint(e.clientX, e.clientY);
    if (hoverEl === this.deck) fanout = true;
    else if (this.isLeft ? e.clientX > this.rect.left + fanDepth : e.clientY < this.rect.bottom - fanDepth)
      fanout = false;
    if (fanout === this.fanout) return;
    if (this.owner.transient && !fanout) this.remove();
    this.deck?.classList.toggle('fanout');
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

    const offsetPt = this.clientToElementOffset([e.clientX, e.clientY]);
    const offsetX = offsetPt[0] - this.cardSize / 2;
    const offsetY = offsetPt[1] - this.cardSize / 2;
    const newAngle = this.getAngle([e.clientX, e.clientY]);
    this.dragCard.style.transform = `translate(${offsetX}px, ${offsetY}px) rotate(${newAngle}rad)`;
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
    const target = this.dropTarget(e);
    if (target || (this.pointerDownTime && Date.now() - this.pointerDownTime < 500))
      this.select(target, this.dragCard.id);
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

  private dragStart = (e: DragEvent) => e.preventDefault();

  private animate = () => {
    if (document.contains(this.view)) this.placeCards();
    this.animFrame = requestAnimationFrame(this.animate);
  };

  get cardSize(): number {
    return this.scaleFactor * 192; // base card size is 192
  }

  private get drops() {
    return this.owner.getDrops();
  }
  private get view() {
    return this.owner.getView();
  }
  private get select() {
    if (this.owner.transient) this.remove();
    return this.owner.select;
  }
  /*private get selected() {
    return this.drops.map(x => this.cards.find(y => y.id === x.selected));
  }*/
  private get deck() {
    return this.owner.getDeck?.();
  }
  private get fanout() {
    return this.owner.transient || !this.deck || this.deck.classList.contains('fanout');
  }
  private get fanRadius() {
    return this.isBottom ? this.view.offsetWidth : this.view.offsetHeight;
  }
  private get isLeft() {
    return this.owner.orientation === 'left';
  }
  private get isBottom() {
    return !this.isLeft;
  }
}

class TouchDragShape {
  // WIP
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
