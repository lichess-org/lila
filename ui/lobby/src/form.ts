export interface FormLines {
  [key: string]: string;
}
export interface FormObject {
  [key: string]: any;
}
export interface FormStore {
  get: () => FormLines | null;
  set: (lines: FormLines) => void;
}

export const toFormLines = (form: HTMLFormElement): FormLines =>
  Array.from(new FormData(form).entries())
    .filter(([k, _]) => !k.includes('_range'))
    .reduce((o, [k, v]) => ({ ...o, [k]: v }), {});

export const toFormObject = (lines: FormLines): FormObject =>
  Object.keys(lines).reduce((o, k) => {
    const i = k.indexOf('[');
    const fk = i > 0 ? k.slice(0, i) : k;
    return i > 0 ? { ...o, [fk]: [...(o[fk] || []), lines[k]] } : { ...o, [fk]: lines[k] };
  }, {});

export const makeStore = (storage: LichessStorage): FormStore => ({
  get: () => JSON.parse(storage.get() || 'null') as FormLines,
  set: lines => storage.set(JSON.stringify(lines)),
});
