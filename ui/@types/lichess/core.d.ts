// These polyfills give us `using` and `await using`. See
// https://developer.mozilla.org/en-US/docs/Web/JavaScript/Guide/Resource_management
//
// We don't want to add esnext.disposable to tsconfig.base.json "lib" because that implies support for
// additional polyfills we don't include. See file://./../../site/src/site.core.ts

interface SymbolConstructor {
  readonly dispose: unique symbol;
  readonly asyncDispose: unique symbol;
}

interface Disposable {
  [Symbol.dispose](): void;
}

interface AsyncDisposable {
  [Symbol.asyncDispose](): PromiseLike<void>;
}
