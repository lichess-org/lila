import { h } from 'snabbdom';

const li = window.lichess;

type State = 'off' | 'opening' | 'getting-media' | 'ready' | 'calling' | 'answering' | 'getting-stream' | 'on' | 'stopping';

export function palantir(opts: PalantirOpts) {
  // const peerId = `lichess:${opts.uid}`;

  let state: State = 'off',
    peer: any | undefined,
    myStream: any | undefined,
    remoteStream: any | undefined;

  function setState(s: State) {
    console.log(s);
    state = s;
    opts.redraw();
  }

  function peerIdOf(uid: string) {
    // return `org-lichess-${uid}`;
    return `org-l-${uid}`;
  }

  function callStart(s: any) {
    remoteStream = s;
    setState('on');
  }

  function start() {
    setState('opening');
    peer = peer || new window['Peer'](peerIdOf(opts.uid));
    window.peer = peer;
    peer.on('open', () => {
      setState('getting-media');
      navigator.mediaDevices.getUserMedia({video: false, audio: true}).then((s: any) => {
        myStream = s;
        setState('on');
        notifyLichess();
        setInterval(notifyLichess, 10 * 1000);
        setState('ready');
        peer.on('call', (call: any) => {
          if (!peer.connections[call.peer].find((c: any) => c.open)) {
            setState('answering');
            monitorCall(call);
            call.answer(myStream);
          }
        });
      }, function(err) {
        console.log('Failed to get local stream' ,err);
      }).catch(err => {
        console.log(err);
      });
    });
    peer.on('stream', s => {
      console.log('stream', s);
    });
    peer.on('connection', function (c) {
      console.log("Connected to: " + c.peer);
    });
    peer.on('disconnected', function() {
      if (state == 'stopping') {
        peer.destroy(); // 'off' means manual disconnect
        peer = undefined; // 'off' means manual disconnect
        setState('off');
      }
      else {
        setState('opening');
        peer.reconnect();
      }
    });
    peer.on('close', function() {
      console.log('Connection destroyed');
    });
    peer.on('error', function (err) {
      console.log(err);
    });
  }

  function monitorCall(call: any) {
    console.log(call, 'monitor');
    call
      .on('stream', callStart)
      .on('close', s => {
        console.log('call close', s);
        stop();
      })
      .on('error', s => {
        console.log('call error', s);
        stop();
      });
  }

  function notifyLichess() {
    li.pubsub.emit('socket.send', 'palantir', { on: true });
  }

  function call(uid: string) {
    const peerId = peerIdOf(uid);
    if (peer && myStream && peer.id < peerId && !peer.connections[peerId]) {
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

  let started = false;
  function click() {
    if (!started) {
      start();
      started = true;
    } else if (state == 'off') start();
    else stop();
  }

  li.pubsub.on('socket.in.palantir', uids => {
    uids.forEach(call);
  });

  return {
    button() {
      return h('button.mchat__palantir.fbt.pal-' + state, {
        attrs: {
          'data-icon': '',
          title: state
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
      ]);
    }
  };
}
