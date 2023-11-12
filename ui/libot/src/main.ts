import { Libots } from './interfaces';
import makeZerofish from 'zerofish';
import { makeCtrl, registry } from './ctrl';
export { type Ctrl as LibotCtrl } from './ctrl';
export * from './interfaces';
export * from './index.gen';

export async function initModule(stubs = false) {
  const libots: Libots = {
    sort: () => Object.values(libots).sort((a, b) => (a.ordinal < b.ordinal ? -1 : 1)),
    bots: {},
  };
  const zf = !stubs ? await makeZerofish({ root: lichess.assetUrl('npm') }) : undefined;
  for (const name in registry) {
    libots.bots[name] = new registry[name](zf);
  }
  if (zf) return makeCtrl(libots, zf);
  else return libots;
}
