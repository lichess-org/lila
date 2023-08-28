import { h } from 'snabbdom';
import { MaybeVNodes, onInsert } from 'common/snabbdom';
import { SetupCtrl } from '../ctrl';
import { fenInput } from './components/fenInput';
import { timePickerAndSliders } from './components/timePickerAndSliders';
import { colorButtons } from './components/colorButtons';
import { ratingView } from './components/ratingView';
import { localBots, type BotInfo } from 'libot';

const botInfos = Object.values(localBots);
let selector: HTMLDivElement;
const cards: HTMLDivElement[] = [];
let userMidX: number;
let userMidY: number;
let startAngle = 0,
  startMag = 0,
  handRotation: number = 0;
let selectedCard: HTMLDivElement | null = null;

export default function localContent(ctrl: SetupCtrl): MaybeVNodes {
  return [
    h('h2', 'Local Play'),
    h('div.setup-content', [
      h('div#bot-selector', {
        key: 'bot-selector',
        hook: onInsert(el => {
          selector = el as HTMLDivElement;
          /*selector.addEventListener('click', () => {
            console.log(selector.offsetWidth, selector.offsetHeight);
          });*/
          botInfos.forEach(bot => createCard(bot));
          setTimeout(animate);
        }),
      }),
      fenInput(ctrl),
      timePickerAndSliders(ctrl, true),
      colorButtons(ctrl),
    ]),
    ratingView(ctrl),
  ];
}

function createCard(bot: BotInfo) {
  const card = document.createElement('div');
  card.classList.add('card');
  const img = document.createElement('img');
  img.src = bot.image;
  card.appendChild(img);
  card.addEventListener('pointerdown', startDrag);
  card.addEventListener('pointermove', duringDrag);
  card.addEventListener('pointerup', endDrag);
  card.addEventListener('mouseenter', mouseEnter);
  card.addEventListener('mouseleave', mouseLeave);
  cards.push(card);
  selector.appendChild(card);
  return card;
}

function placeCards(index: number) {
  const radius = selector.offsetWidth;
  const containerHeight = selector.offsetHeight;

  userMidX = radius / 2;
  userMidY = containerHeight + Math.sqrt(3) * userMidX;

  const beginAngle = -Math.PI / 8;
  const visibleCards = cards.length;
  const hovered = $as<HTMLElement>($('.pull'));
  const hoveredIndex = cards.findIndex(x => x == hovered);
  cards.forEach((card, cardIndex) => {
    index;
    const angleNudge =
      !hovered || cardIndex == hoveredIndex
        ? 0
        : cardIndex < hoveredIndex
        ? 0 //(-Math.PI * 0.25) / visibleCards
        : (Math.PI * 0.5) / visibleCards;
    let angle = beginAngle + angleNudge + handRotation + ((Math.PI / 4) * (cardIndex + 0.5)) / visibleCards;
    const mag = 15 + radius + ($(card).hasClass('pull') ? 40 : 0);
    const x = userMidX + mag * Math.sin(angle) - 64;
    const y = userMidY - mag * Math.cos(angle);
    if (cardIndex === hoveredIndex) angle -= Math.PI / 8;
    card.style.transform = `translate(${x}px, ${y}px) rotate(${angle}rad)`;
  });
}

function clientToOrigin(client: [number, number]): [number, number] {
  const originX = client[0] - selector.offsetLeft - userMidX;
  const originY = selector.offsetTop + userMidY - client[1];
  return [originX, originY];
}

function getAngle(client: [number, number]): number {
  const translated = clientToOrigin(client);
  return Math.atan2(translated[0], translated[1]);
}

function getMag(client: [number, number]): number {
  const userPt = clientToOrigin(client);
  return Math.sqrt(userPt[0] * userPt[0] + userPt[1] * userPt[1]);
}

function mouseEnter(e: MouseEvent) {
  $(e.target as HTMLElement).addClass('pull');
}

function mouseLeave(e: MouseEvent) {
  $(e.target as HTMLElement).removeClass('pull');
}

function startDrag(e: PointerEvent): void {
  e.preventDefault();

  startAngle = getAngle([e.clientX, e.clientY]);
  startMag = getMag([e.clientX, e.clientY]);

  selectedCard = e.currentTarget as HTMLDivElement;

  selectedCard.setPointerCapture(e.pointerId);
}

function duringDrag(e: PointerEvent): void {
  e.preventDefault();
  if (!selectedCard) return;

  const newAngle = getAngle([e.clientX, e.clientY]);

  handRotation = newAngle - startAngle;
  placeCards(0);
}

function endDrag(e: PointerEvent): void {
  if (selectedCard) {
    selectedCard.releasePointerCapture(e.pointerId);
  }
  selectedCard = null;
}

function animate() {
  requestAnimationFrame(animate);
  placeCards(handRotation);
}

/*function botInfo(ctrl: SetupCtrl, bot: BotInfo) {
  ctrl;
  return h('div', [h('img', { attrs: { src: bot.image } }), h('div', [h('h2', bot.name), h('p', bot.description)])]);
}*/
