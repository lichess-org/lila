import { State } from './state'
import { colors, translateAway, translateAbs, posToTranslateAbs, key2pos, createEl, allKeys } from './util'
import { createElement as createSVG } from './svg'
import { Elements} from './types'

const files: number[] = [46, 47, 48, 49, 50];
const filesBlack: number[] = [1, 2, 3, 4, 5];
const ranks: number[] = [5, 15, 25, 35, 45];
const ranksBlack: number[] = [6, 16, 26, 36, 46];

export default function wrap(element: HTMLElement, s: State, bounds?: ClientRect): Elements {

  element.innerHTML = '';

  element.classList.add('cg-board-wrap');
  colors.forEach(c => {
    element.classList.toggle('orientation-' + c, s.orientation === c);
  });
  element.classList.toggle('manipulable', !s.viewOnly);

  const board = createEl('div', 'cg-board');

  element.appendChild(board);

  let svg: SVGElement | undefined;
  if (s.drawable.visible && bounds) {
    svg = createSVG('svg');
    svg.appendChild(createSVG('defs'));
    element.appendChild(svg);
  }

  if (s.coordinates) {
    if (s.orientation === 'black') {
      element.appendChild(renderCoords(ranksBlack, 'ranks black'));
      element.appendChild(renderCoords(filesBlack, 'files black'));
    } else {
      element.appendChild(renderCoords(ranks, 'ranks'));
      element.appendChild(renderCoords(files, 'files'));
    }
  }

  if (s.bigCoordinates && bounds)
    renderBigCoords(element, s, bounds);

  let ghost: HTMLElement | undefined;
  if (bounds && s.draggable.showGhost) {
    ghost = createEl('piece', 'ghost');
    translateAway(ghost);
    element.appendChild(ghost);
  }

  return {
    board: board,
    ghost: ghost,
    svg: svg
  };
}

function renderBigCoords(element: HTMLElement, s: State, bounds: ClientRect) {
  const asWhite = s.orientation !== 'black';
  for (var f = 1; f <= 50; f++) {
    const field = createEl('fieldnumber', 'black');
    field.textContent = f.toString();
    const coords = posToTranslateAbs(bounds)(key2pos(allKeys[f - 1]), asWhite, 0);
    translateAbs(field, [coords["0"], coords["1"] + bounds.height / 26]);
    element.appendChild(field);
  }
}

function renderCoords(elems: any[], className: string): HTMLElement {
  const el = createEl('coords', className);
  let f: HTMLElement;
  for (let i in elems) {
    f = createEl('coord');
    f.textContent = elems[i];
    el.appendChild(f);
  }
  return el;
}
