/** promisify [indexedDB](https://developer.mozilla.org/en-US/docs/Web/API/IndexedDB_API) and add nothing
 * ### basic usage:
 * ```ts
 *   import { objectStorage } from 'lib/objectStorage';
 *
 *   const store = await objectStorage<number>({ store: 'store' });
 *   const value = await store.get('someKey') ?? 10;
 *   await store.put('someOtherKey', value + 1);
 * ```
 * ### cursors/indices:
 * ```ts
 *   import { objectStorage, range } from 'lib/objectStorage';
 *
 *   const store = await objectStorage<MyObj>({
 *     store: 'store',
 *     indices: [{ name: 'size', keyPath: 'size' }]
 *   });
 *
 *   await store.readCursor({ index: 'size', query: range({ above: 5 }) }, obj => {
 *     console.log(obj);
 *   });
 *
 *   await store.writeCursor(
 *     { index: 'size', query: range({ min: 4, max: 12 }) },
 *     async ({ value, update, delete }) => {
 *       if (value.size < 10) await update({ ...value, size: value.size + 1 });
 *       else await delete();
 *     }
 *   );
 * ```
 * ### upgrade/migration:
 * ```ts
 *   import { objectStorage } from 'lib/objectStorage';
 *
 *   const upgradedStore = await objectStorage<MyObj>({
 *     store: 'upgradedStore',
 *     version: 2,
 *     upgrade: (e, store) => {
 *       // raw idb needed here
 *       if (e.oldVersion < 2) store.createIndex('color', 'color'); // manual index creation
 *       const req = store.openCursor();
 *       req.onsuccess = cursorEvent => {
 *         const cursor = (cursorEvent.target as IDBRequest<IDBCursorWithValue>).result;
 *         if (!cursor) return;
 *         cursor.update(transformYourObject(e.oldVersion, cursor.value));
 *         cursor.continue();
 *       };
 *     }
 *   });
 * ```
 * other needs can be met by raw idb calls on the `txn` function result
 * @see https://developer.mozilla.org/en-US/docs/Web/API/IndexedDB_API
 */
export async function objectStorage<V, K extends IDBValidKey = IDBValidKey>(
  dbInfo: DbInfo,
): Promise<ObjectStorage<V, K>> {
  const db = await dbConnect(dbInfo);

  return {
    list: () => promise(() => objectStore('readonly').getAllKeys()),
    has: (key: K) =>
      promise(() => objectStore('readonly').getKey(key))
        .then(Boolean)
        .catch(() => false),
    get: (key: K) => promise(() => objectStore('readonly').get(key)),
    getOpt: (key: K) => promise<V | undefined>(() => objectStore('readonly').get(key)).catch(() => undefined),
    getMany: (keys?: IDBKeyRange) => promise(() => objectStore('readonly').getAll(keys)),
    put: (key: K, value: V) => promise(() => objectStore('readwrite').put(value, key)),
    count: (key?: K | IDBKeyRange) => promise(() => objectStore('readonly').count(key)),
    remove: (key: K | IDBKeyRange) => promise(() => objectStore('readwrite').delete(key)),
    clear: () => promise(() => objectStore('readwrite').clear()),
    txn: (mode: IDBTransactionMode) => db.transaction(dbInfo.store, mode),
    cursor,
    readCursor: async (opts: CursorOpts, it: (v: V) => any): Promise<void> => {
      for await (const c of cursor(opts, 'readonly')) await it(c.value);
    },
    writeCursor: async (opts: CursorOpts, it: WriteCursorCallback<V>): Promise<void> => {
      for await (const c of cursor(opts, 'readwrite')) {
        await it({
          value: c.value,
          update: (v: V) => promise(() => c.update(v)),
          delete: () => promise(() => c.delete()),
        });
      }
    },
  };

  function objectStore(mode: IDBTransactionMode) {
    return db.transaction(dbInfo.store, mode).objectStore(dbInfo.store);
  }

  function promise<V>(f: () => IDBRequest) {
    return new Promise<V>((resolve, reject) => {
      const res = f();
      res.onsuccess = (e: Event) => resolve((e.target as IDBRequest).result);
      res.onerror = (e: Event) => reject((e.target as IDBRequest).result);
    });
  }

  function cursor(opts: CursorOpts = {}, mode: IDBTransactionMode): AsyncGenerator<IDBCursorWithValue> {
    const store = objectStore(mode);
    const req = opts.index
      ? store.index(opts.index).openCursor(opts.query, opts.dir)
      : store.openCursor(opts.query, opts.dir);
    return (async function* () {
      while (true) {
        const cursor = await promise<IDBCursorWithValue | null>(() => req);
        if (!cursor) break;
        yield cursor;
        cursor.continue();
      }
    })();
  }
}

export function range<K extends IDBValidKey>(range: {
  min?: K; // closed lower bound
  max?: K; // closed upper bound
  above?: K; // open lower bound
  below?: K; // open upper bound
}): IDBKeyRange | undefined {
  const lowerOpen = 'above' in range;
  const upperOpen = 'below' in range;
  const lower = range.above ?? range.min;
  const upper = range.below ?? range.max;
  if (lower !== undefined && upper !== undefined)
    return IDBKeyRange.bound(lower, upper, lowerOpen, upperOpen);
  if (lower !== undefined) return IDBKeyRange.lowerBound(lower, lowerOpen);
  if (upper !== undefined) return IDBKeyRange.upperBound(upper, upperOpen);
  return undefined;
}

export async function nonEmptyStore(info: DbInfo): Promise<boolean> {
  const dbName = info.db ?? info.store;
  if (window.indexedDB.databases) {
    const dbs = await window.indexedDB.databases();
    if (dbs.every(db => db.name !== dbName)) return false;
  }

  return new Promise<boolean>(resolve => {
    const request = window.indexedDB.open(dbName);

    request.onerror = () => resolve(false);
    request.onsuccess = (e: Event) => {
      const db = (e.target as IDBOpenDBRequest).result;
      if (!db.objectStoreNames.contains(info.store)) {
        db.close();
        resolve(false);
      }
      const cursorReq = db.transaction(info.store, 'readonly').objectStore(info.store).openCursor();
      cursorReq.onsuccess = () => {
        db.close();
        resolve(Boolean(cursorReq.result));
      };
      cursorReq.onerror = () => {
        db.close();
        resolve(false);
      };
    };
  });
}

export interface DbInfo {
  /** name of the object store */
  store: string;
  /** defaults to store name because you should aim for one store per db to minimize version
   * upgrade callback complexity. raw idb is best for versioned multi-store dbs */
  db?: string;
  /** db version (default: 1), your upgrade callback receives e.oldVersion */
  version?: number;
  /** indices for the object store, changes must increment version */
  indices?: { name: string; keyPath: string | string[]; options?: IDBIndexParameters }[];
  /** upgrade function to handle schema changes @see objectStorage */
  upgrade?: (e: IDBVersionChangeEvent, store?: IDBObjectStore) => void;
}

export type WriteCursorCallback<V> = {
  (it: {
    /** just the value */
    value: V;
    /** await this to modify the store value */
    update: (v: V) => Promise<void>;
    /** await this to delete the entry from the store. iteration is not affected */
    delete: () => Promise<void>;
  }): any;
};

export interface CursorOpts {
  /** supply an index name to use for the cursor, otherwise iterate the store */
  index?: string;
  /** The key range to filter the cursor results */
  query?: IDBKeyRange | IDBValidKey | null;
  /** 'prev', 'prevunique', 'next', or 'nextunique' (default is 'next')*/
  dir?: IDBCursorDirection;
}

export interface ObjectStorage<V, K extends IDBValidKey = IDBValidKey> {
  /** list all keys in the object store */
  list(): Promise<K[]>;
  /** check if a key exists */
  has(key: K): Promise<boolean>;
  /** retrieve a value by key */
  get(key: K): Promise<V>;
  /** retrieve or fail gracefully */
  getOpt(key: K): Promise<V | undefined>;
  /** retrieve multiple values by key range, or all values if omitted */
  getMany(keys?: IDBKeyRange): Promise<V[]>;
  /** put a value into the store under a specific key and return that key */
  put(key: K, value: V): Promise<K>;
  /** count the number of entries matching a key or range. Count all values if omitted */
  count(key?: K | IDBKeyRange): Promise<number>;
  /** remove value(s) by key or key range */
  remove(key: K | IDBKeyRange): Promise<void>;
  /** clear all entries from the object store */
  clear(): Promise<void>;
  /** initiate a database transaction */
  txn(mode: IDBTransactionMode): IDBTransaction;
  /** create a raw cursor to iterate over an index or store's records */
  cursor(opts: CursorOpts, mode: IDBTransactionMode): AsyncGenerator<IDBCursorWithValue>;
  /** read records using an idb cursor via simple value callback. resolves when iteration completes */
  readCursor(o: CursorOpts, it: (v: V) => any): Promise<void>;
  /** read, write, or delete records via cursor callback. promise resolves when iteration is done */
  writeCursor(o: CursorOpts, it: WriteCursorCallback<V>): Promise<void>;
}

async function dbConnect(info: DbInfo): Promise<IDBDatabase> {
  const dbName = info.db ?? info.store;

  return new Promise<IDBDatabase>((resolve, reject) => {
    const result = window.indexedDB.open(dbName, info?.version ?? 1);

    result.onsuccess = (e: Event) => resolve((e.target as IDBOpenDBRequest).result);
    result.onerror = (e: Event) => reject((e.target as IDBOpenDBRequest).error ?? 'IndexedDB Unavailable');
    result.onupgradeneeded = (e: IDBVersionChangeEvent) => {
      const db = (e.target as IDBOpenDBRequest).result;
      const txn = (e.target as IDBOpenDBRequest).transaction;
      const store = db.objectStoreNames.contains(info.store)
        ? txn!.objectStore(info.store)
        : db.createObjectStore(info.store);

      const existing = new Set(store.indexNames);

      info.indices?.forEach(({ name, keyPath, options }) => {
        if (!existing.has(name)) store.createIndex(name, keyPath, options);
        else {
          const idx = store.index(name);
          if (
            idx.keyPath !== keyPath ||
            idx.unique !== !!options?.unique ||
            idx.multiEntry !== !!options?.multiEntry
          ) {
            store.deleteIndex(name);
            store.createIndex(name, keyPath, options);
          }
        }
        existing.delete(name);
      });
      existing.forEach(indexName => store.deleteIndex(indexName));
      info.upgrade?.(e, store);
    };
  });
}
