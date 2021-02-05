import { State } from "./state";
import { createEl, droppableRoles } from "./util";

export function addPocketEl(
  s: State,
  element: HTMLElement,
  position: string
) {
  const pocket = createEl("div", "pocket is2d pocket-" + position),
    color = (s.orientation === "white") !== (position == "top") ? "white" : "black";
  element.appendChild(pocket);
  for (const role of droppableRoles) {
    const c1 = createEl("div", "pocket-c1");
    pocket.appendChild(c1);
    const c2 = createEl("div", "pocket-c2");
    c1.appendChild(c2);
    const piece = createEl("piece", role + " " + color);
    piece.setAttribute("data-role", role);
    piece.setAttribute("data-color", color);
    piece.setAttribute("data-nb", "0");
    c2.appendChild(piece);

  }
}
