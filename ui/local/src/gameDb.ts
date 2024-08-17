import { objectStorage, type ObjectStorage } from 'common/objectStorage';
//import type { GameState } from './game';
export interface GameState {
  initialFen: string;
  moves: Uci[];
  threefoldFens?: Map<string, number>;
  fiftyHalfMove?: number;
  white: string | undefined;
  black: string | undefined;
}
type GameId = string;

export class GameDb {
  private storage: ObjectStorage<GameState, GameId>;

  constructor() {
    this.init();
  }

  private async init() {
    this.storage = await objectStorage<GameState, GameId>({
      store: 'games',
    });
  }

  async save(gameId: GameId, state: GameState): Promise<void> {
    const stateWithTimestamp = {
      ...state,
      timestamp: Date.now(),
    };
    await this.storage.put(gameId, stateWithTimestamp);
  }

  async get(gameId: GameId): Promise<GameState | undefined> {
    try {
      return await this.storage.get(gameId);
    } catch (error) {
      console.error(`Error retrieving game state for ID ${gameId}:`, error);
      return undefined;
    }
  }

  async list(): Promise<GameId[]> {
    return await this.storage.list();
  }

  async delete(gameId: GameId): Promise<void> {
    await this.storage.remove(gameId);
  }

  async clear(): Promise<void> {
    await this.storage.clear();
  }
}
