import { h } from 'snabbdom'
import { Ctrl } from './interfaces'

export default function(ctrl: Ctrl) {
  var d = ctrl.data();
  if (!d || ctrl.initiating()) return h('div.initiating', {
    props: {
      innerHTML: window.lichess.spinnerHtml
    }
  });
  var nb = d.in.length + d.out.length;
  return nb ? allChallenges(ctrl, d, nb) : empty();
}

function allChallenges(ctrl, d, nb) {
  return h('div', {
    class: {
      challenges: true,
      reloading: ctrl.reloading(),
      many: nb > 3
    },
    hook: {
      postpatch: () => window.lichess.pubsub.emit('content_loaded')()
    }
  }, [
    // d.in.map(challenge(ctrl, 'in')),
    // d.out.map(challenge(ctrl, 'out')),
  ]);
}

function empty() {
  return h('div.empty.text', {
    attrs: {
      'data-icon': '',
    }
  }, 'No challenges.');
}
