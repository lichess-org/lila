import { Pubsub, PubsubCallback, PubsubEvent } from 'common/pubsub';

// #TODO document these somewhere
const publicEvents = ['ply', 'analysis.change', 'chat.resize', 'analysis.chart.click', 'analysis.closeAll'];

export const api = (ps: Pubsub) => ({
  initializeDom: (root?: HTMLElement) => ps.emit('content-loaded', root),
  events: {
    on(name: PubsubEvent, cb: PubsubCallback): void {
      if (!publicEvents.includes(name)) throw 'This event is not part of the public API';
      ps.on(name, cb);
    },
    off(name: PubsubEvent, cb: PubsubCallback): void {
      ps.off(name, cb);
    },
  },
  socket: (() => {
    const inKeys = ['mlat', 'fen', 'notifications', 'endData'];
    const keys = ['lag', 'close'];
    return {
      subscribeToMoveLatency: () => ps.emit('socket.send', 'moveLat', true),
      events: {
        on(key: string, cb: PubsubCallback): void {
          if (inKeys.includes(key)) ps.on(`socket.in.${key}` as PubsubEvent, cb);
          else if (keys.includes(key)) ps.on(`socket.${key}` as PubsubEvent, cb);
          else throw 'This event is not part of the public API';
        },
        off(key: string, cb: PubsubCallback): void {
          const ev = inKeys.includes(key)
            ? (`socket.in.${key}` as PubsubEvent)
            : (`socket.${key}` as PubsubEvent);
          ps.off(ev, cb);
        },
      },
    };
  })(),
  onlineFriends: (() => {
    const keys = ['playing', 'stopped_playing', 'onlines', 'enters', 'leaves'];
    return {
      request: () => ps.emit('socket.send', 'following_onlines'),
      events: {
        on(key: string, cb: PubsubCallback): void {
          if (!keys.includes(key)) throw 'This event is not part of the public API';
          ps.on(`socket.in.following_${key}` as PubsubEvent, cb);
        },
        off(key: string, cb: PubsubCallback): void {
          ps.off(`socket.in.following_${key}` as PubsubEvent, cb);
        },
      },
    };
  })(),
});
