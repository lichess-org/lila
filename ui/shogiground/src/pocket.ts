import { State } from "./state";
import { createEl, droppableRoles } from "./util";
import { Pockets, Role } from "./types";

const droppableLetters: { [letter: string]: Role } = {
  p: "pawn",
  l: "lance",
  n: "knight",
  s: "silver",
  g: "gold",
  b: "bishop",
  r: "rook",
};

export function makePockets(str?: string): Pockets | undefined {
  const pockets = [
    { pawn: 0, lance: 0, knight: 0, silver: 0, gold: 0, bishop: 0, rook: 0 },
    { pawn: 0, lance: 0, knight: 0, silver: 0, gold: 0, bishop: 0, rook: 0 }
  ];
  if (!str) return pockets;

  try {
    let jsonParsed = JSON.parse(str);
    // if pocket is a json like "[{pawn: 1, gold: 1},{pawn: 3, silver: 1}]
    for (const i of [0, 1]) {
      for (const role of droppableRoles) {
        pockets[i][role] = jsonParsed[i][role] || 0;
      }
    }
  } catch {
    // if pocket is a string like "PGppps"
    let num = 0;
    for (const c of str) {
      const role = droppableLetters[c.toLowerCase()];
      if (role) {
        pockets[c.toLowerCase() === c ? 1 : 0][role] += num ? num : 1;
        num = 0;
      } else {
        num = num * 10 + Number(c);
      }
    }
  }

  return pockets;
}

export function addPocketEl(
  s: State,
  element: HTMLElement,
  color: string
): HTMLElement {
  const position = (s.orientation === "sente") !== (color == "sente") ? "top" : "bottom",
    pocket = createEl("div", "pocket is2d pocket-" + position);
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
  return pocket;
}
