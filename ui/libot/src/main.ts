import { Libots } from './interfaces';
import makeZerofish from 'zerofish';
import { makeCtrl } from './ctrl';
import { ZfBot } from './zfbot';
export { type Ctrl as LibotCtrl } from './ctrl';
export * from './interfaces';

export async function initModule(stubs = false) {
  const libots: Libots = {
    sort: () => Object.values(libots.bots).sort((a, b) => (a.ordinal < b.ordinal ? -1 : 1)),
    bots: {},
  };

  const zfAsync = !stubs ? makeZerofish({ root: lichess.assetUrl('npm') }) : Promise.resolve(undefined);

  const botsAsync = fetch(lichess.assetUrl('bots.json')).then(x => x.json());

  const [zf, bots] = await Promise.all([zfAsync, botsAsync]);

  for (const bot of bots) {
    if (zf) libots.bots[bot.uid.slice(1)] = new ZfBot(bot, zf);
    else libots.bots[bot.uid.slice(1)] = bot;
  }
  return zf ? makeCtrl(libots, zf) : libots;
}
