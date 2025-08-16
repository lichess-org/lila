import type { DrawBrush, DrawShape } from '@lichess-org/chessground/draw';

export const brushes: Map<string, DrawBrush> = new Map<string, DrawBrush>([
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

export function numberedArrows(choices: [string, Uci][], timer: number | undefined): DrawShape[] {
  if (!choices) return [];
  const shapes: DrawShape[] = [];
  const preferred = choices[0][0] === 'yes' ? choices.shift()?.[1] : undefined;
  choices.forEach(([, uci], i) => {
    shapes.push({
      orig: uci.slice(0, 2) as Key,
      dest: uci.slice(2, 4) as Key,
      brush: `v-grey`,
      modifiers: uci === preferred ? { hilite: 'white' } : undefined,
      label: choices.length > 1 ? { text: `${i + 1}` } : undefined,
    });
  });
  if (timer)
    shapes[0].customSvg = {
      center: choices.length > 1 ? 'label' : 'orig',
      html: timerShape(timer, choices.length > 1 ? 'grey' : 'white', 0.6),
    };
  return shapes.reverse();
}

export function coloredArrows(choices: [string, Uci][], timer: number | undefined): DrawShape[] {
  if (!choices) return [];
  const shapes: DrawShape[] = [];
  const preferred = choices[0][0] === 'yes' ? choices.shift()?.[1] : undefined;
  choices.forEach(([c, uci]) => {
    shapes.push({
      orig: uci.slice(0, 2) as Key,
      dest: uci.slice(2, 4) as Key,
      brush: `v-${c}`,
      modifiers: uci === preferred ? { hilite: 'white' } : undefined,
    });
  });
  if (timer) {
    const [mainBrush] = [...brushes.values()];
    shapes[0].customSvg = {
      center: 'orig',
      html: timerShape(timer, mainBrush.color),
    };
  }
  return shapes.reverse();
}

function timerShape(duration: number, color: string, alpha = 0.4) {
  // works around a firefox stroke-dasharray animation bug
  setTimeout(() => {
    for (const anim of document.querySelectorAll<SVGAnimateElement>('.voice-timer-arc')) {
      anim.beginElement();
      anim.parentElement?.setAttribute('visibility', 'visible');
      if (color === 'grey') return; // don't show numbered arrow outlines
    }
  });
  return $html`
    <svg width="100" height="100">
      <circle cx="50" cy="50" r="25" fill="transparent" stroke="${color}" transform="rotate(270,50,50)"
              stroke-width="50" stroke-opacity="${alpha}" begin="indefinite" visibility="hidden">
        <animate class="voice-timer-arc" attributeName="stroke-dasharray" dur="${duration}s"
                 values="0 ${Math.PI * 50}; ${Math.PI * 50} ${Math.PI * 50}"/>
      </circle>
      <circle cx="50" cy="50" r="50" fill="transparent" stroke="white" transform="rotate(270,50,50)"
              stroke-width="4" stroke-opacity="0.7" begin="indefinite" visibility="hidden">
        <animate class="voice-timer-arc" attributeName="stroke-dasharray" dur="${duration}s"
                 values="0 ${Math.PI * 100}; ${Math.PI * 100} ${Math.PI * 100}"/>
      </circle>
    </svg>`;
}
