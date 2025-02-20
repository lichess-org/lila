import { showSetupDialog } from './setupDialog';
import { BotCtrl } from './botCtrl';
import { Assets } from './assets';
import { env, makeEnv } from './localEnv';
import type { LocalSetup } from './types';

export default async function initModule(opts: LocalSetup = {}): Promise<void> {
  opts = { ...JSON.parse(localStorage.getItem('local.setup') ?? '{}'), ...opts };
  opts.initial ??= Infinity;
  makeEnv({ redraw: () => {}, bot: await new BotCtrl().initBots(), assets: await new Assets().init() });
  console.log(env.bot.bots);
  showSetupDialog(opts);
}
