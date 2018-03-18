export function defined<A>(v: A | undefined): v is A {
  return typeof v !== 'undefined';
}

export function empty(a: any): boolean {
  return !a || a.length === 0;
}

export interface Prop<T> {
  (): T
  (v: T): T
}

// like mithril prop but with type safety
export function prop<A>(initialValue: A): Prop<A> {
  let value = initialValue;
  const fun = function(v: A | undefined) {
    if (defined(v)) value = v;
    return value;
  };
  return fun as Prop<A>;
}

export interface StoredProp<T> {
  (): string;
  (v: T): void;
}

export interface StoredBooleanProp {
  (): boolean;
  (v: boolean): void;
}

const storage = window.lichess.storage;

export function storedProp(k: string, defaultValue: boolean): StoredBooleanProp;
export function storedProp<T>(k: string, defaultValue: T): StoredProp<T>;
export function storedProp(k: string, defaultValue: any) {
  const sk = 'analyse.' + k;
  const isBoolean = defaultValue === true || defaultValue === false;
  let value: any;
  return function(v: any) {
    if (defined(v) && v != value) {
      value = v + '';
      storage.set(sk, v);
    } else if (!defined(value)) {
      value = storage.get(sk);
      if (value === null) value = defaultValue + '';
    }
    return isBoolean ? value === 'true' : value;
  };
}

export interface StoredJsonProp<T> {
  (): T;
  (v: T): void;
}

export function storedJsonProp<T>(key: string, defaultValue: T): StoredJsonProp<T> {
  return function(v?: T) {
    if (defined(v)) {
      storage.set(key, JSON.stringify(v));
      return v;
    }
    const ret = JSON.parse(storage.get(key));
    return (ret !== null) ? ret : defaultValue;
  };
}

export interface Sync<T> {
  promise: Promise<T>;
  sync: T | undefined;
}

export function sync<T>(promise: Promise<T>): Sync<T> {
  const sync: Sync<T> = {
    sync: undefined,
    promise: promise.then(v => {
      sync.sync = v;
      return v;
    })
  };
  return sync;
}

// Ensures calls to the wrapped function are spaced by the given delay.
// Any extra calls are dropped, except the last one.
export function throttle(delay: number, callback: (...args: any[]) => void): (...args: any[]) => void {
  let timer: number | undefined;
  let lastExec = 0;

  return function(this: any, ...args: any[]): void {
    const self: any = this;
    const elapsed = Date.now() - lastExec;

    function exec() {
      timer = undefined;
      lastExec = Date.now();
      callback.apply(self, args);
    }

    if (timer) clearTimeout(timer);

    if (elapsed > delay) exec();
    else timer = setTimeout(exec, delay - elapsed);
  };
}

const movePattern = /\b(\d+)\s?(\.+)\s?(?:[o0-]+|[NBRQK]*[a-h]?[1-8]?x?@?[a-h][0-9]=?[NBRQK]?)\+?\#?[!\?=]*/gi;
function moveReplacer(match: string, turn: number, dots: string) {
  if (turn < 1 || turn > 200) return match;
  const ply = turn * 2 - (dots.length > 1 ? 0 : 1);
  return '<a class="jump" data-ply="' + ply + '">' + match + '</a>';
}

export function addPlies(html: string) {
  return html.replace(movePattern, moveReplacer);
}
