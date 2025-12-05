import { type PubsubEventKey, type PubsubEvents, pubsub } from './pubsub';
import { domDialog } from './view/dialog';
import { alert, confirm, prompt } from './view/dialogs';

// #TODO document these somewhere
const publicEvents = ['ply', 'analysis.change', 'chat.resize', 'analysis.closeAll'] as const;
type PublicEventKey = (typeof publicEvents)[number] & keyof PubsubEvents;
const socketEvents = ['lag', 'close'] as const;
type SocketEventKey = (typeof socketEvents)[number];
const socketInEvents = ['mlat', 'fen', 'notifications', 'endData'] as const;
type SocketInEventKey = (typeof socketInEvents)[number];
const friendsEvents = ['playing', 'stopped_playing', 'onlines', 'enters', 'leaves'] as const;
type FriendsEventKey = (typeof friendsEvents)[number];

type SocketCallback<K extends SocketEventKey | SocketInEventKey> = K extends SocketInEventKey
  ? PubsubEvents[`socket.in.${K}`]
  : K extends SocketEventKey
    ? PubsubEvents[`socket.${K}`]
    : never;

type FriendsCallback<K extends FriendsEventKey> = PubsubEvents[`socket.in.following_${K}`];

export interface Api {
  initializeDom: (root?: HTMLElement) => void;
  events: {
    on<K extends PublicEventKey>(name: K, cb: PubsubEvents[K]): void;
    off<K extends PublicEventKey>(name: K, cb: PubsubEvents[K]): void;
  };
  socket: {
    subscribeToMoveLatency: () => void;
    events: {
      on<K extends SocketEventKey | SocketInEventKey>(key: K, cb: SocketCallback<K>): void;
      off<K extends SocketEventKey | SocketInEventKey>(key: K, cb: SocketCallback<K>): void;
    };
  };
  onlineFriends: {
    request: () => void;
    events: {
      on<K extends FriendsEventKey>(key: K, cb: FriendsCallback<K>): void;
      off<K extends FriendsEventKey>(key: K, cb: FriendsCallback<K>): void;
    };
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
    on<K extends PublicEventKey>(name: K, cb: PubsubEvents[K]): void {
      if (!publicEvents.includes(name)) throw 'This event is not part of the public API';
      pubsub.on(name, cb);
    },
    off<K extends PublicEventKey>(name: K, cb: PubsubEvents[K]): void {
      pubsub.off(name, cb);
    },
  },
  socket: {
    subscribeToMoveLatency: () => pubsub.emit('socket.send', 'moveLat', true),
    events: {
      on<K extends SocketEventKey | SocketInEventKey>(key: K, cb: SocketCallback<K>): void {
        if (socketInEvents.includes(key as SocketInEventKey))
          pubsub.on(`socket.in.${key as SocketInEventKey}`, cb);
        else if (socketEvents.includes(key as SocketEventKey))
          pubsub.on(`socket.${key as SocketEventKey}`, cb as PubsubEvents[`socket.${SocketEventKey}`]);
        else throw 'This event is not part of the public API';
      },
      off<K extends SocketEventKey | SocketInEventKey>(key: K, cb: SocketCallback<K>): void {
        const ev: PubsubEventKey = socketInEvents.includes(key as SocketInEventKey)
          ? `socket.in.${key as SocketInEventKey}`
          : `socket.${key as SocketEventKey}`;
        pubsub.off(ev, cb);
      },
    },
  },
  onlineFriends: {
    request: () => pubsub.emit('socket.send', 'following_onlines'),
    events: {
      on<K extends FriendsEventKey>(key: K, cb: FriendsCallback<K>): void {
        if (!friendsEvents.includes(key as FriendsEventKey)) throw 'This event is not part of the public API';
        pubsub.on(`socket.in.following_${key}`, cb);
      },
      off<K extends FriendsEventKey>(key: K, cb: FriendsCallback<K>): void {
        pubsub.off(`socket.in.following_${key}`, cb);
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
