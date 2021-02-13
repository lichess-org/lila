import { h } from 'snabbdom';
import { VNode } from 'snabbdom/vnode';

import { Redraw, defined } from './util';

export interface PingData {
  ping: number | undefined;
  server: number | undefined;
}

export interface PingCtrl {
  data: PingData;
  trans: Trans;
}

export function ctrl(trans: Trans, redraw: Redraw): PingCtrl {
  const data: PingData = {
    ping: undefined,
    server: undefined,
  };

  const hub = lichess.pubsub;

  hub.emit('socket.send', 'moveLat', true);
  hub.on('socket.lag', lag => {
    data.ping = Math.round(lag);
    redraw();
  });
  hub.on('socket.in.mlat', lat => {
    data.server = lat as number;
    redraw();
  });

  return { data, trans };
}

function signalBars(d: PingData) {
  const lagRating = !d.ping ? 0 : d.ping < 150 ? 4 : d.ping < 300 ? 3 : d.ping < 500 ? 2 : 1;
  const bars = [];
  for (let i = 1; i <= 4; i++) bars.push(h(i <= lagRating ? 'i' : 'i.off'));
  return h('signal.q' + lagRating, bars);
}

function showMillis(m: number): [string, VNode] {
  return ['' + Math.floor(m), h('small', '.' + Math.round((m - Math.floor(m)) * 10))];
}

export function view(ctrl: PingCtrl): VNode {
  const d = ctrl.data;

  return h('a.status', { attrs: { href: '/lag' } }, [
    signalBars(d),
    h(
      'span.ping',
      {
        attrs: { title: 'PING: ' + ctrl.trans.noarg('networkLagBetweenYouAndLichess') },
      },
      [h('em', 'PING'), h('strong', defined(d.ping) ? '' + d.ping : '?'), h('em', 'ms')]
    ),
    h(
      'span.server',
      {
        attrs: { title: 'SERVER: ' + ctrl.trans.noarg('timeToProcessAMoveOnLichessServer') },
      },
      [h('em', 'SERVER'), h('strong', defined(d.server) ? showMillis(d.server) : ['?']), h('em', 'ms')]
    ),
  ]);
}
