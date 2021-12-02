import { h } from 'snabbdom';
import { VNodeData } from 'snabbdom/vnode';
import { Hooks } from 'snabbdom/hooks';
import * as cg from 'shogiground/types';
import { Redraw, EncodedDests, Dests } from './interfaces';
import { parseFen } from 'shogiops/fen';
import { lishogiVariantRules, shogigroundDropDests } from 'shogiops/compat';
import { setupPosition } from 'shogiops/variant';

export function justIcon(icon: string): VNodeData {
  return {
    attrs: { 'data-icon': icon },
  };
}

export function uci2move(uci: string): cg.Key[] | undefined {
  if (!uci) return undefined;
  if (uci[1] === '*') return [uci.slice(2, 4) as cg.Key];
  return [uci.slice(0, 2), uci.slice(2, 4)] as cg.Key[];
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

export function parsePossibleMoves(dests?: EncodedDests): Dests {
  const dec = new Map();
  if (!dests) return dec;
  if (typeof dests == 'string')
    for (const ds of dests.split(' ')) {
      dec.set(ds.slice(0, 2), ds.slice(2).match(/.{2}/g) as cg.Key[]);
    }
  else for (const k in dests) dec.set(k, dests[k].match(/.{2}/g) as cg.Key[]);
  return dec;
}

export function getDropDests(fen: string, variant: VariantKey): cg.DropDests {
  return parseFen(fen)
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
