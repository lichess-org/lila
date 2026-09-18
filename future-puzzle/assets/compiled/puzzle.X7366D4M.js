import {
  makeVoiceMove,
  renderVoiceBar,
  toggleZenMode
} from "./lib.T6LS6H4A.js";
import "./lib.OVIGHX3H.js";
import "./lib.IVO3KPE3.js";
import {
  chessground_default,
  config,
  first,
  last as last2,
  mergeSolution,
  next,
  nextCorrectMove,
  pgnToTree,
  prev,
  puzzleBox,
  replay,
  streakBox,
  theme,
  userBox
} from "./lib.TGXG7DI3.js";
import {
  renderBlindfoldToggle
} from "./lib.N6HEUFVE.js";
import "./lib.GS5SIWVX.js";
import {
  annotationShapes
} from "./lib.KWWXD4IL.js";
import {
  ctrl,
  render
} from "./lib.WIAZUV32.js";
import {
  menuHover_default
} from "./lib.ZWX3OLRL.js";
import {
  PromotionCtrl
} from "./lib.JMKYI5NS.js";
import {
  renderNodesTxt
} from "./lib.35YPWAOB.js";
import {
  last,
  makeTree,
  ops_exports,
  path_exports
} from "./lib.6PSVMHHY.js";
import "./lib.DW3B6WVR.js";
import {
  completeNode
} from "./lib.AEC6Y5GK.js";
import {
  dispatchChessgroundResize
} from "./lib.3SKK427Y.js";
import {
  Coords
} from "./lib.TSMVECCD.js";
import "./lib.LDYEPMQF.js";
import "./lib.NGOOIAJR.js";
import {
  CevalCtrl,
  main_exports,
  renderEval,
  winningChances_exports
} from "./lib.UU5BVHFY.js";
import {
  stepwiseScroll
} from "./lib.6CC5EXC5.js";
import "./lib.OFKEFUE3.js";
import {
  a,
  addPointerListeners,
  alert,
  boardMenu,
  boolPrefXhrToggle,
  button,
  div,
  domDialog,
  icon,
  snabDialog,
  span,
  strong,
  toggleButton
} from "./lib.FPZPN4FK.js";
import {
  fenColor,
  pieceCount,
  plyColor,
  plyOpponentColor,
  plyToTurn
} from "./lib.MIKWYDBM.js";
import "./lib.NNS7OYZ5.js";
import "./lib.AJZLY5PU.js";
import {
  uciToMove
} from "./lib.SHCCD7AE.js";
import "./lib.HGK4A3SW.js";
import {
  Chess,
  Result,
  chessgroundDests,
  makeFen,
  makeSanAndPlay,
  normalizeMove,
  parseFen
} from "./lib.GLHZJLY7.js";
import {
  makeSquare,
  makeUci,
  opposite,
  parseSquare,
  parseUci
} from "./lib.PM233RJM.js";
import "./lib.23HTWUBN.js";
import "./lib.FT6SWZ72.js";
import "./lib.5BY6QUVG.js";
import {
  pubsub
} from "./lib.YID4KMSR.js";
import {
  licon
} from "./lib.NQPUCFNU.js";
import {
  form,
  json
} from "./lib.46QQJMTS.js";
import {
  defer,
  storage,
  storedBooleanProp,
  storedBooleanPropWithEffect,
  storedIntProp,
  storedJsonProp,
  throttle
} from "./lib.LWAJSRDS.js";
import {
  bind,
  bindNonPassive,
  defined,
  hl,
  myUserId,
  onInsert,
  prop,
  propWithEffect,
  requestIdleCallbackSafe,
  requiresI18n,
  toggle
} from "./lib.J54GEVE2.js";
import {
  attributesModule,
  classModule,
  h,
  init
} from "./lib.4BTYE6MH.js";
import "./lib.KO2KTNGK.js";

// ../puzzle/src/autoShape.ts
function makeAutoShapesFromUci(color, uci, brush, modifiers) {
  const move = parseUci(uci);
  const to = makeSquare(move.to);
  return [
    { orig: makeSquare(move.from), dest: to, brush, modifiers },
    ...move.promotion ? [{ orig: to, piece: { color, role: move.promotion, scale: 0.8 }, brush: "green" }] : []
  ];
}
function autoShape_default(ctrl2) {
  var _a, _b, _c, _d;
  const n = ctrl2.node;
  const hovering = ctrl2.ceval.hovering();
  const color = fenColor(n.fen);
  let shapes = [];
  if (hovering && hovering.fen === n.fen)
    shapes = shapes.concat(makeAutoShapesFromUci(color, hovering.uci, "paleBlue"));
  if (ctrl2.showEvaluation() && ctrl2.ceval.storedPv() > 0) {
    if (n.eval) shapes = shapes.concat(makeAutoShapesFromUci(color, n.eval.best, "paleGreen"));
    if (!hovering) {
      let nextBest = ctrl2.nextNodeBest();
      if (!nextBest && ctrl2.cevalEnabled() && n.ceval) nextBest = (_a = n.ceval.pvs[0]) == null ? void 0 : _a.moves[0];
      if (nextBest) shapes = shapes.concat(makeAutoShapesFromUci(color, nextBest, "paleBlue"));
      if (ctrl2.cevalEnabled() && ((_c = (_b = n.ceval) == null ? void 0 : _b.pvs) == null ? void 0 : _c[1]) && !(ctrl2.threatMode() && ((_d = n.threat) == null ? void 0 : _d.pvs[2]))) {
        n.ceval.pvs.forEach((pv) => {
          if (pv.moves[0] === nextBest) return;
          const shift = winningChances_exports.povDiff(color, n.ceval.pvs[0], pv);
          if (shift > 0.2 || isNaN(shift) || shift < 0) return;
          shapes = shapes.concat(
            makeAutoShapesFromUci(color, pv.moves[0], "paleGrey", {
              lineWidth: Math.round(12 - shift * 50)
              // 12 to 2
            })
          );
        });
      }
    }
  }
  if (ctrl2.cevalEnabled() && ctrl2.threatMode() && n.threat) {
    if (n.threat.pvs[1]) {
      shapes = shapes.concat(makeAutoShapesFromUci(opposite(color), n.threat.pvs[0].moves[0], "paleRed"));
      n.threat.pvs.slice(1).forEach((pv) => {
        const shift = winningChances_exports.povDiff(opposite(color), pv, n.threat.pvs[0]);
        if (shift > 0.2 || isNaN(shift) || shift < 0) return;
        shapes = shapes.concat(
          makeAutoShapesFromUci(opposite(color), pv.moves[0], "paleRed", {
            lineWidth: Math.round(11 - shift * 45)
            // 11 to 2
          })
        );
      });
    } else shapes = shapes.concat(makeAutoShapesFromUci(opposite(color), n.threat.pvs[0].moves[0], "red"));
  }
  const feedback = feedbackAnnotation(n);
  const hint = ctrl2.hintSquare() !== void 0 ? { orig: makeSquare(ctrl2.hintSquare()), brush: "green" } : void 0;
  return [
    ...shapes,
    ...annotationShapes(n),
    ...feedback ? annotationShapes(feedback) : [],
    ...hint ? [hint] : [],
    ...ctrl2.googlyEyes ? ctrl2.googlyEyes() : []
  ];
}
function feedbackAnnotation(n) {
  let glyph;
  switch (n.puzzle) {
    case "good":
    case "win":
      glyph = { id: 7, name: "good", symbol: "\u2713" };
      break;
    case "fail":
      glyph = { id: 4, name: "fail", symbol: "\u2717" };
  }
  return glyph && { ...n, glyphs: [glyph] };
}

// ../puzzle/src/keyboard.ts
var keyboard_default = (ctrl2) => site.mousetrap.bind(["left", "k"], () => {
  prev(ctrl2);
  ctrl2.redraw();
}).bind(["right", "j"], () => {
  next(ctrl2);
  ctrl2.redraw();
}).bind(["up", "0", "home"], () => {
  first(ctrl2);
  ctrl2.redraw();
}).bind(["down", "$", "end"], () => {
  last2(ctrl2);
  ctrl2.redraw();
}).bind("l", () => {
  if (ctrl2.isCevalAllowed()) ctrl2.cevalEnabled(!ctrl2.cevalEnabled());
}).bind("x", ctrl2.toggleThreatMode).bind("space", () => {
  if (ctrl2.isCevalAllowed()) {
    if (ctrl2.cevalEnabled()) ctrl2.playBestMove();
    else ctrl2.cevalEnabled(true);
  }
}).bind("z", () => pubsub.emit("zen")).bind("?", () => ctrl2.keyboardHelp(!ctrl2.keyboardHelp())).bind("f", ctrl2.flip).bind("n", ctrl2.nextPuzzle).bind("h", ctrl2.menu.toggle).bind("G", ctrl2.googlyEyesStart);
var view = (ctrl2) => snabDialog({
  class: "help",
  htmlUrl: "/training/help",
  onClose: () => ctrl2.keyboardHelp(false),
  modal: true,
  easyClose: "clickOutside"
});

// ../puzzle/src/moveTest.ts
var altCastles = {
  e1a1: "e1c1",
  e1h1: "e1g1",
  e8a8: "e8c8",
  e8h8: "e8g8"
};
function isAltCastle(str) {
  return str in altCastles;
}
function moveTest(ctrl2) {
  if (ctrl2.mode === "view") return void 0;
  if (!path_exports.contains(ctrl2.path, ctrl2.initialPath)) return void 0;
  const playedByColor = plyOpponentColor(ctrl2.node.ply);
  if (playedByColor !== ctrl2.pov) return void 0;
  const nodes = ctrl2.nodeList.slice(path_exports.size(ctrl2.initialPath) + 1).map((node) => ({
    uci: node.uci,
    castle: node.san.startsWith("O-O"),
    checkmate: node.san.endsWith("#")
  }));
  for (let i = 0; i < nodes.length; i++) {
    if (nodes[i].checkmate) return ctrl2.node.puzzle = "win";
    const uci = nodes[i].uci;
    const solUci = ctrl2.data.puzzle.solution[i];
    if (uci !== solUci && (!nodes[i].castle || !isAltCastle(uci) || altCastles[uci] !== solUci))
      return ctrl2.node.puzzle = "fail";
  }
  const nextUci = ctrl2.data.puzzle.solution[nodes.length];
  if (!nextUci) return ctrl2.node.puzzle = "win";
  ctrl2.node.puzzle = "good";
  return {
    move: parseUci(nextUci),
    fen: ctrl2.node.fen,
    path: ctrl2.path
  };
}

// ../puzzle/src/xhr.ts
var complete = (puzzleId, theme2, win, rated, replay2, streak, color) => json(`/training/complete/${theme2}/${puzzleId}`, {
  method: "POST",
  body: form({
    win,
    ...replay2 ? { replayDays: replay2.days } : {},
    ...streak ? { streakId: streak.nextId(), streakScore: streak.data.index } : {},
    rated,
    color
  })
});
var vote = (puzzleId, vote2) => json(`/training/${puzzleId}/vote`, {
  method: "POST",
  body: form({ vote: vote2 })
});
var voteTheme = (puzzleId, theme2, vote2) => json(`/training/${puzzleId}/vote/${theme2}`, {
  method: "POST",
  body: defined(vote2) ? form({ vote: vote2 }) : void 0
});
var maxReportLength = 2e3;
var report = (puzzleId, reason) => json(`/training/${puzzleId}/report`, {
  method: "POST",
  body: form({ reason: reason.slice(0, maxReportLength) })
});

// ../puzzle/src/report.ts
var version = 11;
var Report = class {
  constructor() {
    // if local eval suspect multiple solutions, report the puzzle, once at most
    this.reported = false;
    // number of evals that have triggered the `winningChances.hasMultipleSolutions` method
    // this is used to reduce the number of fps due to fluke eval
    this.evalsWithMultipleSolutions = 0;
    this.reportDialog = (puzzleId, reason) => {
      const switchButton = `<div class="switch switch-report-puzzle" title="temporarily disable reporting puzzles"><input id="puzzle-toggle-report" class="cmn-toggle" type="checkbox"><label for="puzzle-toggle-report"></label></div>`;
      const hideButtonDiv = `<div style="display:flex; flex-flow: row nowrap; align-items: center; justify-content: center">${switchButton}<span style="padding-left: 1em"> Hide this for a week</span></div>`;
      const hideDialogInput = () => document.querySelector(".switch-report-puzzle input");
      domDialog({
        focus: ".apply",
        modal: true,
        easyClose: "clickOutside",
        htmlText: '<div><strong style="font-size:1.5em">Report multiple solutions</strong><br /><br /><p>You have found a puzzle with multiple solutions, report it?</p><br />' + hideButtonDiv + `<br /><br /><button type="reset" class="button button-empty button-red text reset" data-icon="${licon.X}">No</button><button type="submit" class="button button-green text apply" data-icon="${licon.Checkmark}">Yes</button>`
      }).then((dlg) => {
        $(".switch-report-puzzle", dlg.view).on("click", () => {
          const input = hideDialogInput();
          input.checked = !input.checked;
        });
        $(".reset", dlg.view).on("click", () => {
          if (hideDialogInput().checked) {
            this.tsHideReportDialog(Date.now());
          }
          dlg.close();
        });
        $(".apply", dlg.view).on("click", () => {
          report(puzzleId, reason);
          dlg.close();
        });
        dlg.show();
      });
    };
    this.tsHideReportDialog = storedIntProp("puzzle.report.hide.ts", 0);
  }
  // (?)take the eval as arg instead of taking it from the node to be sure it's the most up to date
  // All non-mates puzzle should have one and only one solution, if that is not the case, report it back to backend
  checkForMultipleSolutions(ev, ctrl2, threatMode) {
    var _a;
    if (!ctrl2.session.userId || this.reported || ctrl2.mode !== "view" || // Sometimes there is a race condition where a threat eval is sent, while `ctrl.threatMode()`
    // is not yet set to true. So we need to check for `threatMode` as well.
    ctrl2.threatMode() || threatMode || // the `mate` key theme is not sent, as it is considered redubant with `mateInX`
    ctrl2.data.puzzle.themes.some((t) => t.toLowerCase().includes("mate")) || // positions with 7 pieces or less can be checked with the tablebase
    pieceCount(ev.fen) <= 7 || !((_a = ctrl2.ceval.engines.active()) == null ? void 0 : _a.supportsPuzzleReport) || // if the user has chosen to hide the dialog less than a week ago
    this.tsHideReportDialog() > Date.now() - 1e3 * 3600 * 24 * 7)
      return;
    const node = ctrl2.node;
    const nodeTurn = fenColor(node.fen);
    if (nextMoveInSolution(node) && nodeTurn === ctrl2.pov && ctrl2.mainline.some((n) => n.id === node.id)) {
      const [bestEval, secondBestEval] = [ev.pvs[0], ev.pvs[1]];
      if (ev.depth >= 18 && (ev.depth > 50 || ev.nodes > 25e6) && bestEval && secondBestEval && // filter out incomplete searches
      bestEval.moves.length > 1 && secondBestEval.moves.length > 1 && winningChances_exports.hasMultipleSolutions(ctrl2.pov, bestEval, secondBestEval)) {
        this.evalsWithMultipleSolutions += 1;
      } else {
        this.evalsWithMultipleSolutions = 0;
      }
      if (this.evalsWithMultipleSolutions === 2) {
        this.reported = true;
        const engine = ctrl2.ceval.engines.active();
        const engineName = engine.short || engine.name;
        const reason = `(v${version}, ${engineName}) after move ${plyToTurn(node.ply)}. ${node.san}, at depth ${ev.depth}, multiple solutions:

${ev.pvs.map((pv) => `${pvEvalToStr(pv)}: ${pv.moves.join(" ")}`).join("\n\n")}`;
        this.reportDialog(ctrl2.data.puzzle.id, reason);
      }
    }
  }
};
var nextMoveInSolution = (before) => {
  const node = before.children[0];
  return node && (node.puzzle === "good" || node.puzzle === "win");
};
var pvEvalToStr = (pv) => {
  return pv.mate ? `#${pv.mate}` : `${pv.cp}`;
};

// ../puzzle/src/session.ts
var PuzzleSession = class {
  constructor(theme2, userId, streak) {
    this.theme = theme2;
    this.userId = userId;
    this.streak = streak;
    this.maxSize = 100;
    this.maxAge = 1e3 * 3600;
    this.default = () => ({
      theme: this.theme,
      rounds: [],
      at: Date.now()
    });
    this.store = this.streak ? prop(this.default()) : storedJsonProp(`puzzle.session.${this.userId || "anon"}`, this.default);
    this.clear = () => this.update((s) => ({ ...s, rounds: [] }));
    this.get = () => {
      const prev2 = this.store();
      return prev2.theme === this.theme && prev2.at > Date.now() - this.maxAge ? prev2 : this.default();
    };
    this.update = (f) => this.store(f(this.get()));
    this.complete = (id, result) => this.update((s) => {
      const i = s.rounds.findIndex((r) => r.id === id);
      if (i === -1) {
        s.rounds.push({ id, result });
        if (s.rounds.length > this.maxSize) s.rounds.shift();
      } else s.rounds[i].result = result;
      s.at = Date.now();
      return s;
    });
    this.setRatingDiff = (id, ratingDiff) => this.update((s) => {
      s.rounds.forEach((r) => {
        if (r.id === id) r.ratingDiff = ratingDiff;
      });
      return s;
    });
    this.isNew = () => this.store().rounds.length < 2;
  }
};

// ../puzzle/src/streak.ts
var PuzzleStreak = class {
  constructor(data) {
    this.fail = false;
    this.onComplete = (win, current) => {
      if (win) {
        if (this.nextId()) {
          this.data.index++;
          if (current)
            this.data.current = {
              puzzle: current.puzzle,
              game: current.game
            };
          this.store(this.data);
        } else {
          this.store(null);
          site.reload();
        }
      } else {
        this.fail = true;
        this.store(null);
      }
    };
    this.nextId = () => this.data.ids[this.data.index + 1];
    this.skip = () => {
      this.data.skip = false;
      this.store(this.data);
    };
    this.store = storedJsonProp(`puzzle.streak.${myUserId() || "anon"}`, () => null);
    this.data = this.store() || {
      ids: data.streak.split(" "),
      index: 0,
      skip: true,
      current: {
        puzzle: data.puzzle,
        game: data.game
      }
    };
  }
};

// ../puzzle/src/ctrl.ts
var PuzzleCtrl = class {
  constructor(opts, redraw) {
    this.opts = opts;
    this.redraw = redraw;
    this.next = defer();
    this.ground = prop(void 0);
    this.threatMode = toggle(false);
    this.streakFailStorage = storage.make("puzzle.streak.fail");
    this.flipped = toggle(false);
    this.canViewSolution = toggle(false);
    this.showHint = toggle(false);
    this.hintHasBeenShown = toggle(false);
    this.cgVersion = 0;
    this.loadSound = (name, volume) => {
      site.sound.load(name, site.sound.url(`${name}.mp3`));
      return () => site.sound.play(name, volume);
    };
    this.sound = {
      good: this.loadSound("lisp/PuzzleStormGood", 0.7),
      end: this.loadSound("lisp/PuzzleStormEnd", 1)
    };
    this.setPath = (path) => {
      this.path = path;
      this.nodeList = this.tree.getNodeList(path);
      this.node = ops_exports.last(this.nodeList);
      this.mainline = ops_exports.mainlineNodeList(this.tree.root);
      this.showHint(false);
    };
    this.setChessground = (cg) => {
      this.ground(cg);
      const makeRoot = () => ({
        data: {
          game: { variant: { key: "standard" } },
          player: { color: this.pov }
        },
        pluginMove: this.pluginMove,
        redraw: this.redraw,
        flipNow: this.flip,
        userJumpPlyDelta: this.userJumpPlyDelta,
        nextPuzzle: this.nextPuzzle,
        vote: this.vote,
        solve: this.viewSolution,
        blindfold: this.blindfold
      });
      const up = { fen: this.node.fen, canMove: true, cg };
      if (this.opts.pref.voiceMove) {
        if (this.voiceMove) this.voiceMove.update(up);
        else this.voiceMove = makeVoiceMove(makeRoot(), up);
      }
      if (this.opts.pref.keyboardMove) {
        if (!this.keyboardMove) this.keyboardMove = ctrl(makeRoot());
        this.keyboardMove.update(up);
      }
      requestAnimationFrame(() => this.redraw());
      this.googlyEyesAuto();
    };
    this.googlyEyesStart = () => {
      if (!this.googlyEyes)
        this.withGround((cg) => {
          site.asset.loadEsm("bits.googlyHorsey", {
            init: { cg, redraw: this.setAutoShapes }
          }).then(({ makeGooglyShapes }) => {
            this.googlyEyes = makeGooglyShapes;
            this.setAutoShapes();
          });
        });
    };
    this.googlyEyesAuto = () => {
      if (this.isDaily && (/* @__PURE__ */ new Date()).getMonth() === 3 && (/* @__PURE__ */ new Date()).getDate() === 1) this.googlyEyesStart();
    };
    this.pref = this.opts.pref;
    this.withGround = (f) => {
      const g = this.ground();
      return g ? f(g) : void 0;
    };
    this.initiate = (fromData) => {
      this.data = fromData;
      this.tree = makeTree(pgnToTree(this.data.game.pgn.split(" ")));
      const initialPath = path_exports.fromNodeList(ops_exports.mainlineNodeList(this.tree.root));
      this.mode = "play";
      this.next = defer();
      this.round = void 0;
      this.resultSent = false;
      this.lastFeedback = "init";
      this.initialPath = initialPath;
      this.initialNode = this.tree.nodeAtPath(initialPath);
      this.pov = plyColor(this.initialNode.ply);
      this.isDaily = !!this.data.isDaily;
      this.hintHasBeenShown(false);
      this.canViewSolution(false);
      this.report = new Report();
      this.voted = void 0;
      this.setPath(site.blindMode ? initialPath : path_exports.init(initialPath));
      setTimeout(
        () => {
          this.jump(initialPath);
          this.redraw();
        },
        this.opts.pref.animation.duration > 0 ? 500 : 0
      );
      setTimeout(
        () => {
          this.canViewSolution(true);
          this.redraw();
        },
        this.rated() ? 4e3 : 2e3
      );
      this.cgVersion++;
    };
    this.position = () => {
      const setup = parseFen(this.node.fen).unwrap();
      return Chess.fromSetup(setup).unwrap();
    };
    this.makeCgOpts = () => {
      const node = this.node;
      const color = plyColor(node.ply);
      const dests = chessgroundDests(this.position());
      const nextNode = this.node.children[0];
      const canMove = this.mode === "view" || color === this.pov && (!nextNode || nextNode.puzzle === "fail");
      const movable = canMove ? {
        color: dests.size > 0 ? color : void 0,
        dests
      } : {
        color: void 0,
        dests: /* @__PURE__ */ new Map()
      };
      const config2 = {
        fen: node.fen,
        orientation: this.flipped() ? opposite(this.pov) : this.pov,
        turnColor: color,
        movable,
        premovable: {
          enabled: false
        },
        check: node.check(),
        lastMove: uciToMove(node.uci)
      };
      if (node.ply >= this.initialNode.ply) {
        if (this.mode !== "view" && color !== this.pov && !nextNode) {
          config2.movable.color = this.pov;
          config2.premovable.enabled = true;
        }
      }
      this.cgConfig = config2;
      return config2;
    };
    this.showGround = (g) => {
      g.set(this.makeCgOpts());
      this.setAutoShapes();
    };
    this.pluginMove = (orig, dest, role) => {
      if (role) this.playUserMove(orig, dest, role);
      else
        this.withGround((g) => {
          g.move(orig, dest);
          g.state.movable.dests = void 0;
          g.state.turnColor = opposite(g.state.turnColor);
        });
    };
    this.pluginUpdate = (fen) => {
      var _a, _b;
      (_a = this.voiceMove) == null ? void 0 : _a.update({ fen, canMove: true });
      (_b = this.keyboardMove) == null ? void 0 : _b.update({ fen, canMove: true });
    };
    this.userMove = (orig, dest) => {
      var _a;
      const isPromoting = this.promotion.start(orig, dest, {
        submit: this.playUserMove,
        show: (_a = this.voiceMove) == null ? void 0 : _a.promotionHook()
      });
      if (!isPromoting) this.playUserMove(orig, dest);
      this.pluginUpdate(this.node.fen);
    };
    this.playUci = (uci) => this.sendMove(parseUci(uci));
    this.playUciList = (uciList) => uciList.forEach(this.playUci);
    this.playUserMove = (orig, dest, promotion) => this.sendMove({
      from: parseSquare(orig),
      to: parseSquare(dest),
      promotion
    });
    this.sendMove = (move) => this.sendMoveAt(this.path, this.position(), move);
    this.sendMoveAt = (path, pos, move) => {
      move = normalizeMove(pos, move);
      const san = makeSanAndPlay(pos, move);
      this.addNode(
        completeNode("standard")({
          ply: 2 * (pos.fullmoves - 1) + (pos.turn === "white" ? 0 : 1),
          fen: makeFen(pos.toSetup()),
          uci: makeUci(move),
          san,
          pos: () => Result.ok(pos)
        }),
        path
      );
    };
    this.addNode = (node, path) => {
      const newPath = this.tree.addNode(node, path);
      this.jump(newPath);
      this.withGround((g) => g.playPremove());
      const progress = moveTest(this);
      this.setAutoShapes();
      if (progress === "fail") site.sound.say(i18n.puzzle.failed);
      if (progress) this.applyProgress(progress);
      this.reorderChildren(path);
      this.redraw();
    };
    this.reorderChildren = (path, recursive) => {
      const node = this.tree.nodeAtPath(path);
      node.children.sort((c1, _) => {
        const p = c1.puzzle;
        if (p === "fail") return 1;
        if (p === "good" || p === "win") return -1;
        return 0;
      });
      if (recursive) node.children.forEach((child) => this.reorderChildren(path + child.id, true));
    };
    this.instantRevertUserMove = () => {
      this.withGround((g) => {
        g.cancelPremove();
        g.selectSquare(null);
      });
      this.jump(path_exports.init(this.path));
      this.redraw();
    };
    this.revertUserMove = () => {
      if (site.blindMode) this.instantRevertUserMove();
      else setTimeout(this.instantRevertUserMove, 300);
    };
    this.applyProgress = (progress) => {
      if (progress === "fail") {
        this.lastFeedback = "fail";
        this.revertUserMove();
        if (this.mode === "play") {
          if (this.streak) {
            this.failStreak(this.streak);
            this.streakFailStorage.fire();
          } else {
            this.canViewSolution(true);
            this.mode = "try";
            this.sendResult(false);
          }
        }
      } else if (progress === "win") {
        if (this.streak) this.sound.good();
        this.lastFeedback = "win";
        if (this.mode !== "view") {
          const sent = this.mode === "play" ? this.sendResult(true) : Promise.resolve();
          this.mode = "view";
          this.withGround(this.showGround);
          sent.then((_) => this.autoNext() ? this.nextPuzzle() : this.startCeval());
        }
      } else if (progress) {
        this.lastFeedback = "good";
        setTimeout(
          () => {
            const pos = Chess.fromSetup(parseFen(progress.fen).unwrap()).unwrap();
            this.sendMoveAt(progress.path, pos, progress.move);
          },
          this.opts.pref.animation.duration * (this.autoNext() ? 1 : 1.5)
        );
      }
    };
    this.failStreak = (streak) => {
      this.mode = "view";
      streak.onComplete(false);
      setTimeout(this.viewSolution, 500);
      this.sound.end();
    };
    this.sendResult = async (win) => {
      var _a;
      if (this.resultSent) return Promise.resolve();
      this.resultSent = true;
      this.session.complete(this.data.puzzle.id, win);
      const res = await complete(
        this.data.puzzle.id,
        this.data.angle.key,
        win,
        this.rated() && !this.hintHasBeenShown(),
        this.data.replay,
        this.streak,
        this.opts.settings.color
      );
      const next2 = res.next;
      if ((next2 == null ? void 0 : next2.user) && this.data.user) {
        this.data.user.rating = next2.user.rating;
        this.data.user.provisional = next2.user.provisional;
        this.round = res.round;
        if ((_a = res.round) == null ? void 0 : _a.ratingDiff) this.session.setRatingDiff(this.data.puzzle.id, res.round.ratingDiff);
      }
      if (win) site.sound.say(i18n.puzzle.puzzleSuccess);
      if (next2) {
        this.next.resolve(this.data.replay && res.replayComplete ? this.data.replay : next2);
        if (this.streak && win) this.streak.onComplete(true, res.next);
      }
      this.redraw();
      if (!next2 && !this.data.replay) {
        await alert("No more puzzles available! Try another theme.");
        site.redirect("/training/themes");
      }
    };
    this.isPuzzleData = (d) => "puzzle" in d;
    this.nextPuzzle = () => {
      if (this.streak && this.lastFeedback !== "win") {
        if (this.lastFeedback === "fail") site.redirect(this.routerWithLang("/streak"));
        return;
      }
      if (this.mode !== "view") return;
      this.ceval.reset();
      this.next.promise.then((n) => {
        if (this.isPuzzleData(n)) {
          this.initiate(n);
          this.redraw();
        }
      });
      if (this.data.replay && this.round === void 0) {
        site.redirect(`/training/dashboard/${this.data.replay.days}`);
      }
      if (!this.streak && !this.data.replay) {
        const path = this.routerWithLang(`/training/${this.data.angle.key}`);
        if (location.pathname !== path) history.replaceState(null, "", path);
      }
    };
    this.setAutoShapes = () => this.withGround(
      (g) => g.setAutoShapes(
        autoShape_default({
          ...this,
          node: this.node,
          hint: this.hintSquare()
        })
      )
    );
    this.hintSquare = () => {
      const hint = this.showHint() ? nextCorrectMove(this) : void 0;
      return hint == null ? void 0 : hint.from;
    };
    this.isCevalAllowed = () => this.mode === "view";
    this.startCeval = () => {
      if (this.cevalEnabled()) this.doStartCeval();
    };
    this.doStartCeval = throttle(800, () => {
      this.ceval.reset();
      this.ceval.start(this.path, this.nodeList, this.data.puzzle.id, this.threatMode());
    });
    this.nextNodeBest = () => ops_exports.withMainlineChild(this.node, (n) => {
      var _a;
      return (_a = n.eval) == null ? void 0 : _a.best;
    });
    this.cevalEnabledProp = storedBooleanProp("engine.enabled", false);
    this.cevalEnabled = (enable) => {
      if (enable === void 0) return this.cevalEnabledProp() && this.isCevalAllowed();
      this.cevalEnabledProp(enable);
      if (enable && this.isCevalAllowed()) this.startCeval();
      else {
        this.threatMode(false);
        this.ceval.reset();
      }
      this.autoScrollRequested = true;
      this.setAutoShapes();
      this.ceval.showEnginePrefs(false);
      this.redraw();
      return enable;
    };
    this.toggleThreatMode = () => {
      if (this.node.check()) return;
      if (!this.cevalEnabled()) return;
      this.threatMode.toggle();
      this.setAutoShapes();
      this.startCeval();
      this.redraw();
    };
    this.outcome = () => this.position().outcome();
    this.jump = (path) => {
      const pathChanged = path !== this.path, isForwardStep = pathChanged && path.length === this.path.length + 2;
      this.setPath(path);
      this.withGround(this.showGround);
      if (pathChanged) {
        if (isForwardStep) {
          site.sound.saySan(this.node.san);
          site.sound.move(this.node);
        }
        this.threatMode(false);
        this.ceval.reset();
        this.startCeval();
      }
      this.promotion.cancel();
      this.autoScrollRequested = true;
      this.pluginUpdate(this.node.fen);
      pubsub.emit("ply", this.node.ply);
    };
    this.userJump = (path) => {
      var _a;
      if (((_a = this.tree.nodeAtPath(path)) == null ? void 0 : _a.puzzle) === "fail" && this.mode !== "view") return;
      this.withGround((g) => g.selectSquare(null));
      this.jump(path);
    };
    this.userJumpPlyDelta = (plyDelta) => {
      var _a;
      let maxValidPly = this.mainline.length - 1;
      if (((_a = last(this.mainline)) == null ? void 0 : _a.puzzle) === "fail" && this.mode !== "view") maxValidPly -= 1;
      const newPly = Math.min(Math.max(this.node.ply + plyDelta, 0), maxValidPly);
      this.userJump(path_exports.fromNodeList(this.mainline.slice(0, newPly + 1)));
    };
    this.toggleHint = () => {
      if (!this.showHint()) {
        this.hintHasBeenShown(true);
        this.userJump(path_exports.fromNodeList(this.mainline.filter((node) => node.puzzle !== "fail")));
      }
      this.showHint.toggle();
      this.setAutoShapes();
      const hint = this.hintSquare();
      this.withGround((g) => g.selectSquare(hint ? makeSquare(hint) : null));
      this.redraw();
    };
    this.viewSolution = () => {
      this.sendResult(false);
      this.mode = "view";
      mergeSolution(this.tree, this.initialPath, this.data.puzzle.solution, this.pov);
      this.reorderChildren(this.initialPath, true);
      const next2 = this.node.children[0];
      if ((next2 == null ? void 0 : next2.puzzle) === "good") this.userJump(this.path + next2.id);
      else {
        const firstGoodPath = ops_exports.takePathWhile(this.mainline, (node) => node.puzzle !== "good");
        if (firstGoodPath) this.userJump(firstGoodPath + this.tree.nodeAtPath(firstGoodPath).children[0].id);
      }
      this.autoScrollRequested = true;
      this.redraw();
      this.startCeval();
    };
    this.skip = () => {
      if (!this.streak || !this.streak.data.skip || this.mode !== "play") return;
      this.streak.skip();
      this.userJump(path_exports.fromNodeList(this.mainline));
      const moveIndex = path_exports.size(this.path) - path_exports.size(this.initialPath);
      const solution = this.data.puzzle.solution[moveIndex];
      this.playUci(solution);
      this.playBestMove();
    };
    this.flip = () => {
      this.flipped.toggle();
      this.cgVersion++;
      this.withGround((g) => g.toggleOrientation());
      this.redraw();
    };
    this.vote = (v) => {
      vote(this.data.puzzle.id, v);
      this.voted = this.voted === v ? void 0 : v;
      this.redraw();
    };
    this.voteTheme = (theme2, v) => {
      if (this.round) {
        this.round.themes = this.round.themes || {};
        if (v === this.round.themes[theme2]) {
          delete this.round.themes[theme2];
          voteTheme(this.data.puzzle.id, theme2, void 0);
        } else {
          if (v || this.data.puzzle.themes.includes(theme2)) this.round.themes[theme2] = v;
          else delete this.round.themes[theme2];
          voteTheme(this.data.puzzle.id, theme2, v);
        }
        this.redraw();
      }
    };
    this.blindfold = (v) => {
      if (v !== void 0 && v !== this.blindfolded()) {
        this.blindfolded(v);
        this.redraw();
      }
      return this.blindfolded();
    };
    this.playBestMove = () => {
      var _a;
      const uci = this.nextNodeBest() || ((_a = this.node.ceval) == null ? void 0 : _a.pvs[0].moves[0]);
      if (uci) this.playUci(uci);
    };
    this.autoNexting = () => this.lastFeedback === "win" && this.autoNext();
    this.showEvalGauge = () => this.showEvaluation() && this.isCevalAllowed() && !this.outcome();
    this.getOrientation = () => this.withGround((g) => g.state.orientation);
    this.allThemes = this.opts.themes && {
      dynamic: this.opts.themes.dynamic.split(" "),
      static: new Set(this.opts.themes.static.split(" "))
    };
    this.toggleRated = () => this.rated(!this.rated());
    this.getCeval = () => this.ceval;
    this.ongoing = false;
    this.getNode = () => this.node;
    this.showEvaluation = () => this.mode === "view";
    this.routerWithLang = (path) => {
      if (document.body.hasAttribute("data-user")) return path;
      const language = document.documentElement.lang.slice(0, 2);
      return language === "en" ? path : `/${language}${path}`;
    };
    var _a;
    this.rated = storedBooleanPropWithEffect("puzzle.rated", true, this.redraw);
    this.autoNext = storedBooleanProp(
      `puzzle.autoNext${opts.data.streak ? ".streak" : ""}`,
      !!opts.data.streak
    );
    this.blindfolded = storedBooleanProp(`puzzle.${myUserId() || "anon"}.blindfolded`, false);
    this.streak = opts.data.streak ? new PuzzleStreak(opts.data) : void 0;
    if (this.streak) {
      opts.data = { ...opts.data, ...this.streak.data.current };
      this.streakFailStorage.listen((_) => this.failStreak(this.streak));
    }
    this.session = new PuzzleSession(opts.data.angle.key, myUserId(), !!opts.data.streak);
    this.menu = toggle(false, redraw);
    this.initiate(opts.data);
    this.promotion = new PromotionCtrl(
      this.withGround,
      () => this.withGround((g) => g.set(this.cgConfig)),
      redraw
    );
    this.ceval = new CevalCtrl({
      redraw: this.redraw,
      variant: {
        short: "Std",
        name: "Standard",
        key: "standard"
      },
      externalEngines: ((_a = this.data.externalEngines) == null ? void 0 : _a.map((engine) => ({
        ...engine,
        endpoint: this.opts.externalEngineEndpoint
      }))) || [],
      initialFen: void 0,
      // always standard starting position
      emit: (ev, meta) => {
        if (!ev) {
          this.cevalEnabled(false);
        } else {
          this.tree.updateAt(meta.path, (node) => {
            if (meta.threatMode) {
              const threat = ev;
              if (!node.threat || node.threat.depth <= threat.depth) node.threat = threat;
            } else if (!node.ceval || node.ceval.depth <= ev.depth) node.ceval = ev;
            if (meta.path === this.path) {
              this.report.checkForMultipleSolutions(ev, this, meta.threatMode);
              this.setAutoShapes();
              this.redraw();
            }
          });
        }
      },
      onUciHover: this.setAutoShapes
    });
    this.keyboardHelp = propWithEffect(location.hash === "#keyboard", this.redraw);
    keyboard_default(this);
    this.report = new Report();
    document.addEventListener(
      "visibilitychange",
      () => requestIdleCallbackSafe(() => this.jump(this.path), 500)
    );
    pubsub.on("board.change", (is3d) => {
      this.withGround((g) => {
        g.state.addPieceZIndex = is3d;
        g.redrawAll();
      });
      this.setAutoShapes();
    });
    pubsub.on("zen", toggleZenMode);
    $("body").addClass("playing");
    $("#zentog").on("click", () => pubsub.emit("zen"));
    window.lichess.puzzle = {
      playUci: (uci) => this.sendMove(parseUci(uci))
    };
    window.lichess.chessground = this.ground;
  }
  clearCeval() {
    this.tree.removeCeval();
    this.ceval.reset();
    this.startCeval();
    this.redraw();
  }
};

// ../puzzle/src/view/boardMenu.ts
function boardMenu_default(ctrl2) {
  return boardMenu(ctrl2.redraw, ctrl2.menu, (menu) => [
    h("section", [
      menu.flip(i18n.site.flipBoard, ctrl2.flipped(), () => {
        ctrl2.flip();
        ctrl2.menu.toggle();
      })
    ]),
    h("section", [
      menu.zenMode(true),
      menu.blindfold(
        toggle(ctrl2.blindfold(), (v) => ctrl2.blindfold(v)),
        true
      ),
      menu.voiceInput(boolPrefXhrToggle("voice", !!ctrl2.voiceMove), true),
      menu.keyboardInput(boolPrefXhrToggle("keyboardMove", !!ctrl2.keyboardMove), true)
    ]),
    studyButton(ctrl2),
    h("section.board-menu__links", [
      h(
        "a.text",
        { attrs: { target: "_blank", href: "/account/preferences/display", "data-icon": licon.Gear } },
        i18n.preferences.display
      )
    ])
  ]);
}
var hiddenInput = (name, value) => h("input", { attrs: { type: "hidden", name, value } });
function renderPgnInput(ctrl2) {
  const puzURL = `${location.origin}/training/${ctrl2.data.puzzle.id}`;
  const tags = [
    ["Site", puzURL],
    ["FEN", ctrl2.initialNode.fen]
  ].map(([k, v]) => `[${k} "${v}"]
`).join("");
  const linkPreamble = ` {${puzURL}} `;
  return tags + linkPreamble + renderNodesTxt(ctrl2.initialNode, true);
}
function studyButton(ctrl2) {
  if (ctrl2.mode === "play") return void 0;
  return h(
    "section.board-menu__study",
    hl("form", { attrs: { action: "/study/as", method: "post", target: "_blank" } }, [
      hiddenInput("pgn", renderPgnInput(ctrl2)),
      hiddenInput("fen", ctrl2.initialNode.fen),
      hiddenInput("orientation", ctrl2.pov),
      hiddenInput("mode", "gamebook"),
      hl(
        "button.button.text",
        { attrs: { type: "submit", "data-icon": licon.StudyBoard } },
        i18n.site.toStudy
      )
    ])
  );
}

// ../puzzle/src/view/after.ts
var renderVote = (ctrl2) => {
  var _a;
  if (!ctrl2.data.user) return null;
  if (ctrl2.autoNexting()) return div(".puzzle__vote");
  return div(".puzzle__vote", [
    ctrl2.session.isNew() && ((_a = ctrl2.data.user) == null ? void 0 : _a.provisional) ? div(".puzzle__vote__help", i18n.puzzle.didYouLikeThisPuzzle) : null,
    div(".puzzle__vote__buttons", [
      button(".button.button-empty.vote-up", {
        class: { active: ctrl2.voted === true },
        title: i18n.puzzle.upVote,
        hook: bind("click", () => ctrl2.vote(true))
      }),
      button(".button.button-empty.vote-down", {
        class: { active: ctrl2.voted === false },
        title: i18n.puzzle.downVote,
        hook: bind("click", () => ctrl2.vote(false))
      })
    ])
  ]);
};
var renderStreak = (ctrl2) => {
  var _a, _b;
  return [
    div(".complete", [
      span(".game-over", i18n.site.gameOver),
      span(i18n.puzzle.yourStreakX.asArray(strong((_b = (_a = ctrl2.streak) == null ? void 0 : _a.data.index) != null ? _b : 0)))
    ]),
    a(ctrl2.routerWithLang("/streak"))(".continue", [icon(licon.PlayTriangle)(), i18n.puzzle.newStreak])
  ];
};
function after_default(ctrl2) {
  var _a;
  const win = ctrl2.lastFeedback === "win";
  const canPlayComputer = !((_a = ctrl2.node.san) == null ? void 0 : _a.includes("#"));
  return div(
    ".puzzle__feedback.after",
    ctrl2.streak && !win ? renderStreak(ctrl2) : [
      div(".complete", i18n.puzzle[win ? "puzzleSuccess" : "puzzleComplete"]),
      button(".continue", { hook: bind("click", ctrl2.nextPuzzle) }, [
        icon(licon.PlayTriangle)(),
        i18n.puzzle[ctrl2.streak ? "continueTheStreak" : "continueTraining"]
      ]),
      div(".puzzle__more", [
        canPlayComputer ? a(`/analysis/${ctrl2.node.fen.replace(/ /g, "_")}?color=${ctrl2.pov}#practice`)(
          ".practice.button.button-empty",
          {
            "data-icon": licon.Bullseye,
            title: i18n.site.playAgainstComputer,
            target: "_blank"
          }
        ) : null,
        renderVote(ctrl2)
      ])
    ]
  );
}

// ../puzzle/src/view/feedback.ts
var viewSolution = (ctrl2) => {
  var _a;
  return ctrl2.streak ? h("div.view_solution.skip", { class: { show: (_a = ctrl2.streak) == null ? void 0 : _a.data.skip } }, [
    requiresI18n(
      "storm",
      ctrl2.redraw,
      (cat) => h(
        "button.button.button-empty",
        { hook: bind("click", ctrl2.skip), attrs: { title: i18n.puzzle.streakSkipExplanation } },
        cat.skip
      )
    )
  ]) : h("div.view_solution", { class: { show: ctrl2.canViewSolution() } }, [
    ctrl2.mode !== "view" ? h(
      "button.button" + (ctrl2.showHint() ? "" : ".button-empty"),
      { hook: bind("click", ctrl2.toggleHint) },
      i18n.site.getAHint
    ) : void 0,
    h(
      "button.button.button-empty",
      { hook: bind("click", ctrl2.viewSolution) },
      i18n.site.viewTheSolution
    )
  ]);
};
var initial = (ctrl2) => h("div.puzzle__feedback.play", [
  h("div.player", [
    h("div.no-square", h("piece.king." + ctrl2.pov)),
    h("div.instruction", [
      h("strong", i18n.site.yourTurn),
      h("em", i18n.puzzle[ctrl2.pov === "white" ? "findTheBestMoveForWhite" : "findTheBestMoveForBlack"])
    ])
  ]),
  viewSolution(ctrl2)
]);
var good = (ctrl2) => h("div.puzzle__feedback.good", [
  h("div.player", [
    h("div.icon", "\u2713"),
    h("div.instruction", [h("strong", i18n.puzzle.bestMove), h("em", i18n.puzzle.keepGoing)])
  ]),
  viewSolution(ctrl2)
]);
var fail = (ctrl2) => h("div.puzzle__feedback.fail", [
  h("div.player", [
    h("div.icon", "\u2717"),
    h("div.instruction", [h("strong", i18n.puzzle.notTheMove), h("em", i18n.puzzle.trySomethingElse)])
  ]),
  viewSolution(ctrl2)
]);
function feedback_default(ctrl2) {
  if (ctrl2.mode === "view") return after_default(ctrl2);
  switch (ctrl2.lastFeedback) {
    case "init":
      return initial(ctrl2);
    case "good":
      return good(ctrl2);
    case "fail":
      return fail(ctrl2);
  }
  return void 0;
}

// ../puzzle/src/view/tree.ts
var autoScroll = throttle(150, (ctrl2, el) => {
  const cont = el.parentNode;
  const target = el.querySelector(".active");
  if (!target) {
    cont.scrollTop = ctrl2.path === path_exports.root ? 0 : 99999;
    return;
  }
  const targetOffset = target.getBoundingClientRect().y - el.getBoundingClientRect().y;
  cont.scrollTop = targetOffset - cont.offsetHeight / 2 + target.offsetHeight;
});
function renderIndex(ply, withDots) {
  return hl("index", plyToTurn(ply) + (withDots ? ply % 2 === 1 ? "." : "..." : ""));
}
function renderChildrenOf(ctx, node, opts) {
  const cs = node.children;
  const main = cs[0];
  if (!main) return [];
  if (opts.isMainline) {
    const isWhite = main.ply % 2 === 1;
    if (!cs[1])
      return [
        isWhite && renderIndex(main.ply, false),
        renderMoveAndChildrenOf(ctx, main, { parentPath: opts.parentPath, isMainline: true })
      ];
    const mainChildren = renderChildrenOf(ctx, main, {
      parentPath: opts.parentPath + main.id,
      isMainline: true
    }), passOpts = { parentPath: opts.parentPath, isMainline: true };
    return [
      isWhite && renderIndex(main.ply, false),
      renderMoveOf(ctx, main, passOpts),
      isWhite && emptyMove(),
      hl("interrupt", renderLines(ctx, cs.slice(1), { parentPath: opts.parentPath, isMainline: true })),
      isWhite && mainChildren && [renderIndex(main.ply, false), emptyMove()],
      mainChildren
    ];
  }
  return cs[1] ? [renderLines(ctx, cs, opts)] : renderMoveAndChildrenOf(ctx, main, opts);
}
function renderLines(ctx, nodes, opts) {
  return hl(
    "lines",
    { class: { single: !!nodes[1] } },
    nodes.map(function(n) {
      return hl(
        "line",
        renderMoveAndChildrenOf(ctx, n, { parentPath: opts.parentPath, isMainline: false, withIndex: true })
      );
    })
  );
}
function renderMoveOf(ctx, node, opts) {
  return opts.isMainline ? renderMainlineMoveOf(ctx, node, opts) : renderVariationMoveOf(ctx, node, opts);
}
function renderMainlineMoveOf(ctx, node, opts) {
  const path = opts.parentPath + node.id;
  const classes = {
    active: path === ctx.ctrl.path,
    current: path === ctx.ctrl.initialPath,
    hist: node.ply < ctx.ctrl.initialNode.ply
  };
  if (node.puzzle) classes[node.puzzle] = true;
  return hl("move", { attrs: { p: path }, class: classes }, renderMove(node));
}
var renderGlyph = (glyph) => hl("glyph", { attrs: { title: glyph.name } }, glyph.symbol);
function puzzleGlyph(node) {
  switch (node.puzzle) {
    case "good":
    case "win":
      return renderGlyph({ name: i18n.puzzle.bestMove, symbol: "\u2713" });
    case "fail":
      return renderGlyph({
        name: "Puzzle failed",
        //puzzleFailed key never worked, it's in learn/*.xml
        symbol: "\u2717"
      });
    case "retry":
      return renderGlyph({ name: i18n.puzzle.goodMove, symbol: "?!" });
    default:
      return void 0;
  }
}
function renderMove(node) {
  const ev = node.eval || node.ceval;
  return [
    node.san,
    ev && (defined(ev.cp) ? renderEval2(renderEval(ev.cp)) : defined(ev.mate) && renderEval2("#" + ev.mate)),
    puzzleGlyph(node)
  ];
}
function renderVariationMoveOf(ctx, node, opts) {
  const path = opts.parentPath + node.id;
  const classes = { active: path === ctx.ctrl.path };
  if (node.puzzle) classes[node.puzzle] = true;
  const withIndex = opts.withIndex || node.ply % 2 === 1;
  return hl("move", { attrs: { p: path }, class: classes }, [
    withIndex && renderIndex(node.ply, true),
    node.san,
    puzzleGlyph(node)
  ]);
}
function renderMoveAndChildrenOf(ctx, node, opts) {
  return [
    renderMoveOf(ctx, node, opts),
    renderChildrenOf(ctx, node, { parentPath: opts.parentPath + node.id, isMainline: opts.isMainline })
  ];
}
function emptyMove() {
  return hl("move.empty", "...");
}
function renderEval2(e) {
  return hl("eval", e);
}
function eventPath(e) {
  const target = e.target;
  return target.getAttribute("p") || target.parentNode.getAttribute("p");
}
function render2(ctrl2) {
  const root = ctrl2.tree.root;
  const ctx = { ctrl: ctrl2, showComputer: false };
  return hl(
    "div.tview2.tview2-column",
    {
      hook: {
        ...onInsert((el) => {
          if (ctrl2.path !== path_exports.root) autoScroll(ctrl2, el);
          el.addEventListener("mousedown", (e) => {
            if (defined(e.button) && e.button !== 0) return;
            const path = eventPath(e);
            if (path) ctrl2.userJump(path);
            ctrl2.redraw();
          });
        }),
        postpatch: (_, vnode) => {
          if (ctrl2.autoScrollNow) {
            autoScroll(ctrl2, vnode.elm);
            ctrl2.autoScrollNow = false;
            ctrl2.autoScrollRequested = false;
          } else if (ctrl2.autoScrollRequested) {
            if (ctrl2.path !== path_exports.root) autoScroll(ctrl2, vnode.elm);
            ctrl2.autoScrollRequested = false;
          }
        }
      }
    },
    [
      root.ply % 2 === 1 && [renderIndex(root.ply, false), emptyMove()],
      renderChildrenOf(ctx, root, { parentPath: "", isMainline: true })
    ]
  );
}

// ../puzzle/src/view/main.ts
var renderAnalyse = (ctrl2) => hl("div.puzzle__moves.areplay", [render2(ctrl2)]);
function dataAct(e) {
  const target = e.target;
  return target.getAttribute("data-act") || target.parentNode.getAttribute("data-act");
}
function jumpButton(icon2, effect, disabled, glowing = false) {
  return hl("button.fbt", { class: { glowing }, attrs: { disabled, "data-act": effect, "data-icon": icon2 } });
}
function controls(ctrl2) {
  const node = ctrl2.node;
  const nextNode = node.children[0];
  const notOnLastMove = ctrl2.mode === "play" && nextNode && nextNode.puzzle !== "fail";
  return hl("div.puzzle__controls.analyse-controls", [
    hl(
      "div.jumps",
      {
        hook: onInsert(
          (el) => addPointerListeners(el, {
            click: (e) => {
              const action = dataAct(e);
              if (action === "prev") prev(ctrl2);
              else if (action === "next") next(ctrl2);
              else if (action === "first") first(ctrl2);
              else if (action === "last") last2(ctrl2);
              ctrl2.redraw();
            }
          })
        )
      },
      [
        jumpButton(licon.JumpFirst, "first", !node.ply),
        jumpButton(licon.JumpPrev, "prev", !node.ply),
        jumpButton(licon.JumpNext, "next", !nextNode),
        jumpButton(licon.JumpLast, "last", !nextNode, notOnLastMove),
        toggleButton(ctrl2.menu, i18n.site.menu)
      ]
    ),
    boardMenu_default(ctrl2)
  ]);
}
var cevalShown = false;
function main_default(ctrl2) {
  const gaugeOn = ctrl2.showEvalGauge();
  if (cevalShown !== ctrl2.showEvaluation()) {
    if (!cevalShown) ctrl2.autoScrollNow = true;
    cevalShown = ctrl2.showEvaluation();
  }
  return hl(
    `main.puzzle.puzzle-${ctrl2.data.replay ? "replay" : "play"}${ctrl2.streak ? ".puzzle--streak" : ""}`,
    {
      class: { "gauge-on": gaugeOn },
      hook: {
        postpatch(old, vnode) {
          if (old.data.gaugeOn !== gaugeOn) {
            if (ctrl2.pref.coords === Coords.Outside) {
              $("body").toggleClass("coords-in", gaugeOn).toggleClass("coords-out", !gaugeOn);
            }
            dispatchChessgroundResize();
          }
          vnode.data.gaugeOn = gaugeOn;
        }
      }
    },
    [
      renderBlindfoldToggle(ctrl2.blindfold),
      hl("aside.puzzle__side", [
        replay(ctrl2),
        puzzleBox(ctrl2),
        ctrl2.streak ? streakBox(ctrl2) : userBox(ctrl2),
        theme(ctrl2),
        config(ctrl2)
      ]),
      hl(
        "div.puzzle__board.main-board" + (ctrl2.blindfold() ? ".blindfold" : ""),
        {
          hook: "ontouchstart" in window || !storage.boolean("scrollMoves").getOrDefault(true) ? void 0 : bindNonPassive(
            "wheel",
            stepwiseScroll(
              (e) => {
                if (e.deltaY > 0) next(ctrl2);
                else if (e.deltaY < 0) prev(ctrl2);
                ctrl2.redraw();
              },
              (e) => !["PIECE", "SQUARE", "CG-BOARD"].includes(e.target.tagName)
            )
          )
        },
        [chessground_default(ctrl2), ctrl2.promotion.view()]
      ),
      main_exports.renderGauge(ctrl2),
      hl("div.puzzle__tools", [
        ctrl2.voiceMove ? renderVoiceBar(ctrl2.voiceMove.ctrl, ctrl2.redraw, "puz") : null,
        // we need the wrapping div here
        // so the siblings are only updated when ceval is added
        hl(
          "div.ceval-wrap",
          { class: { none: !ctrl2.showEvaluation() } },
          ctrl2.showEvaluation() ? [main_exports.renderCeval(ctrl2), main_exports.renderPvs(ctrl2)] : []
        ),
        renderAnalyse(ctrl2),
        feedback_default(ctrl2)
      ]),
      controls(ctrl2),
      ctrl2.keyboardMove && render(ctrl2.keyboardMove),
      session(ctrl2),
      ctrl2.keyboardHelp() && view(ctrl2)
    ]
  );
}
function session(ctrl2) {
  const rounds = ctrl2.session.get().rounds;
  if (!rounds.length) return void 0;
  const { id: currentId } = ctrl2.data.puzzle;
  const { theme: theme2 } = ctrl2.session;
  return hl("div.puzzle__session", [
    rounds.map(({ id, result, ratingDiff }) => {
      const rd = ratingDiff && ctrl2.opts.showRatings ? ratingDiff > 0 ? "+" + ratingDiff : ratingDiff : null;
      return h(
        `a.result-${result}`,
        {
          key: id,
          class: { current: currentId === id, "result-empty": !rd },
          attrs: {
            href: `/training/${theme2}/${id}`,
            ...ctrl2.streak ? { target: "_blank" } : {}
          }
        },
        rd
      );
    }),
    rounds.some((r) => r.id === currentId) ? !ctrl2.streak && hl("a.session-new", { key: "new", attrs: { href: `/training/${theme2}` } }) : hl(
      "a.result-cursor.current",
      {
        key: currentId,
        attrs: ctrl2.streak ? {} : { href: `/training/${theme2}/${currentId}` }
      },
      ctrl2.streak && (ctrl2.streak.data.index + 1).toString()
    )
  ]);
}

// ../puzzle/src/puzzle.ts
var patch = init([classModule, attributesModule]);
async function initModule(opts) {
  await site.asset.loadPieces;
  const element = document.querySelector("main.puzzle");
  const ctrl2 = new PuzzleCtrl(opts, redraw);
  const nvui = site.blindMode && await site.asset.loadEsm("puzzle.nvui", { init: ctrl2 });
  const render3 = nvui ? nvui.render : () => main_default(ctrl2);
  const blueprint = render3();
  element.innerHTML = "";
  let vnode = patch(element, blueprint);
  function redraw() {
    vnode = patch(vnode, render3());
  }
  menuHover_default();
}
export {
  initModule
};
//# sourceMappingURL=puzzle.X7366D4M.js.map
