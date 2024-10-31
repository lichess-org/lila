import { Pubsub, PubsubCallback, PubsubEvent } from 'common/pubsub';

// #TODO document these somewhere
const publicEvents = ['ply', 'analysis.change', 'chat.resize', 'analysis.chart.click', 'analysis.closeAll'];
const socketEvents = ['lag', 'close'];
const socketInEvents = ['mlat', 'fen', 'notifications', 'endData'];
const friendsEvents = ['playing', 'stopped_playing', 'onlines', 'enters', 'leaves'];

export const api = (pubsub: Pubsub) => ({
  initializeDom: (root?: HTMLElement) => pubsub.emit('content-loaded', root),
  events: {
    on(name: PubsubEvent, cb: PubsubCallback): void {
      if (!publicEvents.includes(name)) throw 'This event is not part of the public API';
      pubsub.on(name, cb);
    },
    off(name: PubsubEvent, cb: PubsubCallback): void {
      pubsub.off(name, cb);
    },
  },
  socket: {
    subscribeToMoveLatency: () => pubsub.emit('socket.send', 'moveLat', true),
    events: {
      on(key: string, cb: PubsubCallback): void {
        if (socketInEvents.includes(key)) pubsub.on(`socket.in.${key}` as PubsubEvent, cb);
        else if (socketEvents.includes(key)) pubsub.on(`socket.${key}` as PubsubEvent, cb);
        else throw 'This event is not part of the public API';
      },
      off(key: string, cb: PubsubCallback): void {
        const ev = socketInEvents.includes(key)
          ? (`socket.in.${key}` as PubsubEvent)
          : (`socket.${key}` as PubsubEvent);
        pubsub.off(ev, cb);
      },
    },
  },
  onlineFriends: {
    request: () => pubsub.emit('socket.send', 'following_onlines'),
    events: {
      on(key: string, cb: PubsubCallback): void {
        if (!friendsEvents.includes(key)) throw 'This event is not part of the public API';
        pubsub.on(`socket.in.following_${key}` as PubsubEvent, cb);
      },
      off(key: string, cb: PubsubCallback): void {
        pubsub.off(`socket.in.following_${key}` as PubsubEvent, cb);
      },
    },
  },
  // some functions will be exposed here
  // to be overriden by browser extensions
  hackMe: {},
});
