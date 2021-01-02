import { State } from "./state";
import { setVisible, createEl } from "./util";
//import { colors, files, ranks } from "./types";
import { colors, Notation } from "./types";
import { createElement as createSVG } from "./svg";
import { Elements } from "./types";

export function renderWrap(
  element: HTMLElement,
  s: State,
  relative: boolean
): Elements {
  // .cg-wrap (element passed to Shogiground)
  //   cg-helper (12.5%)
  //     cg-container (800%)
  //       cg-board
  //       svg
  //       coords.ranks
  //       coords.files
  //       piece.ghost

  element.innerHTML = "";

  // ensure the cg-wrap class is set
  // so bounds calculation can use the CSS width/height values
  // add that class yourself to the element before calling shogiground
  // for a slight performance improvement! (avoids recomputing style)
  element.classList.add("cg-wrap");

  for (const c of colors)
    element.classList.toggle("orientation-" + c, s.orientation === c);
  element.classList.toggle("manipulable", !s.viewOnly);

  const helper = createEl("cg-helper");
  element.appendChild(helper);
  const container = createEl("cg-container");
  helper.appendChild(container);

  const board = createEl("cg-board");
  container.appendChild(board);

  let svg: SVGElement | undefined;
  if (s.drawable.visible && !relative) {
    svg = createSVG("svg");
    svg.appendChild(createSVG("defs"));
    container.appendChild(svg);
  }

  if (s.coordinates) {
    const orientClass = s.orientation === "black" ? " black" : "";
    if(s.notation === Notation.WESTERN || s.notation === Notation.KAWASAKI){
      container.appendChild(
        renderCoords(
          ["9", "8", "7", "6", "5", "4", "3", "2", "1"],
          "ranks" + orientClass
        )
      );
    }
    else if(s.notation === Notation.WESTERN2){
      container.appendChild(
        renderCoords(
          ["i", "h", "g", "f", "e", "d", "c", "b", "a"],
          "ranks" + orientClass
        )
      );
    }
    else{
      container.appendChild(
        renderCoords(
          ["九", "八", "七", "六", "五", "四", "三", "二", "一"],
          "ranks" + orientClass
        )
      );
    }
    container.appendChild(
      renderCoords(
        ["9", "8", "7", "6", "5", "4", "3", "2", "1"],
        "files" + orientClass
      )
    );
  }

  let ghost: HTMLElement | undefined;
  if (s.draggable.showGhost && !relative) {
    ghost = createEl("piece", "ghost");
    setVisible(ghost, false);
    container.appendChild(ghost);
  }

  return {
    board,
    container,
    ghost,
    svg,
  };
}

function renderCoords(
  elems: readonly string[],
  className: string
): HTMLElement {
  const el = createEl("coords", className);
  let f: HTMLElement;
  for (const elem of elems) {
    f = createEl("coord");
    f.textContent = elem;
    el.appendChild(f);
  }
  return el;
}
