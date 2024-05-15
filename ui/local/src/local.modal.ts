import { LocalDialog } from './view/modal';
import { Libots } from './bots/interfaces';

interface LocalModalOpts {}

export default async function initModule(opts: LocalModalOpts) {
  opts;
  const libots: Libots = {
    sort: () => Object.values(libots.bots).sort((a, b) => (a.ordinal < b.ordinal ? -1 : 1)),
    bots: {},
  };

  const bots = await fetch(site.asset.url('bots.json')).then(x => x.json());

  let i = 0;
  for (const bot of bots) {
    libots.bots[bot.uid.slice(1)] = {
      ...bot,
      imageUrl: site.asset.url(`lifat/bots/images/${bot.image}`, { version: 'bot000' }),
      ordinal: i++,
    };
  }

  return new LocalDialog(libots);
}
