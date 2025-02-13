import { frag, defined } from 'common';
import { clamp, isEquivalent } from 'common/algo';
import { Janitor } from 'common/event';

export interface CardData {
  imageUrl?: string;
  label: string;
  domId: DomId;
  classList: string[];
  group?: string; // user, advanced, intermediate, etc
}

export type Drop = { el: HTMLElement; selected?: DomId };

export interface HandOfCards {
  remove(): void;
  update(): void;
  resize(): void;
  redraw(): void;
  events: Janitor;
}

export function handOfCards(opts: HandOpts): HandOfCards {
  return new HandOfCardsImpl(opts);
}

type DomId = string; // no selector chars allowed

interface HandOpts {
  getCardData: () => Iterable<CardData>;
  getDrops: () => Drop[];
  getGroup?: () => string;

  select: (drop: HTMLElement | undefined, domId?: DomId) => void;

  onRemove?: () => void;

  viewEl: HTMLElement;
  deckEl?: HTMLElement;
  transient?: boolean; // default false
  orientation?: 'bottom' | 'left'; // default bottom
  opaqueSelectedBackground?: boolean; // default false. when true, selected card background is opaque
  fanCenterToWidthRatio?: number; // default 0.5
}

class HandOfCardsImpl {
  cards: HTMLElement[] = [];
  container: HTMLElement;
  originX: number;
  originY: number;
  killAnimationTimer = 0;
  animFrame = 0;
  scaleFactor = 1;
  groups: Set<string | undefined>;
  group: string | undefined;
  pointerDownTime?: number;
  touchDragShape?: TouchDragShape;
  dragCard: HTMLElement | undefined;
  events: Janitor = new Janitor();
  rect: DOMRect;
  deckRect: DOMRect;
  lastCardData: CardData[];

  constructor(readonly opts: HandOpts) {
    this.container = frag<HTMLElement>('<div class="card-container">');
    this.view.append(this.container);
    if (opts.transient) this.events.addListener(window, 'pointerdown', this.remove);
    this.events.addListener(window, 'resize', () => {
      this.resize();
      this.redraw();
    });
    this.events.addListener(this.view, 'mousemove', this.mouseMove);
    // this.events.addListener(document, 'visibilitychange', () => {}); // transition off, redraw, back on
    this.group = opts.getGroup?.();
    setTimeout(this.layout);
  }

  layout = (): void => {
    this.resize();
    this.update();
  };

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
  };

  update(): void {
    const data = [...this.opts.getCardData()].reverse();
    if (isEquivalent(data, this.lastCardData)) return;
    const fragment = document.createDocumentFragment();
    const cards: HTMLElement[] = [];
    this.lastCardData = data;
    this.groups = new Set(data.map(cd => cd.group));
    if (!this.group || !this.groups.has(this.group)) this.group = this.groups.values().next().value;

    for (const cd of data) {
      const card = this.cards.find(x => x.id === cd.domId) ?? this.createCard(cd);
      const label = card.lastElementChild as HTMLElement;
      const img = card.firstElementChild as HTMLImageElement;
      if (cd.group === this.group) fragment.appendChild(card);
      if (cd.imageUrl !== img.src) img.src = cd.imageUrl ?? '';
      if (cd.label !== label.textContent) label.textContent = cd.label;
      card.className = 'card';
      cd.group ? card.setAttribute('data-group', cd.group) : card.removeAttribute('data-group');
      cd.classList.forEach(c => card.classList.add(c));
      cards.push(card);
      //card.tabIndex = i--;
    }
    for (const removed of this.cards.filter(x => !cards.includes(x))) removed.remove();
    this.cards = cards;
    if (this.opts.transient)
      this.cards.forEach(x => (x.style.transform = `translate(${this.originX}px, ${this.originY}px)`));
    else this.placeCards();
    this.container.appendChild(fragment);
    this.redraw();
  }

  remove = (): void => {
    if (!this.cards.length) return;
    const cards = this.cards.slice();
    this.cards = [];
    this.events.cleanup();
    cards.forEach(x => (x.style.transform = `translate(${this.originX}px, ${this.originY}px)`));
    setTimeout(() => {
      cards.forEach(x => x.remove());
      if (this.container) this.container.remove();
      this.opts.onRemove?.();
    }, 300);
  };

  redraw = (): void => {
    clearTimeout(this.killAnimationTimer);
    this.killAnimationTimer = setTimeout(() => {
      cancelAnimationFrame(this.animFrame);
      this.animFrame = 0;
    }, 300);
    if (this.animFrame === 0) this.animate();
  };

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
    const hovered = this.cards.find(x => x.classList.contains('pull'));
    const hoverIndex = this.visible.findIndex(x => x === hovered);
    const unplaced = this.visible.filter(x => !this.selectedTransform(x));
    for (const [i, card] of unplaced.entries()) {
      if (!this.opts.opaqueSelectedBackground) card.style.backgroundColor = '';
      if (card === this.dragCard) continue;
      if (this.fanned) this.fanTransform(card, hoverIndex);
      else this.deckTransform(card, i);
    }
  }

  private deckTransform(card: HTMLElement, i: number) {
    if (this.opts.transient || !this.deck) return false;
    const to = this.deck;
    const toSide = Math.min(to.offsetWidth, to.offsetHeight);
    const scale = (0.8 * toSide) / this.cardSize;
    const x = to.offsetLeft - card.offsetLeft + (toSide - this.cardSize) / 2 + i;
    const y = to.offsetTop - card.offsetTop + (toSide - this.cardSize) / 2 - i;
    card.style.transform = `translate(${x}px, ${y}px) rotate(-5deg) scale(${scale})`;
    return true;
  }

  private fanTransform(card: HTMLElement, hoverIndex: number) {
    const fanArc = Math.PI / 5;
    const centerCard = -this.cardSize / 2;
    const visibleCards = this.visible.length; // this works for what few cards we have now
    const index = this.visible.indexOf(card);
    const rindex = this.visible.length - index - 0.5;
    const isHovered = hoverIndex === index;
    const isAfterHovered = hoverIndex > -1 && index > hoverIndex;

    let x, y, cardRotation;
    let angle = fanArc * (rindex / visibleCards - 0.5);

    if (this.isLeft) {
      if (isAfterHovered) angle -= Math.PI / 32;

      cardRotation = angle - (isAfterHovered ? Math.PI / 16 : 0);

      const radiusOffset = isAfterHovered ? 0 : isHovered ? this.cardSize / 4 : this.cardSize / 8;

      x = this.originX + (this.fanRadius + radiusOffset) * Math.cos(angle);
      y = this.originY + (this.fanRadius + radiusOffset) * Math.sin(angle);
    } else {
      const hoverFactor = hoverIndex === -1 ? 0 : isAfterHovered ? -1 : 1;
      if (this.touchDragShape) {
        angle += hoverFactor * Math.PI * (Math.abs(index - hoverIndex) * 0.05);
      } else {
        angle += (hoverFactor * Math.PI) / 96;

        cardRotation = angle + (isHovered ? Math.PI / 12 : 0);
      }

      const radiusOffset = centerCard / (isHovered ? 2 : 1);

      x = this.originX + (this.fanRadius + radiusOffset) * Math.sin(angle);
      y = this.originY - (this.fanRadius + radiusOffset) * Math.cos(angle);
    }
    card.style.transform = `translate(${x + centerCard}px, ${y + centerCard}px) rotate(${cardRotation}rad)`;
    //console.log(x, y);
  }

  private selectedTransform(card: HTMLElement) {
    if (this.opts.transient || card === this.dragCard) return false;
    const dindex = this.drops.findIndex(x => x.selected === card.id);
    card.classList.toggle('selected', dindex >= 0);
    if (dindex < 0) return false;
    const to = this.drops[dindex].el;
    const scale = to.offsetHeight / this.cardSize;
    const x = to.offsetLeft + (to.offsetWidth - this.cardSize) / 2;
    const y = to.offsetTop + (to.offsetHeight - this.cardSize) / 2;
    card.style.transform = `translate(${x}px, ${y}px) scale(${scale})`;
    if (!this.opts.opaqueSelectedBackground)
      card.style.backgroundColor = window.getComputedStyle(to).backgroundColor;
    return true;
  }

  private clientToOriginOffset(client: [number, number]): [number, number] {
    // 'origin' is the origin coordinates of the fan arc relative to this.view's [left, top]
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
    let fanned = this.fanned;
    const fanDepth = this.cardSize * 1.5;
    const r = this.deckRect;
    if (r && e.clientX >= r.left && e.clientX <= r.right && e.clientY >= r.top && e.clientY <= r.bottom)
      fanned = true;
    else if (this.isLeft ? e.clientX > this.rect.left + fanDepth : e.clientY < this.rect.bottom - fanDepth)
      fanned = false;
    if (fanned === this.fanned) return;
    if (this.opts.transient && !fanned) this.remove();
    this.deck?.classList.toggle('fanned');
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
    this.pointerDownTime = performance.now();
    if (e.pointerType === 'touch') {
      //this.container.classList.add('no-transition');
      this.touchDragShape = new TouchDragShape(e, this.visible, card, this.rect.width / this.visible.length);
      //this.select(this.drops[0].el, card.id);
      this.dragCard = card;
      //this.dragCard.classList.add('dragging');
      this.view.querySelectorAll('.pull')?.forEach(x => x.classList.remove('pull'));
      this.dragCard.classList.add('pull');
      this.redraw();
      return;
    }
    this.dragCard?.releasePointerCapture(e.pointerId);
    this.dragCard = card;
    this.dragCard.setPointerCapture(e.pointerId);
    this.pointerDrag(e);
    this.redraw();
  };

  private pointerMove = (e: PointerEvent) => {
    if (e.pointerType === 'touch') {
      if (this.touchDragShape?.update(e)) {
        this.view.querySelectorAll('.dragging')?.forEach(x => x.classList.remove('dragging'));
        this.view.querySelectorAll('.pull')?.forEach(x => x.classList.remove('pull'));
        this.dragCard = this.visible[this.touchDragShape.currentIndex];
        this.dragCard.classList.add('pull');
        //this.dragCard.classList.add('dragging');
        //this.select(this.drops[0].el, this.visible[this.touchDragShape.currentIndex].id);
        //this.redraw();
        //return;
      }
    }
    if (!this.pointerDownTime || !this.dragCard) return;
    e.preventDefault();
    this.pointerDrag(e);
    this.dragCard.classList.add('dragging');
    for (const drop of this.drops) drop.el?.classList.remove('drag-over');
    this.dropTarget(e)?.classList.add('drag-over');
    this.redraw();
  };

  private pointerDrag = (e: PointerEvent) => {
    if (!this.dragCard) return;
    const offsetPt = this.clientToViewOffset([e.clientX, e.clientY]);
    const offsetX = offsetPt[0] - this.cardSize / 2;
    const offsetY = offsetPt[1] - this.cardSize / 2;
    const newAngle = this.clientToOriginAngle([e.clientX, e.clientY]) - (this.isLeft ? Math.PI / 2 : 0);
    this.dragCard.style.transform = `translate(${offsetX}px, ${offsetY}px) rotate(${newAngle}rad)`;
  };

  private pointerUp = (e: PointerEvent) => {
    if (e.pointerType === 'touch') {
      //this.container.classList.remove('no-transition');
      //this.view.querySelectorAll('.dragging')?.forEach(x => x.classList.remove('dragging'));
      //this.view.querySelectorAll('.pull')?.forEach(x => x.classList.remove('pull'));
      this.touchDragShape = undefined;
      return;
    }
    for (const drop of this.drops) drop.el?.classList.remove('drag-over');
    this.view.querySelectorAll('.dragging')?.forEach(x => x.classList.remove('dragging'));
    if (!this.dragCard) return;

    this.dragCard.classList.remove('pull');
    this.dragCard.releasePointerCapture(e.pointerId);
    const target = this.dropTarget(e);
    if (target || (this.pointerDownTime && performance.now() - this.pointerDownTime < 500))
      this.select(target, this.dragCard.id);
    this.dragCard = undefined;
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
    if (document.contains(this.view)) {
      this.container.classList.toggle('no-transition', this.noTransition);
      this.placeCards();
    }
    this.animFrame = requestAnimationFrame(this.animate);
  };
  private get noTransition() {
    return Boolean(
      !this.touchDragShape &&
        this.dragCard &&
        this.pointerDownTime &&
        performance.now() - this.pointerDownTime > 300,
    );
  }
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
    return this.opts.viewEl;
  }
  private get deck() {
    return this.opts.deckEl;
  }
  private get visible() {
    return this.cards.filter(card => card.dataset.group === this.group);
  }
  private get fanned() {
    return this.opts.transient || !this.deck || this.deck.classList.contains('fanned');
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
    return this.opts.fanCenterToWidthRatio ?? 0.5;
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
    readonly touchRadius: number,
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
