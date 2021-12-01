import { HeadlessState } from './state';
import { setVisible, createEl, isMiniBoard } from './util';
import { colors, Notation, Elements } from './types';
import { createElement as createSVG } from './svg';

export function renderWrap(element: HTMLElement, s: HeadlessState, relative: boolean): Elements {
  // .cg-wrap (element passed to Shogiground)
  //     cg-container
  //       cg-board
  //       svg
  //       coords.ranks
  //       coords.files
  //       piece.ghost

  element.innerHTML = '';

  // ensure the cg-wrap class is set
  // so bounds calculation can use the CSS width/height values
  // add that class yourself to the element before calling shogiground
  // for a slight performance improvement! (avoids recomputing style)
  element.classList.add('cg-wrap');

  for (const c of colors) element.classList.toggle('orientation-' + c, s.orientation === c);
  element.classList.toggle('manipulable', !s.viewOnly);

  const container = createEl('cg-container');
  element.appendChild(container);

  const board = createEl('cg-board');
  container.appendChild(board);

  let pockets;

  if (isMiniBoard(element)) {
    if (s.pockets) {
      pockets = [createEl('cg-pocket'), createEl('cg-pocket')];
      container.insertBefore(pockets[s.orientation === 'sente' ? 1 : 0], board);
      container.insertBefore(pockets[s.orientation === 'sente' ? 0 : 1], board.nextSibling);
    } else {
      element.classList.add('no-pockets');
    }
  } else {
    delete s.pockets;
  }

  let svg: SVGElement | undefined;
  if (s.drawable.visible && !relative) {
    svg = createSVG('svg');
    svg.appendChild(createSVG('defs'));
    container.appendChild(svg);
  }

  if (s.coordinates) {
    const orientClass = s.orientation === 'gote' ? ' gote' : '';
    if (s.notation === Notation.WESTERN || s.notation === Notation.KAWASAKI) {
      container.appendChild(
        renderCoords(['9', '8', '7', '6', '5', '4', '3', '2', '1'], 'ranks' + orientClass, s.dimensions.ranks)
      );
    } else if (s.notation === Notation.WESTERN2) {
      container.appendChild(
        renderCoords(['i', 'h', 'g', 'f', 'e', 'd', 'c', 'b', 'a'], 'ranks' + orientClass, s.dimensions.ranks)
      );
    } else {
      container.appendChild(
        renderCoords(['九', '八', '七', '六', '五', '四', '三', '二', '一'], 'ranks' + orientClass, s.dimensions.ranks)
      );
    }
    container.appendChild(
      renderCoords(['9', '8', '7', '6', '5', '4', '3', '2', '1'], 'files' + orientClass, s.dimensions.files)
    );
  }

  let ghost: HTMLElement | undefined;
  if (s.draggable.showGhost && !relative) {
    ghost = createEl('piece', 'ghost');
    setVisible(ghost, false);
    container.appendChild(ghost);
  }

  return {
    pockets,
    board,
    container,
    ghost,
    svg,
  };
}

function renderCoords(elems: readonly string[], className: string, trim: number): HTMLElement {
  const el = createEl('coords', className);
  let f: HTMLElement;
  for (const elem of elems.slice(-trim)) {
    f = createEl('coord');
    f.textContent = elem;
    el.appendChild(f);
  }
  return el;
}
