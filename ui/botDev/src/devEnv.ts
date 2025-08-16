import type { DevBotCtrl } from './devBotCtrl';
import type { DevCtrl } from './devCtrl';
import type { GameCtrl } from './gameCtrl';
import type { LocalDb } from './localDb';
import type { RoundController } from 'round';
import type { DevAssets } from './devAssets';
import type { PushCtrl } from './pushCtrl';

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

  redraw: () => void = () => {};

  constructor(cfg: Partial<DevEnv>) {
    Object.assign(this, cfg);
    env = this;
    if (this.game) this.game.observer = this.dev;
    this.canPost = Boolean(this.canPost);
  }
}
