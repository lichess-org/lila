import { h } from "snabbdom";
import { VNode } from "snabbdom/vnode";

import { Redraw, Close, bind, header } from "./util";

type Key = string;

export type Sound = string[];

export interface SoundData {
  current: Key;
  list: Sound[];
}

export interface SoundCtrl {
  makeList(): Sound[];
  api: any;
  set(k: Key): void;
  volume(v: number): void;
  redraw: Redraw;
  trans: Trans;
  close: Close;
}

export function ctrl(
  raw: string[],
  trans: Trans,
  redraw: Redraw,
  close: Close
): SoundCtrl {
  const list: Sound[] = raw.map((s) => s.split(" "));

  const api = window.lishogi.sound;

  return {
    makeList() {
      const canSpeech = window.speechSynthesis?.getVoices().length;
      return list.filter((s) => s[0] != "speech" || canSpeech);
    },
    api,
    set(k: Key) {
      api.speech(k == "speech");
      window.lishogi.pubsub.emit("speech.enabled", api.speech());
      if (api.speech()) api.say("Speech synthesis ready");
      else {
        api.changeSet(k);
        // If we want to play move for all sets we need to get move sound for pentatonic
        if(k.includes("shogi"))
          api.move();
        else
          api.genericNotify();
        $.post("/pref/soundSet", { set: k }).fail(() =>
          window.lishogi.announce({ msg: "Failed to save sound preference" })
        );
      }
      redraw();
    },
    volume(v: number) {
      api.setVolume(v);
      // plays a move sound if speech is off
      api.move("knight F 7");
    },
    redraw,
    trans,
    close,
  };
}

export function view(ctrl: SoundCtrl): VNode {
  const current = ctrl.api.speech() ? "speech" : ctrl.api.set();

  return h(
    "div.sub.sound." + ctrl.api.set(),
    {
      hook: {
        insert() {
          if (window.speechSynthesis)
            window.speechSynthesis.onvoiceschanged = ctrl.redraw;
        },
      },
    },
    [
      header(ctrl.trans("sound"), ctrl.close),
      h("div.content", [
        h("div.slider", { hook: { insert: (vn) => makeSlider(ctrl, vn) } }),
        h("div.selector", ctrl.makeList().map(soundView(ctrl, current))),
      ]),
    ]
  );
}

function makeSlider(ctrl: SoundCtrl, vnode: VNode) {
  const setVolume = window.lishogi.debounce(ctrl.volume, 50);
  window.lishogi.slider().done(() => {
    $(vnode.elm as HTMLElement).slider({
      orientation: "vertical",
      min: 0,
      max: 1,
      range: "min",
      step: 0.01,
      value: ctrl.api.getVolume(),
      slide: (_: any, ui: any) => setVolume(ui.value),
    });
  });
}

function soundView(ctrl: SoundCtrl, current: Key) {
  return (s: Sound) =>
    h(
      "a.text",
      {
        hook: bind("click", () => ctrl.set(s[0])),
        class: { active: current === s[0] },
        attrs: { "data-icon": "E" },
      },
      s[1]
    );
}
