"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const snabbdom_1 = require("snabbdom");
function palantir(opts) {
    // const peerId = `lichess:${opts.uid}`;
    console.log(opts);
    console.log(window.Peer);
    // const peer = new peerjs.Peer(peerId);
    // peer.on('open', id => {
    //   console.log('My peer ID is: ' + id);
    //   navigator.getUserMedia({video: false, audio: true}, function(stream) {
    //     // var call = peer.call('another-peers-id', stream);
    //     // call.on('stream', function(remoteStream) {
    //     //   // Show stream in some video/canvas element.
    //     // });
    //   }, function(err) {
    //     console.log('Failed to get local stream' ,err);
    //   });
    // });
    return {
        button() {
            return snabbdom_1.h('button.mchat__palantir.fbt', {
                attrs: { 'data-icon': '' }
            });
        }
    };
}
exports.palantir = palantir;
