import { SetupDialog } from './setupDialog';
import { BotCtrl } from './botCtrl';

interface LocalModalOpts {}

export default async function initModule(opts: LocalModalOpts): Promise<SetupDialog> {
  opts;
  return new SetupDialog(await new BotCtrl().initLibots());
}
