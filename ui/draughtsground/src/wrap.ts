import { State } from './state'
import { colors, translateAway, translateAbs, posToTranslateAbs, key2pos, createEl, allKeys, san2alg } from './util'
import { ranksRev as allRanks, files as allFiles } from './util'
import { createElement as createSVG } from './svg'
import { boardFields } from './board'
import { Elements} from './types'

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

  // ensure the cg-wrap class is set
  // so bounds calculation can use the CSS width/height values
  // add that class yourself to the element before calling draughtsground
  // for a slight performance improvement! (avoids recomputing style)
  element.classList.add('cg-wrap');
  
  colors.forEach(c => element.classList.toggle('orientation-' + c, s.orientation === c));
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
      if (s.coordSystem === 1) {
        container.appendChild(renderCoords(allRanks, 'ranks is64' + (s.orientation === 'black' ? ' black' : '')));
        container.appendChild(renderCoords(allFiles, 'files is64' + (s.orientation === 'black' ? ' black' : '')));
      } else if (s.orientation === 'black') {
        const filesBlack: number[] = [], ranksBlack: number[] = [],
          rankBase = s.boardSize[0] / 2,
          fileSteps = s.boardSize[1] / 2;
        for (let i = 1; i <= rankBase; i++) filesBlack.push(i);
        for (let i = 0; i < fileSteps; i++) ranksBlack.push(rankBase + s.boardSize[0] * i + 1);
        container.appendChild(renderCoords(ranksBlack, 'ranks is100 black'));
        container.appendChild(renderCoords(filesBlack, 'files is100 black'));
      } else {
        const files: number[] = [], ranks: number[] = [],
          rankBase = s.boardSize[0] / 2,
          fields = s.boardSize[0] * s.boardSize[1] / 2,
          fileSteps = s.boardSize[1] / 2;
        for (let i = fields - rankBase + 1; i <= fields; i++) files.push(i);
        for (let i = 0; i < fileSteps; i++) ranks.push(rankBase + s.boardSize[0] * i);
        container.appendChild(renderCoords(ranks, 'ranks is100'));
        container.appendChild(renderCoords(files, 'files is100'));
      }
    } else if (!relative && s.coordinates === 1) {
      renderFieldnumbers(container, s, board.getBoundingClientRect());
    }
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
  const asWhite = s.orientation !== 'black',
    count = boardFields(s);
  for (let f = 1; f <= count; f++) {
    const field = createEl('fieldnumber', 'black'), san = f.toString();
    field.textContent = s.coordSystem === 1 ? san2alg[san] : san ;
    const coords = posToTranslateAbs(bounds, s.boardSize)(key2pos(allKeys[f - 1], s.boardSize), asWhite, 0);
    translateAbs(field, [coords['0'], coords['1']]);
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
