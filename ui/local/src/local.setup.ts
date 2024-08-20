import { showSetupDialog } from './setupDialog';
import { BotCtrl } from './botCtrl';
import { Assets } from './assets';
import { env, initEnv } from './localEnv';
import type { LocalSetup } from './types';

export default async function initModule(opts: LocalSetup = {}): Promise<void> {
  if (localStorage.getItem('local.setup')) {
    opts = { ...JSON.parse(localStorage.getItem('local.setup')!), ...opts };
    opts.initial ??= Infinity;
  }
  initEnv({ redraw: () => {}, bot: await new BotCtrl().initBots(), assets: await new Assets().init() });
  showSetupDialog(env.bot, opts);
}
