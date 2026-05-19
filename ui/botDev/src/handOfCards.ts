import { frag } from 'lib';
import { clamp, isEquivalent } from 'lib/algo';
import { Janitor } from 'lib/event';

export interface CardData {
  imageUrl?: string;
  label: string;
  domId: DomId;
  classList: string[];
  group?: string; // advanced, intermediate, beginner
}

export type Drop = { el: HTMLElement; selected?: DomId };

export interface HandOfCards {
  updateCards(): void;
  remove(): void;
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
  peek?: number; // number of card heights the fan extends above the bottom of the view, default 1
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
  drag?: {
    when: number;
    card: HTMLElement;
    transform?: { x: number; y: number; angle: number };
    shape?: TouchDragShape;
  };
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
    this.updateCards();
  };

  resize: () => void = () => {
    const r = this.view.getBoundingClientRect();
    if (this.deck) this.deckRect = this.deck.getBoundingClientRect();
    if (this.rect && r.width === this.rect.width && r.height === this.rect.height) return;
    this.rect = r;
    this.scaleFactor = 0.6 + 0.5 * clamp((r.width - 360) / 360, { min: 0, max: 1 });
    this.view.style.setProperty('---scale-factor', String(this.scaleFactor));
    const h2 = this.cardSize * this.peek - (1 - Math.sqrt(3 / 4)) * this.fanRadius;
    this.originX = this.isLeft ? -Math.sqrt(3 / 4) * this.fanRadius : r.width * this.center;
    this.originY = this.isLeft
      ? (r.height - h2) * this.center
      : r.height + Math.sqrt(3 / 4) * this.fanRadius - h2;
  };

  updateCards(): void {
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
    if (!this.rect || this.opts.transient)
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
    if (this.animFrame) return;
    const animate = () => {
      this.animFrame = requestAnimationFrame(() => {
        this.placeCards();
        animate();
      });
    };
    animate();
  };

  createCard(c: CardData) {
    const card = frag<HTMLElement>($html`
      <div id="${c.domId}" class="card">
        <img src="${c.imageUrl}" alt="${c.label}">
        <label>${c.label}</label>
      </div>`);
    c.classList.forEach(x => card.classList.add(x));
    this.events.addListener(card, 'pointerdown', this.pointerDownCard);
    this.events.addListener(card, 'pointermove', this.pointerMoveCard);
    this.events.addListener(card, 'pointerup', this.pointerUpCard);
    this.events.addListener(card, 'mouseenter', this.mouseEnterCard);
    this.events.addListener(card, 'mouseleave', this.mouseLeaveCard);
    this.events.addListener(card, 'dragstart', this.dragStart);
    return card;
  }

  placeCards() {
    const hovered = this.cards.find(x => x.classList.contains('hovered'));
    const hoverIndex = this.visible.findIndex(x => x === hovered);
    const unplaced = this.visible.filter(x => !this.selectedTransform(x));
    for (const [i, card] of unplaced.entries()) {
      if (!this.opts.opaqueSelectedBackground) card.style.backgroundColor = '';
      if (card === this.drag?.card && this.drag.transform)
        card.style.transform = `translate(${this.drag.transform.x}px, ${this.drag.transform.y}px) rotate(${this.drag.transform.angle}rad)`;
      else if (this.fanned) this.fanTransform(card, hoverIndex);
      else this.deckTransform(card, i);
    }
  }

  deckTransform(card: HTMLElement, n: number) {
    if (this.opts.transient || !this.deck) return false;
    const to = this.deck;
    const toSide = Math.min(to.offsetWidth, to.offsetHeight);
    const scale = (0.8 * toSide) / this.cardSize;
    const x = to.offsetLeft - card.offsetLeft + (toSide - this.cardSize) / 2 + n;
    const y = to.offsetTop - card.offsetTop + (toSide - this.cardSize) / 2 - n;
    card.style.transform = `translate(${x}px, ${y}px) rotate(-5deg) scale(${scale})`;
    return true;
  }

  fanTransform(card: HTMLElement, hoverIndex: number) {
    // all angles are measured as clockwise radial offsets from the fan's vertical midline
    const index = this.visible.indexOf(card);
    const visibleArc = Math.PI / 5;
    const centerOffset = -this.cardSize / 2;
    const visibleCards = this.visible.length; // for what few cards we have now
    const isHovered = hoverIndex === index;
    const isAfterHovered = hoverIndex === -1 || index <= hoverIndex;

    let x, y, cardRotation;
    let angle = visibleArc * (0.46 - index / visibleCards);

    if (this.isLeft) {
      if (!isAfterHovered) angle -= Math.PI / 32;

      cardRotation = angle - (isAfterHovered ? 0 : Math.PI / 16);

      let mag = this.fanRadius;
      if (isAfterHovered) mag += isHovered ? this.cardSize / 4 : this.cardSize / 8;

      x = this.originX + mag * Math.cos(angle);
      y = this.originY + mag * Math.sin(angle);
    } else {
      const direction = hoverIndex === -1 ? 0 : isAfterHovered ? 1 : -1;

      if (this.drag?.shape) {
        angle += (direction * Math.PI * Math.abs(index - hoverIndex)) / 20;
        cardRotation = angle;
      } else {
        angle += (direction * Math.PI) / 96;

        cardRotation = angle + (isHovered ? Math.PI / 12 : 0);
      }
      const radiusOffset = centerOffset / (isHovered ? 2 : 1);

      x = this.originX + (this.fanRadius + radiusOffset) * Math.sin(angle);
      y = this.originY - (this.fanRadius + radiusOffset) * Math.cos(angle);
    }
    this.visible[index].style.transform =
      `translate(${x + centerOffset}px, ${y + centerOffset}px) rotate(${cardRotation}rad)`;
  }

  selectedTransform(card: HTMLElement) {
    if (this.opts.transient || card === this.drag?.card) return false;
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

  clientToAzimuth(client: [number, number]): number {
    const translated = this.clientToOrigin(client);
    return Math.atan2(translated[0], translated[1]);
  }

  // coords relative to the fan arc's origin
  clientToOrigin(client: [number, number]): [number, number] {
    const ooX = client[0] - this.rect.left /*+ window.scrollX*/ - this.originX;
    const ooY = this.rect.top /*+ window.scrollY*/ + this.originY - client[1];
    return [ooX, ooY];
  }

  // coords relative to top left of this.view
  clientToView(client: [number, number]): [number, number] {
    const elX = client[0] - this.rect.left; /*+ window.scrollX*/
    const elY = client[1] - this.rect.top; /*+ window.scrollY*/
    return [elX, elY];
  }

  mouseMove = (e: MouseEvent) => {
    if (this.drag || !this.rect || (!this.deck && !this.opts.transient)) return;
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

  mouseEnterCard = (e: MouseEvent) => {
    (e.target as HTMLElement)?.classList.add('hovered');
    this.redraw();
  };

  mouseLeaveCard = (e: MouseEvent) => {
    (e.target as HTMLElement)?.classList.remove('hovered');
    this.redraw();
  };

  pointerDownCard = (e: PointerEvent) => {
    if (!(e.currentTarget instanceof HTMLElement)) return;
    this.drag?.card.releasePointerCapture(e.pointerId);
    this.drag = { when: performance.now(), card: e.currentTarget };
    this.drag.card.setPointerCapture(e.pointerId);
    if (e.pointerType === 'touch') {
      this.drag.shape = new TouchDragShape(e, this.visible, this.drag.card, this.rect, this.drops);
      this.drag.card.classList.add('hovered', 'dragging');
      this.touchDragCard(e);
      this.redraw();
    }
    e.preventDefault();
    e.stopPropagation();
  };

  pointerMoveCard = (e: PointerEvent) => {
    if (!this.drag) return;
    if (e.pointerType === 'touch') this.touchDragCard(e);
    else this.mouseDragCard(e);
    this.drops.forEach(drop => drop.el?.classList.remove('drag-over'));
    this.dropTarget(e)?.classList.add('drag-over');
    this.redraw();
    e.preventDefault();
  };

  touchDragCard = (e: PointerEvent) => {
    if (!this.drag?.shape) return;
    if (this.drag.shape.update(e) && e.clientY > this.rect.bottom - this.cardSize) {
      this.cards.forEach(x => x.classList.remove('dragging', 'hovered'));
      this.drag.card = this.visible[this.drag.shape.index];
      this.drag.card.classList.add('hovered', 'dragging');
    }
    const [viewX, viewY] = this.clientToView([e.clientX, e.clientY]);
    this.drag.transform = {
      x: viewX * (1 - this.cardSize / this.rect.width),
      y: viewY - this.cardSize,
      angle: -this.clientToAzimuth([e.clientX, e.clientY]) / 2 - (this.isLeft ? Math.PI / 2 : 0),
    };
  };

  mouseDragCard = (e: PointerEvent) => {
    if (!this.drag?.card) return;
    this.drag.card.classList.add('dragging');
    const offsetPt = this.clientToView([e.clientX, e.clientY]);
    const offsetX = offsetPt[0] - this.cardSize / 2;
    const offsetY = offsetPt[1] - this.cardSize / 2;
    const newAngle = this.clientToAzimuth([e.clientX, e.clientY]) - (this.isLeft ? Math.PI / 2 : 0);
    this.drag.transform = { x: offsetX, y: offsetY, angle: newAngle };
  };

  pointerUpCard = (e: PointerEvent) => {
    this.drops.forEach(drop => drop.el?.classList.remove('drag-over'));
    this.cards.forEach(x => x.classList.remove('dragging'));
    if (!this.drag) return;

    //this.drag.card.classList.remove('hovered');
    this.drag.card.releasePointerCapture(e.pointerId);

    if (e.pointerType === 'touch') this.touchUpCard(e);
    else this.mouseUpCard(e);
    this.drag = undefined;
    this.redraw();
  };

  mouseUpCard = (e: PointerEvent) => {
    if (!this.drag) return;
    const target = this.dropTarget(e);
    if (target || performance.now() - this.drag.when < 300) this.select(target, this.drag.card.id);
  };

  touchUpCard = (e: PointerEvent) => {
    if (!this.drag?.shape) return;
    const outcome = this.drag.shape.outcome(e) || this.dropTarget(e);
    if (outcome === 'next-group') {
      const groups = [...this.groups];
      const index = groups.indexOf(this.group);
      this.group = groups[(index + 1) % groups.length];
      this.updateCards();
    } else if (outcome) {
      this.select(outcome, this.drag.card.id);
    }
    this.cards.forEach(x => x.classList.remove('hovered'));
  };

  dropTarget(e: PointerEvent): HTMLElement | undefined {
    for (const drop of this.drops) {
      const r = drop.el.getBoundingClientRect();
      if (e.clientX >= r.left && e.clientX <= r.right && e.clientY >= r.top && e.clientY <= r.bottom)
        return drop.el;
    }
    return undefined;
  }
  dragStart = (e: DragEvent) => e.preventDefault();

  select(drop: HTMLElement | undefined, domId?: DomId) {
    if (this.opts.transient) this.remove();
    return this.opts.select(drop, domId);
  }
  get cardSize(): number {
    return this.scaleFactor * 192;
  }
  get peek(): number {
    return this.opts.peek ?? 1;
  }
  get drops() {
    return this.opts.getDrops();
  }
  get view() {
    return this.opts.viewEl;
  }
  get deck() {
    return this.opts.deckEl;
  }
  get visible() {
    return this.cards.filter(card => card.dataset.group === this.group);
  }
  get fanned() {
    return this.opts.transient || !this.deck || this.deck.classList.contains('fanned');
  }
  get fanRadius() {
    return this.isBottom ? this.view.offsetWidth : this.view.offsetHeight;
  }
  get isLeft() {
    return this.opts.orientation === 'left';
  }
  get isBottom() {
    return !this.isLeft;
  }
  get center() {
    return this.opts.fanCenterToWidthRatio ?? 0.5;
  }
}

type TouchPoint = {
  x: number;
  y: number;
};

type DragFrame = {
  at: TouchPoint;
  when: number;
};

const TOUCH_FRAMES = 10;
class TouchDragShape {
  start: DragFrame;
  recent: DragFrame[];
  index: number;

  constructor(
    e: PointerEvent,
    readonly cards: HTMLElement[],
    readonly startCard: HTMLElement,
    readonly box: DOMRect,
    readonly drops: Drop[],
  ) {
    this.start = { when: performance.now(), at: { x: e.clientX, y: e.clientY } };
    this.recent = [this.start, this.start];

    this.index = this.cards.indexOf(startCard);
  }
  get last(): DragFrame {
    return this.recent[this.recent.length - 1];
  }

  update(e: PointerEvent): boolean {
    if (performance.now() < this.last.when + 16) return false;
    while (this.recent.length >= TOUCH_FRAMES) this.recent.shift();
    this.recent.push({ when: performance.now(), at: { x: e.clientX, y: e.clientY } });

    const currentIndex = clamp(
      Math.round((this.box.right - this.last.at.x) / (this.box.width / this.cards.length)),
      {
        min: 0,
        max: this.cards.length - 1,
      },
    );
    const lastIndex = this.index;
    this.index = currentIndex;
    return (
      this.momentum.dir === 'lateral' &&
      lastIndex !== currentIndex &&
      this.drops[0].selected !== this.cards[currentIndex].id
    );
  }

  get momentum(): { speed: number; dir: 'towards-drop' | 'next-group' | 'lateral' | undefined } {
    let towardsDrop = 0,
      nextGroup = 0,
      lateral = 0,
      total = 0;
    const r = this.recent;

    for (let i = 1; i < r.length; i++) {
      const dx = r[i].at.x - r[i - 1].at.x;
      const dy = r[i].at.y - r[i - 1].at.y;
      total++;
      Math.abs(dx) > Math.abs(dy) ? lateral++ : dy < 0 ? towardsDrop++ : nextGroup++;
    }

    const dir =
      (towardsDrop > total / 2 && 'towards-drop') ||
      (nextGroup > total / 2 && 'next-group') ||
      (lateral > total / 2 && towardsDrop < 2 && nextGroup < 2 && 'lateral') ||
      undefined;

    const speed =
      r.length > 1
        ? Math.hypot(this.last.at.x - r[0].at.x, this.last.at.y - r[0].at.y) / (this.last.when - r[0].when)
        : 0;

    return { speed, dir };
  }

  outcome(_: PointerEvent): HTMLElement | 'next-group' | undefined {
    const momentum = this.momentum;
    if (momentum.speed < 0.15) return undefined;
    if (momentum.dir === 'towards-drop') return this.drops[0].el;
    if (momentum.dir === 'next-group' && this.last.at.y > this.start.at.y) return 'next-group';
    return undefined;
  }
}
