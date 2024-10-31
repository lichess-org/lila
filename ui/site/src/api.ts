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
  'socket.in.following_playing',
  'socket.in.following_stopped_playing',
  'socket.in.following_onlines',
  'socket.in.following_enters',
  'socket.in.following_leaves',
  'socket.in.notifications',
  'socket.in.endData',
]);

export const api = (ps: Pubsub) => ({
  pubsub: {
    on(name: PubsubEvent, cb: PubsubCallback): void {
      if (!publicEvents.has(name)) throw 'This event is not part of the public API';
      ps.on(name, cb);
    },
    off(name: PubsubEvent, cb: PubsubCallback): void {
      ps.off(name, cb);
    },
  },
});
