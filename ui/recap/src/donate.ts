import { currencyFormat, numberFormat } from 'lib/i18n';

import { donationTiers, fiveMinutes, uptimeUsdPerMinute } from './constants';
import { countSpan, esc, pluralLabel, raw, trHtml } from './html';
import type { SlideCtx } from './interfaces';
import { countAll, type Scope } from './scope';

type Freq = keyof typeof donationTiers;
type Region = 'na' | 'eu' | 'as' | 'sa' | 'af' | 'oc' | 'an';

interface PriceAnchor {
  region: Region;
  flag: string;
  text: () => string;
}

// things that cost about $7.50 around the world, the visitor's region first
const priceAnchors: PriceAnchor[] = [
  { region: 'na', flag: '🇺🇸', text: () => i18n.recap.donateAnchorNewYork },
  { region: 'eu', flag: '🇬🇧', text: () => i18n.recap.donateAnchorLondon },
  { region: 'eu', flag: '🇫🇷', text: () => i18n.recap.donateAnchorParis },
  { region: 'as', flag: '🇯🇵', text: () => i18n.recap.donateAnchorTokyo },
  { region: 'sa', flag: '🇧🇷', text: () => i18n.recap.donateAnchorSaoPaulo },
  { region: 'af', flag: '🇿🇦', text: () => i18n.recap.donateAnchorCapeTown },
  { region: 'oc', flag: '🇦🇺', text: () => i18n.recap.donateAnchorMelbourne },
];
const antarctica: PriceAnchor = { region: 'an', flag: '🐧', text: () => i18n.recap.donateAnchorAntarctica };

const southAmerica =
  /^America\/(Sao_Paulo|Argentina|Buenos_Aires|Bogota|Lima|Santiago|Caracas|Montevideo|La_Paz|Asuncion|Guayaquil|Cayenne|Paramaribo|Guyana|Recife|Fortaleza|Belem|Manaus|Bahia)/;

// guessed from the time zone; ?region=xx overrides it for testing
const detectRegion = (): Region => {
  const forced = new URLSearchParams(location.search).get('region');
  if (forced) return forced as Region;
  const tz = Intl.DateTimeFormat().resolvedOptions().timeZone || '';
  if (tz.startsWith('Antarctica/')) return 'an';
  if (southAmerica.test(tz)) return 'sa';
  if (tz.startsWith('America/') || tz.startsWith('US/') || tz.startsWith('Canada/')) return 'na';
  if (tz.startsWith('Europe/') || tz === 'Atlantic/Reykjavik') return 'eu';
  if (tz.startsWith('Asia/')) return 'as';
  if (tz.startsWith('Africa/')) return 'af';
  if (tz.startsWith('Australia/') || tz.startsWith('Pacific/')) return 'oc';
  return 'na';
};

const usd = (n: number) =>
  currencyFormat(n, 'USD', { minimumFractionDigits: n % 1 ? 2 : 0, maximumFractionDigits: 2 });

export function renderDonate(el: HTMLElement, sc: Scope, { opts, bg, restart }: SlideCtx): void {
  const minutes = Math.round(opts.recap!.games.timePlaying / 60);
  const region = detectRegion();
  const anchors =
    region === 'an'
      ? [antarctica]
      : [...priceAnchors.filter(a => a.region === region), ...priceAnchors.filter(a => a.region !== region)];
  const state: { freq: Freq; tier: number } = { freq: 'monthly', tier: 1 };
  const fiveMins = i18n.site.nbMinutes(5);
  const impact = (n: number, plural: I18nPlural) =>
    `<div><b class="num" data-count="${n}" data-delay="2.3">0</b><small>${esc(pluralLabel(plural, n))}</small></div>`;

  el.innerHTML = `
    <p class="kicker reveal">${esc(opts.user.patron ? i18n.recap.donateAlreadyPatron : i18n.recap.donateOneMoreThing)}</p>
    <p class="headline donate__hook reveal" style="--d:.15s">${
      minutes
        ? trHtml(
            i18n.recap.donateYouPlayedMinutes.asArray(minutes, countSpan(minutes, 0.3, 'span', 'hl num')),
          )
        : esc(i18n.patron.freeChess)
    }</p>
    <p class="lead donate__pitch reveal" style="--d:.9s"></p>
    <div class="donate__panel reveal" style="--d:1.3s">
      <div class="freq" role="radiogroup">
        <i class="freq__pill"></i>
        <button role="radio" data-freq="monthly">${esc(i18n.patron.monthly)}</button>
        <button role="radio" data-freq="onetime">${esc(i18n.patron.onetime)}</button>
      </div>
      <div class="tiers" role="radiogroup"></div>
      <div class="donate__actions">
        <a class="cta donate__go" target="_blank" rel="noopener"></a>
        <button class="donate__skip">${esc(i18n.recap.donateNotToday)}</button>
      </div>
    </div>
    <div class="donate__after" hidden></div>
    <p class="anchor reveal" style="--d:1.8s">
      ${trHtml(i18n.recap.donateUptimeCosts.asArray(raw(`<b>${esc(fiveMins)}</b>`), raw(`<b>${esc(usd(5 * uptimeUsdPerMinute))}</b>`)))}
      ${trHtml(i18n.recap.donateSameAs.asArray(raw('<span class="ticker"></span>')))}
    </p>
    <div class="impact reveal" style="--d:2.2s">
      <p class="small">${esc(i18n.recap.donateInThoseMinutes(fiveMins))}</p>
      <div class="impact__grid">
        ${impact(fiveMinutes.moves, i18n.recap.nbMoves)}
        ${impact(fiveMinutes.games, i18n.site.nbGames)}
        ${impact(fiveMinutes.players, i18n.site.nbPlayers)}
        ${impact(fiveMinutes.schools, i18n.recap.nbSchools)}
      </div>
    </div>`;
  countAll(sc, el, { dur: 2000 });

  const $in = <E extends Element = HTMLElement>(s: string) => el.querySelector<E>(s)!;
  const tiersEl = $in('.tiers');
  const go = $in<HTMLAnchorElement>('.donate__go');
  const panel = $in('.donate__panel');
  const afterBox = $in('.donate__after');

  const update = () => {
    const monthly = state.freq === 'monthly';
    const tierMins = donationTiers[state.freq];
    $in('.freq').dataset.freq = state.freq;
    el.querySelectorAll<HTMLElement>('.freq button').forEach(b =>
      b.setAttribute('aria-checked', String(b.dataset.freq === state.freq)),
    );
    tiersEl.innerHTML = tierMins
      .map((m, i) => {
        const on = i === state.tier;
        const uptime = monthly ? i18n.recap.donateMinutesOfUptimePerMonth : i18n.recap.donateMinutesOfUptime;
        return `<button role="radio" aria-checked="${on}" class="tier${on ? ' tier--on' : ''}" data-tier="${i}">
          ${i === 1 ? `<em>${esc(i18n.recap.donateSuggested)}</em>` : ''}
          <b>${esc(usd(m * uptimeUsdPerMinute))}</b>
          <small>${esc(uptime(m, numberFormat(m)))}</small>
        </button>`;
      })
      .join('');
    const m = tierMins[state.tier];
    const amount = usd(m * uptimeUsdPerMinute);
    const pitch = raw(`<strong>${esc(i18n.site.nbMinutes(m, numberFormat(m)))}</strong>`);
    $in('.donate__pitch').innerHTML = trHtml(
      (minutes
        ? monthly
          ? i18n.recap.donateConsiderMonthly
          : i18n.recap.donateConsiderOnce
        : monthly
          ? i18n.recap.donateConsiderMonthlyNoGames
          : i18n.recap.donateConsiderOnceNoGames
      ).asArray(pitch),
    );
    go.textContent = monthly ? i18n.recap.donateAmountPerMonth(amount) : i18n.recap.donateAmount(amount);
    // the patron page picks its own currency and amounts, only the frequency carries over
    go.href = `/patron?freq=${state.freq}`;
  };
  update();

  el.querySelectorAll<HTMLElement>('.freq button').forEach(
    b =>
      (b.onclick = () => {
        state.freq = b.dataset.freq as Freq;
        update();
      }),
  );
  tiersEl.addEventListener('click', e => {
    const b = (e.target as HTMLElement).closest<HTMLElement>('[data-tier]');
    if (!b) return;
    state.tier = parseInt(b.dataset.tier!);
    update();
  });

  const after = (html: string, celebrate: boolean) => {
    panel.hidden = true;
    afterBox.hidden = false;
    afterBox.innerHTML = html;
    afterBox.querySelector<HTMLElement>('[data-act=replay]')!.onclick = restart;
    const back = afterBox.querySelector<HTMLElement>('[data-act=back]');
    if (back)
      back.onclick = () => {
        afterBox.hidden = true;
        panel.hidden = false;
      };
    if (celebrate) {
      const r = afterBox.getBoundingClientRect();
      bg.burst(r.left + r.width / 2, r.top + r.height / 2, 160, ['#f59e0b', '#f43f5e', '#fde047']);
      sc.onDispose(bg.fireworks(6, 2000));
    }
  };
  const replayButton = `<button class="cta cta--ghost" data-act="replay">${esc(i18n.recap.donateReplay)} ↺</button>`;
  go.addEventListener('click', () =>
    after(
      `<p class="headline">💖</p>
       <p class="lead">${esc(i18n.recap.donateThanks)}</p>
       ${replayButton}`,
      true,
    ),
  );
  $in('.donate__skip').onclick = () =>
    after(
      `<p class="lead">${esc(i18n.recap.donateNoWorries)} 💜</p>
       <div class="donate__actions">
         ${replayButton}
         <button class="donate__skip" data-act="back">${esc(i18n.recap.donateChangedMyMind)}</button>
       </div>`,
      false,
    );

  const ticker = $in('.ticker');
  let n = 0;
  const show = () => {
    const a = anchors[n++ % anchors.length];
    ticker.innerHTML = `<span class="ticker__item">${a.flag} <b>${esc(a.text())}</b></span>`;
    if (anchors.length > 1) sc.after(n === 1 ? 4500 : 2800, show);
  };
  show();
}
