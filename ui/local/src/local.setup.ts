import { showSetupDialog } from './setupDialog';
import { BotCtrl } from './botCtrl';
import { Assets } from './assets';
import { env } from './localEnv';
import type { LocalSetupOpts } from './types';

export default async function initModule(opts: LocalSetupOpts = {}): Promise<void> {
  if (localStorage.getItem('local.setup')) {
    opts = { ...opts, ...JSON.parse(localStorage.getItem('local.setup')!) };
  }
  opts.bots ??= (await fetch('/local/bots').then(res => res.json())).bots;
  env.redraw = () => {};
  env.assets = new Assets();
  env.bot = await new BotCtrl().initBots(opts.bots!);
  showSetupDialog(env.bot, opts);
}
