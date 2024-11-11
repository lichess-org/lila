import { createStore, get, set, del } from 'idb-keyval';

export class Cache {
  private store;

  constructor(name: string) {
    this.store = createStore(`${name}--db`, `${name}--store`);
  }

  async get(key: string, version: string): Promise<[boolean, any]> {
    const cachedVersion = await get(`${key}--version`, this.store);
    if (cachedVersion !== version) {
      return [false, undefined];
    }
    const data = await get(`${key}--data`, this.store);
    return [true, data];
  }

  async set(key: string, version: string, data: any): Promise<void> {
    const cachedVersion = await get(`${key}--version`, this.store);
    if (cachedVersion === version) {
      return;
    }
    await del(`${key}--version`, this.store);
    await set(`${key}--data`, data, this.store);
    await set(`${key}--version`, version, this.store);
  }
}
