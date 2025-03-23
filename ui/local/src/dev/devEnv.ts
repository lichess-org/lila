import type { DevBotCtrl } from './devBotCtrl';
import type { DevCtrl } from './devCtrl';
import type { DevAssets } from './devAssets';
import type { PushCtrl } from './pushCtrl';
import { LocalEnv } from '../localEnv';

export let env: DevEnv;

export function makeEnv(cfg: Partial<DevEnv>): DevEnv {
  return new DevEnv(cfg);
}

export class DevEnv extends LocalEnv {
  canPost: boolean;

  bot: DevBotCtrl;
  dev: DevCtrl;
  assets: DevAssets;
  push: PushCtrl;

  constructor(cfg: Partial<LocalEnv>) {
    super(cfg);
    env = this;
    if (this.game) this.game.observer = this.dev;
    this.canPost = Boolean(this.canPost);
  }
}
