import { Hook } from './interfaces';
import { FormLines, FormObject, FormStore, toFormLines, toFormObject, makeStore } from './form';
import LobbyController from './ctrl';

interface FilterData {
  form: FormLines;
  filter: FormObject;
}

interface Filtered {
  visible: Hook[];
  hidden: number;
}

export default class Filter {
  store: FormStore;
  data: FilterData | null;
  open = false;

  constructor(storage: LichessStorage, readonly root: LobbyController) {
    this.store = makeStore(storage);
    this.set(this.store.get());
  }

  toggle = () => {
    this.open = !this.open;
  };

  set = (data: FormLines | null) => {
    this.data = data && {
      form: data,
      filter: toFormObject(data),
    };
  };

  save = (form: HTMLFormElement) => {
    const lines = toFormLines(form);
    this.store.set(lines);
    this.set(lines);
    this.root.onSetFilter();
  };

  filter = (hooks: Hook[]): Filtered => {
    if (!this.data) return { visible: hooks, hidden: 0 };
    const f = this.data.filter,
      ratingRange = f.ratingRange && f.ratingRange.split('-').map((r: string) => parseInt(r, 10)),
      seen: string[] = [],
      visible: Hook[] = [];
    let variant: string,
      hidden = 0;
    hooks.forEach(hook => {
      variant = hook.variant;
      if (hook.action === 'cancel') visible.push(hook);
      else {
        if (
          !f.variant?.includes(variant) ||
          !f.speed?.includes((hook.s || 1).toString() /* ultrabullet = bullet */) ||
          (f.mode?.length == 1 && f.mode[0] != (hook.ra || 0).toString()) ||
          (f.increment?.length == 1 && f.increment[0] != hook.i.toString()) ||
          (ratingRange && (!hook.rating || hook.rating < ratingRange[0] || hook.rating > ratingRange[1]))
        ) {
          hidden++;
        } else {
          const hash = hook.ra + variant + hook.t + hook.rating;
          if (!seen.includes(hash)) visible.push(hook);
          seen.push(hash);
        }
      }
    });
    return { visible, hidden };
  };
}
