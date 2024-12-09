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
  | 'chat.enabled'
  | 'chat.permissions'
  | 'chat.resize'
  | 'chat.writeable'
  | 'content-loaded'
  | 'flip'
  | 'jump'
  | 'notify-app.set-read'
  | 'palantir.toggle'
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
  | 'socket.in.palantir'
  | 'socket.in.palantirOff'
  | 'socket.in.palantirPing'
  | 'socket.in.redirect'
  | 'socket.in.reload'
  | 'socket.in.sk1'
  | 'socket.in.tournamentReminder'
  | 'socket.in.unblockedBy'
  | 'socket.lag'
  | 'socket.open'
  | 'socket.send'
  | 'speech.enabled'
  | 'study.search.open'
  | 'theme'
  | 'top.toggle.user_tag'
  | 'zen';

export type PubsubOneTimeEvent = 'dialog.polyfill' | 'socket.hasConnected';

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

  after(event: PubsubOneTimeEvent): Promise<void> {
    const found = this.oneTimeEvents.get(event);
    if (found) return found.promise;

    const handler = {} as OneTimeHandler;
    handler.promise = new Promise<void>(resolve => (handler!.resolve = resolve));
    this.oneTimeEvents.set(event, handler);

    return handler.promise;
  }

  complete(event: PubsubOneTimeEvent): void {
    const found = this.oneTimeEvents.get(event);
    if (found) {
      found.resolve?.();
      found.resolve = undefined;
    } else this.oneTimeEvents.set(event, { promise: Promise.resolve() });
  }

  past(event: PubsubOneTimeEvent): boolean {
    return this.oneTimeEvents.has(event) && !this.oneTimeEvents.get(event)?.resolve;
  }
}

export const pubsub: Pubsub = new Pubsub();

interface OneTimeHandler {
  promise: Promise<void>;
  resolve?: () => void;
}
