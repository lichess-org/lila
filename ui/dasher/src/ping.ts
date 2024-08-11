import { h, VNode } from 'snabbdom';
import { defined } from 'common';
import { DasherCtrl } from './interfaces';
export class PingCtrl {
  ping: number | undefined;
  server: number | undefined;

  constructor(readonly root: DasherCtrl) {
    site.pubsub.on('dasher.toggle', v => (v ? this.connect() : this.disconnect()));
  }

  onLag = (lag: number): void => {
    this.ping = Math.round(lag);
    this.root.redraw();
  };
  onMlat = (lat: number): void => {
    this.server = lat;
    this.root.redraw();
  };

  connect = (): void => {
    site.pubsub.emit('socket.send', 'moveLat', true);
    site.pubsub.on('socket.lag', this.onLag);
    site.pubsub.on('socket.in.mlat', this.onMlat);
  };

  disconnect = (): void => {
    site.pubsub.off('socket.lag', this.onLag);
    site.pubsub.off('socket.in.mlat', this.onMlat);
  };

  render = (): VNode =>
    h('a.status', { attrs: { href: '/lag' }, hook: { insert: this.connect, destroy: this.disconnect } }, [
      this.signalBars(),
      h(
        'span.ping',
        { attrs: { title: 'PING: ' + this.root.trans.noarg('networkLagBetweenYouAndLichess') } },
        this.showMillis('PING', this.ping),
      ),
      h(
        'span.server',
        { attrs: { title: 'SERVER: ' + this.root.trans.noarg('timeToProcessAMoveOnLichessServer') } },
        this.showMillis('SERVER', this.server),
      ),
    ]);

  signalBars(): VNode {
    const lagRating = !this.ping ? 0 : this.ping < 150 ? 4 : this.ping < 300 ? 3 : this.ping < 500 ? 2 : 1;
    const bars = [];
    for (let i = 1; i <= 4; i++) bars.push(h(i <= lagRating ? 'i' : 'i.off'));
    return h('signal.q' + lagRating, bars);
  }

  showMillis = (name: string, m?: number): VNode[] => [
    h('em', name),
    h('strong', defined(m) ? m : '?'),
    h('em', 'ms'),
  ];
}
