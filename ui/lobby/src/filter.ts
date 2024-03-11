import LobbyController from './ctrl';
import { FormLines, FormObject, FormStore, makeStore, toFormLines, toFormObject } from './form';
import { Hook, Seek } from './interfaces';
import { action } from './util';

interface FilterData {
  form: FormLines;
  filter: FormObject;
}

interface Filtered {
  visible: Hook[];
  hidden: number;
}

interface FilteredSeeks {
  visible: Seek[];
  hidden: number;
}

export default class Filter {
  store: FormStore;
  data: FilterData | null;
  open: boolean = false;

  constructor(
    storage: LishogiStorage,
    readonly root: LobbyController
  ) {
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

  save = (form: HTMLFormElement | null) => {
    const lines = form && toFormLines(form);
    if (lines) this.store.set(lines);
    else this.store.remove();
    this.set(lines);
    this.root.onSetFilter();
  };

  filter = (hooks: Hook[]): Filtered => {
    if (!this.data) return { visible: hooks, hidden: 0 };
    const f = this.data.filter,
      ratingRange = f.ratingRange && f.ratingRange.split('-').map(r => parseInt(r, 10)),
      visible: Hook[] = [];
    let variant: string,
      hidden = 0;
    hooks.forEach(hook => {
      variant = hook.variant || 'standard';
      if (action(hook) === 'cancel') visible.push(hook);
      else {
        if (
          !f.variant?.includes(variant) ||
          !f.speed?.includes((hook.s || 1).toString() /* ultrabullet = bullet */) ||
          (f.mode?.length == 1 && f.mode[0] != (hook.ra || 0).toString()) ||
          (f.increment?.length == 1 && f.increment[0] != hook.i.toString()) ||
          (f.byoyomi?.length == 1 && f.byoyomi[0] != hook.b.toString()) ||
          (ratingRange && hook.rating && (hook.rating < ratingRange[0] || hook.rating > ratingRange[1])) ||
          (!hook.u && (!f.anonymous || f.anonymous.length == 0))
        ) {
          hidden++;
        } else {
          visible.push(hook);
        }
      }
    });
    return {
      visible: visible,
      hidden: hidden,
    };
  };

  filterSeeks = (seeks: Seek[]): FilteredSeeks => {
    if (!this.data) return { visible: seeks, hidden: 0 };
    const f = this.data.filter,
      ratingRange = f.ratingRange?.split('-').map(r => parseInt(r, 10)),
      visible: Seek[] = [];
    let variant: string,
      hidden = 0;

    seeks.forEach(seek => {
      variant = seek.variant || 'standard';
      if (action(seek) === 'cancel') visible.push(seek);
      else {
        if (
          !f.variant?.includes(variant) ||
          (f.mode?.length == 1 && f.mode[0] != (seek.mode || 0).toString()) ||
          (seek.days && !f.days?.includes(seek.days.toString())) ||
          (ratingRange && (seek.rating < ratingRange[0] || seek.rating > ratingRange[1]))
        ) {
          hidden++;
        } else visible.push(seek);
      }
    });
    return {
      visible: visible,
      hidden: hidden,
    };
  };
}
