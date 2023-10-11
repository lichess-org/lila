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

export interface ObjectStorage<V> {
  get(key: string): Promise<V>;
  put(key: string, value: V): Promise<string>; // returns key
  count(key: string): Promise<number>;
  remove(key: string): Promise<void>;
  clear(): Promise<void>; // remove all
  txn(mode: IDBTransactionMode): IDBTransaction; // do anything else
}

export async function objectStorage<V>(dbInfo: DbInfo): Promise<ObjectStorage<V>> {
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
    get(key: string) {
      return actionPromise<V>(() => objectStore('readonly').get(key));
    },
    put(key: string, value: V) {
      return actionPromise<string>(() => objectStore('readwrite').put(value, key));
    },
    count(key: string) {
      return actionPromise<number>(() => objectStore('readonly').count(key));
    },
    remove(key: string) {
      return actionPromise<void>(() => objectStore('readwrite').delete(key));
    },
    clear() {
      return actionPromise<void>(() => objectStore('readwrite').clear());
    },
    txn(mode: IDBTransactionMode) {
      return db.transaction(dbInfo.store, mode);
    },
  };
}

async function dbConnect(dbInfo: DbInfo) {
  const dbName = dbInfo?.db || `${dbInfo.store}--db`;

  return new Promise<IDBDatabase>((resolve, reject) => {
    const result = window.indexedDB.open(dbName, dbInfo?.version ?? 1);

    result.onsuccess = (e: Event) => resolve((e.target as IDBOpenDBRequest).result);
    result.onerror = (e: Event) => reject((e.target as IDBOpenDBRequest).result ?? 'IndexedDB Unavailable');
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
