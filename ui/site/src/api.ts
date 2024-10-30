import { Pubsub, PubsubCallback, PubsubEvent } from 'common/pubsub';

const publicEvents = new Set<PubsubEvent>(['content-loaded', 'zen']);

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
