import {
  embedChessground
} from "./lib.WN5ZVELZ.js";
import {
  uciToMove
} from "./lib.SHCCD7AE.js";
import "./lib.HGK4A3SW.js";
import "./lib.GLHZJLY7.js";
import "./lib.PM233RJM.js";
import "./lib.46QQJMTS.js";
import "./lib.LWAJSRDS.js";
import "./lib.J54GEVE2.js";
import "./lib.4BTYE6MH.js";
import "./lib.KO2KTNGK.js";

// ../site/src/site.puzzleEmbed.ts
window.onload = async () => {
  var _a, _b;
  const el = document.querySelector("#daily-puzzle");
  const board = el == null ? void 0 : el.querySelector(".mini-board");
  if (!el || !board) return;
  const [fen, orientation, lm] = (_b = (_a = board.getAttribute("data-state")) == null ? void 0 : _a.split(",")) != null ? _b : [];
  (await embedChessground()).Chessground(board.firstChild, {
    coordinates: false,
    drawable: { enabled: false, visible: false },
    viewOnly: true,
    fen,
    lastMove: uciToMove(lm),
    orientation
  });
  const resize = () => {
    var _a2, _b2;
    const windowHeight = window.innerHeight;
    if (el.offsetHeight > windowHeight) {
      const textHeightOffset = (_b2 = (_a2 = el.querySelector("span.text")) == null ? void 0 : _a2.offsetHeight) != null ? _b2 : 0;
      el.style.maxWidth = windowHeight - textHeightOffset + "px";
    }
  };
  resize();
  window.addEventListener("resize", resize);
};
//# sourceMappingURL=site.puzzleEmbed.NNTAD66H.js.map
