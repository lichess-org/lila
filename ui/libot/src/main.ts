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

  const zfAsync = !stubs ? makeZerofish({ root: lichess.asset.url('npm') }) : Promise.resolve(undefined);

  const botsAsync = fetch(lichess.asset.url('bots.json')).then(x => x.json());

  const [zf, bots] = await Promise.all([zfAsync, botsAsync]);

  console.log(bots);
  for (const bot of bots) {
    libots.bots[bot.uid.slice(1)] = zf
      ? new ZfBot(bot, zf)
      : { ...bot, imageUrl: lichess.asset.url(`lifat/bots/images/${bot.image}`, { noVersion: true }) };
  }
  return zf ? makeCtrl(libots, zf) : libots;
}
