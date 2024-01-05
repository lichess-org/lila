/*
usage:
  const store = await objectStorage<MyObject>({store: 'my-store'});
  const value = await store.get(key);
*/

export interface DbInfo {
  store: string;
  db?: string; // default `${store}--db`
  version?: number; // default 1
  upgrade?: (e: IDBVersionChangeEvent, store?: IDBObjectStore) => void;
}

export interface ObjectStorage<V, K extends IDBValidKey = IDBValidKey> {
  list(): Promise<K[]>;
  get(key: K): Promise<V>;
  getMany(keys?: IDBKeyRange): Promise<V[]>;
  put(key: K, value: V): Promise<K>; // returns key
  count(key?: K | IDBKeyRange): Promise<number>;
  remove(key: K | IDBKeyRange): Promise<void>;
  clear(): Promise<void>; // remove all
  txn(mode: IDBTransactionMode): IDBTransaction; // do anything else
}

export async function objectStorage<V, K extends IDBValidKey = IDBValidKey>(
  dbInfo: DbInfo,
): Promise<ObjectStorage<V, K>> {
  const db = await dbConnect(dbInfo);

  function objectStore(mode: IDBTransactionMode) {
    return db.transaction(dbInfo.store, mode).objectStore(dbInfo.store);
  }

  function actionPromise<V>(f: () => IDBRequest) {
    return new Promise<V>((resolve, reject) => {
      const res = f();
      res.onsuccess = (e: Event) => resolve((e.target as IDBRequest).result);
      res.onerror = (e: Event) => reject((e.target as IDBRequest).result);
    });
  }

  return {
    list: () => actionPromise<K[]>(() => objectStore('readonly').getAllKeys()),
    get: (key: K) => actionPromise<V>(() => objectStore('readonly').get(key)),
    getMany: (keys?: IDBKeyRange) => actionPromise<V[]>(() => objectStore('readonly').getAll(keys)),
    put: (key: K, value: V) => actionPromise<K>(() => objectStore('readwrite').put(value, key)),
    count: (key?: K | IDBKeyRange) => actionPromise<number>(() => objectStore('readonly').count(key)),
    remove: (key: K | IDBKeyRange) => actionPromise<void>(() => objectStore('readwrite').delete(key)),
    clear: () => actionPromise<void>(() => objectStore('readwrite').clear()),
    txn: (mode: IDBTransactionMode) => db.transaction(dbInfo.store, mode),
  };
}

async function dbConnect(dbInfo: DbInfo) {
  const dbName = dbInfo?.db || `${dbInfo.store}--db`;

  return new Promise<IDBDatabase>((resolve, reject) => {
    const result = window.indexedDB.open(dbName, dbInfo?.version ?? 1);

    result.onsuccess = (e: Event) => resolve((e.target as IDBOpenDBRequest).result);
    result.onerror = (e: Event) => reject((e.target as IDBOpenDBRequest).error ?? 'IndexedDB Unavailable');
    result.onupgradeneeded = (e: IDBVersionChangeEvent) => {
      const db = (e.target as IDBOpenDBRequest).result;
      const txn = (e.target as IDBOpenDBRequest).transaction;
      const store = db.objectStoreNames.contains(dbInfo.store)
        ? txn!.objectStore(dbInfo.store)
        : db.createObjectStore(dbInfo.store);

      dbInfo.upgrade?.(e, store);
    };
  });
}
