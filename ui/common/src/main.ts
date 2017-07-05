/// <reference types="types/lichess" />

import throttle from './throttle';

export function defined(v: any): boolean {
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
    if (defined(v)) value = v!;
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

export function storedProp(k: string, defaultValue: boolean): StoredBooleanProp;
export function storedProp<T>(k: string, defaultValue: T): StoredProp<T>;
export function storedProp(k: string, defaultValue: any) {
  const sk = 'analyse.' + k;
  const isBoolean = defaultValue === true || defaultValue === false;
  var value: any;
  return function(v: any) {
    if (defined(v) && v != value) {
      value = v + '';
      window.lichess.storage.set(sk, v);
    } else if (!defined(value)) {
      value = window.lichess.storage.get(sk);
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
  return function() {
    if (arguments.length) window.lichess.storage.set(key, JSON.stringify(arguments[0]));
    const ret = JSON.parse(window.lichess.storage.get(key));
    return (ret !== null) ? ret : defaultValue;
  };
}

export { throttle };

export type F = () => void;

export function dropThrottle(delay: number): (f: F) => void  {
  var task: F | undefined;
  const run = function(f: F) {
    task = f;
    f();
    setTimeout(function() {
      if (task !== f) run(task!);
      else task = undefined;
    }, delay);
  };
  return function(f) {
    if (task) task = f;
    else run(f);
  };
}
