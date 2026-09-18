import {
  makeConfig,
  next,
  nextCorrectMove,
  prev,
  puzzleBox,
  renderDifficultyForm,
  theme,
  userBox
} from "./lib.TGXG7DI3.js";
import "./lib.GS5SIWVX.js";
import {
  addBreaks,
  arrowKeyHandler,
  boardCommands,
  boardCommandsHandler,
  castlingFlavours,
  commands,
  inputToMove,
  lastCapturedCommandHandler,
  leaveSquareHandler,
  makeContext,
  pieceJumpingHandler,
  positionJumpHandler,
  possibleMovesHandler,
  renderBoard,
  renderMainline,
  renderPieces,
  renderSan,
  renderSetting,
  scanDirectionsHandler,
  selectionHandler
} from "./lib.XGI55MZR.js";
import "./lib.6PSVMHHY.js";
import "./lib.DW3B6WVR.js";
import "./lib.AEC6Y5GK.js";
import "./lib.3SKK427Y.js";
import "./lib.TSMVECCD.js";
import "./lib.LDYEPMQF.js";
import "./lib.NGOOIAJR.js";
import "./lib.OFKEFUE3.js";
import "./lib.FPZPN4FK.js";
import "./lib.MIKWYDBM.js";
import "./lib.NNS7OYZ5.js";
import {
  Chessground
} from "./lib.AJZLY5PU.js";
import "./lib.SHCCD7AE.js";
import "./lib.HGK4A3SW.js";
import "./lib.GLHZJLY7.js";
import {
  makeSquare,
  opposite
} from "./lib.PM233RJM.js";
import "./lib.23HTWUBN.js";
import "./lib.FT6SWZ72.js";
import {
  isTouchDevice
} from "./lib.5BY6QUVG.js";
import "./lib.YID4KMSR.js";
import "./lib.NQPUCFNU.js";
import "./lib.46QQJMTS.js";
import {
  throttle
} from "./lib.LWAJSRDS.js";
import {
  bind,
  hl,
  onInsert,
  requiresI18n
} from "./lib.J54GEVE2.js";
import "./lib.4BTYE6MH.js";
import "./lib.KO2KTNGK.js";

// ../puzzle/src/view/nvuiView.ts
var throttled = (sound) => throttle(100, () => site.sound.play(sound));
var selectSound = throttled("select");
var borderSound = throttled("outOfBound");
var errorSound = throttled("error");
function renderNvui(ctx) {
  const { ctrl, notify, moveStyle, pieceStyle, prefixStyle, positionStyle, boardStyle, pageStyle } = ctx;
  notify.redraw = ctrl.redraw;
  const ground = ctrl.ground() || Chessground(document.createElement("div"), {
    ...makeConfig(ctrl),
    animation: { enabled: false },
    drawable: { enabled: false },
    coordinates: false
  });
  ctrl.ground(ground);
  const boardFirst = isTouchDevice() && pageStyle.get() === "board-actions";
  if (boardFirst) {
    pieceStyle.set("name");
    prefixStyle.set("name");
    boardStyle.set("plain");
  }
  const pov = ctrl.flipped() ? opposite(ctrl.pov) : ctrl.pov;
  const boardView = [
    hl("h2", "Board"),
    hl(
      "div.board",
      {
        hook: {
          insert: (el) => boardEventsHook(ctx, ground, el.elm),
          update: (_, vnode) => boardEventsHook(ctx, ground, vnode.elm)
        }
      },
      renderBoard(
        ground.state.pieces,
        pov,
        pieceStyle.get(),
        prefixStyle.get(),
        positionStyle.get(),
        boardStyle.get()
      )
    )
  ];
  return hl(
    `main.puzzle.puzzle--nvui.puzzle-${ctrl.data.replay ? "replay" : "play"}${ctrl.streak ? ".puzzle--streak" : ""}`,
    hl("div.nvui", [
      ...boardFirst ? boardView : [],
      boardFirst && renderTouchDeviceCommands(ctx),
      hl("h2", "Puzzle info"),
      puzzleBox(ctrl),
      theme(ctrl),
      ctrl.streak ? void 0 : userBox(ctrl),
      hl("h2", "Moves"),
      hl(
        "p.moves",
        { attrs: { role: "log", "aria-live": "off" } },
        renderMainline(ctrl.mainline, ctrl.path, moveStyle.get())
      ),
      hl("h2", "Pieces"),
      renderPieces(ground.state.pieces, moveStyle.get(), pov),
      hl("h2", "Puzzle status"),
      hl(
        "div.status",
        { attrs: { role: "status", "aria-live": "polite", "aria-atomic": "true" } },
        renderStatus(ctrl)
      ),
      ctrl.data.replay && hl("div.replay", renderReplay(ctrl)),
      ctrl.streak && renderStreak(ctrl),
      hl("h2", "Last move"),
      hl(
        "p.lastMove",
        { attrs: { "aria-live": "assertive", "aria-atomic": "true" } },
        lastMove(ctrl, moveStyle.get())
      ),
      hl("h2", "Move form"),
      hl(
        "form#move-form",
        {
          hook: onInsert((el) => {
            const $form = $(el), $input = $form.find(".move").val("");
            $form.on("submit", onSubmit(ctrl, notify.set, moveStyle.get, $input, ground));
          })
        },
        [
          hl("label", [
            ctrl.mode === "view" ? "Command input" : i18n.puzzle[ctrl.pov === "white" ? "findTheBestMoveForWhite" : "findTheBestMoveForBlack"],
            hl("input.move.mousetrap", {
              attrs: { name: "move", type: "text", autocomplete: "off", autofocus: true }
            })
          ])
        ]
      ),
      notify.render(),
      hl("h2", "Actions"),
      ctrl.mode === "view" ? afterActions(ctrl) : playActions({ ctrl, notify }),
      ...!boardFirst ? boardView : [],
      hl("div.boardstatus", { attrs: { "aria-live": "polite", "aria-atomic": "true" } }, ""),
      hl("h2", i18n.site.advancedSettings),
      hl("label", ["Move notation", renderSetting(moveStyle, ctrl.redraw)]),
      hl("h3", "Board settings"),
      hl("label", ["Piece style", renderSetting(pieceStyle, ctrl.redraw)]),
      hl("label", ["Piece prefix style", renderSetting(prefixStyle, ctrl.redraw)]),
      hl("label", ["Show position", renderSetting(positionStyle, ctrl.redraw)]),
      hl("label", ["Board layout", renderSetting(boardStyle, ctrl.redraw)]),
      ...!ctrl.data.replay && !ctrl.streak ? [hl("h3", "Puzzle Settings"), renderDifficultyForm(ctrl)] : [],
      hl("h2", i18n.site.keyboardShortcuts),
      hl("p", [
        `Left and right arrow keys: ${i18n.site.keyMoveBackwardOrForward}`,
        hl("br"),
        `Up and down arrow keys, or 0 and $, or home and end: ${i18n.site.keyGoToStartOrEnd}`
      ]),
      hl("h2", "Commands"),
      hl(
        "p",
        [
          "Type these commands in the move input.",
          `v: ${i18n.site.viewTheSolution}`,
          "l: Read last move.",
          commands().piece.help,
          commands().scan.help
        ].reduce(addBreaks, [])
      ),
      ...boardCommands(),
      hl("h2", "Promotion"),
      hl("p", [
        "Standard PGN notation selects the piece to promote to. Example: a8=n promotes to a knight.",
        hl("br"),
        "Omission results in promotion to queen"
      ])
    ])
  );
}
function touchDeviceButton(cls, text, onClick) {
  return hl(`button.${cls}`, { attrs: { type: "button" }, hook: bind("click", onClick) }, text);
}
function renderTouchDeviceCommands({ notify, ctrl }) {
  return hl("div.actions", [
    ctrl.mode !== "view" && touchDeviceButton("last-move", "Last move", () => notify.set($(".lastMove").text())),
    ctrl.mode !== "view" && touchDeviceButton("touch-hint", i18n.site.getAHint, () => {
      const hint = nextCorrectMove(ctrl);
      if (hint) notify.set(makeSquare(hint.from));
    }),
    ctrl.mode !== "view" && touchDeviceButton("touch-solution", i18n.site.viewTheSolution, ctrl.viewSolution),
    ctrl.mode === "view" && touchDeviceButton("touch-continue", i18n.puzzle.continueTraining, ctrl.nextPuzzle)
  ]);
}
function boardEventsHook({ ctrl, moveStyle, pieceStyle, prefixStyle, notify }, ground, el) {
  const $board = $(el);
  $board.off(".nvui");
  const steps = ctrl.tree.getNodeList(ctrl.path);
  const fenSteps = () => steps.map((step) => step.fen);
  $board.on("blur", "button", (e) => leaveSquareHandler($board.find("button"))(e));
  $board.on("click", "button", (e) => selectionHandler(() => opposite(ctrl.pov))(e));
  $board.on("keydown", "button", (e) => {
    var _a;
    if (e.shiftKey && e.key.match(/^[ad]$/i)) nextOrPrev(ctrl)(e);
    else if (/^x$/i.test(e.key))
      scanDirectionsHandler(
        ctrl.flipped() ? opposite(ctrl.pov) : ctrl.pov,
        ground.state.pieces,
        moveStyle.get()
      )(e);
    else if (e.key.toLowerCase() === "f") {
      notify.set("Flipping the board");
      setTimeout(() => ctrl.flip(), 1e3);
    } else if (["o"].includes(e.key)) boardCommandsHandler()(e);
    else if (e.key.startsWith("Arrow"))
      arrowKeyHandler(ctrl.flipped() ? opposite(ctrl.pov) : ctrl.pov, borderSound)(e);
    else if (/^Digit([1-8])$/.test(e.code)) positionJumpHandler()(e);
    else if (/^[kqrbnp]$/i.test(e.key)) pieceJumpingHandler(selectSound, errorSound)(e);
    else if (e.key.toLowerCase() === "m") possibleMovesHandler(ctrl.pov, ground, "standard", steps)(e);
    else if (e.key === "c") lastCapturedCommandHandler(fenSteps, pieceStyle.get(), prefixStyle.get())();
    else if (e.key === "i") {
      e.preventDefault();
      (_a = $("input.move").get(0)) == null ? void 0 : _a.focus();
    }
  });
}
function lastMove({ node }, style) {
  return node.ply === 0 ? "Initial position" : (
    // make sure consecutive moves are different so that they get re-read
    renderSan(node.san || "", node.uci, style) + (node.ply % 2 === 0 ? "" : "\xA0")
  );
}
function onSubmit(ctrl, notify, style, $input, ground) {
  return (ev) => {
    ev.preventDefault();
    let input = castlingFlavours($input.val().trim());
    if (isShortCommand(input)) input = "/" + input;
    if (input.startsWith("/")) onCommand(ctrl, notify, input.slice(1), style());
    else {
      const uci = inputToMove(input, ctrl.node.fen, ground);
      if (uci && typeof uci === "string") {
        ctrl.playUci(uci);
        const fback = ctrl.lastFeedback;
        if (fback === "fail") notify(i18n.puzzle.notTheMove);
        else if (fback === "good") notify(i18n.puzzle.bestMove);
        else if (fback === "win") notify(i18n.puzzle.puzzleSuccess);
      } else notify([`Invalid move: ${input}`, ...browseHint(ctrl)].join(". "));
    }
    $input.val("");
  };
}
var isYourMove = ({ node }) => node.children.length === 0 || node.children[0].puzzle === "fail";
var browseHint = (ctrl) => ctrl.mode !== "view" && !isYourMove(ctrl) ? [i18n.site.youBrowsedAway] : [];
var shortCommands = /* @__PURE__ */ new Set(["b", "l", "last", "p", "s", "v"]);
var isShortCommand = (input) => shortCommands.has(input.split(" ")[0].toLowerCase());
function onCommand(ctrl, notify, c, style) {
  const lowered = c.toLowerCase();
  const pieces = ctrl.ground().state.pieces;
  if (lowered === "l" || lowered === "last") notify($(".lastMove").text());
  else if (lowered === "v") viewOrAdvanceSolution(ctrl, notify);
  else if (lowered.startsWith("b")) commands().board.apply(c, pieces, style);
  else
    notify(
      commands().piece.apply(c, pieces, style) || commands().scan.apply(c, pieces, style) || `Invalid command: ${c}`
    );
}
function viewOrAdvanceSolution(ctrl, notify) {
  if (ctrl.mode === "view") {
    const node = ctrl.node;
    const next2 = nextNode(node);
    const nextNext = nextNode(next2);
    if (isInSolution(next2) || isInSolution(node) && isInSolution(nextNext)) {
      next(ctrl);
      ctrl.redraw();
    } else if (isInSolution(node)) notify(i18n.puzzle.puzzleComplete);
    else ctrl.viewSolution();
  } else ctrl.viewSolution();
}
var isInSolution = (node) => !!node && (node.puzzle === "good" || node.puzzle === "win");
var nextNode = (node) => {
  var _a;
  return ((_a = node == null ? void 0 : node.children) == null ? void 0 : _a.length) ? node.children[0] : void 0;
};
var renderStreak = ({ streak }) => !streak ? [] : [hl("h2", "Puzzle streak"), hl("p", streak.data.index || i18n.puzzle.streakDescription)];
function renderStatus(ctrl) {
  if (ctrl.mode !== "view") return "Solving";
  else if (ctrl.streak) return `GAME OVER. ${i18n.puzzle.yourStreakX(ctrl.streak.data.index)}`;
  else if (ctrl.lastFeedback === "win") return i18n.puzzle.puzzleSuccess;
  else return i18n.puzzle.puzzleComplete;
}
function renderReplay({ data, mode }) {
  const replay = data.replay;
  if (!replay) return "";
  const i = replay.i + (mode === "play" ? 0 : 1);
  const text = i18n.puzzleTheme[data.angle.key];
  return `Replaying ${text} puzzles: ${i} of ${replay.of}`;
}
var playActions = ({ ctrl, notify }) => {
  return ctrl.streak ? requiresI18n(
    "storm",
    ctrl.redraw,
    (cat) => {
      var _a;
      return button(cat.skip, ctrl.skip, i18n.puzzle.streakSkipExplanation, !((_a = ctrl.streak) == null ? void 0 : _a.data.skip));
    }
  ) : hl("div.actions-play", [
    button(i18n.site.getAHint, () => {
      const hint = nextCorrectMove(ctrl);
      if (hint) {
        notify.set(makeSquare(hint.from));
      }
    }),
    button(i18n.site.viewTheSolution, ctrl.viewSolution)
  ]);
};
var afterActions = (ctrl) => hl(
  "div.actions-after",
  ctrl.streak && ctrl.lastFeedback === "win" ? hl("a", { attrs: { href: "/streak" } }, i18n.puzzle.newStreak) : [...renderVote(ctrl), button(i18n.puzzle.continueTraining, ctrl.nextPuzzle)]
);
var renderVoteTutorial = (ctrl) => {
  var _a;
  return ctrl.session.isNew() && ((_a = ctrl.data.user) == null ? void 0 : _a.provisional) && hl("p", i18n.puzzle.didYouLikeThisPuzzle);
};
var renderVote = (ctrl) => !ctrl.data.user || ctrl.autoNexting() ? [] : [
  renderVoteTutorial(ctrl),
  button(i18n.puzzle.upVote, () => ctrl.vote(true), void 0),
  button(i18n.puzzle.downVote, () => ctrl.vote(false), void 0)
];
var button = (text, action, title, disabled) => hl(
  "button",
  { hook: bind("click", action), attrs: { ...title ? { title } : {}, disabled: !!disabled } },
  text
);
function nextOrPrev(ctrl) {
  return (e) => {
    if (e.key === "A") doAndRedraw(ctrl, prev);
    else if (e.key === "D") doAndRedraw(ctrl, next);
  };
}
var doAndRedraw = (ctrl, fn) => {
  fn(ctrl);
  ctrl.redraw();
};

// ../puzzle/src/puzzle.nvui.ts
function initModule(ctrl) {
  const ctx = makeContext({ ctrl });
  return {
    render: () => renderNvui(ctx)
  };
}
export {
  initModule
};
//# sourceMappingURL=puzzle.nvui.ME3TOLUJ.js.map
