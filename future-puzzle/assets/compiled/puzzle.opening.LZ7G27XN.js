import {
  initMiniBoardWith
} from "./lib.FPZPN4FK.js";
import "./lib.MIKWYDBM.js";
import "./lib.NNS7OYZ5.js";
import "./lib.AJZLY5PU.js";
import "./lib.SHCCD7AE.js";
import "./lib.HGK4A3SW.js";
import "./lib.GLHZJLY7.js";
import "./lib.PM233RJM.js";
import "./lib.23HTWUBN.js";
import "./lib.FT6SWZ72.js";
import "./lib.5BY6QUVG.js";
import "./lib.YID4KMSR.js";
import "./lib.NQPUCFNU.js";
import "./lib.46QQJMTS.js";
import "./lib.LWAJSRDS.js";
import "./lib.J54GEVE2.js";
import "./lib.4BTYE6MH.js";
import "./lib.KO2KTNGK.js";

// ../puzzle/src/puzzle.opening.ts
site.load.then(() => {
  const rootEl = document.querySelector(".puzzle-openings");
  if (rootEl && !("ontouchstart" in window)) loadBoardTips(rootEl);
});
function loadBoardTips(rootEl) {
  rootEl.addEventListener("mouseover", (e) => {
    var _a;
    const el = e.target;
    if (el.classList.contains("blpt")) makeBoardTip(el, e);
    else {
      const parent = el.parentNode;
      if ((_a = parent == null ? void 0 : parent.classList) == null ? void 0 : _a.contains("blpt")) makeBoardTip(parent, e);
    }
  });
}
var makeBoardTip = (el, e) => {
  $(el).removeClass("blpt").powerTip({
    popupId: "miniBoard",
    async render(el2) {
      const tipEl = document.getElementById("miniBoard");
      tipEl.innerHTML = `<div class="mini-board mini-board--init cg-wrap standard is2d"/>`;
      initMiniBoardWith(tipEl.querySelector(".cg-wrap"), {
        fen: el2.dataset["fen"],
        orientation: "white"
      });
    }
  });
  $.powerTip.show(el, e);
};
//# sourceMappingURL=puzzle.opening.LZ7G27XN.js.map
