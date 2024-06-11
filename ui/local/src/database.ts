import { ObjectStorage, objectStorage } from 'common/objectStorage';
import { GameState } from './gameCtrl';
import { Libot } from './types';

export async function makeDatabase(netCacheSize: number): Promise<Database> {
  return await new Database().init();
}

export class Database {
  gameState: ObjectStorage<GameState>;

  constructor() {}

  async init() {
    [this.gameState] = await Promise.all([objectStorage<GameState>({ store: 'local.state' })]);
    return this;
  }

  async getState(): Promise<GameState> {
    return this.gameState.get('current');
  }

  async putState(state: GameState) {
    return this.gameState.put('current', state);
  }
}
