import { LocalDb } from './localDb';
import { BotCtrl } from './botCtrl';
import { type LocalEnv, makeEnv } from './localEnv';

export default async function initModule(opts = {}): Promise<LocalEnv> {
  const [db, bot] = await Promise.all([new LocalDb().init(), new BotCtrl().initBots()]);
  return makeEnv({ db, bot });
}
