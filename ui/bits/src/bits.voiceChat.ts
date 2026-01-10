import { hl, alert } from 'lib/view';
import * as licon from 'lib/licon';
import Peer from 'peerjs';
import { pubsub } from 'lib/pubsub';
import type { VoiceChat } from 'lib/chat/interfaces';

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

interface VoiceChatOpts {
  uid: string;
  redraw(): void;
}

export function initModule(opts: VoiceChatOpts): VoiceChat | undefined {
  const devices = navigator.mediaDevices;
  if (!devices) {
    alert('Voice chat requires navigator.mediaDevices');
    return;
  }

  let state: State = 'off',
    peer: any | undefined,
    myStream: any | undefined;

  function start() {
    setState('opening');
    peer = new Peer(peerIdOf(opts.uid))
      .on('open', () => {
        setState('getting-media');
        devices
          .getUserMedia({ video: false, audio: true })
          .then(
            (s: any) => {
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
      .on('call', (call: any) => {
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
          peer.reconnect();
        }
      })
      .on('close', () => log('peer.close'))
      .on('error', err => log(`peer.error: ${err}`));
  }

  function startCall(call: any) {
    call
      .on('stream', () => {
        log('call.stream');
        setState('on', call.peer);
        site.sound.say('Connected', true, true);
      })
      .on('close', () => {
        log('call.close');
        stopCall(call);
      })
      .on('error', (e: any) => {
        log(`call.error: ${e}`);
        stopCall(call);
      });
    closeOtherConnectionsTo(call.peer);
  }

  function stopCall(_: any) {
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
      myStream.getTracks().forEach((t: any) => t.stop());
      myStream = undefined;
    }
    setState('off');
  }

  const connectionsTo = (peerId: any) => (peer && peer.connections[peerId]) || [];

  const findOpenConnectionTo = (peerId: any) => connectionsTo(peerId).find((c: any) => c.open);

  function closeOtherConnectionsTo(peerId: any) {
    const conns = connectionsTo(peerId);
    for (let i = 0; i < conns.length - 1; i++) conns[i].close();
  }
  function closeDisconnectedCalls() {
    if (!peer) return;
    for (const otherPeer in peer.connections) {
      peer.connections[otherPeer].forEach((c: any) => {
        if (c.peerConnection && c.peerConnection.connectionState == 'disconnected') {
          log(`close disconnected call to ${c.peer}`);
          c.close();
          opts.redraw();
        }
      });
    }
  }
  const allOpenConnections = (): any[] => {
    if (!peer) return [];
    return Object.keys(peer.connections).map(findOpenConnectionTo).filter(Boolean);
  };
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

  setInterval(function () {
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
            'div.mchat__tab.voicechat.data-count.voicechat-' + state,
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
