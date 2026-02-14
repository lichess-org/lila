import type { LichessStorage } from 'lib/storage';

export type FormLines = Record<string, string>;
export type FormObject = Record<string, any>;
export type FormStore = {
  get: () => FormLines | null;
  set: (lines: FormLines) => void;
  remove: () => void;
};

export const toFormLines = (form: HTMLFormElement): FormLines =>
  Array.from(new FormData(form).entries())
    .filter(([k]) => !k.includes('_range'))
    .reduce<FormLines>((o, [k, v]) => (typeof v === 'string' ? ((o[k] = v), o) : o), {});

export const toFormObject = (lines: FormLines): FormObject =>
  Object.keys(lines).reduce((o, k) => {
    const i = k.indexOf('[');
    const fk = i > 0 ? k.slice(0, i) : k;
    return i > 0 ? { ...o, [fk]: [...(o[fk] || []), lines[k]] } : { ...o, [fk]: lines[k] };
  }, {} as FormObject);

export const makeStore = (storage: LichessStorage): FormStore => ({
  get: () => JSON.parse(storage.get() || 'null'),
  set: lines => storage.set(JSON.stringify(lines)),
  remove: () => storage.remove(),
});
