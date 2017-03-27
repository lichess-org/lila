interface WebAssemblyStatic {
  validate: (code: Uint8Array) => boolean;
}

declare var WebAssembly: WebAssemblyStatic | undefined;
