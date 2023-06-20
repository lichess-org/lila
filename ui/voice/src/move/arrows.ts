import { Key } from 'chessground/types';
import { DrawBrush, DrawShape } from 'chessground/draw';
import { src, dest } from 'chess';
import { pushMap, type SparseSet } from '../util';

export const brushes = new Map<string, DrawBrush>([
  ['green', { key: 'vgn', color: '#15781B', opacity: 0.8, lineWidth: 12 }],
  ['blue', { key: 'vbl', color: '#003088', opacity: 0.8, lineWidth: 12 }],
  ['purple', { key: 'vpu', color: '#68217a', opacity: 0.85, lineWidth: 12 }],
  ['pink', { key: 'vpn', color: '#ee2080', opacity: 0.5, lineWidth: 12 }],
  ['yellow', { key: 'vyl', color: '#ffef00', opacity: 0.6, lineWidth: 12 }],
  ['orange', { key: 'vor', color: '#f6931f', opacity: 0.8, lineWidth: 12 }],
  ['red', { key: 'vrd', color: '#881010', opacity: 0.8, lineWidth: 12 }],
  ['brown', { key: 'vgy', color: '#7b3c13', opacity: 0.8, lineWidth: 12 }],
  ['grey', { key: 'vgr', color: '#666666', opacity: 0.8, lineWidth: 12 }],
  ['white', { key: 'vwh', color: '#ffffff', opacity: 1.0, lineWidth: 15 }],
]);

const LABEL_SIZE = 40; // size of arrow labels in svg user units, 100 is the width of a board square

export function numberedArrows(
  choices: [string, Uci][],
  timer: number | undefined,
  asWhite: boolean
): DrawShape[] {
  if (!choices) return [];
  const shapes: DrawShape[] = [];
  const dests: Map<Key, SparseSet<number>> = new Map();
  const preferred = choices[0][0] === 'yes' ? choices.shift()?.[1] : undefined;
  choices.forEach(([, uci]) => {
    shapes.push({
      orig: src(uci),
      dest: dest(uci),
      brush: `v-grey`,
      modifiers: { hilite: uci === preferred },
    });
    if (uci.length > 2) pushMap(dests, dest(uci), moveAngle(uci, asWhite));
  });
  if (timer) {
    shapes.push(
      timerShape(
        choices[0][1],
        choices.length > 1 ? labelOffset(choices[0][1], dests, asWhite) : [0, 0],
        timer,
        choices.length > 1 ? 'grey' : 'white',
        0.6
      )
    );
  }
  if (choices.length > 1)
    choices.forEach(([, uci], i) => {
      shapes.push(labelShape(uci, dests, `${i + 1}`, asWhite));
    });
  return shapes;
}

export function coloredArrows(choices: [string, Uci][], timer: number | undefined): DrawShape[] {
  if (!choices) return [];
  const shapes: DrawShape[] = [];
  const preferred = choices[0][0] === 'yes' ? choices.shift()?.[1] : undefined;
  choices.forEach(([c, uci]) => {
    shapes.push({
      orig: src(uci),
      dest: dest(uci),
      brush: `v-${c}`,
      modifiers: { hilite: uci === preferred },
    });
  });
  if (timer) {
    shapes.push(timerShape(choices[0][1], [0, 0], timer, brushes.values().next().value.color));
  }
  return shapes;
}

function timerShape(uci: Uci, offset: [number, number], duration: number, color: string, alpha = 0.4) {
  // works around a firefox stroke-dasharray animation bug
  setTimeout(() => {
    for (const anim of $('.voice-timer-arc').get() as any[]) {
      anim?.beginElement();
      anim?.parentElement!.setAttribute('visibility', 'visible');
      if (color === 'grey') return; // don't show numbered arrow outlines
    }
  });
  return {
    orig: src(uci),
    brush: 'v-grey',
    customSvg:
      (color !== 'grey'
        ? `<svg width="100" height="100">`
        : `<svg viewBox="${offset[0]} ${offset[1]} 100 100">`) +
      `<circle cx="50" cy="50" r="25" fill="transparent" stroke="${color}" transform="rotate(270,50,50)"
               stroke-width="50" stroke-opacity="${alpha}" begin="indefinite" visibility="hidden">
         <animate class="voice-timer-arc" attributeName="stroke-dasharray" dur="${duration}s"
                  values="0 ${Math.PI * 50}; ${Math.PI * 50} ${Math.PI * 50}"/>
       </circle>
       <circle cx="50" cy="50" r="50" fill="transparent" stroke="white" transform="rotate(270,50,50)"
               stroke-width="4" stroke-opacity="0.7" begin="indefinite" visibility="hidden">
         <animate class="voice-timer-arc" attributeName="stroke-dasharray" dur="${duration}s"
                  values="0 ${Math.PI * 100}; ${Math.PI * 100} ${Math.PI * 100}"/>
       </circle></svg>`,
  };
}

function labelShape(
  uci: Uci,
  dests: Map<Key, number | Set<number>>,
  label: string,
  asWhite: boolean
): DrawShape {
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
// if there's more than 1 arrow pointing to a square, the arrow shortens by 125 / 8 units.

// to account for knights, find round(theta * 8 / pi) for every approach angle of incoming
// moves to a destination square and fractions (knights) are rounded to the nearest odd
// number yielding a mod 16 group. each member is considered a slot
//
// if knight arrows arrive at a square with an adjacent slot (+/- 1)%16, we further shorten
// the knight move arrow by the label size value to avoid collision

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
  return asSlot ? (Math.round((angle * 8) / Math.PI) + 16) % 16 : angle;
}

function destOffset(uci: Uci, asWhite: boolean): [number, number] {
  const sign = asWhite ? 1 : -1;
  const cc = [...uci].map(c => c.charCodeAt(0));
  return uci.length < 4 ? [0, 0] : [sign * (cc[0] - cc[2]) * 100, -sign * (cc[1] - cc[3]) * 100];
}

function destMag(uci: Uci, asWhite: boolean): number {
  return Math.sqrt(destOffset(uci, asWhite).reduce((acc, x) => acc + x * x, 0));
}

function labelOffset(uci: Uci, dests: Map<Key, SparseSet<number>>, asWhite: boolean): [number, number] {
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
