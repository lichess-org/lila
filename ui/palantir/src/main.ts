import { h } from 'snabbdom'

export function palantir() {

  navigator.getUserMedia({video: true, audio: true}, function(stream) {
    var call = peer.call('another-peers-id', stream);
    call.on('stream', function(remoteStream) {
      // Show stream in some video/canvas element.
    });
  }, function(err) {
    console.log('Failed to get local stream' ,err);
  });

  return {
    button() {
      return h('button.mchat__palantir.fbt', {
        attrs: { 'data-icon': '' }
      });
    }
  };
}
