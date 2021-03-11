import * as idb from 'idb-keyval';

export class Cache {
  private store;

  constructor(name: string) {
    this.store = idb.createStore(`${name}--db`, `${name}--store`);
  }

  async get(key: string, version: string): Promise<[boolean, any]> {
    const cachedVersion = await idb.get(`${key}--version`, this.store);
    if (cachedVersion !== version) {
      return [false, undefined];
    }
    const data = await idb.get(`${key}--data`, this.store);
    return [true, data];
  }

  async set(key: string, version: string, data: any): Promise<void> {
    const cachedVersion = await idb.get(`${key}--version`, this.store);
    if (cachedVersion === version) {
      return;
    }
    await idb.set(`${key}--version`, version, this.store);
    await idb.set(`${key}--data`, data, this.store);
  }
}
