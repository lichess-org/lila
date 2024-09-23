export type PubsubEvent =
  | 'ab.rep'
  | 'analyse.close-all'
  | 'analysis.change'
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
  | 'socket.in.following_enters'
  | 'socket.in.following_leaves'
  | 'socket.in.following_onlines'
  | 'socket.in.following_playing'
  | 'socket.in.following_stopped_playing'
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

export type PubsubOneTimeEvent =
  | 'dialog.polyfill'
  | 'socket.hasConnected';

export type PubsubCallback = (...data: any[]) => void;

export interface Pubsub {
  on(msg: PubsubEvent, f: PubsubCallback): void;
  off(msg: PubsubEvent, f: PubsubCallback): void;
  emit(msg: PubsubEvent, ...args: any[]): void;

  after(event: PubsubOneTimeEvent): Promise<void>;
  complete(event: PubsubOneTimeEvent): void;
  past(event: PubsubOneTimeEvent): boolean;
}

export const pubsub: Pubsub = {
  on(name: PubsubEvent, cb) {
    const subs = allSubs.get(name);
    if (subs) subs.add(cb);
    else allSubs.set(name, new Set([cb]));
  },
  off(name: PubsubEvent, cb) {
    allSubs.get(name)?.delete(cb);
  },
  emit(name: PubsubEvent, ...args: any[]) {
    for (const fn of allSubs.get(name) || []) fn.apply(null, args);
  },
  after(event: PubsubOneTimeEvent): Promise<void> {
    const found = oneTimeEvents.get(event);
    if (found) return found.promise;

    const handler = {} as OneTimeHandler;
    handler.promise = new Promise<void>(resolve => (handler!.resolve = resolve));
    oneTimeEvents.set(event, handler);

    return handler.promise;
  },
  complete(event: PubsubOneTimeEvent): void {
    const found = oneTimeEvents.get(event);
    if (found) {
      found.resolve?.();
      found.resolve = undefined;
    } else oneTimeEvents.set(event, { promise: Promise.resolve() });
  },
  past(event: PubsubOneTimeEvent): boolean {
    return oneTimeEvents.has(event) && !oneTimeEvents.get(event)?.resolve;
  },
};

const allSubs: Map<PubsubEvent, Set<() => void>> = new Map();
const oneTimeEvents: Map<PubsubOneTimeEvent, OneTimeHandler> = new Map();

interface OneTimeHandler {
  promise: Promise<void>;
  resolve?: () => void;
}

//export default pubsub;
