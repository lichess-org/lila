interface LichessGlobal {
  storage: LichessStorageHelper;
  assetUrl: (url: string, opts?: AssetUrlOpts) => string;
  engineName: string;
}

interface AssetUrlOpts {
  sameDomain?: boolean;
  noVersion?: boolean;
}

interface LichessStorageHelper {
  make: (k: string) => LichessStorage;
  get: (k: string) => string;
  set: (k: string, v: string) => string;
  remove: (k: string) => void;
}

interface LichessStorage {
  get: () => string;
  set: (v: string) => string;
  remove: () => void;
  listen: (f: (e: StorageEvent) => void) => void;
}

declare var lichess: LichessGlobal;
