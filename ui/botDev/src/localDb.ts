import { type ObjectStorage, objectStorage, range } from 'lib/objectStorage';
import { type LocalGame, LocalGameData } from './localGame';
import { type StatusId, clockToSpeed, status } from 'lib/game';
import { myUserId } from 'lib';
import { hasFeature } from 'lib/device';

export class LocalDb {
  store: ObjectStorage<LocalGameData> | undefined;
  liteStore: ObjectStorage<LiteGame> | undefined;

  constructor() {}

  async init(): Promise<this> {
    if (!hasFeature('structuredClone')) globalThis.structuredClone = obj => JSON.parse(JSON.stringify(obj));

    [this.store, this.liteStore] = await Promise.all([
      objectStorage<LocalGameData>({
        store: 'botdev.games',
        version: 1,
      }),
      objectStorage<LiteGame>({
        store: 'botdev.games.lite',
        version: 1,
        indices: [
          { name: 'createdAt', keyPath: 'createdAt' },
          { name: 'white', keyPath: 'white' },
          { name: 'black', keyPath: 'black' },
          { name: 'speed', keyPath: 'speed' },
          { name: 'status', keyPath: 'status' },
          { name: 'winner', keyPath: 'winner' },
        ],
      }),
    ]).catch(() => []);
    return this;
  }

  get lastId(): string | undefined {
    return localStorage.getItem(`botdev.${myUserId() ?? 'anonymous'}.gameId`) || undefined;
  }

  set lastId(id: string | undefined) {
    if (id) localStorage.setItem(`botdev.${myUserId() ?? 'anonymous'}.gameId`, id);
    else localStorage.removeItem(`botdev.${myUserId() ?? 'anonymous'}.gameId`);
  }

  async get(id: string | undefined = this.lastId): Promise<LocalGameData | undefined> {
    // optional chaining in query methods as idb can be undefined in legacy incognito
    return id ? await this.store?.get(id) : Promise.resolve(undefined);
  }

  async byDate(after: number | undefined, until: number | undefined): Promise<LiteGame[]> {
    const games: LiteGame[] = [];
    await this.liteStore?.readCursor(
      { index: 'createdAt', query: range({ above: after, max: until }) },
      info => games.push(info),
    );
    return games;
  }

  async ongoing(): Promise<LiteGame[]> {
    const games: LiteGame[] = [];
    await this.liteStore?.readCursor({ index: 'status', query: status.started }, info => games.push(info));
    return games;
  }

  async put(game: LocalGame): Promise<void> {
    const lite = makeLite(game);
    const data = structuredClone(game);
    await Promise.all([this.store?.put(data.id, data), this.liteStore?.put(data.id, lite)]);
    this.lastId = lite.status === status['started'] ? data.id : undefined;
  }

  delete(ids?: string[] | string): Promise<void[]> {
    if (!ids) return Promise.all([this.store?.clear(), this.liteStore?.clear()]);
    else
      return Promise.all(
        [ids].flat().map(id => {
          this.store?.remove(id);
          this.liteStore?.remove(id);
        }),
      );
  }
}

export class LiteGame extends LocalGameData {
  speed: Speed;
  fen: FEN;
  turn: Color;
  lastMove: Uci;
  status: StatusId;
  winner?: string;
}

function makeLite(game: LocalGame): LiteGame {
  return {
    ...game,
    speed: game.initial === Infinity ? 'correspondence' : clockToSpeed(game.initial, game.increment),

    fen: game.fen,
    turn: game.turn,
    lastMove: !game.moves.length ? '' : game.moves[game.moves.length - 1].uci,
    status: game.finished?.status?.id ?? status['started'],
    winner: game.finished?.winner,
  };
}
