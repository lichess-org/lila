const registration = {
  version: 2,
  name: 'lichess',
  stores: ['analyse-state', 'service-worker-params'],
};

export interface ObjectStorage<V> {
  get(key: string): Promise<V>;
  put(key: string, value: V): Promise<string>; // returns key
  remove(key: string): Promise<void>; // delete one
  clear(): Promise<void>; // delete all
  list(): Promise<string[]>;
}

export async function objectStorage<V>(storeName: string): Promise<ObjectStorage<V>> {
  const name = storeName;
  const db = await dbAsync;

  const get = (key: string) => {
    const store = db.transaction(name, 'readonly').objectStore(name);
    return actionPromise<V>(store.get.bind(store, key));
  };

  const put = (key: string, value: V) => {
    const store = db.transaction(name, 'readwrite').objectStore(name);
    return actionPromise<string>(store.put.bind(store, value, key));
  };

  const remove = (key: string) => {
    const store = db.transaction(name, 'readwrite').objectStore(name);
    return actionPromise<void>(store.delete.bind(store, key));
  };

  const clear = () => {
    const store = db.transaction(name, 'readwrite').objectStore(name);
    return actionPromise<void>(store.clear.bind(store));
  };

  const list = () =>
    new Promise<string[]>((resolveOp, _) => {
      resolveOp([]); // TODO
    });

  const actionPromise = <V>(f: () => IDBRequest) =>
    new Promise<V>((resolve, reject) => {
      const res = f();
      res.onsuccess = (e: Event) => resolve((e.target as IDBRequest).result);
      res.onerror = (e: Event) => reject((e.target as IDBRequest).result);
    });

  return {
    get,
    put,
    remove,
    clear,
    list,
  };
}

const dbAsync = new Promise<IDBDatabase>((resolve, reject) => {
  const result = globalThis.indexedDB.open(registration.name, registration.version);
  result.onsuccess = (e: Event) => resolve((e.target as IDBOpenDBRequest).result);
  result.onerror = (e: Event) => reject((e.target as IDBOpenDBRequest).result);

  result.onupgradeneeded = function (ev: IDBVersionChangeEvent) {
    const db = (ev.target as IDBOpenDBRequest).result;
    const previousStores = Array.from(db.objectStoreNames);

    const deletes = previousStores.filter(x => !registration.stores.includes(x));
    const creates = registration.stores.filter(x => !previousStores.includes(x));

    deletes.forEach(x => db.deleteObjectStore(x));
    creates.forEach(x => db.createObjectStore(x));
  };
});
