
export interface RelayData {
  syncSeconds?: number; // how long until lichess stops syncing
  pgnUrl: string;
}

export class RelayCtrl {

  data: RelayData;

  constructor(d: RelayData, readonly send: SocketSend, readonly redraw: () => void) {
    this.data = d;
  }

  setData = (d: RelayData) => {
    this.data = d;
    this.redraw();
  }

  setSync = (v: Boolean) => {
    this.send('relaySync', v);
    this.redraw();
  }
}
