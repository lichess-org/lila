import { showSetupDialog } from './setupDialog';
import { BotCtrl } from './botCtrl';
import { Assets } from './assets';
import type { LocalSetupOpts } from './types';

export default async function initModule(opts: LocalSetupOpts = {}): Promise<void> {
  if (localStorage.getItem('local.setup')) {
    opts = { ...opts, ...JSON.parse(localStorage.getItem('local.setup')!) };
  }
  opts.bots ??= (await fetch('/local/bots').then(res => res.json())).bots;
  showSetupDialog(await new BotCtrl().initBots(opts.bots!, new Assets()), opts);
}
