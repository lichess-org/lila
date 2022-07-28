const registration = {
  version: 1,
  name: 'lichess',
  stores: ['analyse-state'],
};

/* 
To add a new object store to browser databases, add the store name to
registration.stores above and then bump registration.version otherwise it will
not be created.  that's necessary because new object stores can only be created
during an "upgrade" event issued when the database is opened, see bottom of file.

Maybe in the future we check new store requests against the
currently open connection's list of stores, and if it's not there, bump the 
version number and reopen the connection which triggers the event where we can
create the new store.  That's TODO, for now just hard code store names manually
in the above registration object before requesting them from outside code.
*/

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
  const result = indexedDB.open(registration.name, registration.version);
  result.onsuccess = (e: Event) => resolve((e.target as IDBOpenDBRequest).result);
  result.onerror = (e: Event) => reject((e.target as IDBOpenDBRequest).result);

  /* 
  This obviously does not handle upgrades to specific object store versions.
  We can add a mechanism where calling code can register an upgrade hook to get
  db and store object as parameters on upgrade event, otherwise the below will
  get messy quick. again, idb object stores can only be non-trivially upgraded
  within a version upgrade event context.
*/
  result.onupgradeneeded = function (ev: IDBVersionChangeEvent) {
    const db = (ev.target as IDBOpenDBRequest).result;
    const previousStores = Array.from(db.objectStoreNames);

    // const deletes = previousStores.filter(x => !registration.stores.includes(x));
    // const creates = registration.stores.filter(x => !previousStores.includes(x));
    // just blow everything away for now
    previousStores.forEach(x => db.deleteObjectStore(x));
    registration.stores.forEach(x => db.createObjectStore(x));
  };
});
