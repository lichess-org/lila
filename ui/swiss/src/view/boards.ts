import { h } from "snabbdom";
import { VNode } from "snabbdom/vnode";
import { player as renderPlayer } from "./util";
import { Board, BoardPlayer } from "../interfaces";

export function many(boards: Board[]): VNode {
  return h("div.swiss__boards.now-playing", boards.map(renderBoard));
}

export function top(boards: Board[]): VNode {
  return h(
    "div.swiss__board__top.swiss__table",
    boards.slice(0, 1).map(renderBoard)
  );
}

const renderBoard = (board: Board): VNode =>
  h("div.swiss__board", [
    boardPlayer(board.black),
    miniBoard(board),
    boardPlayer(board.white),
  ]);

const boardPlayer = (player: BoardPlayer) =>
  h("div.swiss__board__player", [
    h("strong", "#" + player.rank),
    renderPlayer(player, true, true),
  ]);

function miniBoard(board: Board) {
  return h(
    "a.mini-board.live.is2d.mini-board-" + board.id,
    {
      key: board.id,
      attrs: {
        href: "/" + board.id,
        "data-live": board.id,
        "data-color": "white",
        "data-fen": board.fen,
        "data-lastmove": board.lastMove,
      },
      hook: {
        insert(vnode) {
          window.lishogi.parseFen($(vnode.elm as HTMLElement));
          window.lishogi.pubsub.emit("content_loaded");
        },
      },
    },
    [h("div.cg-wrap")]
  );
}
