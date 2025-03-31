import type { BotLoader } from './botLoader';
import type { GameCtrl } from './dev/gameCtrl';
import type { LocalDb } from './localDb';
import type { RoundController } from 'round';
import { myUserId, myUsername } from 'lib';

/** spaghetti and globals */
export let env: LocalEnv;

export function makeEnv(cfg: Partial<LocalEnv>): LocalEnv {
  return new LocalEnv(cfg);
}

export class LocalEnv {
  bot: BotLoader;
  game: GameCtrl;
  db: LocalDb;
  round: RoundController;
  redraw: () => void;

  constructor(cfg: Partial<LocalEnv>) {
    Object.assign(this, cfg);
    env = this;
  }

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
}
