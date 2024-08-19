import { clamp, isEquivalent, frag } from 'common';
import { type EventJanitor, eventJanitor } from 'common/event';

export interface CardData {
  imageUrl?: string;
  label: string;
  domId: DomId;
  classList: string[];
}

export type Drop = { el: HTMLElement; selected?: DomId };

export interface HandOfCards {
  remove(): void;
  update(): void;
  resize(): void;
  redraw(): void;
}

export function handOfCards(opts: HandOpts): HandOfCards {
  return new HandOfCardsImpl(opts);
}

type DomId = string; // no selector chars allowed

interface HandOpts {
  getCardData: () => Iterable<CardData>;
  getDrops: () => Drop[];
  select: (drop: HTMLElement | undefined, domId?: DomId) => void;
  onRemove?: () => void;

  view: HTMLElement;
  deck?: HTMLElement;
  autoResize?: boolean; // default false
  transient?: boolean; // default false
  orientation?: 'bottom' | 'left'; // default bottom
  opaque?: boolean; // default false. when selected, card is fully opaque over target background
  center?: number; // default 0.5
}

class HandOfCardsImpl {
  cards: HTMLElement[] = [];
  container: HTMLElement;
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
  deckRect: DOMRect;
  lastCardData: CardData[];

  constructor(readonly opts: HandOpts) {
    this.container = frag<HTMLElement>(`<div class="card-container">`);
    this.view.append(this.container);
    if (opts.transient) this.events.addListener(window, 'pointerdown', this.clickToDismiss);
    if (opts.autoResize) this.events.addListener(window, 'resize', this.resize);
    this.events.addListener(this.view, 'mousemove', this.mouseMove);
    this.update();
    setTimeout(this.resize);
  }

  resize: () => void = () => {
    const r = this.view.getBoundingClientRect();
    if (this.deck) this.deckRect = this.deck.getBoundingClientRect();
    if (this.rect && r.width === this.rect.width && r.height === this.rect.height) return;
    this.rect = r;
    this.scaleFactor = 0.8 + 0.3 * clamp((r.width - 720) / 360, { min: 0, max: 1 });
    this.view.style.setProperty('---scale-factor', String(this.scaleFactor));
    const h2 = this.cardSize - (1 - Math.sqrt(3 / 4)) * this.fanRadius;
    this.originX = this.isLeft ? -Math.sqrt(3 / 4) * this.fanRadius : r.width * this.center;
    this.originY = this.isLeft
      ? (r.height - h2) * this.center
      : r.height + Math.sqrt(3 / 4) * this.fanRadius - h2;
    this.redraw();
  };

  update(): void {
    const data = [...this.opts.getCardData()].reverse();
    if (isEquivalent(data, this.lastCardData)) return;
    const fragment = document.createDocumentFragment();
    this.lastCardData = data;
    const cards: HTMLElement[] = [];
    //let i = data.length;
    for (const cd of data) {
      const card = this.cards.find(x => x.id === cd.domId) ?? this.createCard(cd);
      fragment.appendChild(card);
      const label = card.lastElementChild as HTMLElement;
      if (cd.label !== label.textContent) label.textContent = cd.label;
      const img = card.firstElementChild as HTMLImageElement;
      if (cd.imageUrl !== img.src) img.src = cd.imageUrl ?? '';
      //card.tabIndex = i--;
      card.className = 'card';
      cd.classList.forEach(c => card.classList.add(c));
      cards.push(card);
    }
    for (const removed of this.cards.filter(x => !cards.includes(x))) removed.remove();
    this.container.appendChild(fragment);
    this.cards = cards;
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
      if (this.container) this.container.remove();
      this.opts.onRemove?.();
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
    const card = frag<HTMLElement>(`<div id="${c.domId}" class="card">
        <img src="${c.imageUrl}">
        <label>${c.label}</label>
      </div>`);
    c.classList.forEach(x => card.classList.add(x));
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
    const unplaced = this.cards.filter(x => !this.selectedTransform(x));
    for (const [i, card] of unplaced.entries()) {
      if (!this.opts.opaque) card.style.backgroundColor = '';
      if (this.fanout) this.fanoutTransform(card, hoverIndex);
      else if (this.opts.transient) card.style.transform = `translate(${this.originX}px, ${this.originY}px)`;
      else this.deckTransform(card, i);
    }
  }

  private deckTransform(card: HTMLElement, i: number) {
    if (this.opts.transient || !this.deck) return false;
    const to = this.deck;
    const toSide = Math.min(to.offsetWidth, to.offsetHeight);
    const cardSide = Math.min(card.offsetWidth, card.offsetHeight);
    const scale = (0.8 * toSide) / cardSide;
    const x = to.offsetLeft - card.offsetLeft + (toSide - cardSide) / 2 + i;
    const y = to.offsetTop - card.offsetTop + (toSide - cardSide) / 2 - i;
    card.style.transform = `translate(${x}px, ${y}px) rotate(-5deg) scale(${scale})`;
    return true;
  }

  private fanoutTransform(card: HTMLElement, hoverIndex: number) {
    // could make half of this function disappear with better symmetry
    const fanArc = Math.PI / 5;
    const centerCard = -this.cardSize / 2;
    const index = this.cards.indexOf(card);
    const visibleCards = this.cards.length; // for the few cards we have now
    let x, y, cardRotation;
    if (this.isLeft) {
      const pull = hoverIndex === -1 || index <= hoverIndex ? 0 : -Math.PI / visibleCards / 2;
      const angle = pull + fanArc * ((this.cards.length - index - 0.5) / visibleCards - 0.5);
      const mag = this.fanRadius + (hoverIndex > -1 && index > hoverIndex ? centerCard : 0);
      x = this.originX + mag * Math.cos(angle);
      y = this.originY + mag * Math.sin(angle);
      cardRotation = angle + (hoverIndex === -1 ? 0 : index <= hoverIndex ? 0 : -Math.PI / 6);
    } else {
      const isPulled = card.classList.contains('pull');
      const pull =
        hoverIndex === -1
          ? 0
          : ((index > hoverIndex ? -1 : 1) * (Math.PI * this.cardSize)) / (this.fanRadius * visibleCards);
      const angle = pull + fanArc * ((this.cards.length - index - 0.5) / visibleCards - 0.5);
      const mag = this.fanRadius + centerCard / (isPulled ? 2 : 1);
      x = this.originX + mag * Math.sin(angle);
      y = this.originY - mag * Math.cos(angle);
      cardRotation = angle + (isPulled ? Math.PI / 12 : 0);
    }

    card.style.transform = `translate(${x + centerCard}px, ${y + centerCard}px) rotate(${cardRotation}rad)`;
  }

  private selectedTransform(card: HTMLElement) {
    if (this.opts.transient || card === this.dragCard) return false;
    const dindex = this.drops.findIndex(x => x.selected === card.id);
    card.classList.toggle('selected', dindex >= 0);
    if (dindex < 0) return false;
    const to = this.drops[dindex].el;
    const scale = to.offsetHeight / card.offsetHeight;
    const x = to.offsetLeft - card.offsetLeft + (to.offsetWidth - card.offsetWidth) / 2;
    const y = to.offsetTop - card.offsetTop + (to.offsetHeight - card.offsetHeight) / 2;
    card.style.transform = `translate(${x}px, ${y}px) scale(${scale})`;
    if (!this.opts.opaque) card.style.backgroundColor = window.getComputedStyle(to).backgroundColor;
    return true;
  }

  private clientToOriginOffset(client: [number, number]): [number, number] {
    // 'origin' is the origin coordinates of the fanout circle relative to this.view's [left, top]
    const ooX = client[0] - (this.rect.left + window.scrollX) - this.originX;
    const ooY = this.rect.top + window.scrollY + this.originY - client[1];
    return [ooX, ooY];
  }

  private clientToViewOffset(client: [number, number]): [number, number] {
    const elX = client[0] - (this.rect.left + window.scrollX);
    const elY = client[1] - (this.rect.top + window.scrollY);
    return [elX, elY];
  }

  private clientToOriginAngle(client: [number, number]): number {
    const translated = this.clientToOriginOffset(client);
    return Math.atan2(translated[0], translated[1]);
  }

  private mouseMove = (e: MouseEvent) => {
    if (this.dragCard || !this.rect || (!this.deck && !this.opts.transient)) return;
    let fanout = this.fanout;
    const fanDepth = this.cardSize * 1.5;
    const r = this.deckRect;
    if (r && e.clientX >= r.left && e.clientX <= r.right && e.clientY >= r.top && e.clientY <= r.bottom)
      fanout = true;
    else if (this.isLeft ? e.clientX > this.rect.left + fanDepth : e.clientY < this.rect.bottom - fanDepth)
      fanout = false;
    if (fanout === this.fanout) return;
    if (this.opts.transient && !fanout) this.remove();
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
    e.stopPropagation();
    this.pointerDownTime = Date.now();
    if (e.pointerType === 'touch') {
      this.view.classList.add('no-transition');
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

    const offsetPt = this.clientToViewOffset([e.clientX, e.clientY]);
    const offsetX = offsetPt[0] - this.cardSize / 2;
    const offsetY = offsetPt[1] - this.cardSize / 2;
    const newAngle = this.clientToOriginAngle([e.clientX, e.clientY]);
    this.dragCard.style.transform = `translate(${offsetX}px, ${offsetY}px) rotate(${newAngle}rad)`;
    for (const drop of this.drops) drop.el?.classList.remove('drag-over');
    this.dropTarget(e)?.classList.add('drag-over');
    this.redraw();
  };

  private pointerUp = (e: PointerEvent) => {
    if (e.pointerType === 'touch') {
      this.view.classList.remove('no-transition');
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

  private clickToDismiss = () => {
    this.remove();
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

  private select(drop: HTMLElement | undefined, domId?: DomId) {
    if (this.opts.transient) this.remove();
    return this.opts.select(drop, domId);
  }
  private get cardSize(): number {
    return this.scaleFactor * 192;
  }
  private get drops() {
    return this.opts.getDrops();
  }
  private get view() {
    return this.opts.view;
  }
  private get deck() {
    return this.opts.deck;
  }
  private get fanout() {
    return this.opts.transient || !this.deck || this.deck.classList.contains('fanout');
  }
  private get fanRadius() {
    return this.isBottom ? this.view.offsetWidth : this.view.offsetHeight;
  }
  private get isLeft() {
    return this.opts.orientation === 'left';
  }
  private get isBottom() {
    return !this.isLeft;
  }
  private get center() {
    return this.opts.center ?? 0.5;
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
