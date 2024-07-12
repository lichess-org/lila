// mutually exclusive group selector with state change hooks

export interface Selectable<C = any> {
  select?: (ctx?: C) => void;
  deselect?: (ctx?: C) => void;
  close?: (ctx?: C) => void;
}

export class Selector<T extends Selectable, C = any> {
  group: Map<string, T> = new Map<string, T>();
  context?: C;
  key: string | false = false;

  set ctx(ctx: any) {
    if (this.context === ctx) return;
    this.selected?.deselect?.(this.context);
    this.context = ctx;
    this.selected?.select?.(this.context);
  }

  get selected(): T | undefined {
    return this.key ? this.group.get(this.key) : undefined;
  }

  select(key: string | false): void {
    if (this.key) {
      if (this.key === key) return;
      this.selected?.deselect?.(this.context);
    }
    this.key = key;
    this.selected?.select?.(this.context);
  }

  get(key: string): T | undefined {
    return this.group.get(key);
  }

  set(key: string, val: T): void {
    const reselect = this.key === key;
    this.close(key);
    this.group.set(key, val);
    if (reselect) this.select(key);
  }

  close(key?: string): void {
    if (key === undefined) {
      for (const k of this.group.keys()) this.close(k);
      return;
    }
    if (key === this.key) {
      this.group.get(key)?.deselect?.(this.context);
      this.key = false;
    }
    this.group.get(key)?.close?.(this.context);
  }

  delete(key?: string): void {
    this.close(key);
    key ? this.group.delete(key) : this.group.clear();
  }
}
