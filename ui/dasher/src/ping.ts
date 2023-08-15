import { h, VNode } from 'snabbdom';

import { Redraw, defined } from './util';

export class PingCtrl {
  ping: number | undefined;
  server: number | undefined;

  constructor(
    readonly trans: Trans,
    readonly redraw: Redraw,
  ) {
    lichess.pubsub.on('dasher.toggle', v => (v ? this.connect() : this.disconnect()));
  }

  onLag = (lag: number) => {
    this.ping = Math.round(lag);
    this.redraw();
  };
  onMlat = (lat: number) => {
    this.server = lat as number;
    this.redraw();
  };

  connect = () => {
    lichess.pubsub.emit('socket.send', 'moveLat', true);
    lichess.pubsub.on('socket.lag', this.onLag);
    lichess.pubsub.on('socket.in.mlat', this.onMlat);
  };

  disconnect = () => {
    lichess.pubsub.off('socket.lag', this.onLag);
    lichess.pubsub.off('socket.in.mlat', this.onMlat);
  };
}

function signalBars(ctrl: PingCtrl) {
  const lagRating = !ctrl.ping ? 0 : ctrl.ping < 150 ? 4 : ctrl.ping < 300 ? 3 : ctrl.ping < 500 ? 2 : 1;
  const bars = [];
  for (let i = 1; i <= 4; i++) bars.push(h(i <= lagRating ? 'i' : 'i.off'));
  return h('signal.q' + lagRating, bars);
}

const showMillis = (name: string, m?: number) => [
  h('em', name),
  h('strong', defined(m) ? m : '?'),
  h('em', 'ms'),
];

export const view = (ctrl: PingCtrl): VNode =>
  h(
    'a.status',
    {
      attrs: { href: '/lag' },
      hook: { insert: ctrl.connect, destroy: ctrl.disconnect },
    },
    [
      signalBars(ctrl),
      h(
        'span.ping',
        {
          attrs: { title: 'PING: ' + ctrl.trans.noarg('networkLagBetweenYouAndLichess') },
        },
        showMillis('PING', ctrl.ping),
      ),
      h(
        'span.server',
        {
          attrs: { title: 'SERVER: ' + ctrl.trans.noarg('timeToProcessAMoveOnLichessServer') },
        },
        showMillis('SERVER', ctrl.server),
      ),
    ],
  );
