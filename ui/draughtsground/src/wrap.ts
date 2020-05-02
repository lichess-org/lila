import { State } from './state'
import { colors, translateAway, translateAbs, posToTranslateAbs, key2pos, createEl, allKeys } from './util'
import { createElement as createSVG } from './svg'
import { Elements} from './types'

const files: number[] = [46, 47, 48, 49, 50];
const filesBlack: number[] = [1, 2, 3, 4, 5];
const ranks: number[] = [5, 15, 25, 35, 45];
const ranksBlack: number[] = [6, 16, 26, 36, 46];

export default function wrap(element: HTMLElement, s: State, relative: boolean): Elements {

  // .cg-wrap (element passed to Chessground)
  //   cg-helper (10.0%)
  //     cg-container (1000%)
  //       cg-board
  //       svg
  //       coords.ranks
  //       coords.files
  //       piece.ghost

  element.innerHTML = '';

  element.classList.add('cg-wrap');
  colors.forEach(c => {
    element.classList.toggle('orientation-' + c, s.orientation === c);
  });
  element.classList.toggle('manipulable', !s.viewOnly);

  const helper = createEl('cg-helper');
  element.appendChild(helper);
  const container = createEl('cg-container');
  helper.appendChild(container);

  const board = createEl('cg-board');
  container.appendChild(board);

  let svg: SVGElement | undefined;
  if (s.drawable.visible && !relative) {
    svg = createSVG('svg');
    svg.appendChild(createSVG('defs'));
    container.appendChild(svg);
  }

  if (s.coordinates) {
    if (s.coordinates === 2) {
      if (s.orientation === 'black') {
        container.appendChild(renderCoords(ranksBlack, 'ranks black'));
        container.appendChild(renderCoords(filesBlack, 'files black'));
      } else {
        container.appendChild(renderCoords(ranks, 'ranks'));
        container.appendChild(renderCoords(files, 'files'));
      }
    } else if (!relative && s.coordinates === 1)
      renderFieldnumbers(container, s, board.getBoundingClientRect());
  }

  let ghost: HTMLElement | undefined;
  if (s.draggable.showGhost && !relative) {
    ghost = createEl('piece', 'ghost');
    translateAway(ghost);
    container.appendChild(ghost);
  }

  return {
    board,
    container,
    ghost,
    svg
  };
}

function renderFieldnumbers(element: HTMLElement, s: State, bounds: ClientRect) {
  const asWhite = s.orientation !== 'black';
  for (let f = 1; f <= 50; f++) {
    const field = createEl('fieldnumber', 'black');
    field.textContent = f.toString();
    const coords = posToTranslateAbs(bounds)(key2pos(allKeys[f - 1]), asWhite, 0);
    translateAbs(field, [coords["0"], coords["1"]]);
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
