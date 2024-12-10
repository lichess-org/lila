import { h, type VNode } from 'snabbdom';
import type { LichessStorage } from 'common/storage';

export interface Setting<A> {
  choices: Choices<A>;
  get(): A;
  set(v: A): A;
}

type Choice<A> = [A, string];
type Choices<A> = Array<Choice<A>>;

interface Opts<A> {
  choices: Choices<A>;
  default: A;
  storage: LichessStorage;
}

export function makeSetting<A>(opts: Opts<A>): Setting<A> {
  return {
    choices: opts.choices,
    get: () => (opts.storage.get() as A) || opts.default,
    set(v: A) {
      opts.storage.set(v);
      return v;
    },
  };
}

export function renderSetting<A>(setting: Setting<A>, redraw: () => void): VNode {
  const v = setting.get();
  return h(
    'select',
    {
      hook: {
        insert(vnode) {
          (vnode.elm as HTMLSelectElement).addEventListener('change', e => {
            setting.set((e.target as HTMLSelectElement).value as A);
            redraw();
          });
        },
      },
    },
    setting.choices.map(choice => {
      const [key, name] = choice;
      return h('option', { attrs: { value: '' + key, selected: key === v } }, name);
    }),
  );
}
