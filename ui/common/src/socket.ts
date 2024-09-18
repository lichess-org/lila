import * as xhr from './xhr';
import { idleTimer, browserTaskQueueMonitor } from './timing';
import { storage, once, type LichessStorage } from './storage';
import { objectStorage, dbExists, type ObjectStorage } from './objectStorage';
import { pubsub, type PubsubEvent } from './pubsub';

type Sri = string;
type Tpe = string;
type Payload = any;
type Version = number;
interface MsgBase {
  t: Tpe;
  d?: Payload;
}
interface MsgIn extends MsgBase {
  v?: Version;
}
interface MsgOut extends MsgBase {}
interface MsgAck extends MsgOut {
  at: number;
}

interface Options {
  idle: boolean;
  pongTimeout: number; // time to wait for pong before resetting the connection
  pingDelay: number; // time between pong and ping
  autoReconnectDelay: number;
  protocol: string;
  isAuth: boolean;
  debug?: boolean;
}
interface Params extends Record<string, any> {
  sri?: Sri;
  flag?: string;
}

interface Settings {
  receive?: (t: Tpe, d: Payload) => void;
  events: {
    [tpe: string]: (d: Payload | null, msg: MsgIn) => any;
  };
  params?: Partial<Params>;
  options?: Partial<Options>;
}
// TODO - find out why are there three different types of settings

const isOnline = () => !('onLine' in navigator) || navigator.onLine;

export default class StrongSocket implements SocketI {
  averageLag = 0;

  private settings: Settings;
  private options: Options;
  private version: number | false;
  private ws: WebSocket | undefined;
  private pingSchedule: Timeout;
  private connectSchedule: Timeout;
  private ackable: Ackable = new Ackable((t, d, o) => this.send(t, d, o));
  private lastPingTime: number = performance.now();
  private pongCount = 0;
  private tryOtherUrl = false;
  private storage: LichessStorage = storage.make('surl17', 30 * 60 * 1000);
  private _sign?: string;
  private resendWhenOpen: [string, any, any][] = [];
  private baseUrls = document.body.dataset.socketDomains!.split(',');

  private lastUrl?: string;
  private heartbeat = browserTaskQueueMonitor(1000);
  private isTestUser = document.body.dataset.socketTestUser === 'true';
  private isTestRunning = document.body.dataset.socketTestRunning === 'true';
  private stats: { store?: ObjectStorage<any>, m2: number, n: number, mean: number } =
    { m2: 0, n: 0, mean: 0 };

  constructor(
    readonly url: string,
    version: number | false,
    settings: Partial<Settings> = {},
  ) {
    this.options = {
      idle: false,
      debug: false,
      pongTimeout: 9000,
      autoReconnectDelay: 3500,
      protocol: location.protocol === 'https:' ? 'wss:' : 'ws:',
      isAuth: document.body.hasAttribute('data-user'),
      ...(settings.options || {}),
      pingDelay: 2500,
    };
    this.settings = {
      receive: settings.receive,
      events: settings.events || {},
      params: {
        sri: site.sri,
        ...(settings.params || {}),
      },
    };
    this.version = version;
    pubsub.on('socket.send', this.send);
    this.connect();
    this.flushStats();
    window.addEventListener('pagehide', () => this.storeStats({ event: 'pagehide' }));
  }

  sign = (s: string): void => {
    this._sign = s;
    this.ackable.sign(s);
  };

  private connect = (): void => {
    this.destroy();
    if (!isOnline()) {
      document.body.classList.remove('online');
      document.body.classList.add('offline');
      $('#network-status').text(site ? site.trans('noNetwork') : 'Offline');
      this.scheduleConnect(4000);
      return;
    }
    this.lastUrl = xhr.url(this.options.protocol + '//' + this.nextBaseUrl() + this.url, {
      ...this.settings.params,
      v: this.version === false ? undefined : this.version,
    });
    this.debug('connection attempt to ' + this.lastUrl);
    try {
      const ws = (this.ws = new WebSocket(this.lastUrl));
      ws.onerror = e => this.onError(e);
      ws.onclose = this.onClose;
      ws.onopen = () => {
        this.lastUrl = ws.url;
        this.debug('connected to ' + this.lastUrl);
        const cl = document.body.classList;
        if (pubsub.past('socket.hasConnected')) cl.add('reconnected');
        cl.remove('offline');
        cl.add('online');
        this.onSuccess();
        this.pingNow();
        this.resendWhenOpen.forEach(([t, d, o]) => this.send(t, d, o));
        this.resendWhenOpen = [];
        pubsub.emit('socket.open');
        this.ackable.resend();
      };
      ws.onmessage = e => {
        if (e.data == 0) return this.pong();
        const m = JSON.parse(e.data);
        if (m.t === 'n') this.pong();
        this.handle(m);
      };
    } catch (e) {
      this.onClose({ code: 4000, reason: String(e) } as CloseEvent);
    }
    this.scheduleConnect();
  };

  send = (t: string, d: any, o: any = {}, noRetry = false): void => {
    const msg: Partial<MsgOut> = { t };
    if (d !== undefined) {
      if (o.withLag) d.l = Math.round(this.averageLag);
      if (o.millis >= 0) d.s = Math.round(o.millis * 0.1).toString(36);
      msg.d = d;
    }
    if (o.ackable) {
      msg.d = msg.d || {}; // can't ack message without data
      this.ackable.register(t, msg.d); // adds d.a, the ack ID we expect to get back
    }

    const message = JSON.stringify(msg);
    if (t == 'racerScore' && o.sign != this._sign) return;
    if (t == 'move' && o.sign != this._sign) {
      let stack: string;
      try {
        stack = new Error().stack!.split('\n').join(' / ').replace(/\s+/g, ' ');
      } catch (e: any) {
        stack = `${e.message} ${navigator.userAgent}`;
      }
      if (!stack.includes('round.nvui'))
        setTimeout(() => {
          if (once(`socket.rep.${Math.round(Date.now() / 1000 / 3600 / 3)}`))
            this.send('rep', { n: `soc: ${message} ${stack}` });
          else site.socket.destroy();
        }, 10000);
    }
    this.debug('send ' + message);
    if (!this.ws || this.ws.readyState === WebSocket.CONNECTING) {
      if (!noRetry) this.resendWhenOpen.push([t, msg.d, o]);
    } else this.ws.send(message);
  };

  private scheduleConnect = (delay: number = this.options.pongTimeout): void => {
    if (this.options.idle) delay = 10 * 1000 + Math.random() * 10 * 1000;
    // debug('schedule connect ' + delay);
    clearTimeout(this.pingSchedule);
    clearTimeout(this.connectSchedule);
    this.connectSchedule = setTimeout(() => {
      document.body.classList.add('offline');
      document.body.classList.remove('online');
      $('#network-status').text(site.trans ? site.trans('reconnecting') : 'Reconnecting');
      this.tryOtherUrl = true;
      this.connect();
    }, delay);
  };

  private schedulePing = (delay: number): void => {
    clearTimeout(this.pingSchedule);
    this.pingSchedule = setTimeout(this.pingNow, delay);
  };

  private pingNow = (): void => {
    clearTimeout(this.pingSchedule);
    clearTimeout(this.connectSchedule);
    const pingData =
      this.options.isAuth && this.pongCount % 10 == 2
        ? JSON.stringify({
          t: 'p',
          l: Math.round(0.1 * this.averageLag),
        })
        : 'null';
    try {
      this.ws!.send(pingData);
      this.lastPingTime = performance.now();
    } catch (e) {
      this.debug(e, true);
    }
    this.scheduleConnect();
  };

  private computePingDelay = (): number => this.options.pingDelay + (this.options.idle ? 1000 : 0);

  private pong = (): void => {
    clearTimeout(this.connectSchedule);
    this.schedulePing(this.computePingDelay());
    const currentLag = Math.min(performance.now() - this.lastPingTime, 10000);
    this.pongCount++;

    // Average first 4 pings, then switch to decaying average.
    const mix = this.pongCount > 4 ? 0.1 : 1 / this.pongCount;
    this.averageLag += mix * (currentLag - this.averageLag);

    pubsub.emit('socket.lag', this.averageLag);
    this.updateStats(currentLag);
  };

  private handle = (m: MsgIn): void => {
    if (m.v && this.version !== false) {
      if (m.v <= this.version) {
        this.debug('already has event ' + m.v);
        return;
      }
      // it's impossible but according to previous logging, it happens nonetheless
      if (m.v > this.version + 1) return site.reload();
      this.version = m.v;
    }
    switch (m.t || false) {
      case false:
        break;
      case 'resync':
        setTimeout(() => site.reload('lila-ws resync'), 500);
        break;
      case 'ack':
        this.ackable.onServerAck(m.d);
        break;
      default:
        // return true in a receive handler to prevent pubsub and events
        if (!(this.settings.receive && this.settings.receive(m.t, m.d))) {
          const sentAsEvent = this.settings.events[m.t] && this.settings.events[m.t](m.d || null, m);
          if (!sentAsEvent) {
            pubsub.emit('socket.in.' + m.t as PubsubEvent, m.d, m);
          }
        }
    }
  };

  private debug = (msg: unknown, always = false): void => {
    if (always || this.options.debug) console.debug(msg);
  };

  destroy = (): void => {
    this.storeStats();
    clearTimeout(this.pingSchedule);
    clearTimeout(this.connectSchedule);
    this.disconnect();
    this.ws = undefined;
  };

  private disconnect = (): void => {
    const ws = this.ws;
    if (ws) {
      this.debug('Disconnect');
      ws.onerror = ws.onclose = ws.onopen = ws.onmessage = () => {};
      ws.close();
    }
  };

  private onError = (e: unknown): void => {
    if (this.heartbeat.wasSuspended) return;
    this.options.debug = true;
    this.debug(`error: ${e} ${JSON.stringify(e)}`); // e not always from lila
  };

  private onClose = (e: CloseEvent): void => {
    pubsub.emit('socket.close');

    if (this.heartbeat.wasSuspended) return this.onSuspended();
    this.storeStats({ event: 'close', code: e.code });

    if (this.ws) {
      this.debug('Will autoreconnect in ' + this.options.autoReconnectDelay);
      this.scheduleConnect(this.options.autoReconnectDelay);
    }
    if (e.wasClean && e.code < 1002) return;

    if (isOnline()) this.tryOtherUrl = true;
    clearTimeout(this.pingSchedule);
  };

  private onSuccess = (): void => {
    if (pubsub.past('socket.hasConnected')) return;

    pubsub.complete('socket.hasConnected');
    let disconnectTimeout: Timeout | undefined;
    idleTimer(
      10 * 60 * 1000,
      () => {
        this.options.idle = true;
        disconnectTimeout = setTimeout(this.destroy, 2 * 60 * 60 * 1000);
      },
      () => {
        this.options.idle = false;
        if (this.ws) clearTimeout(disconnectTimeout);
        else location.reload();
      },
    );
  };

  private onSuspended() {
    this.heartbeat.reset(); // not a networking error, just get our connection back
    clearTimeout(this.pingSchedule);
    clearTimeout(this.connectSchedule);
    this.storeStats({ event: 'suspend' }).then(this.connect);
  }

  private nextBaseUrl = (): string => {
    let url = this.storage.get();
    if (!url || !this.baseUrls.includes(url)) {
      url = this.baseUrls[Math.floor(Math.random() * this.baseUrls.length)];
      this.storage.set(url);
    } else if ((this.isTestUser && this.isTestRunning) || this.tryOtherUrl) {
      const i = this.baseUrls.findIndex(u => u === url);
      url = this.baseUrls[(i + 1) % this.baseUrls.length];
      this.storage.set(url);
    }
    this.tryOtherUrl = false;
    return url;
  };

  pingInterval = (): number => this.computePingDelay() + this.averageLag;
  getVersion = (): number | false => this.version;

  private async storeStats(event?: any) {
    if (!this.lastUrl || !this.isTestUser || !this.isTestRunning) return;
    if (!event && this.stats.n < 2) return;

    const data = {
      dns: this.lastUrl.includes(`//${this.baseUrls[0]}`) ? 'ovh': 'cf',
      n: this.stats.n,
      ...event,
    };
    if (this.stats.n > 0) data.mean = this.stats.mean;
    if (this.stats.n > 1) data.stdev = Math.sqrt(this.stats.m2 / (this.stats.n - 1));
    this.stats.m2 = this.stats.n = this.stats.mean = 0;

    localStorage.setItem(`socket.test.${document.body.dataset.user}`, JSON.stringify(data));
    return this.flushStats();
  }

  private async flushStats() {
    if (!this.isTestUser) return;

    const storeKey = `socket.test.${document.body.dataset.user}`;
    const last = localStorage.getItem(storeKey);

    if (!last && !this.isTestRunning && !await dbExists({ store: storeKey })) return;

    this.stats.store ??= await objectStorage<any, number>({ store: storeKey });
    if (last) await this.stats.store.put(await this.stats.store.count(), JSON.parse(last));

    localStorage.removeItem(storeKey);

    if (this.isTestRunning) return;

    const data = await this.stats.store.getMany();
    const rsp = await fetch('/dev/socket-test', {
      method: 'POST',
      body: JSON.stringify(data),
      headers: { 'Content-Type': 'application/json' },
    });
    if (!rsp.ok) return;

    window.indexedDB.deleteDatabase(`${storeKey}--db`);
  }

  private updateStats(lag: number) {
    if (!this.isTestUser || !this.isTestRunning) return;

    this.stats.n++;
    const delta = lag - this.stats.mean;
    this.stats.mean += delta / this.stats.n;
    this.stats.m2 += delta * (lag - this.stats.mean);
  }
}

class Ackable {
  currentId = 1; // increment with each ackable message sent
  messages: MsgAck[] = [];
  private _sign: string;

  constructor(readonly send: (t: Tpe, d: Payload, o?: any) => void) {
    setInterval(this.resend, 1200);
  }

  sign = (s: string): string => (this._sign = s);

  resend = (): void => {
    const resendCutoff = performance.now() - 2500;
    this.messages.forEach(m => {
      if (m.at < resendCutoff) this.send(m.t, m.d, { sign: this._sign });
    });
  };

  register = (t: Tpe, d: Payload): void => {
    d.a = this.currentId++;
    this.messages.push({
      t: t,
      d: d,
      at: performance.now(),
    });
  };

  onServerAck = (id: number): void => {
    this.messages = this.messages.filter(m => m.d.a !== id);
  };
}
