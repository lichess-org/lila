import { type Zerofish } from 'zerofish';
import { Bot } from '../bot';
import { RateBot } from './rateBot';
import { BotCtrl } from '../botCtrl';
import { type ObjectStorage, objectStorage } from 'common/objectStorage';
import { deepFreeze } from 'common/algo';
import type { CardData, BotInfo, LocalSpeed } from '../types';

export class DevBotCtrl extends BotCtrl {
  readonly rateBots: RateBot[] = [];
  private store: ObjectStorage<BotInfo>;

  constructor(zf?: Zerofish) {
    super(zf);
    for (let i = 0; i <= RateBot.MAX_LEVEL; i++) {
      this.rateBots.push(new RateBot(i));
    }
  }

  get firstUid(): string | undefined {
    return this.bots.keys().next()?.value;
  }

  storeBot(bot: BotInfo): Promise<any> {
    delete this.localBots[bot.uid];
    this.bots.set(bot.uid, new Bot(bot));
    if (Bot.isSame(this.serverBots[bot.uid], bot)) return this.store.remove(bot.uid);
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
    else if (local && !Bot.isSame(local, server)) cd?.classList.push('local-changes');
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
      objectStorage<BotInfo>({ store: 'local.bots', version: 2, upgrade: this.upgrade }).then(s => {
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
      cursor.update(Bot.migrate(change.oldVersion, cursor.value));
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
