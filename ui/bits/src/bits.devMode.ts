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
              <img class="uflair" src="${ds.assetUrl}/assets/______3/flair/img/activity.lichess-horsey.webp">
              <span class="rating">2649</span>
            </span>
            <span class="mini-game__clock mini-game__clock--black clock--run" data-time="60">0:26</span>
          </span>
          <span id="fake-tv" data-state="3R1r1k/pp4p1/2n1Q1bp/1Bp5/PqN4P/2b2NP1/1P4P1/2K4R,black,d1d8"></span>
          <span class="mini-game__player">
            <span class="mini-game__user">
              <span class="utitle" title="FIDE Master">FM</span>
              &nbsp;tester2
              <img class="uflair" src="${ds.assetUrl}/assets/______3/flair/img/activity.lichess-berserk.webp">
              <span class="rating">2760</span>
            </span>
            <span class="mini-game__clock mini-game__clock--white" data-time="60">0:19</span>
          </span>
        </a>
      </div>`);
    initMiniBoard(tv.querySelector('#fake-tv')!);
    this.append(tv);
  });

  (() => {
    // Patron tier style debugging on /patron page
    const months = [1, 2, 3, 6, 9];
    const years = Array.from({ length: 5 }, (_, i) => i + 1);

    const userLink = (className: string) =>
      ['online', 'offline']
        .map(
          state =>
            `<a class="${state} user-link" href="/@/${className}"><i class="line patron ${className}" title="Lichess Patron"></i>${className}-${state}</a>`,
        )
        .join('<br>');
    $('.best_patrons').append(
      `<hr>
        <h2>Debug Patron tiers</h2>
        <div class="list">
          <div class="paginated">${userLink('current')}</div>
        </div>
        <div class="list">
          ${months.map(m => `<div class="paginated">${userLink(`months${m}`)}</div>`).join(' ')}
        </div>
        <div class="list">
          ${years.map(y => `<div class="paginated">${userLink(`years${y}`)}</div>`).join(' ')}
        </div>
      `,
    );
  })();
}
