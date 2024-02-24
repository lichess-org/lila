import { init, classModule, attributesModule, eventListenersModule } from 'snabbdom';
import { LobbyOpts } from './interfaces';
import { init as initBoard } from 'common/miniBoard';
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

  site.requestIdleCallback(() => {
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

      // fake tv for debugging layout, ui/build with -d flag
      if (site.debug && !this.querySelector('.lobby__tv')) {
        const ds = document.body.dataset;
        const tv = $as<HTMLElement>(
          `<div class="lobby__tv">
            <a href="/tv" class="mini-game mini-game-abcd1234 standard is2d">
              <span class="mini-game__player"><span class="mini-game__user"><span class="utitle" title="Candidate Master">CM</span>&nbsp;Tester1<img class="uflair" src="${ds.assetUrl}/assets/______2/flair/img/activity.lichess-horsey.webp"><span class="rating">2649</span></span><span class="mini-game__clock mini-game__clock--black clock--run" data-time="60">0:26</span></span>
              <span id="fake-tv" data-state="3R1r1k/pp4p1/2n1Q1bp/1Bp5/PqN4P/2b2NP1/1P4P1/2K4R,black,d1d8"></span>
              <span class="mini-game__player"><span class="mini-game__user"><span class="utitle" title="FIDE Master">FM</span>&nbsp;tester2<img class="uflair" src="${ds.assetUrl}/assets/______2/flair/img/activity.lichess-berserk.webp"><span class="rating">2760</span></span><span class="mini-game__clock mini-game__clock--white" data-time="60">0:19</span></span>
            </a>
          </div>`,
        );
        initBoard(tv.querySelector('#fake-tv')!);
        this.append(tv);
      }
    });
  });
