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
  remove(key: string): Promise<void>; // delete one
  clear(): Promise<void>; // delete all
  txn(mode: IDBTransactionMode): IDBTransaction;
}

export async function objectStorage<V>(dbInfo: DbInfo): Promise<ObjectStorage<V>> {
  const name = dbInfo.store;
  const db = await dbAsync(dbInfo);

  function actionPromise<V>(f: () => IDBRequest) {
    return new Promise<V>((resolve, reject) => {
      const res = f();
      res.onsuccess = (e: Event) => resolve((e.target as IDBRequest).result);
      res.onerror = (e: Event) => reject((e.target as IDBRequest).result);
    });
  }
  return {
    get(key: string) {
      const store = db.transaction(name, 'readonly').objectStore(name);
      return actionPromise<V>(store.get.bind(store, key));
    },

    put(key: string, value: V) {
      const store = db.transaction(name, 'readwrite').objectStore(name);
      return actionPromise<string>(store.put.bind(store, value, key));
    },

    count(key: string) {
      const store = db.transaction(name, 'readonly').objectStore(name);
      return actionPromise<number>(store.count.bind(store, key));
    },

    remove(key: string) {
      const store = db.transaction(name, 'readwrite').objectStore(name);
      return actionPromise<void>(store.delete.bind(store, key));
    },

    clear() {
      const store = db.transaction(name, 'readwrite').objectStore(name);
      return actionPromise<void>(store.clear.bind(store));
    },

    txn(mode: IDBTransactionMode) {
      return db.transaction(name, mode);
    },
  };
}

async function dbAsync(dbInfo: DbInfo) {
  const dbName = dbInfo?.db || `${dbInfo.store}--db`;
  return new Promise<IDBDatabase>((resolve, reject) => {
    const result = window.indexedDB.open(dbName, dbInfo?.version ?? 1);
    result.onsuccess = (e: Event) => resolve((e.target as IDBOpenDBRequest).result);
    result.onerror = (e: Event) => reject((e.target as IDBOpenDBRequest).result);
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
