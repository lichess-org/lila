import { initMiniBoard } from 'lib/view/miniBoard';
import { frag } from 'lib';

export function initModule(): void {
  $('main.lobby').each(function (this: HTMLElement) {
    // fake tv for debugging layout, ui/build with -d flag
    if (this.querySelector('.lobby__tv')) return;

    const ds = document.body.dataset;
    const tv = frag<HTMLElement>($html`
      <div class="lobby__tv">
        <a href="/tv" class="mini-game mini-game-abcd1234 standard is2d">
          <span class="mini-game__player">
            <span class="mini-game__user">
              <span class="utitle" title="Candidate Master">CM</span>
              &nbsp;Tester1
              <img class="uflair" src="${ds.assetUrl}/assets/______4/flair/img/activity.lichess-horsey.webp">
              <span class="rating">2649</span>
            </span>
            <span class="mini-game__clock mini-game__clock--black clock--run" data-time="60">0:26</span>
          </span>
          <span id="fake-tv" data-state="3R1r1k/pp4p1/2n1Q1bp/1Bp5/PqN4P/2b2NP1/1P4P1/2K4R,black,d1d8"></span>
          <span class="mini-game__player">
            <span class="mini-game__user">
              <span class="utitle" title="FIDE Master">FM</span>
              &nbsp;tester2
              <img class="uflair" src="${ds.assetUrl}/assets/______4/flair/img/activity.lichess-berserk.webp">
              <span class="rating">2760</span>
            </span>
            <span class="mini-game__clock mini-game__clock--white" data-time="60">0:19</span>
          </span>
        </a>
      </div>`);
    initMiniBoard(tv.querySelector('#fake-tv')!);
    this.append(tv);
  });
}
