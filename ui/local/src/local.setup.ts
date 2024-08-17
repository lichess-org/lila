import { showSetupDialog } from './setupDialog';
import { BotCtrl } from './botCtrl';
import { Assets } from './assets';
import { env, initEnv } from './localEnv';
import type { LocalSetup } from './types';

export default async function initModule(opts: LocalSetup = {}): Promise<void> {
  if (localStorage.getItem('local.setup')) {
    opts = { ...JSON.parse(localStorage.getItem('local.setup')!), ...opts };
  }
  initEnv();
  env.redraw = () => {};
  [env.assets, env.bot] = await Promise.all([new Assets().init(), new BotCtrl().initBots()]);
  showSetupDialog(env.bot, opts);
}
