import { RelayData, LogEvent } from './interfaces';

export default class RelayCtrl {

  data: RelayData;
  log: LogEvent[] = [];
  cooldown: boolean = false;

  constructor(d: RelayData, readonly send: SocketSend, readonly redraw: () => void, readonly members: any) {
    this.data = d;
    setInterval(this.redraw, 1000);
  }

  setSync = (v: Boolean) => {
    this.send('relaySync', v);
    this.redraw();
  }

  loading = () => !this.cooldown && !!this.data.sync.seconds;

  socketHandlers = {
    relayData: (d: RelayData) => {
      this.data = d;
      this.redraw();
    },
    relayLog: (event: LogEvent) => {
      this.data.sync.log.push(event);
      this.data.sync.log = this.data.sync.log.slice(-20);
      this.cooldown = true;
      setTimeout(() => { this.cooldown = false; this.redraw(); }, 3000);
      this.redraw();
      if (event.error) console.warn(`relay synchronisation error: ${event.error}`);
    }
  };

  socketHandler = (t: string, d: any) => {
    const handler = this.socketHandlers[t];
    if (handler) {
      handler(d);
      return true;
    }
    return false;
  }
}
