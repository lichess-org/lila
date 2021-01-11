import { h } from "snabbdom";
import { VNode } from "snabbdom/vnode";
import { MouchEvent, NumberPair, Role } from "shogiground/types";
import { dragNewPiece } from "shogiground/drag";
import { eventPosition, opposite } from "shogiground/util";

import EditorCtrl from "./ctrl";
import shogiground from "./shogiground";
import { displaySfen, undisplaySfen, switchColorSfen } from "shogiutil/util";
import { OpeningPosition, Selected, EditorState } from "./interfaces";
// @ts-ignore
import { Shogi } from "shogiutil/vendor/Shogi.js";


function pocket(ctrl: EditorCtrl, c: Color): VNode {
  return h(
    `div.editorPocket.${c}`,
    {},
    Object.keys(ctrl.pockets[c === "white" ? 0 : 1]).map(
        (r) => {
          const nb = ctrl.pockets[c === "white" ? 0 : 1][r];
          return h(
            "div.no-square",
            {
              on: {
                mousedown: dragFromPocket(ctrl, [c, r as Role], nb, "mouseup"),
                touchstart: dragFromPocket(ctrl, [c, r as Role], nb, "touchend"),
                click: (e) => {
                  e.preventDefault();
                  ctrl.addToPocket(c, r as Role);
                }
              },
            },
            [
              h("div",
                h(
                "piece",
                {
                  attrs: {
                    class: c + " " + r,
                    "data-role": r,
                    "data-color": c,
                    "data-nb": nb,
                  },
                },
                []
            ),
            )
            ]
          );
        }
      )
  );
}

function dragFromPocket(
  ctrl: EditorCtrl,
  s: Selected,
  nb: number,
  upEvent: string,
): (e: MouchEvent) => void {
  return function(e: MouchEvent): void {
    e.preventDefault();
    if(s !== "pointer" && s !== "trash" && nb > 0){
      ctrl.removeFromPocket(s[0], s[1]);
      dragNewPiece(
        ctrl.shogiground!.state,
        {
          color: s[0],
          role: s[1],
        },
        e,
        true
      );
      document.addEventListener(
        upEvent,
        (e: MouchEvent) => {
          const eventPos = eventPosition(e) || lastTouchMovePos;
          const eventTarget = e.target as HTMLElement;
          if (eventPos && ctrl.shogiground!.getKeyAtDomPos(eventPos))
            ctrl.selected("pointer");
          // todo, this is ugly
          else if(eventTarget && (eventTarget.parentElement?.classList.contains("editorPocket") || eventTarget.parentElement?.parentElement?.classList.contains("editorPocket"))){
            if(eventTarget.parentElement?.classList.contains("white") || eventTarget.parentElement?.parentElement?.classList.contains("white"))
              ctrl.addToPocket("white", s[1]);
            else ctrl.addToPocket("black", s[1]);
          }
          else ctrl.selected(s);
          ctrl.redraw();
        },
        { once: true }
      );
    }
  }
}

function optgroup(name: string, opts: VNode[]): VNode {
  return h('optgroup', { attrs: { label: name } }, opts);
}

function studyButton(ctrl: EditorCtrl, state: EditorState): VNode {
  return h(
    "form",
    {
      attrs: {
        method: "post",
        action: "/study/as",
      },
    },
    [
      h("input", {
        attrs: {
          type: "hidden",
          name: "orientation",
          value: ctrl.bottomColor(),
        },
      }),
      //h('input', { attrs: { type: 'hidden', name: 'variant', value: ctrl.rules } }),
      h("input", {
        attrs: { type: "hidden", name: "fen", value: state.legalFen || "" },
      }),
      h(
        "button",
        {
          attrs: {
            type: "submit",
            "data-icon": "4",
            disabled: !state.legalFen,
          },
          class: {
            button: true,
            "button-empty": true,
            text: true,
            disabled: !state.legalFen,
          },
        },
        ctrl.trans.noarg("toStudy")
      ),
    ]
  );
}

function controls(ctrl: EditorCtrl, state: EditorState): VNode {
  const position2option = function(pos: OpeningPosition): VNode {
    return h('option', {
      attrs: {
        value: pos.epd || pos.fen,
        'data-fen': pos.fen,
      }
    }, pos.eco ? `${pos.eco} ${pos.name}` : pos.name);
  };
  return h("div.board-editor__tools", [
    pocket(ctrl, "black"),
    ...(ctrl.cfg.embed || !ctrl.cfg.positions ? [] : [h('div', [
      h('select.positions', {
        props: {
          value: state.fen.split(' ').slice(0, 4).join(' ')
        },
        on: {
          change(e) {
            const el = e.target as HTMLSelectElement;
            let value = el.selectedOptions[0].getAttribute('data-fen');
            if (value == 'prompt') value = (prompt('Paste FEN') || '').trim();
            if (!value || !ctrl.setFen(value)) el.value = '';
          }
        }
      }, [
        optgroup(ctrl.trans.noarg('setTheBoard'), [
          h('option', {
            attrs: {
              selected: true
            }
          }, `- ${ctrl.trans.noarg('boardEditor')}  -`),
          ...ctrl.extraPositions.map(position2option)
        ]),
        optgroup("Handicaps", ctrl.cfg.positions.map(position2option))
      ])
    ])]),
    h("div.metadata", [
      h(
        "div.color",
        h(
          "select",
          {
            on: {
              change(e) {
                ctrl.setTurn((e.target as HTMLSelectElement).value as Color);
              },
            },
          },
          ["blackPlays", "whitePlays"].map(function (key) {
            return h(
              "option",
              {
                attrs: {
                  value: key[0] == "w" ? "white" : "black",
                  selected: ctrl.turn[0] !== key[0], // ===
                },
              },
              ctrl.trans(key)
            );
          })
        )
      ),
    ]),
    ...(ctrl.cfg.embed
      ? [
          h("div.actions", [
            h(
              "a.button.button-empty",
              {
                on: {
                  click() {
                    ctrl.startPosition();
                  },
                },
              },
              ctrl.trans.noarg("startPosition")
            ),
            h(
              "a.button.button-empty",
              {
                on: {
                  click() {
                    ctrl.clearBoard();
                  },
                },
              },
              ctrl.trans.noarg("clearBoard")
            ),
          ]),
        ]
      : [
          h("div.actions", [
            h(
              "a.button.button-empty.text",
              {
                attrs: { "data-icon": "q" },
                on: {
                  click() {
                    ctrl.setFen("9/9/9/9/9/9/9/9/9");
                  },
                },
              },
              ctrl.trans.noarg("clearBoard")
            ),
            h(
              "a.button.button-empty.text",
              {
                attrs: { "data-icon": "B" },
                on: {
                  click() {
                    ctrl.shogiground!.toggleOrientation();
                  },
                },
              },
              ctrl.trans.noarg("flipBoard")
            ),
            h(
              "a",
              {
                attrs: {
                  "data-icon": "A",
                  rel: "nofollow",
                  ...(state.legalFen
                    ? { href: ctrl.makeAnalysisUrl(state.legalFen) }
                    : {}),
                },
                class: {
                  button: true,
                  "button-empty": true,
                  text: true,
                  disabled: !state.legalFen,
                },
              },
              ctrl.trans.noarg("analysis")
            ),
            h('a', {
              class: {
                button: true,
                'button-empty': true,
                disabled: !state.playable,
              },
              on: {
                click: () => {
                  if (state.playable) $.modal($('.continue-with'));
                }
              }
            },
            [h('span.text', { attrs: { 'data-icon': 'U' } }, ctrl.trans.noarg('continueFromHere'))]),
            studyButton(ctrl, state),
          ]),
          h("div.continue-with.none", [
            h(
              "a.button",
              {
                attrs: {
                  href: "/?fen=" + switchColorSfen(state.legalFen || "") + "#ai",
                  rel: "nofollow",
                },
              },
              ctrl.trans.noarg("playWithTheMachine")
            ),
            h(
              "a.button",
              {
                attrs: {
                  href: "/?fen=" + switchColorSfen(state.legalFen || "") + "#friend",
                  rel: "nofollow",
                },
              },
              ctrl.trans.noarg("playWithAFriend")
            ),
          ]),
        ]),
        pocket(ctrl, "white")
  ]);
}

function inputs(ctrl: EditorCtrl, fen: string): VNode | undefined {
  if (ctrl.cfg.embed) return;
  return h("div.copyables", [
    h("p", [
      h("strong", "SFEN"),
      h("input.copyable", {
        attrs: {
          spellcheck: false,
        },
        props: {
          value: displaySfen(fen),
        },
        on: {
          change(e) {
            const el = e.target as HTMLInputElement;
            ctrl.setFen(undisplaySfen(el.value.trim()));
            el.reportValidity();
          },
          input(e) {
            const el = e.target as HTMLInputElement;
            const gs = Shogi.init(undisplaySfen(el.value.trim()));
            el.setCustomValidity(gs.validity ? "" : "Invalid SFEN");
          },
          blur(e) {
            const el = e.target as HTMLInputElement;
            el.value = displaySfen(ctrl.getFen());
            el.setCustomValidity("");
          },
        },
      }),
    ]),
    h("p", [
      h("strong.name", "URL"),
      h("input.copyable.autoselect", {
        attrs: {
          readonly: true,
          spellcheck: false,
          value: ctrl.makeUrl(ctrl.cfg.baseUrl, fen),
        },
      }),
    ]),
  ]);
}

// can be 'pointer', 'trash', or [color, role]
function selectedToClass(s: Selected): string {
  return s === "pointer" || s === "trash" ? s : s.join(" ");
}

let lastTouchMovePos: NumberPair | undefined;

function sparePieces(
  ctrl: EditorCtrl,
  color: Color,
  _orientation: Color,
  position: "top" | "bottom"
): VNode {
  const selectedClass = selectedToClass(ctrl.selected());

  const pieces = [
    "king",
    "rook",
    "bishop",
    "gold",
    "silver",
    "knight",
    "lance",
    "pawn",
    "dragon",
    "horse",
    "promotedSilver",
    "promotedKnight",
    "promotedLance",
    "tokin",
  ].map(function (role) {
    return [color, role];
  });

  return h(
    "div",
    {
      attrs: {
        class: ["spare", "spare-" + position, "spare-" + color].join(" "),
      },
    },
    ["pointer", ...pieces, "trash"].map((s: Selected) => {
      const className = selectedToClass(s);
      const attrs = {
        class: className,
        ...(s !== "pointer" && s !== "trash"
          ? {
              "data-color": s[0],
              "data-role": s[1],
            }
          : {}),
      };
      const selectedSquare =
        selectedClass === className &&
        (!ctrl.shogiground ||
          !ctrl.shogiground.state.draggable.current ||
          !ctrl.shogiground.state.draggable.current.newPiece);
      return h(
        "div",
        {
          class: {
            "no-square": true,
            pointer: s === "pointer",
            trash: s === "trash",
            "selected-square": selectedSquare,
          },
          on: {
            mousedown: onSelectSparePiece(ctrl, s, "mouseup"),
            touchstart: onSelectSparePiece(ctrl, s, "touchend"),
            touchmove: (e) => {
              lastTouchMovePos = eventPosition(e as any);
            },
          },
        },
        [h("div", [h("piece", { attrs })])]
      );
    })
  );
}

function onSelectSparePiece(
  ctrl: EditorCtrl,
  s: Selected,
  upEvent: string
): (e: MouchEvent) => void {
  return function (e: MouchEvent): void {
    e.preventDefault();
    if (s === "pointer" || s === "trash") {
      ctrl.selected(s);
      ctrl.redraw();
    } else {
      ctrl.selected("pointer");

      dragNewPiece(
        ctrl.shogiground!.state,
        {
          color: s[0],
          role: s[1],
        },
        e,
        true
      );

      document.addEventListener(
        upEvent,
        (e: MouchEvent) => {
          const eventPos = eventPosition(e) || lastTouchMovePos;
          const eventTarget = e.target as HTMLElement;
          if (eventPos && ctrl.shogiground!.getKeyAtDomPos(eventPos))
            ctrl.selected("pointer");
          // todo, this is ugly
          else if(eventTarget && (eventTarget.parentElement?.classList.contains("editorPocket") || eventTarget.parentElement?.parentElement?.classList.contains("editorPocket"))){
            if(eventTarget.parentElement?.classList.contains("white") || eventTarget.parentElement?.parentElement?.classList.contains("white"))
              ctrl.addToPocket("white", s[1]);
            else ctrl.addToPocket("black", s[1]);
          }
          else ctrl.selected(s);
          ctrl.redraw();
        },
        { once: true }
      );
    }
  };
}

export default function (ctrl: EditorCtrl): VNode {
  const state = ctrl.getState();
  const color = ctrl.bottomColor();

  return h(
    "div.board-editor",
    [
      sparePieces(ctrl, opposite(color), color, "top"),
      h("div.main-board", [shogiground(ctrl)]),
      sparePieces(ctrl, color, color, "bottom"),
      controls(ctrl, state),
      inputs(ctrl, state.fen),
    ]
  );
}
