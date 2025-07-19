export type PubsubEvent =
  | 'ab.rep'
  | 'analysis.closeAll'
  | 'analysis.change'
  | 'analysis.chart.click'
  | 'analysis.comp.toggle'
  | 'analysis.server.progress'
  | 'board.change'
  | 'challenge-app.open'
  | 'chart.panning'
  | 'chat.permissions'
  | 'chat.writeable'
  | 'content-loaded'
  | 'flip'
  | 'jump'
  | 'botdev.import.book'
  | 'notify-app.set-read'
  | 'voiceChat.toggle'
  | 'ply'
  | 'ply.trigger'
  | 'round.suggestion'
  | 'socket.close'
  | 'socket.in.blockedBy'
  | 'socket.in.challenges'
  | 'socket.in.chat_reinstate'
  | 'socket.in.chat_timeout'
  | 'socket.in.crowd'
  | 'socket.in.announce'
  | 'socket.in.fen'
  | 'socket.in.finish'
  | 'socket.in.message'
  | 'socket.in.mlat'
  | 'socket.in.msgNew'
  | 'socket.in.msgType'
  | 'socket.in.notifications'
  | 'socket.in.voiceChat'
  | 'socket.in.voiceChatOff'
  | 'socket.in.voiceChatPing'
  | 'socket.in.redirect'
  | 'socket.in.reload'
  | 'socket.in.sk1'
  | 'socket.in.tournamentReminder'
  | 'socket.in.unblockedBy'
  | 'socket.in.serverRestart'
  | 'socket.lag'
  | 'socket.open'
  | 'socket.send'
  | 'speech.enabled'
  | 'study.search.open'
  | 'theme'
  | 'top.toggle.user_tag'
  | 'zen';

export type PubsubOneTimeEvent = 'dialog.polyfill' | 'socket.hasConnected' | 'botdev.images.ready';

export type PubsubCallback = (...data: any[]) => void;

export class Pubsub {
  private allSubs: Map<PubsubEvent, Set<() => void>> = new Map();
  private oneTimeEvents: Map<PubsubOneTimeEvent, OneTimeHandler> = new Map();

  on(name: PubsubEvent, cb: PubsubCallback): void {
    const subs = this.allSubs.get(name);
    if (subs) subs.add(cb);
    else this.allSubs.set(name, new Set([cb]));
  }

  off(name: PubsubEvent, cb: PubsubCallback): void {
    this.allSubs.get(name)?.delete(cb);
  }

  emit(name: PubsubEvent, ...args: any[]): void {
    for (const fn of this.allSubs.get(name) || []) fn.apply(null, args);
  }

  after<T>(event: PubsubOneTimeEvent): Promise<T> {
    const found = this.oneTimeEvents.get(event);
    if (found) return found.promise;

    const handler = {} as OneTimeHandler<T>;
    handler.promise = new Promise<T>(resolve => (handler!.resolve = resolve));
    this.oneTimeEvents.set(event, handler);

    return handler.promise;
  }

  complete<T>(event: PubsubOneTimeEvent, value?: T): void {
    const found = this.oneTimeEvents.get(event);
    if (found) {
      found.resolve?.(value);
      found.resolve = undefined;
    } else this.oneTimeEvents.set(event, { promise: Promise.resolve(value) });
  }

  past(event: PubsubOneTimeEvent): boolean {
    return this.oneTimeEvents.has(event) && !this.oneTimeEvents.get(event)?.resolve;
  }
}

export const pubsub: Pubsub = new Pubsub();

interface OneTimeHandler<T = any> {
  promise: Promise<T>;
  resolve?: (value: T) => void;
}
