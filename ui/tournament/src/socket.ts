import TournamentController from './ctrl';
import { Arrangement } from './interfaces';

export interface TournamentSocket {
  send: SocketSend;
  receive(type: string, data: any): void;
}

export default function (send: SocketSend, ctrl: TournamentController) {
  const handlers = {
    reload() {
      setTimeout(ctrl.askReload, Math.floor(Math.random() * 4000));
    },
    redirect(fullId) {
      ctrl.redirectFirst(fullId.slice(0, 8), true);
      return true;
    },
    arrangement(arr: Arrangement) {
      const users = [arr.user1.id, arr.user2.id],
        index = ctrl.data.standing.arrangements.findIndex(
          a => users.includes(a.user1.id) && users.includes(a.user2.id) && a.order === arr.order
        );
      if (index !== -1) ctrl.data.standing.arrangements[index] = arr;
      else ctrl.data.standing.arrangements.push(arr);

      if (ctrl.arrangement && users.includes(ctrl.arrangement.user1.id) && users.includes(ctrl.arrangement.user2.id))
        ctrl.arrangement = arr;

      ctrl.redraw();
    },
  };

  return {
    send,
    receive(type: string, data: any) {
      if (handlers[type]) return handlers[type](data);
      return false;
    },
  };
}
