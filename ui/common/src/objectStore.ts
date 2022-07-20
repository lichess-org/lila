const registration = {
  version: 3,
  name: 'lichess-test',
  stores: ['analyse-state'],
};
/* 
to add a new object store to browser databases, add the store name to
registration.stores above and then bump registration.version otherwise it will
not be created.  that's necessary because new object stores can only be created
during an "upgrade" event issued when the database is opened, see bottom of file.

It seems technically possible to handle this automatically here in objectStore.ts,
by checking a new request for a store against the currently open connection's
list of stores, and if it's not there, bumping the version number and
reopening the connection which triggers the event where we can create the new
store.  That's TODO, for now just hard code store names manually in the above 
registration object before requesting them from outside code.
*/

export interface ObjectStorage<V> {
  get(key: string): Promise<V>;
  put(key: string, value: V): Promise<V>;
  remove(key: string): Promise<void>;
  list(): Promise<string[]>;
}

export async function objectStorage<V>(storeName: string): Promise<ObjectStorage<V>> {
  const name = storeName;
  const db = await dbAsync;
  const accessor = (key: string, v?: V) =>
    new Promise<V>((resolve, reject) => {
      const mode = v ? 'readwrite' : 'readonly';
      const store = db.transaction(name, mode).objectStore(name);

      const rqst = v ? store.put(v, key) : store.get(key);
      rqst.onsuccess = (e: Event) => resolve(v ? v : (e.target as IDBRequest).result);
      rqst.onerror = (e: Event) => reject((e.target as IDBRequest).result);
    });
  const remove = (key: string) =>
    new Promise<void>((resolve, reject) => {
      const res = db.transaction(name, 'readwrite').objectStore(name).delete(key);
      res.onsuccess = () => resolve();
      res.onerror = () => reject();
    });
  const list = () =>
    new Promise<string[]>((resolveOp, _) => {
      resolveOp([]); // TODO
    });
  return {
    get: (key: string) => accessor(key),
    put: (key: string, value: V) => accessor(key, value),
    remove,
    list,
  };
}

const dbAsync = new Promise<IDBDatabase>((resolve, reject) => {
  const result = indexedDB.open(registration.name, registration.version);
  result.onsuccess = (e: Event) => resolve((e.target as IDBOpenDBRequest).result);
  result.onerror = (e: Event) => reject((e.target as IDBOpenDBRequest).result);

  /* 
  this obviously does not handle upgrades to specific object store versions.
  should add a mechanism where calling code can register an upgrade hook to get
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
