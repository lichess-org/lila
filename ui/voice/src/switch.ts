// group selector with state change hooks

export interface Selectable {
  // weak type as documentation
  select?: (ctx?: any) => void;
  deselect?: (ctx?: any) => void;
  close?: (ctx?: any) => void;
  [key: string]: any;
}

export class Switch<K extends string = string, V extends Selectable = Selectable> {
  items: Map<K, V>;

  private selected: K | false = false;
  private ctx: any;

  constructor(opts?: { items?: Map<K, V>; context?: any }) {
    this.items = opts?.items ? opts.items : new Map();
    this.ctx = opts?.context ?? this;
  }

  get key(): K | false {
    if (this.selected && !this.items.has(this.selected)) this.selected = false;
    return this.selected;
  }

  get value(): V | undefined {
    const key = this.key;
    return key ? this.items.get(key) : undefined;
  }

  get context(): any {
    return this.ctx;
  }

  setContext(ctx: any): void {
    if (this.ctx === ctx) return;
    this.value?.deselect?.(this.ctx);
    this.ctx = ctx;
    this.value?.select?.(this.ctx);
  }

  set(newKey: K | false): boolean {
    if (newKey && !this.items.has(newKey)) return false;
    if (this.selected) {
      if (this.selected === newKey) return true;
      this.value?.deselect?.(this.ctx);
    }
    this.selected = newKey;
    this.value?.select?.(this.ctx);
    return true;
  }

  keyOf(val: V): K | false {
    for (const [k, v] of this.items) if (v === val) return k;
    return false;
  }

  add(key: K, val: V): void {
    if (this.items.get(key) === val) return;
    const reselect = this.selected === key;
    if (reselect) this.remove(key);
    this.items.set(key, val);
    if (reselect) this.set(key);
  }

  remove(key?: K): void {
    this.close(key);
    key ? this.items.delete(key) : this.items.clear();
  }

  close(key?: K): void {
    if (key === undefined) {
      for (const k of this.items.keys()) this.close(k);
      return;
    }
    if (key === this.selected) {
      this.items.get(key)?.deselect?.(this.ctx);
      this.selected = false;
    }
    this.items.get(key)?.close?.(this.ctx);
  }
}
