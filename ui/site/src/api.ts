import { Pubsub, PubsubCallback, PubsubEvent } from 'common/pubsub';

// #TODO document these somewhere
const publicEvents = new Set<PubsubEvent>([
  'ply',
  'analysis.change',
  'chat.resize',
  'socket.lag',
  'socket.close',
  'analysis.chart.click',
  'analysis.closeAll',
  'socket.in.mlat',
  'socket.in.crowd',
  'socket.in.fen',
  'socket.in.notifications',
  'socket.in.endData',
]);

export const api = (ps: Pubsub) => ({
  events: {
    on(name: PubsubEvent, cb: PubsubCallback): void {
      if (!publicEvents.has(name)) throw 'This event is not part of the public API';
      ps.on(name, cb);
    },
    off(name: PubsubEvent, cb: PubsubCallback): void {
      ps.off(name, cb);
    },
  },
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
