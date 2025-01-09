import { storage as makeStorage } from './storage';
import { pubsub } from './pubsub';
import { reload } from './navigation';
import { idleTimer } from 'common/timings';
import { urlWithParams } from './xhr';

// versioned events, acks, retries, resync
export class StrongSocket implements IStrongSocket {
  settings: Socket.Settings;
  options: Socket.Options;
  version: number | false;
  ws: WebSocket | undefined;
  pingSchedule: Timeout;
  connectSchedule: Timeout;
  ackable: Ackable = new Ackable((t, d, o) => this.send(t, d, o));
  lastPingTime: number = performance.now();
  pongCount: number = 0;
  averageLag: number = 0;
  isOpen: boolean = false;
  tryOtherUrl: boolean = false;
  autoReconnect: boolean = true;
  nbConnects: number = 0;
  storage: LishogiStorage = makeStorage.make('surl8');
  private _sign?: string;

  static defaultOptions: Socket.Options = {
    idle: false,
    pingMaxLag: 9000, // time to wait for pong before reseting the connection
    pingDelay: 2500, // time between pong and ping
    autoReconnectDelay: 3500,
    protocol: location.protocol === 'https:' ? 'wss:' : 'ws:',
    isAuth: document.body.hasAttribute('data-user'), // todo check
  };
  static defaultParams: { sri: string } = {
    sri: window.lishogi.sri,
  };

  static resolveFirstConnect: (send: Socket.Send) => void;
  static firstConnect: Promise<Socket.Send> = new Promise<Socket.Send>(r => {
    StrongSocket.resolveFirstConnect = r;
  });

  constructor(
    readonly url: string,
    version: number | false,
    settings: Partial<Socket.Settings> = {}
  ) {
    this.settings = {
      receive: settings.receive,
      events: settings.events || {},
      params: {
        sri: window.lishogi.sri,
        ...(settings.params || {}),
      },
    };
    this.options = {
      ...StrongSocket.defaultOptions,
      ...(settings.options || {}),
    };
    this.version = version;
    pubsub.on('socket.send', this.send);
    this.connect();
  }

  sign = (s: string): void => {
    this._sign = s;
    this.ackable.sign(s);
  };

  connect = (): void => {
    this.destroy();
    this.autoReconnect = true;
    const fullUrl = urlWithParams(this.options.protocol + '//' + this.baseUrl() + this.url, {
      ...this.settings.params,
      v: this.version === false ? undefined : this.version.toString(),
    });

    this.debug('connection attempt to ' + fullUrl);
    try {
      const ws = (this.ws = new WebSocket(fullUrl));
      ws.onerror = e => this.onError(e);
      ws.onclose = () => {
        this.isOpen = false;
        pubsub.emit('socket.close');
        if (this.autoReconnect) {
          this.debug('Will autoreconnect in ' + this.options.autoReconnectDelay);
          this.scheduleConnect(this.options.autoReconnectDelay);
        }
      };
      ws.onopen = () => {
        this.debug('connected to ' + fullUrl);
        this.onSuccess();
        const cl = document.body.classList;
        cl.remove('offline');
        cl.add('online');
        cl.toggle('reconnected', this.nbConnects > 1);
        this.pingNow();
        this.isOpen = true;
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
      this.onError(e);
    }
    this.scheduleConnect(this.options.pingMaxLag);
  };

  send = (t: string, d: any, o: any = {}, noRetry = false): void => {
    const msg: Partial<Socket.MsgOut> = { t };
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
    if (t == 'move' && o.sign != this._sign) {
      let stack: string;
      try {
        stack = new Error().stack!.split('\n').join(' / ').replace(/\s+/g, ' ');
      } catch (e) {
        stack = `${e.message} ${navigator.userAgent}`;
      }
      if (!stack.includes('round.nvui'))
        setTimeout(() => this.send('rep', { n: `soc: ${message} ${stack}` }), 10000);
    }
    this.debug('send ' + message);
    try {
      this.ws!.send(message);
    } catch (e) {
      // maybe sent before socket opens,
      // try again a second later.
      if (!noRetry) setTimeout(() => this.send(t, msg.d, o, true), 1000);
    }
  };

  scheduleConnect = (delay: number): void => {
    if (this.options.idle) delay = 10 * 1000 + Math.random() * 10 * 1000;
    // debug('schedule connect ' + delay);
    clearTimeout(this.pingSchedule);
    clearTimeout(this.connectSchedule);
    this.connectSchedule = setTimeout(() => {
      document.body.classList.add('offline');
      document.body.classList.remove('online');
      this.tryOtherUrl = true;
      this.connect();
    }, delay);
  };

  schedulePing = (delay: number): void => {
    clearTimeout(this.pingSchedule);
    this.pingSchedule = setTimeout(this.pingNow, delay);
  };

  pingNow = (): void => {
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
    this.scheduleConnect(this.options.pingMaxLag);
  };

  computePingDelay = (): number => this.options.pingDelay + (this.options.idle ? 1000 : 0);

  pong = (): void => {
    clearTimeout(this.connectSchedule);
    this.schedulePing(this.computePingDelay());
    const currentLag = Math.min(performance.now() - this.lastPingTime, 10000);
    this.pongCount++;

    // Average first 4 pings, then switch to decaying average.
    const mix = this.pongCount > 4 ? 0.1 : 1 / this.pongCount;
    this.averageLag += mix * (currentLag - this.averageLag);

    pubsub.emit('socket.lag', this.averageLag);
  };

  handle = (m: Socket.MsgIn): any => {
    if (m.v && this.version !== false) {
      if (m.v <= this.version) {
        this.debug('already has event ' + m.v);
        return;
      }
      // it's impossible but according to previous logging, it happens nonetheless
      if (m.v > this.version + 1) return reload();
      this.version = m.v;
    }
    switch (m.t || false) {
      case false:
        break;
      case 'resync':
        reload();
        break;
      case 'ack':
        this.ackable.onServerAck(m.d);
        break;
      default:
        // return true in a receive handler to prevent pubsub and events
        if (!(this.settings.receive && this.settings.receive(m.t, m.d))) {
          pubsub.emit('socket.in.' + m.t, m.d, m);
          if (this.settings?.events?.[m.t]) this.settings.events[m.t](m.d || null, m);
        }
    }
  };

  debug = (msg: string, always = false): void => {
    if (always || this.options.debug) console.debug(msg);
  };

  destroy = (): void => {
    console.log('destroy');

    clearTimeout(this.pingSchedule);
    clearTimeout(this.connectSchedule);
    this.disconnect();
    this.ws = undefined;
  };

  disconnect = (): void => {
    console.log('DISCONENNT');

    const ws = this.ws;
    if (ws) {
      this.debug('Disconnect');
      this.autoReconnect = false;
      ws.onerror = ws.onclose = ws.onopen = ws.onmessage = () => {};
      ws.close();
    }
  };

  onError = (e: Event): void => {
    console.log('ERRROR', e);

    this.options.debug = true;
    this.debug('error: ' + JSON.stringify(e));
    this.tryOtherUrl = true;
    clearTimeout(this.pingSchedule);
  };

  onSuccess = (): void => {
    this.nbConnects++;
    if (this.nbConnects == 1) {
      StrongSocket.resolveFirstConnect(this.send);
      let disconnectTimeout: Timeout | undefined;
      idleTimer(
        10 * 60 * 1000,
        () => {
          this.options.idle = true;
          console.log('idle?');

          disconnectTimeout = setTimeout(this.destroy, 2 * 60 * 60 * 1000);
        },
        () => {
          this.options.idle = false;
          if (this.ws) clearTimeout(disconnectTimeout);
          else location.reload();
        }
      );
    }
  };

  baseUrl = (): string => {
    const baseUrls = document.body.getAttribute('data-socket-domains')!.split(',');
    let url = this.storage.get();
    if (!url || this.tryOtherUrl) {
      url = baseUrls[Math.floor(Math.random() * baseUrls.length)];
      this.storage.set(url);
    }
    return url;
  };

  pingInterval = (): number => this.computePingDelay() + this.averageLag;
  getVersion = (): number | false => this.version;
}

class Ackable {
  currentId = 1; // increment with each ackable message sent
  messages: Socket.MsgAck[] = [];
  private _sign: string;

  constructor(readonly send: Socket.Send) {
    setInterval(this.resend, 1200);
  }

  sign = (s: string): string => (this._sign = s);

  resend = (): void => {
    const resendCutoff = performance.now() - 2500;
    this.messages.forEach(m => {
      if (m.at < resendCutoff) this.send(m.t, m.d, { sign: this._sign });
    });
  };

  register = (t: Socket.Tpe, d: Socket.Payload): void => {
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
