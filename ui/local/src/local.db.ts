import { LocalDb } from './localDb';
import { BotCtrl } from './botCtrl';
import { Assets } from './assets';
import { type LocalEnv, makeEnv } from './localEnv';

export default async function initModule(): Promise<LocalEnv> {
  const [db, bot] = await Promise.all([new LocalDb().init(), new BotCtrl().initBotsOnly()]);
  return makeEnv({ db, bot, assets: new Assets() });
}
