import type { BotCtrl } from './botCtrl';
import type { GameCtrl } from './gameCtrl';
import type { DevCtrl } from './dev/devCtrl';
import type { Assets } from './assets';
import type { ShareCtrl } from './dev/shareCtrl';
import type { DevAssets } from './dev/devAssets';
import type { RoundController } from 'round';

class LocalEnv {
  bot: BotCtrl;
  game: GameCtrl;
  dev: DevCtrl;
  assets: Assets;
  share: ShareCtrl;
  round: RoundController;
  redraw: () => void;
  readonly user: string = document.body.dataset.user ?? 'Anonymous';

  get repo(): DevAssets {
    return this.assets as DevAssets;
  }
}

export const env: LocalEnv = new LocalEnv();
