import { hl, type VNode } from './snabbdom';
import { clamp } from '@/algo';
import { storedMap } from '@/storage';
import { myUserId } from '@/index';

interface Opts {
  selector?: string; // selector for element to resize, defaults to the previous sibling
  key: string; // key to store the size (a generic category when id is present)
  id?: string; // optional id to store the size for a specific instance
  min: () => number;
  max: () => number;
  initialMaxHeight?: () => number;
  kid?: VNode;
}

type ResizerElement = HTMLElement & { observer: MutationObserver };

export function verticalResize(o: Opts): VNode {
  // add these directly after the vnode they resize
  return hl(
    'div.vertical-resize',
    {
      hook: {
        insert: vn => {
          const divider = vn.elm as ResizerElement;
          const onDomChange = () => {
            const el = o.selector
              ? document.querySelector<HTMLElement>(o.selector)!
              : (divider.previousElementSibling as HTMLElement);
            if (el.style.height) return;
            let height = o.id && heightStore(`${o.key}.${o.id}`);
            if (typeof height !== 'number') height = heightStore(o.key) ?? o.initialMaxHeight?.();
            if (typeof height !== 'number') height = el.getBoundingClientRect().height;
            el.style.flex = 'none';
            el.style.height = `${clamp(height, { min: o.min(), max: o.max() })}px`;
          };
          onDomChange();

          divider.observer = new MutationObserver(onDomChange);
          divider.observer.observe(divider.parentElement!, { childList: true });

          divider.addEventListener('pointerdown', down => {
            document.body.classList.add('prevent-select');
            divider.classList.add('is-dragging');

            const el = o.selector
              ? document.querySelector<HTMLElement>(o.selector)!
              : (divider.previousElementSibling as HTMLElement);
            const beginFrom = el.getBoundingClientRect().height - down.clientY;
            divider.setPointerCapture(down.pointerId);

            const move = (move: PointerEvent) => {
              el.style.height = `${clamp(beginFrom + move.clientY, { min: o.min(), max: o.max() })}px`;
            };

            const up = () => {
              document.body.classList.remove('prevent-select');
              divider.classList.remove('is-dragging');

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
        destroy: vn => (vn.elm as ResizerElement).observer?.disconnect(),
      },
    },
    [o.kid, hl('hr', { attrs: { role: 'separator' } })],
  );
}

const heightStore = storedMap<number | undefined>(
  `lib.view.verticalResize.height-store.${myUserId()}`,
  100,
  () => undefined,
);
