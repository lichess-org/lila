import { h } from "snabbdom";
import { VNode } from "snabbdom/vnode";
import { defined } from "common";
import { view as cevalView, renderEval as normalizeEval } from "ceval";

export interface Ctx {
  withDots?: boolean;
  showEval: boolean;
  showGlyphs?: boolean;
}

export function plyToTurn(ply: Ply): number {
  return Math.floor((ply - 1) / 2) + 1;
}

export function renderGlyphs(glyphs: Tree.Glyph[]): VNode[] {
  return glyphs.map((glyph) =>
    h(
      "glyph",
      {
        attrs: { title: glyph.name },
      },
      glyph.symbol
    )
  );
}

function renderEval(e): VNode {
  return h("eval", e);
}

export function renderIndexText(ply: Ply, withDots?: boolean): string {
  return plyToTurn(ply) + (withDots ? (ply % 2 === 1 ? "." : "...") : "");
}

export function renderIndex(ply: Ply, withDots?: boolean): VNode {
  return h("index", renderIndexText(ply, withDots));
}

function westernShogiNotation(str: string | undefined): string | undefined {
  if (!str) return;
  if (!str.includes("x") && !str.includes("*")) {
    if (str.length >= 5) str = str.slice(0, 3) + "-" + str.slice(3);
    else str = str.slice(0, 1) + "-" + str.slice(1);
  }
  let builder = "";
  const index = {
    "9": "a",
    "8": "b",
    "7": "c",
    "6": "d",
    "5": "e",
    "4": "f",
    "3": "g",
    "2": "h",
    "1": "i",
    a: "9",
    b: "8",
    c: "7",
    d: "6",
    e: "5",
    f: "4",
    g: "3",
    h: "2",
    i: "1",
    U: "+L",
    M: "+N",
    A: "+S",
    T: "+P",
    H: "+B",
    D: "+R",
  };
  for (let c of str) {
    builder += index[c] ? index[c] : c;
  }

  return builder;
}

export function renderMove(ctx: Ctx, node: Tree.Node): VNode[] {
  const ev: any =
    cevalView.getBestEval({ client: node.ceval, server: node.eval }) || {};
  return [h("san", westernShogiNotation(node.san)!)]
    .concat(node.glyphs && ctx.showGlyphs ? renderGlyphs(node.glyphs) : [])
    .concat(
      ctx.showEval
        ? defined(ev.cp)
          ? [renderEval(normalizeEval(ev.cp))]
          : defined(ev.mate)
          ? [renderEval("#" + ev.mate)]
          : []
        : []
    );
}

export function renderIndexAndMove(
  ctx: Ctx,
  node: Tree.Node
): VNode[] | undefined {
  if (!node.san) return; // initial position
  return [renderIndex(node.ply, ctx.withDots), ...renderMove(ctx, node)];
}
