import { storage } from './storage';

export default function once(key: string, mod?: 'always' | undefined) {
  if (mod === 'always') return true;
  if (storage.get(key)) return false;

  storage.set(key, '1');
  return true;
}
