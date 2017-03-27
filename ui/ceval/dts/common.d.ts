interface StoredProp<T> {
  (): T | string;
  (v: T): void;
}

declare module 'common' {
  function defined(v: any): boolean;
  function storedProp<T>(k: string, defaultValue: T): StoredProp<T>;
  function throttle(delay: number, noTrailing: boolean, callback: (...args: any[]) => void, debounceMode?: boolean): (...args: any[]) => void;
  function classSet(classes: any): string;
}
