/* 
usage:
  const store = await objectStorage<MyObject>({store: 'my-store'});
  const obj = await store.get(key);
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
  list(): Promise<string[]>;
  txn(mode: IDBTransactionMode): IDBTransaction;
}

export async function objectStorage<V>(dbInfo: DbInfo): Promise<ObjectStorage<V>> {
  const name = dbInfo.store;
  const db = await dbAsync(dbInfo);

  function get(key: string) {
    const store = db.transaction(name, 'readonly').objectStore(name);
    return actionPromise<V>(store.get.bind(store, key));
  }

  function put(key: string, value: V) {
    const store = db.transaction(name, 'readwrite').objectStore(name);
    return actionPromise<string>(store.put.bind(store, value, key));
  }

  function count(key: string) {
    const store = db.transaction(name, 'readonly').objectStore(name);
    return actionPromise<number>(store.count.bind(store, key));
  }

  function remove(key: string) {
    const store = db.transaction(name, 'readwrite').objectStore(name);
    return actionPromise<void>(store.delete.bind(store, key));
  }

  function clear() {
    const store = db.transaction(name, 'readwrite').objectStore(name);
    return actionPromise<void>(store.clear.bind(store));
  }

  const list = () => Promise.resolve([]); // TODO

  const actionPromise = <V>(f: () => IDBRequest) =>
    new Promise<V>((resolve, reject) => {
      const res = f();
      res.onsuccess = (e: Event) => resolve((e.target as IDBRequest).result);
      res.onerror = (e: Event) => reject((e.target as IDBRequest).result);
    });

  const txn = (mode: IDBTransactionMode) => db.transaction(name, mode);

  return {
    get,
    put,
    count,
    remove,
    clear,
    list,
    txn,
  };
}

function dbAsync(dbInfo: DbInfo) {
  const dbName = dbInfo?.db || `${dbInfo.store}--db`;
  const version = dbInfo?.version;
  return new Promise<IDBDatabase>((resolve, reject) => {
    const result = window.indexedDB.open(dbName, version);
    result.onsuccess = (e: Event) => resolve((e.target as IDBOpenDBRequest).result);
    result.onerror = (e: Event) => reject((e.target as IDBOpenDBRequest).result);
    result.onupgradeneeded = function (e: IDBVersionChangeEvent) {
      const db = (e.target as IDBOpenDBRequest).result;
      const create = !Array.from(db.objectStoreNames).includes(dbInfo.store);

      dbInfo.upgrade?.(e, create ? db.createObjectStore(dbInfo.store) : undefined);
    };
  });
}
