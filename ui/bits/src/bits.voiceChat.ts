import Peer, { MediaConnection } from 'peerjs';

import type { VoiceChat } from 'lib/chat/interfaces';
import { licon } from 'lib/licon';
import { pubsub } from 'lib/pubsub';
import { hl, alert } from 'lib/view';

type State =
  | 'off'
  | 'opening'
  | 'getting-media'
  | 'ready'
  | 'calling'
  | 'answering'
  | 'getting-stream'
  | 'on'
  | 'stopping';

type VoiceChatOpts = {
  uid: string;
  redraw(): void;
};

type PeerConnectionLike = {
  peer: string;
  open?: boolean;
  close: () => void;
  remoteStream: MediaProvider | null;
  peerConnection?: RTCPeerConnection;
};

type PeerConnectionsMap = Record<string, PeerConnectionLike[]>;

export function initModule(opts: VoiceChatOpts): VoiceChat | undefined {
  const devices = navigator.mediaDevices;
  if (!devices) {
    alert('Voice chat requires navigator.mediaDevices');
    return;
  }

  let state: State = 'off';
  let peer: Peer | undefined;
  let myStream: MediaStream | undefined;

  function start() {
    setState('opening');
    peer = new Peer(peerIdOf(opts.uid))
      .on('open', () => {
        setState('getting-media');
        devices
          .getUserMedia({ video: false, audio: true })
          .then(
            (s: MediaStream) => {
              myStream = s;
              setState('ready');
              site.sound.say('Voice chat is ready.', true, true);
              ping();
            },
            function (err) {
              log(`Failed to get local stream: ${err}`);
            },
          )
          .catch(err => log(err));
      })
      .on('call', (call: MediaConnection) => {
        if (!findOpenConnectionTo(call.peer)) {
          setState('answering', call.peer);
          startCall(call);
          call.answer(myStream);
        }
      })
      .on('connection', c => {
        log('Connected to: ' + c.peer);
      })
      .on('disconnected', () => {
        if (state === 'stopping') destroyPeer();
        else {
          setState('opening', 'reconnect');
          peer?.reconnect();
        }
      })
      .on('close', () => log('peer.close'))
      .on('error', err => log(`peer.error: ${err}`));
  }

  function startCall(call: MediaConnection) {
    call
      .on('stream', () => {
        log('call.stream');
        setState('on', call.peer);
        site.sound.say('Connected', true, true);
      })
      .on('close', () => {
        log('call.close');
        stopCall();
      })
      .on('error', e => {
        log(`call.error: ${e}`);
        stopCall();
      });
    closeOtherConnectionsTo(call.peer);
  }

  function stopCall() {
    if (!hasAnOpenConnection()) setState('ready', 'no call remaining');
  }

  function call(uid: string) {
    const peerId = peerIdOf(uid);
    if (
      peer &&
      myStream &&
      peer.id < peerId && // yes that's how we decide who calls who
      !findOpenConnectionTo(peerId)
    ) {
      setState('calling', peerId);
      startCall(peer.call(peerId, myStream));
    }
  }

  function stop() {
    if (peer && state !== 'off') {
      setState('stopping');
      peer.disconnect();
    }
  }

  function log(msg: string) {
    console.log('[voiceChat]', msg);
  }

  function setState(s: State, msg = '') {
    log(`state: ${state} -> ${s} ${msg}`);
    state = s;
    opts.redraw();
  }

  const reverse = (s: string) => s.split('').reverse().join('');

  function peerIdOf(uid: string) {
    const host = location.hostname;
    const hash = btoa(reverse(btoa(reverse(uid + host)))).replace(/=/g, '');
    return `${host.replace('.', '-')}-${uid}-${hash}`;
  }

  function destroyPeer() {
    if (peer) {
      peer.destroy();
      peer = undefined;
    }
    if (myStream) {
      myStream.getTracks().forEach((t: MediaStreamTrack) => t.stop());
      myStream = undefined;
    }
    setState('off');
  }

  function getConnectionsMap(): PeerConnectionsMap {
    if (!peer) return {};
    return peer.connections as PeerConnectionsMap;
  }

  function connectionsTo(peerId: string) {
    return getConnectionsMap()[peerId] ?? [];
  }

  const findOpenConnectionTo = (peerId: string) => connectionsTo(peerId).find(c => c.open);

  function closeOtherConnectionsTo(peerId: string) {
    const conns = connectionsTo(peerId);
    for (let i = 0; i < conns.length - 1; i++) conns[i].close();
  }

  function closeDisconnectedCalls() {
    if (!peer) return;

    const connections = getConnectionsMap();

    for (const otherPeer in connections) {
      connections[otherPeer].forEach(c => {
        if (c.peerConnection?.connectionState === 'disconnected') {
          log(`close disconnected call to ${c.peer}`);
          c.close();
          opts.redraw();
        }
      });
    }
  }

  function allOpenConnections() {
    return Object.keys(getConnectionsMap())
      .map(findOpenConnectionTo)
      .filter((c): c is PeerConnectionLike => Boolean(c));
  }

  const hasAnOpenConnection = () => allOpenConnections().length > 0;

  function ping() {
    if (state !== 'off') pubsub.emit('socket.send', 'voiceChatPing');
  }

  pubsub.on('socket.in.voiceChat', uids => uids.forEach(call));
  pubsub.on('voiceChat.toggle', v => {
    if (!v) stop();
  });

  start();
  setInterval(closeDisconnectedCalls, 1400);
  setInterval(ping, 5000);

  setInterval(() => {
    peer &&
      Object.keys(peer.connections).forEach(peerId => {
        console.log(peerId, !!findOpenConnectionTo(peerId));
      });
  }, 3000);

  return {
    render: () => {
      const connections = allOpenConnections();
      return devices
        ? hl(
            'button.mchat__tab.voicechat.data-count.voicechat-' + state,
            {
              attrs: {
                'data-icon': licon.Handset,
                title: `Voice chat: ${state}`,
                'data-count': state === 'on' ? connections.length + 1 : 0,
              },
              hook: {
                insert(vnode) {
                  (vnode.elm as HTMLElement).addEventListener('click', () => (peer ? stop() : start()));
                },
              },
            },
            state === 'on'
              ? connections.map(c =>
                  hl('audio.voicechat__audio.' + c.peer, {
                    attrs: { autoplay: true },
                    hook: {
                      insert(vnode) {
                        (vnode.elm as HTMLAudioElement).srcObject = c.remoteStream;
                      },
                    },
                  }),
                )
              : [],
          )
        : undefined;
    },
  };
}
