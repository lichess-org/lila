const li = window.lichess;

type State = 'off' | 'opening' | 'getting-media' | 'ready' | 'calling' | 'answering' | 'getting-stream' | 'on' | 'stopping';

export function palantir(opts: PalantirOpts) {

  const devices = navigator.mediaDevices;
  if (!devices) return alert('Voice chat requires navigator.mediaDevices');

  let state: State = 'off',
    peer: any | undefined,
    myStream: any | undefined,
    remoteStream: any | undefined;

  function start() {
    setState('opening');
    peer = new window['Peer'](peerIdOf(opts.uid))
      .on('open', () => {
        setState('getting-media');
        devices.getUserMedia({video: false, audio: true}).then((s: any) => {
          myStream = s;
          setState('ready');
          li.sound.say("Voice chat is ready.", true, true);
          ping();
        }, function(err) {
          log(`Failed to get local stream: ${err}`);
        }).catch(err => log(err));
      })
      .on('call', (call: any) => {
        if (!findOpenConnectionTo(call.peer)) {
          setState('answering');
          startCall(call);
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

  function startCall(call: any) {
    call
      .on('stream', rs => {
        log('call.stream');
        remoteStream = rs;
        setState('on');
        li.sound.say("Connected", true, true);
      })
      .on('close', () => {
        log('call.close');
        stop();
      })
      .on('error', e => {
        log(`call.error: ${e}`);
        stop();
      });
    closeOtherConnectionsTo(call.peer);
  }

  function call(uid: string) {
    const peerId = peerIdOf(uid);
    if (peer &&
      myStream &&
      peer.id < peerId && // yes that's how we decide who calls who
      !findOpenConnectionTo(peerId)
    ) {
      setState('calling');
      startCall(peer.call(peerId, myStream));
    }
  }

  function stop() {
    if (peer && state != 'off') {
      setState('stopping');
      peer.disconnect();
    }
  }

  function log(msg: string) {
    console.log('[palantir]', msg);
  }

  function setState(s: State) {
    log(`state: ${state} -> ${s}`);
    state = s;
    opts.redraw();
  }

  function peerIdOf(uid: string) {
    const host = location.hostname;
    const hash = btoa(li.reverse(btoa(li.reverse(uid + host)))).replace(/=/g,'');
    return `${host.replace('.', '-')}-${uid}-${hash}`;
  }

  function destroyPeer() {
    if (peer) {
      peer.destroy();
      peer = undefined;
    }
    if (myStream) {
      myStream.getTracks().forEach(t => t.stop());
      myStream = undefined;
    }
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
          if (c.peerConnection && c.peerConnection.connectionState == 'disconnected') {
            log(`close disconnected call to ${c.peer}`);
            c.close();
          }
        });
      }
    }
  }

  function ping() {
    if (state != 'off') li.pubsub.emit('socket.send', 'palantirPing');
  }

  li.pubsub.on('socket.in.palantir', uids => uids.forEach(call));
  li.pubsub.on('palantir.toggle', v => {
    if (!v) stop();
  });

  start();
  setInterval(closeDisconnectedCalls, 1400);
  setInterval(ping, 5000);

  return {
    render: h =>
    devices ? h('div.mchat__tab.palantir.palantir-' + state, {
      attrs: {
        'data-icon': '',
        title: `Voice chat: ${state}`
      },
      hook: {
        insert(vnode) {
          (vnode.elm as HTMLElement).addEventListener('click', () => peer ? stop() : start());
        }
      }
    }, [
      state == 'on' ? h('audio.palantir__audio', {
        attrs: { autoplay: true },
        hook: {
          insert(vnode) { (vnode.elm as HTMLAudioElement).srcObject = remoteStream }
        }
      }) : null
    ]) : null
  }
}
