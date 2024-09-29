import { initMiniBoard } from 'common/miniBoard';
import { frag } from 'common';

export function initModule(): void {
  $('main.lobby').each(function(this: HTMLElement) {
    // fake tv for debugging layout, ui/build with -d flag
    if (this.querySelector('.lobby__tv')) return;

    const ds = document.body.dataset;
    const tv = frag<HTMLElement>(
      `
      <div class="lobby__tv">
          <a href="/tv" class="mini-game mini-game-abcd1234 standard is2d">
              <span class="mini-game__player"><span class="mini-game__user"><span class="utitle" title="Candidate Master">CM</span>&nbsp;Tester1<img class="uflair" src="${ds.assetUrl}/assets/______2/flair/img/activity.lichess-horsey.webp"><span class="rating">2649</span></span><span class="mini-game__clock mini-game__clock--black clock--run" data-time="60">0:26</span></span>
              <span id="fake-tv" data-state="3R1r1k/pp4p1/2n1Q1bp/1Bp5/PqN4P/2b2NP1/1P4P1/2K4R,black,d1d8"></span>
              <span class="mini-game__player"><span class="mini-game__user"><span class="utitle" title="FIDE Master">FM</span>&nbsp;tester2<img class="uflair" src="${ds.assetUrl}/assets/______2/flair/img/activity.lichess-berserk.webp"><span class="rating">2760</span></span><span class="mini-game__clock mini-game__clock--white" data-time="60">0:19</span></span>
          </a>
      </div>`.trim(),
    );
    initMiniBoard(tv.querySelector('#fake-tv')!);
    this.append(tv);
  });
}

const original = { log: console.log, info: console.info, warn: console.warn, error: console.error };
const levels: Array<keyof typeof original> = ['log', 'info', 'warn', 'error'];
const url = typeof site.debug === 'string' ? site.debug : 'http://localhost:8666';

for (const level of levels) console[level] = (...args) => debugLog(level, ...args);

async function debugLog(level: keyof typeof original, ...args: any[]) {
  original[level](...args);
  const allGood =
    await fetch(url, { method: 'POST', body: JSON.stringify({ [level]: args }) })
      .then(rsp => rsp.ok)
      .catch(() => false);
  if (allGood) return;

  // remove our monkey patches, pack up, and go home
  for (const level of levels) console[level] = original[level];
  // fetch errors gonna log in the console. reassure curious users it's ok
  console.log(`cant reach log server at ${url}. this is fine!`);
}
