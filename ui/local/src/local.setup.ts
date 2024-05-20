import { LocalDialog } from './setupDialog';
import { Libots } from './bots/interfaces';

interface LocalModalOpts {}

export default async function initModule(opts: LocalModalOpts) {
  opts;

  const bots = await fetch(site.asset.url('bots.json')).then(x => x.json());
  const libots: Libots = {};
  for (const bot of bots) {
    libots[bot.uid.slice(1)] = {
      ...bot,
      imageUrl: site.asset.url(`lifat/bots/images/${bot.image}`, { version: 'bot000' }),
    };
  }
  //console.log(bots);
  return new LocalDialog(libots);
}
