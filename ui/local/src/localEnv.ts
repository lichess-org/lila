import type { BotCtrl } from './botCtrl';
import type { GameCtrl } from './gameCtrl';
import type { DevCtrl } from './dev/devCtrl';
import type { Assets } from './assets';
import type { PushCtrl } from './dev/pushCtrl';
import type { DevAssets } from './dev/devAssets';
import type { LocalDb } from './localDb';
import type { RoundController } from 'round';
import { myUserId, myUsername } from 'common';

/** spaghetti and globals */
export let env: LocalEnv;

export async function makeEnv(cfg: Partial<LocalEnv>): Promise<LocalEnv> {
  return (env = new LocalEnv(cfg));
}

export class LocalEnv {
  user: string;
  username: string;
  canPost: boolean;

  bot: BotCtrl;
  game: GameCtrl;
  dev: DevCtrl;
  db: LocalDb;
  assets: Assets;
  push: PushCtrl;
  round: RoundController;
  redraw: () => void;

  constructor(cfg: Partial<LocalEnv>) {
    Object.assign(this, cfg);
    this.user ??= myUserId() ?? 'anonymous';
    this.username ??= myUsername() ?? this.user.charAt(0).toUpperCase() + this.user.slice(1);
    this.canPost = Boolean(this.canPost);
  }

  nameOf(uid?: string): string {
    return !uid || uid === this.user
      ? this.username
      : (uid.startsWith('#') && this.bot.bots.get(uid)?.name) || uid.charAt(0).toUpperCase() + uid.slice(1);
  }

  get repo(): DevAssets {
    return this.assets as DevAssets;
  }
}
