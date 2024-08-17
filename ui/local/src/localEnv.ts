import type { BotCtrl } from './botCtrl';
import type { GameCtrl } from './gameCtrl';
import type { DevCtrl } from './dev/devCtrl';
import type { Assets } from './assets';
import type { ShareCtrl } from './dev/shareCtrl';
import type { DevAssets } from './dev/devAssets';
import type { RoundController } from 'round';

export let env: LocalEnv;

export function initEnv(user?: string, username?: string): void {
  user ??= document.body.dataset.user ?? 'anonymous';
  username ??= user.charAt(0).toUpperCase() + user.slice(1);
  env = new LocalEnv(user, username);
}
class LocalEnv {
  constructor(
    readonly user: string,
    readonly username: string,
  ) {}

  bot: BotCtrl;
  game: GameCtrl;
  dev: DevCtrl;
  assets: Assets;
  share: ShareCtrl;
  round: RoundController;
  redraw: () => void;

  get repo(): DevAssets {
    return this.assets as DevAssets;
  }
  get isDev(): boolean {
    return Boolean(this.dev);
  }
}
