import { LocalDialog } from './setupDialog';
//import { Libots } from './bots/interfaces';

interface LocalModalOpts {}

export default async function initModule(opts: LocalModalOpts) {
  opts;

  const bots = await fetch(site.asset.url('bots.json')).then(x => x.json());

  for (const bot of bots) {
    bots[bot.uid.slice(1)] = {
      ...bot,
      imageUrl: site.asset.url(`lifat/bots/images/${bot.image}`, { version: 'bot000' }),
    };
  }

  return new LocalDialog(bots);
}
