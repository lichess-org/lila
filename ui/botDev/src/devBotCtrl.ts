import { type Zerofish } from '@lichess-org/zerofish';
import { Bot } from 'lib/bot/bot';
import { RateBot } from './rateBot';
import { BotLoader, botAssetUrl } from 'lib/bot/botLoader';
import { type ObjectStorage, objectStorage } from 'lib/objectStorage';
import { deepFreeze } from 'lib/algo';
import { pubsub } from 'lib/pubsub';
import { type OpeningBook, makeBookFromPolyglot } from 'lib/game/polyglot';
import { env } from './devEnv';
import type { BotInfo, LocalSpeed, MoveArgs, MoveResult, MoveSource } from 'lib/bot/types';
import type { CardData } from './handOfCards';
import { defined } from 'lib';

const currentBotDbVersion = 3;

export class DevBotCtrl extends BotLoader {
  readonly rateBots: RateBot[] = [];
  readonly uids: Record<Color, string | undefined> = { white: undefined, black: undefined };
  private store: ObjectStorage<BotInfo>;
  localBots: Record<string, BotInfo> = {};
  serverBots: Record<string, BotInfo> = {};

  constructor(zf?: Zerofish) {
    super(zf);
    for (let i = 0; i <= RateBot.MAX_LEVEL; i++) {
      this.rateBots.push(new RateBot(i));
    }
  }

  async init(defBots?: BotInfo[]): Promise<this> {
    const [localBots] = await Promise.all([this.storedBots(), super.init(defBots)]);
    this.localBots = Object.fromEntries(localBots.map(b => [b.uid, deepFreeze(b)]));
    this.serverBots = Object.fromEntries(
      [...this.bots.entries()].map(x => [x[0], deepFreeze(structuredClone(x[1]))]),
    );
    for (const [uid, botInfo] of Object.entries(this.localBots)) this.bots.set(uid, new Bot(botInfo, this));
    await Promise.all(
      [...new Set<string>(Object.values(this.bots).map(b => botAssetUrl('image', b.image)))].map(
        url =>
          new Promise<void>(resolve => {
            const img = new Image();
            img.src = url;
            img.onload = () => resolve();
            img.onerror = () => resolve();
          }),
      ),
    );
    if (this.uids.white && !this.bots.has(this.uids.white)) this.uids.white = undefined;
    if (this.uids.black && !this.bots.has(this.uids.black)) this.uids.black = undefined;
    pubsub.complete('botdev.images.ready');
    return this;
  }

  async move(args: MoveArgs): Promise<MoveResult | undefined> {
    const bot = this[args.chess.turn] as BotInfo & MoveSource;
    if (!bot) return undefined;
    if (this.busy) return undefined; // ignore different call stacks
    this.busy = true;
    const move = await bot.move(args);
    this.busy = false;
    return move?.uci !== '0000' ? move : undefined;
  }

  setUids({ white, black }: { white?: string | undefined; black?: string | undefined }): void {
    this.uids.white = white;
    this.uids.black = black;
    this.reset();
    this.preload([this.uids.white, this.uids.black].filter(defined));
  }

  get white(): BotInfo | undefined {
    return this.info(this.uids.white);
  }

  get black(): BotInfo | undefined {
    return this.info(this.uids.black);
  }

  get isBusy(): boolean {
    return this.busy;
  }

  get all(): BotInfo[] {
    return [...this.bots.values()] as Bot[];
  }

  get playing(): BotInfo[] {
    return [this.white, this.black].filter(defined);
  }

  get firstUid(): string | undefined {
    return this.bots.keys().next()?.value;
  }

  storeBot(bot: BotInfo): Promise<any> {
    delete this.localBots[bot.uid];
    this.bots.set(bot.uid, new Bot(bot, this));
    if (botEquals(this.serverBots[bot.uid], bot)) return this.store.remove(bot.uid);
    this.localBots[bot.uid] = deepFreeze(structuredClone(bot));
    return this.store.put(bot.uid, bot);
  }

  async deleteStoredBot(uid: string): Promise<void> {
    await this.store.remove(uid);
    this.bots.delete(uid);
    await this.init();
  }

  async clearStoredBots(uids?: string[]): Promise<void> {
    await (uids ? Promise.all(uids.map(uid => this.store.remove(uid))) : this.store.clear());
    await this.init();
  }

  async setServerBot(bot: BotInfo): Promise<void> {
    this.bots.set(bot.uid, new Bot(bot, this));
    this.serverBots[bot.uid] = deepFreeze(structuredClone(bot));
    delete this.localBots[bot.uid];
    await this.store.remove(bot.uid);
  }

  info(uid: string | undefined): BotInfo | undefined {
    if (uid === undefined) return undefined;
    return this.bots.get(uid) ?? this.rateBots[Number(uid.slice(1))];
  }

  card(bot: BotInfo): CardData {
    return {
      label: `${bot.name}${bot.ratings?.classical ? ' ' + bot.ratings.classical : ''}`,
      domId: uidToDomId(bot.uid)!,
      imageUrl: this.imageUrl(bot),
      classList: [],
    };
  }

  groupedCard(bot: BotInfo, isDirty?: (b: BotInfo) => boolean): CardData | undefined {
    const cd = this.card(bot);
    const local = this.localBots[bot.uid];
    const server = this.serverBots[bot.uid];
    if (isDirty?.(local ?? server)) cd?.classList.push('dirty');
    if (!server) cd?.classList.push('local-only');
    else if (server.version > bot.version) cd?.classList.push('upstream-changes');
    else if (local && !botEquals(local, server)) cd?.classList.push('local-changes');
    return cd;
  }

  groupedSort(speed: LocalSpeed = 'classical'): (a: CardData, b: CardData) => number {
    return (a, b) => {
      for (const c of ['dirty', 'local-only', 'local-changes', 'upstream-changes']) {
        if (a.classList.includes(c) && !b.classList.includes(c)) return -1;
        if (!a.classList.includes(c) && b.classList.includes(c)) return 1;
      }
      const [ab, bb] = [this.info(domIdToUid(a.domId)), this.info(domIdToUid(b.domId))];
      return Bot.rating(ab, speed) - Bot.rating(bb, speed) || a.label.localeCompare(b.label);
    };
  }

  async getBook(key: string | undefined): Promise<OpeningBook | undefined> {
    if (!key) return undefined;
    if (this.book.has(key)) return this.book.get(key);
    if (!env.assets.idb.book.keyNames.has(key)) return super.getBook(key);
    const bookPromise = env.assets.idb.book
      .get(key)
      .then(res => res.blob.arrayBuffer())
      .then(buf => makeBookFromPolyglot({ bytes: new DataView(buf) }))
      .then(result => result.getMoves);
    this.book.set(key, bookPromise);
    return bookPromise;
  }

  getImageUrl(key: string): string {
    return env.assets.urls.image.get(key) ?? super.getImageUrl(key);
  }

  getSoundUrl(key: string): string {
    return env.assets.urls.sound.get(key) ?? super.getSoundUrl(key);
  }

  protected storedBots(): Promise<BotInfo[]> {
    return (
      this.store?.getMany() ??
      objectStorage<BotInfo>({
        store: 'botdev.bots',
        version: currentBotDbVersion,
        upgrade: this.upgrade,
      }).then(s => {
        this.store = s;
        return s.getMany();
      })
    );
  }

  private upgrade = (change: IDBVersionChangeEvent, store: IDBObjectStore): void => {
    const req = store.openCursor();
    req.onsuccess = e => {
      const cursor = (e.target as IDBRequest<IDBCursorWithValue>).result;
      if (!cursor) return;
      cursor.update(migrate(change.oldVersion, cursor.value));
      cursor.continue();
    };
  };
}

export function uidToDomId(uid: string | undefined): string | undefined {
  return uid?.startsWith('#') ? `bot-id-${uid.slice(1)}` : undefined;
}

export function domIdToUid(domId: string | undefined): string | undefined {
  return domId && domId.startsWith('bot-id-') ? `#${domId.slice(7)}` : undefined;
}

export function botEquals(a: BotInfo | undefined, b: BotInfo | undefined): boolean {
  if (!closeEnough(a, b, ['filters', 'version'])) return false;
  const [aFilters, bFilters] = [a, b].map(bot =>
    Object.entries(bot?.filters ?? {}).filter(([_, v]) => v.move || v.time || v.score),
  );
  return closeEnough(aFilters, bFilters); // allow empty filter craplets to differ
}

function closeEnough(a: any, b: any, ignore: string[] = []): boolean {
  if (a === b) return true;
  if (typeof a !== typeof b) return false;
  if (Array.isArray(a)) {
    return Array.isArray(b) && a.length === b.length && a.every((x, i) => closeEnough(x, b[i], ignore));
  }
  if (typeof a !== 'object') return false;

  const [aKeys, bKeys] = [filteredKeys(a, ignore), filteredKeys(b, ignore)];
  if (aKeys.length !== bKeys.length) return false;

  for (const key of aKeys) {
    if (!bKeys.includes(key) || !closeEnough(a[key], b[key], ignore)) return false;
  }
  return true;
}

function filteredKeys(obj: any, ignore: string[] = []): string[] {
  if (typeof obj !== 'object') return obj;
  return Object.entries(obj)
    .filter(([k, v]) => !ignore.includes(k) && !isEmpty(v))
    .map(([k]) => k);
}

function isEmpty(prop: any): boolean {
  return Array.isArray(prop) ? false : typeof prop === 'object' ? Object.keys(prop).length === 0 : false;
}

function migrate(oldDbVersion: number, oldBot: any): BotInfo {
  const bot = structuredClone(oldBot);
  if (oldDbVersion < currentBotDbVersion && bot && bot.fish) {
    // fish search params recently flattened in BotInfo schema, delete before release
    bot.fish.depth = 'by' in bot.fish && 'depth' in bot.fish.by ? bot.fish.by.depth : 10;
  }
  return bot;
}
