import SwissController from './ctrl';

export interface SwissSocket {
  send: SocketSend;
  receive(tpe: string, data: any): void;
}

export default function(send: SocketSend, ctrl: SwissController) {

  const handlers = {
    reload() { 
      setTimeout(ctrl.askReload, Math.floor(Math.random() * 4000))
    },
    redirect(fullId) {
      ctrl.redirectFirst(fullId.slice(0, 8), true);
      return true;
    }
  };

  return {
    send,
    receive(tpe: string, data: any) {
      if (handlers[tpe]) return handlers[tpe](data);
      return false;
    }
  };
};
