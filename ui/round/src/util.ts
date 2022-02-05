import { h } from 'snabbdom';
import { VNodeData } from 'snabbdom/vnode';
import { Hooks } from 'snabbdom/hooks';
import * as cg from 'shogiground/types';
import { Redraw } from './interfaces';
import { parseSfen } from 'shogiops/sfen';
import { lishogiVariantRules, shogigroundDests, shogigroundDropDests } from 'shogiops/compat';
import { setupPosition } from 'shogiops/variant';

export function justIcon(icon: string): VNodeData {
  return {
    attrs: { 'data-icon': icon },
  };
}

export function usi2move(usi: string): cg.Key[] | undefined {
  if (!usi) return undefined;
  if (usi[1] === '*') return [usi.slice(2, 4) as cg.Key];
  return [usi.slice(0, 2), usi.slice(2, 4)] as cg.Key[];
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

export function getMoveDests(sfen: string, variant: VariantKey): cg.Dests {
  return parseSfen(sfen)
    .chain(s => setupPosition(lishogiVariantRules(variant), s, false))
    .unwrap(
      p => shogigroundDests(p),
      _ => new Map()
    );
}

export function getDropDests(sfen: string, variant: VariantKey): cg.DropDests {
  return parseSfen(sfen)
    .chain(s => setupPosition(lishogiVariantRules(variant), s, false))
    .unwrap(
      p => shogigroundDropDests(p),
      _ => new Map()
    );
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
