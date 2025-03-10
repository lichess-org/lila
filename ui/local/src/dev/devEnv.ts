import type { DevBotCtrl } from './devBotCtrl';
//import type { GameCtrl } from '../gameCtrl';
import type { DevCtrl } from './devCtrl';
import type { DevAssets } from './devAssets';
import type { PushCtrl } from './pushCtrl';
import { LocalEnv, env as localEnv } from '../localEnv';
//import type { RoundController } from 'round';

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
    this.game.dev = this.dev;
    this.canPost = Boolean(this.canPost);
  }

  nameOf(uid?: string): string {
    return !uid || uid === this.user
      ? this.username
      : (uid.startsWith('#') && this.bot.bots.get(uid)?.name) || uid.charAt(0).toUpperCase() + uid.slice(1);
  }
}
