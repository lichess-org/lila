import { h } from 'snabbdom';

const li = window.lichess;

type State = 'off' | 'opening' | 'getting-media' | 'ready' | 'calling' | 'answering' | 'getting-stream' | 'on' | 'stopping';

export function palantir(opts: PalantirOpts) {
  // const peerId = `lichess:${opts.uid}`;

  let state: State = 'off',
    peer: any | undefined,
    myStream: any | undefined,
    remoteStream: any | undefined;

  function log(msg: string) {
    console.log('[palantir]', msg);
  }

  function setState(s: State) {
    log(`state: ${state} -> ${s}`);
    state = s;
    opts.redraw();
    if (peer) console.log('connections', peer.connections);
  }

  function peerIdOf(uid: string) {
    // return `org-lichess-${uid}`;
    return `org-l-${uid}`;
  }

  function callStart(s: any) {
    remoteStream = s;
    setState('on');
  }

  function destroyPeer() {
    if (peer) {
      peer.destroy(); // 'off' means manual disconnect
      peer = undefined; // 'off' means manual disconnect
    }
    myStream = undefined;
    setState('off');
  }

  function connectionsTo(peerId) {
    return (peer && peer.connections[peerId]) || [];
  }
  function findOpenConnectionTo(peerId) {
    return connectionsTo(peerId).find(c => c.open);
  }
  function closeOtherConnectionsTo(peerId) {
    const conns = connectionsTo(peerId);
    for (let i = 0; i < conns.length - 1; i++) conns[i].close();
  }
  function closeDisconnectedCalls() {
    if (peer) {
      for (let otherPeer in peer.connections) {
        peer.connections[otherPeer].forEach(c => {
          if (c.peerConnection.connectionState == 'disconnected') {
            log(`close disconnected call to ${c.peer}`);
            c.close();
          }
        });
      }
    }
  }

  function start() {
    setState('opening');
    peer = new window['Peer'](peerIdOf(opts.uid));
    window.peer = peer;
    peer
      .on('open', () => {
        setState('getting-media');
        navigator.mediaDevices.getUserMedia({video: false, audio: true}).then((s: any) => {
          myStream = s;
          setState('on');
          notifyLichess();
          setInterval(notifyLichess, 10 * 1000);
          setState('ready');
        }, function(err) {
          log(`Failed to get local stream: ${err}`);
        }).catch(err => log(err));
      })
      .on('call', (call: any) => {
        if (!findOpenConnectionTo(call.peer)) {
          setState('answering');
          monitorCall(call);
          call.answer(myStream);
        }
      })
      .on('stream', s => {
        console.log('stream', s);
      })
      .on('connection', c => {
        log("Connected to: " + c.peer);
      })
      .on('disconnected', () => {
        if (state == 'stopping') destroyPeer();
        else {
          setState('opening');
          peer.reconnect();
        }
      })
      .on('close', () => log('peer.close'))
      .on('error', err => log(`peer.error: ${err}`));
  }

  function monitorCall(call: any) {
    console.log(call, 'monitor');
    call
      .on('stream', callStart)
      .on('close', () => {
        log('call.close');
        stop();
      })
      .on('error', e => {
        log(`call.error: ${e}`);
        stop();
      });
    window.call = call;
    closeOtherConnectionsTo(call.peer);
  }

  function notifyLichess() {
    li.pubsub.emit('socket.send', 'palantir', { on: true });
  }

  function call(uid: string) {
    const peerId = peerIdOf(uid);
    if (peer &&
      myStream &&
      peer.id < peerId &&
      !findOpenConnectionTo(peerId)
    ) {
      setState('calling');
      monitorCall(peer.call(peerId, myStream));
    }
  }

  function stop() {
    if (peer && state != 'off') {
      setState('stopping');
      peer.disconnect();
    }
  }

  let booted = false;
  function click() {
    if (!booted) {
      booted = true;
      setInterval(closeDisconnectedCalls, 1500);
    }
    if (peer) stop();
    else start();
  }

  li.pubsub.on('socket.in.palantir', uids => {
    uids.forEach(call);
  });

  return {
    button() {
      return navigator.mediaDevices ? h('div.mchat__tab.palantir.palantir-' + state, {
        attrs: {
          'data-icon': '',
          title: `Voice chat: ${state}`
        },
        hook: {
          insert(vnode) { (vnode.elm as HTMLElement).addEventListener('click', click) }
        }
      }, [
        state == 'on' ? h('audio.palantir__audio', {
          attrs: { autoplay: true },
          hook: {
            insert(vnode) { (vnode.elm as HTMLAudioElement).srcObject = remoteStream }
          }
        }) : null
      ]) : null;
    }
  };
}
