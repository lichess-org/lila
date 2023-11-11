import { Libot } from './interfaces';
import makeZerofish from 'zerofish';
import { makeCtrl, registry } from './ctrl';
export { type Ctrl } from './ctrl';
export * from './interfaces';
export * from './index.gen';

export async function initModule(stubs = false) {
  const libots: { [id: string]: Libot } = {};
  const zf = !stubs ? await makeZerofish() : undefined;
  for (const name in registry) {
    libots[name] = new registry[name](zf);
  }
  if (zf) return makeCtrl(libots, zf);
  else return libots;
}
