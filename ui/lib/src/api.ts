import { type PubsubEventKey, type PubsubEvents, pubsub } from './pubsub';
import { domDialog } from './view/dialog';
import { alert, confirm, prompt } from './view/dialogs';

// #TODO document these somewhere
const publicEvents = ['ply', 'analysis.change', 'chat.resize', 'analysis.closeAll'] as const;
const socketEvents = ['lag', 'close'] as const;
const socketInEvents = ['mlat', 'fen', 'notifications', 'endData'] as const;
const friendsEvents = ['playing', 'stopped_playing', 'onlines', 'enters', 'leaves'] as const;

interface Events {
  on<K extends keyof PubsubEvents>(name: K, cb: PubsubEvents[K]): void;
  off<K extends keyof PubsubEvents>(name: K, cb: PubsubEvents[K]): void;
}

export interface Api {
  initializeDom: (root?: HTMLElement) => void;
  events: Events;
  socket: {
    subscribeToMoveLatency: () => void;
    events: Events;
  };
  onlineFriends: {
    request: () => void;
    events: Events;
  };
  chat: {
    post: (text: string) => void;
  };
  dialog: {
    alert: typeof alert;
    confirm: typeof confirm;
    prompt: typeof prompt;
    domDialog: typeof domDialog;
  };
  overrides: {
    [key: string]: (...args: any[]) => unknown;
  };
  analysis?: any;
}

// this object is available to extensions as window.lichess
export const api: Api = {
  initializeDom: (root?: HTMLElement) => {
    pubsub.emit('content-loaded', root);
  },
  events: {
    on<K extends keyof PubsubEvents>(name: K, cb: PubsubEvents[K]): void {
      if (!(publicEvents as ReadonlyArray<string>).includes(name))
        throw 'This event is not part of the public API';
      pubsub.on(name, cb);
    },
    off<K extends keyof PubsubEvents>(name: K, cb: PubsubEvents[K]): void {
      pubsub.off(name, cb);
    },
  },
  socket: {
    subscribeToMoveLatency: () => pubsub.emit('socket.send', 'moveLat', true),
    events: {
      on<K extends keyof PubsubEvents>(key: K, cb: PubsubEvents[K]): void {
        if (socketInEvents.includes(key as (typeof socketInEvents)[number]))
          pubsub.on(
            `socket.in.${key as (typeof socketInEvents)[number]}`,
            cb as PubsubEvents[`socket.in.${(typeof socketInEvents)[number]}`],
          );
        else if (socketEvents.includes(key as (typeof socketEvents)[number]))
          pubsub.on(
            `socket.${key as (typeof socketEvents)[number]}`,
            cb as PubsubEvents[`socket.${(typeof socketEvents)[number]}`],
          );
        else throw 'This event is not part of the public API';
      },
      off<K extends keyof PubsubEvents>(key: K, cb: PubsubEvents[K]): void {
        const ev: PubsubEventKey = socketInEvents.includes(key as (typeof socketInEvents)[number])
          ? `socket.in.${key as (typeof socketInEvents)[number]}`
          : `socket.${key as (typeof socketEvents)[number]}`;
        pubsub.off(ev, cb as PubsubEvents[typeof ev]);
      },
    },
  },
  onlineFriends: {
    request: () => pubsub.emit('socket.send', 'following_onlines'),
    events: {
      on<K extends keyof PubsubEvents>(key: K, cb: PubsubEvents[K]): void {
        if (!friendsEvents.includes(key as (typeof friendsEvents)[number]))
          throw 'This event is not part of the public API';
        pubsub.on(
          `socket.in.following_${key as (typeof friendsEvents)[number]}`,
          cb as PubsubEvents[`socket.in.following_${(typeof friendsEvents)[number]}`],
        );
      },
      off<K extends keyof PubsubEvents>(key: K, cb: PubsubEvents[K]): void {
        pubsub.off(
          `socket.in.following_${key as (typeof friendsEvents)[number]}`,
          cb as PubsubEvents[`socket.in.following_${(typeof friendsEvents)[number]}`],
        );
      },
    },
  },
  chat: {
    post: (text: string) => pubsub.emit('socket.send', 'talk', text),
  },
  dialog: {
    alert,
    confirm,
    prompt,
    domDialog,
  },
  // some functions will be exposed here
  // to be overriden by browser extensions
  overrides: {},
};
