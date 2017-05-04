import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode'

import { Redraw, Prop, defined } from './util'

export interface PingData {
  ping: number | undefined
  server: number | undefined
}

export interface PingCtrl {
  data: PingData
  trans: Trans
}

export function ctrl(trans: Prop<Trans>, redraw: Redraw): PingCtrl {

  const data: PingData = {
    ping: undefined,
    server: undefined
  };

  const hub = window.lichess.pubsub;

  hub.emit('socket.send')('moveLat', true);
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

function ledStyle(d: PingData): string {
  const l = (d.server || 0) + (d.ping || 0) - 100;
  const ratio = Math.max(Math.min(l / 1200, 1), 0);
  const hue = ((1 - ratio) * 120).toString(10);
  return `background: hsl(${hue},100%,40%)`;
}

export function view(ctrl: PingCtrl): VNode {

  const d = ctrl.data;

  return h('a.status', { attrs: {href: '/lag'} }, [
    h('span.led', {
      attrs: { style: ledStyle(d) }
    }),
    h('span.ping.hint--left', {
      attrs: { 'data-hint': 'PING: ' + ctrl.trans.noarg('networkLagBetweenYouAndLichess') }
    }, [
      h('em', 'PING'),
      h('strong', defined(d.ping) ? '' + d.ping : '?'),
      h('em', 'ms')
    ]),
    h('span.server.hint--left', {
      attrs: { 'data-hint': 'SERVER: ' + ctrl.trans.noarg('timeToProcessAMoveOnLichessServer') }
    }, [
      h('em', 'SERVER'),
      h('strong', defined(d.server) ? '' + d.server : '?'),
      h('em', 'ms')
    ])
  ]);
}

