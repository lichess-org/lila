import type { BotCtrl } from './botCtrl';
import type { GameCtrl } from './gameCtrl';
import type { DevCtrl } from './dev/devCtrl';
import type { Assets } from './assets';
import type { PushCtrl } from './dev/pushCtrl';
import type { DevAssets } from './dev/devAssets';
import type { LocalDb } from 'game/localDb';
import type { RoundController } from 'round';
import { myUserId } from 'common';
import { pubsub } from 'common/pubsub';

export let env: LocalEnv;
let localDb: LocalDb;
pubsub.after<LocalDb>('local.gameDb.ready').then(db => (localDb = db));

export async function initEnv(cfg: Partial<LocalEnv>): Promise<LocalEnv> {
  return (env = new LocalEnv(cfg));
}

class LocalEnv {
  user: string;
  username: string;
  canPost: boolean;

  bot: BotCtrl;
  game: GameCtrl;
  dev: DevCtrl;
  assets: Assets;
  push: PushCtrl;
  round: RoundController;
  redraw: () => void;

  constructor(cfg: Partial<LocalEnv>) {
    Object.assign(this, cfg);
    this.user ??= myUserId() ?? 'anonymous';
    this.username ??= this.user.charAt(0).toUpperCase() + this.user.slice(1);
    this.canPost = Boolean(this.canPost);
  }

  get db(): LocalDb {
    return localDb;
  }

  get repo(): DevAssets {
    return this.assets as DevAssets;
  }

  get isDevPage(): boolean {
    return Boolean(this.dev);
  }
}
