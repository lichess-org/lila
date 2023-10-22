import { init, classModule, attributesModule, eventListenersModule } from 'snabbdom';
import { LobbyOpts } from './interfaces';
import { init as initBoard } from 'common/mini-board';
import makeCtrl from './ctrl';
import appView from './view/main';
import tableView from './view/table';

export const patch = init([classModule, attributesModule, eventListenersModule]);

export default function main(opts: LobbyOpts) {
  const ctrl = new makeCtrl(opts, redraw);

  opts.appElement.innerHTML = '';
  let appVNode = patch(opts.appElement, appView(ctrl));
  opts.tableElement.innerHTML = '';
  let tableVNode = patch(opts.tableElement, tableView(ctrl));

  function redraw() {
    appVNode = patch(appVNode, appView(ctrl));
    tableVNode = patch(tableVNode, tableView(ctrl));
  }

  lichess.requestIdleCallback(() => {
    layoutHacks();
    window.addEventListener('resize', layoutHacks);
  });

  return ctrl;
}

let cols = 0;

/* Move the timeline to/from the bottom depending on screen width.
 * This must not cause any FOUC or layout shifting on page load. */
const layoutHacks = () =>
  requestAnimationFrame(() => {
    $('main.lobby').each(function (this: HTMLElement) {
      const newCols = Number(window.getComputedStyle(this).getPropertyValue('--cols'));
      if (newCols != cols) {
        cols = newCols;
        if (cols > 2) $('.lobby .lobby__timeline').appendTo('.lobby__side');
        else $('.lobby__side .lobby__timeline').appendTo('.lobby');
      }
      if (lichess.debug && !this.querySelector('.lobby__tv')) {
        const tv = $as<HTMLElement>(
          `<div class="lobby__tv"><span class="text">Fake TV</span><span class="mini-board"
          data-state="3R1r1k/pp4p1/2n1Q1bp/1Bp5/PqN4P/2b2NP1/1P4P1/2K4R,black,d1d8"/></div>`,
        );
        initBoard(tv.querySelector('.mini-board')!);
        tv.append($as<HTMLElement>('<span class="text">Cannot hurt you!</span>'));
        this.append(tv);
        // fake tv for debugging layout, ui/build with -d flag
      }
    });
  });
