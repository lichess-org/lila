import type { GameCtrl } from './gameCtrl';
import { showSetupDialog } from './setupDialog';
import type { LocalLila } from 'round';
import { replayable } from 'game';
import * as licon from 'common/licon';
import { looseH as h, bind } from 'common/snabbdom';
import { analyse } from './analyse';

export function makeFakeLila(gameCtrl: GameCtrl): LocalLila {
  const handlers: SocketHandlers = {
    move: (d: any) => gameCtrl.move(d.u),
    'blindfold-no': () => {},
    'blindfold-yes': () => {},
    'rematch-yes': () => gameCtrl.reset(),
    // TODO fix 'new-opponent', should be a typed function not a mock lila-ws message
    'new-opponent': () => showSetupDialog(gameCtrl.botCtrl, gameCtrl.setup, gameCtrl),
    resign: () => gameCtrl.resign(),
    'draw-yes': () => gameCtrl.draw(),
    abort: () => gameCtrl.abort(),
  };
  const send = (t: string, d?: any) => {
    if (handlers[t]) handlers[t]?.(d);
    else console.log('send: no handler for', t, d);
  };
  return {
    send,
    handlers,
    analyseButton: (isIcon: boolean) => {
      return (
        replayable(gameCtrl.data) &&
        h(
          isIcon ? 'button.button-none.fbt.analysis' : 'button.fbt',
          {
            attrs: isIcon ? { title: gameCtrl.round.noarg('analysis'), 'data-icon': licon.Microscope } : {},
            hook: bind('click', () => analyse(gameCtrl)),
          },
          !isIcon && gameCtrl.round.noarg('analysis'),
        )
      );
    },
    moreTime: () => {},
    outoftime: () => gameCtrl.flag(),
    berserk: () => {},
    sendLoading(typ: string, data?: any) {
      send(typ, data);
    },
    receive: (typ: string, data: any) => {
      if (handlers[typ]) handlers[typ]?.(data);
      else console.log('recv: no handler for', typ, data);
      return true;
    },
    reload: site.reload,
  };
}
