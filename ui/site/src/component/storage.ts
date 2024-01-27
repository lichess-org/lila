import sri from './sri';

const builder = (storage: Storage): LichessStorageHelper => {
  const api = {
    get: (k: string): string | null => storage.getItem(k),
    set: (k: string, v: string): void => storage.setItem(k, v),
    fire: (k: string, v?: string) =>
      storage.setItem(
        k,
        JSON.stringify({
          sri,
          nonce: Math.random(), // ensure item changes
          value: v,
        }),
      ),
    remove: (k: string) => storage.removeItem(k),
    make: (k: string, ttl?: number) => {
      const bdKey = ttl && `${k}--bd`;
      const remove = () => {
        api.remove(k);
        if (bdKey) api.remove(bdKey);
      };
      return {
        get: () => {
          if (!bdKey) return api.get(k);
          const birthday = Number(api.get(bdKey));
          if (!birthday) api.set(bdKey, String(Date.now()));
          else if (Date.now() - birthday > ttl) remove();
          return api.get(k);
        },
        set: (v: any) => {
          api.set(k, v);
          if (bdKey) api.set(bdKey, String(Date.now()));
        },
        fire: (v?: string) => api.fire(k, v),
        remove,
        listen: (f: (e: LichessStorageEvent) => void) =>
          window.addEventListener('storage', e => {
            if (e.key !== k || e.storageArea !== storage || e.newValue === null) return;
            let parsed: LichessStorageEvent | null;
            try {
              parsed = JSON.parse(e.newValue);
            } catch (_) {
              return;
            }
            // check sri, because Safari fires events also in the original
            // document when there are multiple tabs
            if (parsed?.sri && parsed.sri !== sri) f(parsed);
          }),
      };
    },
    boolean: (k: string) => ({
      get: () => api.get(k) == '1',
      getOrDefault: (defaultValue: boolean) => {
        const stored = api.get(k);
        return stored === null ? defaultValue : stored == '1';
      },
      set: (v: boolean): void => api.set(k, v ? '1' : '0'),
      toggle: () => api.set(k, api.get(k) == '1' ? '0' : '1'),
    }),
  };
  return api;
};

export const storage = builder(window.localStorage);
export const tempStorage = builder(window.sessionStorage);
