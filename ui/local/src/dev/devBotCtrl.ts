import { type Zerofish } from 'zerofish';
import { Bot } from '../bot';
import { RateBot } from './rateBot';
import { BotCtrl } from '../botCtrl';
import { type ObjectStorage, objectStorage } from 'common/objectStorage';
import { deepFreeze } from 'common/algo';
import { pubsub } from 'common/pubsub';
import { botAssetUrl } from '../assets';
import type { CardData, BotInfo, LocalSpeed } from '../types';

const currentBotDbVersion = 3;

export class DevBotCtrl extends BotCtrl {
  readonly rateBots: RateBot[] = [];
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
    for (const [uid, botInfo] of Object.entries(this.localBots)) this.bots.set(uid, new Bot(botInfo));
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
    pubsub.complete('local.images.ready');
    return this;
  }

  get firstUid(): string | undefined {
    return this.bots.keys().next()?.value;
  }

  storeBot(bot: BotInfo): Promise<any> {
    delete this.localBots[bot.uid];
    this.bots.set(bot.uid, new Bot(bot));
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
    this.bots.set(bot.uid, new Bot(bot));
    this.serverBots[bot.uid] = deepFreeze(structuredClone(bot));
    delete this.localBots[bot.uid];
    await this.store.remove(bot.uid);
  }

  get(uid: string | undefined): BotInfo | undefined {
    if (uid === undefined) return undefined;
    return this.bots.get(uid) ?? this.rateBots[Number(uid.slice(1))];
  }

  card(bot: BotInfo): CardData {
    return {
      label: `${bot.name}${bot.ratings.classical ? ' ' + bot.ratings.classical : ''}`,
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
      const [ab, bb] = [this.get(domIdToUid(a.domId)), this.get(domIdToUid(b.domId))];
      return Bot.rating(ab, speed) - Bot.rating(bb, speed) || a.label.localeCompare(b.label);
    };
  }

  protected storedBots(): Promise<BotInfo[]> {
    return (
      this.store?.getMany() ??
      objectStorage<BotInfo>({
        store: 'local.bots',
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
