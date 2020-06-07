import { Hook } from './interfaces';

interface FilterFormData {
  [key: string]: string;
}

interface FilterData {
  form: FilterFormData;
  filter: {
    [key: string]: string[];
  }
}

interface Filtered {
  visible: Hook[];
  hidden: number;
}

export default class Filter {

  data: FilterData | null;
  open: boolean = false;

  constructor(readonly storage: LichessStorage, readonly redraw: () => void) {
    this.set(JSON.parse(storage.get() || 'null') as FilterFormData);
  }

  toggle = () => {
    this.open = !this.open;
  };

  set = (data: FilterFormData | null) => {
    this.data = data && {
      form: data,
      filter: Object.keys(data).reduce((o, k) => {
        const i = k.indexOf('[');
        const fk = i > 0 ? k.slice(0, i) : k;
        return i > 0 ? {
          ...o,
          [fk]: [...(o[fk] || []), data[k]]
        } : {
          ...o,
          [fk]: data[k]
        };
      }, {})
    };
  }

  save = (form: HTMLFormElement) => {
    const data = Array.from(new FormData(form)).reduce((o,[k,v]) => ({...o, [k]: v}), {});
    this.storage.set(JSON.stringify(data));
    this.set(data);
  }

  filter = (hooks: Hook[]): Filtered => {
    if (!this.data) return { visible: hooks, hidden: 0 };
    const f = this.data.filter,
      seen: string[] = [],
      visible: Hook[] = [];
    let variant: string, hidden = 0;
    hooks.forEach(function(hook) {
      variant = hook.variant;
      if (hook.action === 'cancel') visible.push(hook);
      else {
        if (!f.variant.includes(variant) ||
          !f.mode.includes(hook.ra || 0) ||
          !f.speed.includes(hook.s || 1 /* ultrabullet = bullet */) ||
          (f.increment.length && !f.increment.includes(hook.i)) ||
          (f.rating && (!hook.rating || (hook.rating < f.rating[0] || hook.rating > f.rating[1])))) {
          hidden++;
        } else {
          const hash = hook.ra + variant + hook.t + hook.rating;
          if (!seen.includes(hash)) visible.push(hook);
          seen.push(hash);
        }
      }
    });
    return {
      visible: visible,
      hidden: hidden
    };
  }
}
