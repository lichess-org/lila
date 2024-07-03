//import { isTouchDevice } from 'common/device';

const BASE_CARD_SIZE = 192; // at ---scale-factor: 1

export interface CardData {
  imageUrl: string;
  label: string;
  domId: string;
}

export interface HandOwner {
  view: () => HTMLElement;
  drops: () => { el: HTMLElement; selected?: string }[];
  cardData: () => Iterable<CardData>;
  select: (drop: HTMLElement, domId?: string) => void;
  deck?: () => HTMLElement;
  autoResize?: boolean;
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
  pointerDownTime?: number;
  rect: DOMRect;
  dragCard: HTMLElement | null = null;

  constructor(readonly owner: HandOwner) {
    for (const c of owner.cardData()) this.cards.push(this.createCard(c));
    this.cards.reverse().forEach(card => this.view.appendChild(card));
    //this.deck?.addEventListener('mouseenter', this.mouseEnterDeck);
    this.view.addEventListener('mousemove', this.mouseMove);
    if (owner.autoResize) window.addEventListener('resize', this.resize);
  }

  get drops() {
    return this.owner.drops();
  }
  get view() {
    return this.owner.view();
  }
  get select() {
    return this.owner.select;
  }
  get selected() {
    return this.drops.map(x => this.card(x.selected));
  }
  get deck() {
    return this.owner.deck?.();
  }
  get fanout() {
    return !this.deck || this.deck.classList.contains('fanout');
  }
  get fanRadius() {
    return this.view.offsetWidth;
  }

  card(id: string | undefined) {
    if (id?.startsWith('#')) id = id.slice(1);
    return this.cards.find(c => c.id === id);
  }

  createCard(c: CardData) {
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

  resize = () => {
    const newRect = this.view.getBoundingClientRect();
    if (this.rect && newRect.width === this.rect.width && newRect.height === this.rect.height) return;
    this.scaleFactor = parseFloat(
      window.getComputedStyle(document.documentElement).getPropertyValue('---scale-factor'),
    );
    if (isNaN(this.scaleFactor)) this.scaleFactor = 1;
    const h2 = BASE_CARD_SIZE * this.scaleFactor - (1 - Math.sqrt(3 / 4)) * this.fanRadius;
    this.rect = newRect;
    this.userMidX = this.view.offsetWidth / 2;
    this.userMidY = this.view.offsetHeight + Math.sqrt(3 / 4) * this.fanRadius - h2;
    this.redraw();
  };

  placeCards() {
    const visibleCards = this.cards.length; //Math.min(this.view.offsetWidth / 50, this.cards.length);
    const hovered = $as<HTMLElement>($('.card.pull'));
    const hoveredIndex = this.cards.findIndex(x => x == hovered);
    const deck = this.cards.filter(x => x !== this.dragCard);
    const selected = deck.filter(x => this.selectedTransform(x));
    if (!this.fanout)
      for (const [i, card] of deck.filter(x => !selected.includes(x)).entries()) {
        card.style.backgroundColor = '';
        this.deckTransform(card, deck.length - selected.length - i);
      }
    else
      for (const [i, card] of this.cards.entries()) {
        if (this.selected.includes(card) || card === this.dragCard) continue;
        card.style.backgroundColor = '';
        const pull = !hovered || i <= hoveredIndex ? 0 : (-(Math.PI / 2) * this.scaleFactor) / visibleCards;
        const fanout = ((Math.PI / 4) * (this.cards.length - i - 0.5)) / visibleCards;
        this.fanoutTransform(card, -Math.PI / 8 + pull + this.dragAngle + fanout);
      }
  }

  deckTransform(card: HTMLElement, i: number) {
    const dindex = this.drops.findIndex(x => x.selected?.slice(1) === card.id);
    if (dindex >= 0 || !this.deck) return false;
    const to = this.deck;
    const x = to.offsetLeft - card.offsetLeft + (to.offsetWidth - card.offsetWidth) / 2 + i;
    const y = to.offsetTop - card.offsetTop + (to.offsetHeight - card.offsetHeight) / 2 + i;
    card.style.transform = `translate(${x}px, ${y}px) rotate(-5deg)`;
    return true;
  }

  fanoutTransform(card: HTMLElement, angle: number) {
    const hovered = card.classList.contains('pull');
    const mag =
      15 + this.view.offsetWidth + (hovered ? 40 * this.scaleFactor + this.dragMag - this.startMag : 0);
    const x = this.userMidX + mag * Math.sin(angle) - (BASE_CARD_SIZE * this.scaleFactor) / 2;
    const y = this.userMidY - mag * Math.cos(angle);
    if (hovered) angle += Math.PI / 12;
    card.style.transform = `translate(${x}px, ${y}px) rotate(${angle}rad)`;
  }

  selectedTransform(card: HTMLElement) {
    const dindex = this.drops.findIndex(x => x.selected?.slice(1) === card.id);
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

  clientToOrigin(client: [number, number]): [number, number] {
    const originX = client[0] - (this.rect.left + window.scrollX) - this.userMidX;
    const originY = this.rect.top + window.scrollY + this.userMidY - client[1];
    return [originX, originY];
  }

  clientToView(client: [number, number]): [number, number] {
    const viewX = client[0] - (this.rect.left + window.scrollX);
    const viewY = client[1] - (this.rect.top + window.scrollY);
    return [viewX, viewY];
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

  mouseMove = (e: MouseEvent) => {
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

  mouseEnterCard = (e: MouseEvent) => {
    $(e.target as HTMLElement).addClass('pull');
    this.redraw();
  };

  mouseLeaveCard = (e: MouseEvent) => {
    $(e.target as HTMLElement).removeClass('pull');
    this.redraw();
  };

  pointerDown = (e: PointerEvent) => {
    this.pointerDownTime = Date.now();
    this.dragCard?.releasePointerCapture(e.pointerId);
    this.dragCard = e.currentTarget as HTMLElement;
    this.dragCard.setPointerCapture(e.pointerId);
    this.redraw();
  };

  pointerMove = (e: PointerEvent) => {
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

  pointerUp = (e: PointerEvent) => {
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

  dropTarget(e: PointerEvent): HTMLElement | undefined {
    for (const drop of this.drops) {
      const r = drop.el.getBoundingClientRect();
      if (e.clientX >= r.left && e.clientX <= r.right && e.clientY >= r.top && e.clientY <= r.bottom)
        return drop.el;
    }
    return undefined;
  }

  animate = () => {
    if (document.contains(this.view)) this.placeCards();
    this.frame = requestAnimationFrame(this.animate);
  };

  redraw(andKeepAnimatingFor = 300) {
    if (this.frame === 0) this.animate();
    clearTimeout(this.killAnimation);
    this.killAnimation = setTimeout(() => {
      cancelAnimationFrame(this.frame);
      this.frame = 0;
    }, andKeepAnimatingFor);
  }
}

/* v0.0.1
  startDrag(e: PointerEvent): void {
    this.startAngle = this.getAngle([e.clientX, e.clientY]) - this.dragAngle;
    this.dragMag = this.startMag = this.getMag([e.clientX, e.clientY]);
    this.view.classList.add('dragging');
    this.dragCard = e.currentTarget as HTMLElement;
    if (isTouchDevice()) {
      $('.card').removeClass('pull');
      this.dragCard.classList.add('pull');
    }
    this.dragCard.setPointerCapture(e.pointerId);
    this.dragCard.style.transition = 'none';
    this.resetIdleTimer();
  }

  duringDrag(e: PointerEvent): void {
    e.preventDefault();
    if (!this.dragCard) return;
    for (const drop of this.drops) drop.el?.classList.remove('drag-over');
    this.dropTarget(e)?.classList.add('drag-over');
    const newAngle = this.getAngle([e.clientX, e.clientY]);

    this.dragMag = this.getMag([e.clientX, e.clientY]);
    this.dragAngle = newAngle - this.startAngle;
    this.placeCards();
    this.resetIdleTimer();
  }

  endDrag(e: PointerEvent): void {
    for (const drop of this.drops) drop.el?.classList.remove('drag-over');
    $('.card').removeClass('pull');
    this.view.classList.remove('dragging');
    if (this.dragCard) {
      this.dragCard.style.transition = '';
      this.dragCard.releasePointerCapture(e.pointerId);
      const target = this.dropTarget(e);
      if (target) this.select(target, this.dragCard.id);
    }
    //this.startMag = this.dragMag = this.startAngle = this.dragAngle = 0;
    this.startMag = this.dragMag = this.startAngle = 0;
    this.dragCard = null;
    this.placeCards();
    this.resetIdleTimer();
  }
  */
