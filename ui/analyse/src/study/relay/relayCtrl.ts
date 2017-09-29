
export interface RelayData {
  syncSeconds?: number; // how long until lichess stops syncing
  pgnUrl: string;
}

export class RelayCtrl {

  constructor(readonly data: RelayData, readonly send: SocketSend, readonly redraw: () => void) {
  }

  setSync = (v: Boolean) => {
    this.send('relaySync', v);
    this.redraw();
  }
}
