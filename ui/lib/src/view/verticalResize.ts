import { h, type VNode } from 'snabbdom';
import { clamp } from '@/algo';
import { storedMap } from '@/storage';
import { myUserId } from '@/common';

export function verticalResizeSeparator(o: {
  key: string; // key to store the size (a generic category when id is present)
  id?: string; // optional id to store the size for a specific instance
  min: () => number;
  max: () => number;
  initialMaxHeight?: number;
}): VNode {
  // add these directly after the vnode they resize
  return h(
    'div.vertical-resize-separator',
    {
      hook: {
        insert: vnode => {
          const divider = vnode.elm as HTMLElement;
          const onDomChange = () => {
            const el = divider.previousElementSibling as HTMLElement;
            if (el.style.height) return;
            let height = o.id && heightStore(`${o.key}.${o.id}`);
            if (typeof height !== 'number') height = heightStore(o.key) ?? o.initialMaxHeight;
            if (typeof height !== 'number') height = el.getBoundingClientRect().height;
            el.style.flex = 'none';
            el.style.height = `${clamp(height, { min: o.min(), max: o.max() })}px`;
          };
          onDomChange();

          new MutationObserver(onDomChange).observe(divider.parentElement!, { childList: true });

          divider.addEventListener('pointerdown', down => {
            const el = divider.previousElementSibling as HTMLElement;
            const beginFrom = el.getBoundingClientRect().height - down.clientY;
            divider.setPointerCapture(down.pointerId);

            const move = (move: PointerEvent) => {
              el.style.height = `${clamp(beginFrom + move.clientY, { min: o.min(), max: o.max() })}px`;
            };

            const up = () => {
              divider.releasePointerCapture(down.pointerId);
              window.removeEventListener('pointermove', move);
              window.removeEventListener('pointerup', up);
              window.removeEventListener('pointercancel', up);
              const height = parseInt(el.style.height);
              heightStore(o.key, height);
              if (o.id) heightStore(`${o.key}.${o.id}`, height);
            };
            window.addEventListener('pointermove', move);
            window.addEventListener('pointerup', up);
            window.addEventListener('pointercancel', up);
          });
        },
      },
    },
    [h('hr', { attrs: { role: 'separator' } })],
  );
}

const heightStore = storedMap<number | undefined>(
  `lib.controls.height-store.${myUserId()}`,
  100,
  () => undefined,
);
