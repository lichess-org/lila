import { Key } from 'chessground/types';
import { DrawBrush, DrawShape } from 'chessground/draw';
import { pushMap, src, dest } from './util';

export const brushes = new Map<string, DrawBrush>([
  ['green', { key: 'vgn', color: '#15781B', opacity: 0.8, lineWidth: 12 }],
  ['blue', { key: 'vbl', color: '#003088', opacity: 0.8, lineWidth: 12 }],
  ['purple', { key: 'vpu', color: '#68217a', opacity: 0.85, lineWidth: 12 }],
  ['pink', { key: 'vpn', color: '#ee2080', opacity: 0.5, lineWidth: 12 }],
  ['yellow', { key: 'vyl', color: '#ffef00', opacity: 0.6, lineWidth: 12 }],
  ['orange', { key: 'vor', color: '#f6931f', opacity: 0.8, lineWidth: 12 }],
  ['white', { key: 'vwh', color: '#ffffff', opacity: 1.0, lineWidth: 12 }],
  ['brown', { key: 'vgy', color: '#7b3c13', opacity: 0.8, lineWidth: 12 }],
  ['red', { key: 'vrd', color: '#881010', opacity: 0.8, lineWidth: 12 }],
  ['grey', { key: 'vgr', color: '#666666', opacity: 0.8, lineWidth: 12 }],
]);

const LABEL_SIZE = 40; // size of arrow labels in svg user units, 100 is the width of a board square

export function numberedArrows(choices: [string, Uci][], asWhite: boolean, timer?: number): DrawShape[] {
  if (!choices) return [];
  const shapes: DrawShape[] = [];
  const dests = new Map<Key, number | Set<number>>();
  choices.forEach(([, uci]) => {
    shapes.push({ orig: src(uci), dest: dest(uci), brush: `v-grey` });
    if (uci.length > 2) pushMap(dests, dest(uci), moveAngle(uci, asWhite));
  });
  if (timer) {
    shapes.push(timerShape(choices[0][1], labelOffset(choices[0][1], dests, asWhite), timer, 'grey', 0.6));
  }
  choices.forEach(([, uci], i) => {
    shapes.push(labelShape(uci, dests, `${i + 1}`, asWhite));
  });
  return shapes;
}

export function coloredArrows(choices: [string, Uci][], timer?: number): DrawShape[] {
  if (!choices) return [];
  const shapes: DrawShape[] = [];
  choices.forEach(([c, uci]) => {
    shapes.push({ orig: src(uci), dest: dest(uci), brush: `v-${c}` });
  });
  if (timer) shapes.push(timerShape(choices[0][1], [0, 0], timer, brushes.values().next().value.color));
  return shapes;
}

export const timerShape = (uci: Uci, offset: [number, number], duration: number, color: string, alpha = 0.4) => ({
  orig: src(uci),
  brush: 'v-grey',
  customSvg:
    (color !== 'grey' ? `<svg width="100" height="100">` : `<svg viewBox="${offset[0]} ${offset[1]} 100 100">`) +
    `<circle cx="50" cy="50" r="25" fill="transparent" stroke="${color}"
             stroke-width="50" stroke-opacity="${alpha}"  transform="rotate(270,50,50)">

      <animate attributeName="stroke-dasharray" dur="${duration}s" repeatCount="1"
               values="0 ${Math.PI * 50};${Math.PI * 50} ${Math.PI * 50}" />

    </circle></svg>`,
});

function labelShape(uci: Uci, dests: Map<Key, number | Set<number>>, label: string, asWhite: boolean): DrawShape {
  const fontSize = Math.round(LABEL_SIZE * 0.82);
  const r = LABEL_SIZE / 2;
  const strokeW = 3;
  const [x, y] = labelOffset(uci, dests, asWhite).map(o => o + r - 50);
  return {
    orig: src(uci),
    brush: 'v-grey',
    customSvg: `
        <svg viewBox="${x} ${y} 100 100">
          <circle cx="${r}" cy="${r}" r="${r - strokeW}" opacity="0.6" fill="#666666"/>
          <text font-size="${fontSize}" fill="white" font-family="Noto Sans" text-anchor="middle"
                dominant-baseline="middle" x="${r}" y="${r + 3}">
            ${label}
          </text>
          <circle cx="${r}" cy="${r}" r="${r}" opacity="0.9" stroke="white" fill="transparent" stroke-width="${strokeW}"/>
        </svg>`,
  };
}

// we want to place labels at the junction of the destination shaft and arrowhead if possible.
// if there's more than 1 arrow pointing to a square, we shorten them a bit by 125 / 8 units.

// to account for knights, find round(theta * 8 / pi) for every approach angle of incoming
// moves to a destination square and fractions (knights) are rounded to the nearest odd
// number yielding a mod 16 group. each member is considered a slot
//
// if knight arrows arrive at a square with an adjacent slot (+/- 1)%16, we further shorten
// the knight move arrow by the label size value

function squarePos(sq: Key, asWhite: boolean): [number, number] {
  return [
    asWhite ? sq.charCodeAt(0) - 97 : 104 - sq.charCodeAt(0),
    asWhite ? 56 - sq.charCodeAt(1) : sq.charCodeAt(1) - 49,
  ];
}

function moveAngle(uci: Uci, asWhite: boolean, asSlot = true) {
  const [srcx, srcy] = squarePos(src(uci), asWhite);
  const [destx, desty] = squarePos(dest(uci), asWhite);
  const angle = Math.atan2(desty - srcy, destx - srcx) + Math.PI;
  return asSlot ? (Math.round(angle * 8) / Math.PI + 16) % 16 : angle;
}

function destOffset(uci: Uci, asWhite: boolean): [number, number] {
  const sign = asWhite ? 1 : -1;
  const cc = [...uci].map(c => c.charCodeAt(0));
  return uci.length < 4 ? [0, 0] : [sign * (cc[0] - cc[2]) * 100, -sign * (cc[1] - cc[3]) * 100];
}

function destMag(uci: Uci, asWhite: boolean): number {
  return Math.sqrt(destOffset(uci, asWhite).reduce((acc, x) => acc + x * x, 0));
}

function labelOffset(uci: Uci, dests: Map<Key, number | Set<number>>, asWhite: boolean): [number, number] {
  let mag = destMag(uci, asWhite);
  if (mag === 0) return [0, 0];
  const angle = moveAngle(uci, asWhite, false);
  if (dests.has(dest(uci))) {
    mag -= 52;
    const slots = dests.get(dest(uci))!;
    if (slots instanceof Set) {
      mag -= 125 / 8;
      const arc = moveAngle(uci, asWhite);
      if (slots.has((arc + 1) % 16) || slots.has((arc + 15) % 16)) if (arc & 1) mag -= LABEL_SIZE;
    }
  }
  return [Math.cos(angle) * mag, Math.sin(angle) * mag];
}
