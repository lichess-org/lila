import { type ObjectStorage, objectStorage } from 'common/objectStorage';
import { LocalGame } from './localGame';
import { LocalSetup, clockToSpeed } from './game';
import { myUserId } from 'common';

type GameId = string;
const userId = myUserId() ?? 'anonymous';

export class LocalDb {
  store: ObjectStorage<LocalGame, GameId>;
  idx: ObjectStorage<LocalGameIdx, GameId>;
  current?: GameId;

  constructor() {}
  async init(): Promise<this> {
    [this.store, this.idx] = await Promise.all([
      objectStorage<LocalGame, GameId>({
        db: `local.${userId}.games--db`,
        store: 'local.games',
      }),
      objectStorage<LocalGameIdx, GameId>({
        db: `local.${userId}.games--db`,
        store: 'local.games.idx',
        indices: [
          // { name: 'createdAt', keyPath: 'createdAt' },
          // { name: 'createdBy', keyPath: 'createdBy' },
          // { name: 'white', keyPath: 'white' },
          // { name: 'black', keyPath: 'black' },
        ],
      }),
    ]);
    return this;
  }
  get lastId(): GameId | undefined {
    return localStorage.getItem(`local.${userId}.gameId`) || undefined;
  }
  set lastId(id: GameId | undefined) {
    if (id) localStorage.setItem(`local.${userId}.gameId`, id);
    else localStorage.removeItem(`local.${userId}.gameId`);
  }
  getLast(): Promise<LocalGame | undefined> {
    return this.lastId ? this.store.get(this.lastId) : Promise.resolve(undefined);
  }
  getOne(id: GameId): Promise<LocalGame | undefined> {
    return this.store.get(id);
  }
  async getIdx(ids?: GameId[]): Promise<LocalGameIdx[]> {
    if (!ids) ids = await this.idx.list();
    return Promise.all(ids.map(id => this.idx.get(id))).then(games => games.filter(g => g));
  }
  delete(ids?: GameId[] | GameId): Promise<any> {
    if (!ids) return Promise.all([this.store.clear(), this.idx.clear()]);
    else
      return Promise.all(
        [ids].flat().map(id => {
          this.store.remove(id);
          this.idx.remove(id);
        }),
      );
  }
  async put(game: LocalGame): Promise<void> {
    game = structuredClone(game);
    await Promise.all([this.store.put(game.id, game), this.idx.put(game.id, new LocalGameIdx(game))]);
    this.lastId = game.id;
  }
  async list(): Promise<GameId[]> {
    return this.store.list();
  }
  async listMine(): Promise<GameId[]> {
    return this.store.cursor({ index: 'createdBy', keys: IDBKeyRange.only(userId) }).then(cursor => {
      const ids: GameId[] = [];
      while (cursor?.value) {
        ids.push(cursor.value.id);
        cursor.continue();
      }
      return ids;
    });
  }
}

class LocalGameIdx implements LocalSetup {
  id: string;
  white: string;
  black: string;
  createdBy: string;
  createdAt: Millis;
  initial: Seconds;
  increment: Seconds;
  speed: Speed;
  initialFen: FEN;
  fen: FEN;
  finished: boolean;
  winner?: Color;
  winnerId?: string;
  loserId?: string;
  constructor(game: LocalGame) {
    this.id = game.id;
    this.createdAt = game.createdAt;
    this.createdBy = game.createdBy;
    this.white = game.white ?? userId;
    this.black = game.black ?? userId;
    this.initial = game.initial;
    this.increment = game.increment;
    this.speed = game.initial === Infinity ? 'correspondence' : clockToSpeed(game.initial, game.increment);
    this.initialFen = game.initialFen;
    this.fen = game.fen;
    this.winner = game.status.winner;
    if (this.winner) {
      this.winnerId = this[this.winner];
      this.loserId = this[this.winner === 'white' ? 'black' : 'white'];
    }
    this.finished = game.status.status.id >= 30;
  }
}
