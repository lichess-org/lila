import TournamentController from './ctrl';
import { Arrangement } from './interfaces';

export interface TournamentSocket {
  send: Socket.Send;
  receive(type: string, data: any): void;
}

export default function (send: Socket.Send, ctrl: TournamentController): TournamentSocket {
  const handlers = {
    reload() {
      setTimeout(ctrl.askReload, Math.floor(Math.random() * 4000));
    },
    redirect(fullId) {
      ctrl.redirectFirst(fullId.slice(0, 8), true);
      return true;
    },
    arrangement(arr: Arrangement) {
      const f: (a: Arrangement) => boolean = ctrl.isRobin()
          ? a => {
              return users.includes(a.user1.id) && users.includes(a.user2.id);
            }
          : a => {
              return a.id === arr.id;
            },
        users = [arr.user1.id, arr.user2.id],
        index = ctrl.data.standing.arrangements.findIndex(a => f(a));

      if (index !== -1) ctrl.data.standing.arrangements[index] = arr;
      else ctrl.data.standing.arrangements.push(arr);

      if (ctrl.arrangement && users.includes(ctrl.arrangement.user1.id) && users.includes(ctrl.arrangement.user2.id))
        ctrl.arrangement = arr;
      else {
        const key = arr.user1.id + ';' + arr.user2.id;
        ctrl.highlightArrs.push(key);
        setTimeout(() => {
          ctrl.highlightArrs = ctrl.highlightArrs.filter(k => k !== key);
          ctrl.redraw();
        }, 6000);
      }

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
