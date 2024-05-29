import { LocalDialog } from './setupDialog';
import { withCards } from './local';
import { Libots } from './interfaces';

interface LocalModalOpts {}

export default async function initModule(opts: LocalModalOpts) {
  opts;

  const bots = await fetch(site.asset.url('bots.json')).then(x => x.json());
  const libots: Libots = {};
  for (const bot of withCards(bots)) libots[bot.uid] = bot;
  return new LocalDialog(libots);
}
