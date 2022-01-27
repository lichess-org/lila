import SwissCtrl from './ctrl';

export interface SwissSocket {
  send: SocketSend;
  receive(tpe: string, data: any): void;
}

export function makeSocket(send: SocketSend, ctrl: SwissCtrl) {
  const handlers: any = {
    reload() {
      const delay = Math.min(ctrl.data.nbPlayers * 10, 4000);
      if (delay > 500) setTimeout(ctrl.askReload, Math.floor(Math.random() * delay));
      else ctrl.askReload();
    },
    redirect(fullId: string) {
      ctrl.redirectFirst(fullId.slice(0, 8), true);
      return true;
    },
  };

  return {
    send,
    receive(tpe: string, data: any) {
      if (handlers[tpe]) return handlers[tpe](data);
      return false;
    },
  };
}
