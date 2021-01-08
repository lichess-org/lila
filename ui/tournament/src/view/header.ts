import { h } from "snabbdom";
import { VNode } from "snabbdom/vnode";
import TournamentController from "../ctrl";
import { dataIcon } from "./util";

function startClock(time) {
  return {
    insert: (vnode) => $(vnode.elm as HTMLElement).clock({ time: time }),
  };
}

const oneDayInSeconds = 60 * 60 * 24;

function hasFreq(freq, d) {
  return d.schedule && d.schedule.freq === freq;
}

function clock(d): VNode | undefined {
  if (d.isFinished) return;
  if (d.secondsToFinish)
    return h(
      "div.clock",
      {
        hook: startClock(d.secondsToFinish),
      },
      [h("div.time")]
    );
  if (d.secondsToStart) {
    if (d.secondsToStart > oneDayInSeconds)
      return h("div.clock", [
        h("time.timeago.shy", {
          attrs: {
            title: new Date(d.startsAt).toLocaleString(),
            datetime: Date.now() + d.secondsToStart * 1000,
          },
          hook: {
            insert(vnode) {
              (vnode.elm as HTMLElement).setAttribute(
                "datetime",
                "" + (Date.now() + d.secondsToStart * 1000)
              );
            },
          },
        }),
      ]);
    return h(
      "div.clock.clock-created",
      {
        hook: startClock(d.secondsToStart),
      },
      [h("span.shy", "Starting in"), h("span.time.text")]
    );
  }
}

function image(d): VNode | undefined {
  if (d.isFinished) return;
  if (hasFreq("shield", d) || hasFreq("marathon", d)) return;
  const s = d.spotlight;
  if (s && s.iconImg)
    return h("img.img", {
      attrs: { src: window.lishogi.assetUrl("images/" + s.iconImg) },
    });
  return h("i.img", {
    attrs: dataIcon((s && s.iconFont) || "g"),
  });
}

function title(ctrl: TournamentController) {
  const d = ctrl.data;
  if (hasFreq("marathon", d))
    return h("h1", [h("i.fire-trophy", "\\"), d.fullName]);
  if (hasFreq("shield", d))
    return h("h1", [
      h(
        "a.shield-trophy",
        {
          attrs: { href: "/tournament/shields" },
        },
        d.perf.icon
      ),
      d.fullName,
    ]);
  return h(
    "h1",
    (d.animal
      ? [
          h(
            "a",
            {
              attrs: {
                href: d.animal.url,
                target: "_blank",
              },
            },
            d.animal.name
          ),
          " Arena",
        ]
      : [d.fullName]
    ).concat(d.private ? [" ", h("span", { attrs: dataIcon("a") })] : [])
  );
}

export default function (ctrl: TournamentController): VNode {
  return h("div.tour__main__header", [
    image(ctrl.data),
    title(ctrl),
    clock(ctrl.data),
  ]);
}
