import type { DevBotCtrl } from './devBotCtrl';
import type { DevCtrl } from './devCtrl';
import type { GameCtrl } from './gameCtrl';
import type { LocalDb } from './localDb';
import type { RoundController } from 'round';
import type { DevAssets } from './devAssets';
import type { PushCtrl } from './pushCtrl';
import { myUserId, myUsername } from 'lib';

export let env: DevEnv;

export function makeEnv(cfg: Partial<DevEnv>): DevEnv {
  return new DevEnv(cfg);
}

export class DevEnv {
  canPost: boolean;
  game: GameCtrl;
  db: LocalDb;
  bot: DevBotCtrl;
  dev: DevCtrl;
  assets: DevAssets;
  push: PushCtrl;
  round: RoundController;

  redraw: () => void;

  get user(): string {
    return myUserId() ?? 'anonymous';
  }

  get username(): string {
    return myUsername() ?? 'Anonymous';
  }

  nameOf(uid?: string): string {
    return !uid || uid === this.user
      ? this.username
      : (uid.startsWith('#') && this.bot.bots.get(uid)?.name) || uid.charAt(0).toUpperCase() + uid.slice(1);
  }

  constructor(cfg: Partial<DevEnv>) {
    Object.assign(this, cfg);
    env = this;
    if (this.game) this.game.observer = this.dev;
    this.canPost = Boolean(this.canPost);
  }
}
