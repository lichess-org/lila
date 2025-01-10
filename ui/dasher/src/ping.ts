import { defined } from 'common/common';
import { i18n } from 'i18n';
import { type VNode, h } from 'snabbdom';
import type { Redraw } from './util';

export interface PingData {
  ping: number | undefined;
  server: number | undefined;
}

export interface PingCtrl {
  data: PingData;
}

export function ctrl(redraw: Redraw): PingCtrl {
  const data: PingData = {
    ping: undefined,
    server: undefined,
  };

  const hub = window.lishogi.pubsub;

  hub.emit('socket.send', 'moveLat', true);
  hub.on('socket.lag', lag => {
    data.ping = Math.round(lag);
    redraw();
  });
  hub.on('socket.in.mlat', lat => {
    data.server = lat as number;
    redraw();
  });

  return { data };
}

function signalBars(d: PingData) {
  const lagRating = !d.ping ? 0 : d.ping < 150 ? 4 : d.ping < 300 ? 3 : d.ping < 500 ? 2 : 1;
  const bars = [];
  for (let i = 1; i <= 4; i++) bars.push(h(i <= lagRating ? 'i' : 'i.off'));
  return h(`signal.q${lagRating}`, bars);
}

function showMillis(m: number): [string, VNode] {
  return [`${Math.floor(m)}`, h('small', `.${Math.round((m - Math.floor(m)) * 10)}`)];
}

export function view(ctrl: PingCtrl): VNode {
  const d = ctrl.data;

  return h('a.status', { attrs: { href: '/lag' } }, [
    signalBars(d),
    h(
      'span.ping',
      {
        attrs: {
          title: `PING: ${i18n('networkLagBetweenYouAndLishogi')}`,
        },
      },
      [h('em', 'PING'), h('strong', defined(d.ping) ? `${d.ping}` : '?'), h('em', 'ms')],
    ),
    h(
      'span.server',
      {
        attrs: {
          title: `SERVER: ${i18n('timeToProcessAMoveOnLishogiServer')}`,
        },
      },
      [
        h('em', 'SERVER'),
        h('strong', defined(d.server) ? showMillis(d.server) : ['?']),
        h('em', 'ms'),
      ],
    ),
  ]);
}
