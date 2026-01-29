import type { Line } from '@/chat/interfaces';
import type { Data as WatchersData } from '@/view/watchers';
import type { TreePath } from './tree/types';

export type PubsubEventKey = keyof PubsubEvents;

export interface PubsubEvents {
  'ab.rep': (data: 'kbc') => void;
  'analysis.closeAll': () => void;
  'analysis.change': (fen: FEN, path: TreePath) => void;
  'analysis.chart.click': (index: number) => void;
  'analysis.comp.toggle': (enabled: boolean) => void;
  'analysis.server.progress': (analyseData: any) => void;
  'board.change': (is3d: boolean) => void;
  'challenge-app.open': () => void;
  'chart.panning': () => void;
  'chat.permissions': (perms: { local: boolean }) => void;
  'chat.writeable': (writeable: boolean) => void;
  'content-loaded': (el?: HTMLElement) => void;
  flip: (flip: boolean) => void;
  jump: (ply: string) => void;
  'botdev.import.book': (key: string, oldKey?: string) => void;
  'notify-app.set-read': (user: string) => void;
  'voiceChat.toggle': (enabled: boolean) => void;
  ply: (ply: number, isMainline?: boolean) => void;
  'ply.trigger': () => void;
  'round.suggestion': (text: string | null) => void;
  'socket.close': () => void;
  'socket.in.blockedBy': (userId: string) => void;
  'socket.in.challenges': (data: any) => void;
  'socket.in.chat_reinstate': (userId: string) => void;
  'socket.in.chat_timeout': (userId: string) => void;
  'socket.in.crowd': (data: {
    nb: number;
    users?: string[];
    anons?: number;
    watchers?: WatchersData;
    streams?: [UserId, { name: string; lang: string }][];
  }) => void;
  'socket.in.announce': (data: { msg?: string; date?: string }) => void;
  'socket.in.endData': (data: any) => void;
  'socket.in.fen': (data: { id: string; fen: FEN; lm: Uci; wc?: number; bc?: number }) => void;
  'socket.in.finish': (data: { id: string; win?: 'b' | 'w' }) => void;
  'socket.in.following_enters': (
    titleName: string,
    msg: { playing: boolean; patronColor?: PatronColor },
  ) => void;
  'socket.in.following_leaves': (titleName: string) => void;
  'socket.in.following_onlines': (
    friends: string[],
    msg: { playing: string[]; patronColors: PatronColor[] },
  ) => void;
  'socket.in.following_playing': (titleName: string) => void;
  'socket.in.following_stopped_playing': (titleName: string) => void;
  'socket.in.message': (line: Line) => void;
  'socket.in.mlat': (millis: number) => void;
  'socket.in.msgNew': (data: { text: string; user: UserId; date: number }) => void;
  'socket.in.msgType': (userId: UserId) => void;
  'socket.in.notifications': (data: { notifications: Paginator<any>; unread: number }) => void;
  'socket.in.voiceChat': (uids: UserId[]) => void;
  'socket.in.redirect': (d: RedirectTo) => void;
  'socket.in.reload': (data: any) => void;
  'socket.in.sk1': (signed: string) => void;
  'socket.in.tournamentReminder': (data: { id: string; name: string }) => void;
  'socket.in.unblockedBy': (userId: string) => void;
  'socket.in.serverRestart': () => void;
  'socket.lag': (lag: number) => void;
  'socket.open': () => void;
  'socket.send': (event: string, d?: any, o?: any) => void;
  theme: (theme: string) => void;
  zen: () => void;
}

export interface OneTimeEvents {
  'polyfill.dialog': ((dialog: HTMLElement) => void) | undefined;
  'socket.hasConnected': void;
  'botdev.images.ready': void;
}

export class Pubsub {
  private allSubs: Map<keyof PubsubEvents, Set<PubsubEvents[keyof PubsubEvents]>> = new Map();
  private oneTimeEvents: Map<OneTimeKey, OneTimeHandler<OneTimeEvents[OneTimeKey]>> = new Map();

  on<K extends keyof PubsubEvents>(name: K, cb: PubsubEvents[K]): void {
    const subs = this.allSubs.get(name);
    if (subs) subs.add(cb);
    else this.allSubs.set(name, new Set([cb]));
  }

  off<K extends keyof PubsubEvents>(name: K, cb: PubsubEvents[K]): void {
    this.allSubs.get(name)?.delete(cb);
  }

  emit<K extends keyof PubsubEvents>(name: K, ...args: Parameters<PubsubEvents[K]>): void {
    const callbacks = this.allSubs.get(name);
    if (callbacks) {
      for (const cb of callbacks) {
        (cb as (...args: Parameters<PubsubEvents[K]>) => void)(...args);
      }
    }
  }

  after<K extends OneTimeKey>(event: K): Promise<OneTimeEvents[K]> {
    const found = this.oneTimeEvents.get(event);
    if (found) return found.promise as Promise<OneTimeEvents[K]>;

    const handler = {} as OneTimeHandler<OneTimeEvents[K]>;
    handler.promise = new Promise<OneTimeEvents[K]>(resolve => (handler!.resolve = resolve));
    this.oneTimeEvents.set(event, handler);

    return handler.promise;
  }

  complete<K extends OneTimeKey>(event: K, value?: OneTimeEvents[K]): void {
    const found = this.oneTimeEvents.get(event);
    if (found) {
      found.resolve?.(value);
      found.resolve = undefined;
    } else this.oneTimeEvents.set(event, { promise: Promise.resolve(value) });
  }

  past(event: OneTimeKey): boolean {
    return this.oneTimeEvents.has(event) && !this.oneTimeEvents.get(event)?.resolve;
  }
}

export const pubsub: Pubsub = new Pubsub();

type OneTimeKey = keyof OneTimeEvents;
interface OneTimeHandler<T = any> {
  promise: Promise<T>;
  resolve?: (value: T) => void;
}
