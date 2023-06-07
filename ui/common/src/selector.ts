// mutually exclusive group selector with state change hooks

export interface Selectable<C = any> {
  select?: (ctx?: C) => void;
  deselect?: (ctx?: C) => void;
  dispose?: (ctx?: C) => void;
}

export class Selector<T extends Selectable, C = any> {
  group = new Map<string, T>();
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

  select(key: string | false) {
    if (this.key) {
      if (this.key === key) return;
      this.selected!.deselect?.(this.context);
    }
    this.key = key;
    this.selected?.select?.(this.context);
  }

  set(key: string, val: T) {
    this.delete(key);
    this.group.set(key, val);
    if (this.key === key) val.select?.(this.context);
  }

  get(key: string): T | undefined {
    return this.group.get(key);
  }

  delete(key?: string) {
    if (key === undefined) {
      for (const key of this.group.keys()) this.delete(key);
      this.key = false;
      return;
    }
    if (this.key === key) {
      this.key = false;
      this.group.get(key)?.deselect?.(this.context);
    }
    this.group.get(key)?.dispose?.(this.context);
    this.group.delete(key);
  }
}
