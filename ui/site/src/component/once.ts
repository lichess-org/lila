import { storage } from "./storage";

const perPage = new Set<string>();

export default function once(key: string, mod?: 'always' | 'page' | undefined) {
  if (mod === 'always') return true;
  if (mod === 'page') {
    if (!perPage.has(key)) {
      perPage.add(key);
      return true;
    }
  }
  else if (!storage.get(key)) {
    storage.set(key, '1');
    return true;
  }
  return false;
}
