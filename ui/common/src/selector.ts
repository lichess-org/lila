// mutually exclusive group selector with state change hooks for resource management

export interface Selectable {
  select?: (ctx?: any) => void;
  deselect?: (ctx?: any) => void;
  close?: (ctx?: any) => void;
  name?: string;
}

export class Selector<K extends string = string, V extends Selectable & Record<string, any> = any, C = any> {
  group: Map<K, V>;
  context: C | Selector<K, V>;
  key: K | false = false;
  name?: string;

  constructor(opts?: { group?: Map<K, V>; context?: C; defaultKey?: K; name?: string }) {
    this.group = opts?.group ? opts.group : new Map();
    this.context = opts?.context ?? this;
    this.key = opts?.defaultKey ?? false;
    this.name = opts?.name;
  }

  set ctx(ctx: C) {
    if (this.context === ctx) return;
    this.value?.deselect?.(this.context);
    this.context = ctx;
    this.value?.select?.(this.context);
  }

  get value(): V | undefined {
    return this.key ? this.group.get(this.key) : undefined;
  }

  set(key: K | false) {
    if (this.key) {
      if (this.key === key) return;
      this.value?.deselect?.(this.context);
    }
    this.key = key;
    this.value?.select?.(this.context);
  }

  get(key: K): V | undefined {
    return this.group.get(key);
  }

  add(key: K, val: V) {
    const reselect = this.key === key;
    this.release(key);
    this.group.set(key, val);
    if (reselect) this.set(key);
  }

  keyOf(val: V): K | false {
    for (const [k, v] of this.group) if (v === val) return k;
    return false;
  }

  has(key: string) {
    return this.group.has(key as K);
  }

  release(key?: K) {
    if (key === undefined) {
      for (const k of this.group.keys()) this.release(k);
      return;
    }
    if (key === this.key) {
      this.group.get(key)?.deselect?.(this.context);
      this.key = false;
    }
    this.group.get(key)?.close?.(this.context);
  }

  remove(key?: K) {
    this.release(key);
    key ? this.group.delete(key) : this.group.clear();
  }
}

const user = document.body.dataset.user || 'anon';

export class StoredSelector<K extends string = string, V extends Selectable = any, C = any> extends Selector<
  K,
  V,
  C
> {
  constructor(
    readonly storageKey: string,
    opts?: { group?: Map<K, V>; context?: C; defaultKey?: K; name?: string },
  ) {
    super(opts);
    this.key = (site.storage.get(`${this.storageKey}:${user}`) ||
      (opts?.defaultKey ?? false)) as typeof this.key;
  }

  set(key: K | false) {
    super.set(key);
    if (key === false) site.storage.remove(`${this.storageKey}:${user}`);
    else site.storage.set(`${this.storageKey}:${user}`, String(this.key));
  }
}
