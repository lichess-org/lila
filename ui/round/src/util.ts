import { h } from 'snabbdom';
import { VNodeData } from 'snabbdom/vnode';
import { Hooks } from 'snabbdom/hooks';
import * as sg from 'shogiground/types';
import { Redraw } from './interfaces';
import { parseSfen } from 'shogiops/sfen';
import { shogigroundDests, shogigroundDropDests } from 'shogiops/compat';

export function justIcon(icon: string): VNodeData {
  return {
    attrs: { 'data-icon': icon },
  };
}

export function onInsert(f: (el: HTMLElement) => void): Hooks {
  return {
    insert(vnode) {
      f(vnode.elm as HTMLElement);
    },
  };
}

export function bind(eventName: string, f: (e: Event) => void, redraw?: Redraw, passive: boolean = true): Hooks {
  return onInsert(el => {
    el.addEventListener(
      eventName,
      !redraw
        ? f
        : e => {
            const res = f(e);
            redraw();
            return res;
          },
      { passive }
    );
  });
}

export function getMoveDests(sfen: string, variant: VariantKey): sg.Dests {
  return parseSfen(variant, sfen, false).unwrap(
    p => shogigroundDests(p),
    _ => new Map()
  ) as sg.Dests;
}

export function getDropDests(sfen: string, variant: VariantKey): sg.DropDests {
  return parseSfen(variant, sfen, false).unwrap(
    p => shogigroundDropDests(p),
    _ => new Map()
  ) as sg.DropDests;
}

export function spinner() {
  return h(
    'div.spinner',
    {
      'aria-label': 'loading',
    },
    [
      h('svg', { attrs: { viewBox: '0 0 40 40' } }, [
        h('circle', {
          attrs: { cx: 20, cy: 20, r: 18, fill: 'none' },
        }),
      ]),
    ]
  );
}
