import { numberFormat } from 'lib/i18n';
import { licon } from 'lib/licon';

import { formatMoney, hasStripe, renderPayPal, stripeCheckout } from './checkout';
import { fiveMinutes } from './constants';
import { countSpan, esc, pluralLabel, raw, trHtml } from './html';
import type { Freq, SlideCtx } from './interfaces';
import { countAll, type Scope } from './scope';

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

export function renderDonate(el: HTMLElement, sc: Scope, { opts, bg, restart }: SlideCtx): void {
  const minutes = Math.round(opts.recap!.games.timePlaying / 60);
  const checkout = opts.checkout;
  const money = (n: number) => formatMoney(n, checkout.currency);
  const region = detectRegion();
  const anchors =
    region === 'an'
      ? [antarctica]
      : [...priceAnchors.filter(a => a.region === region), ...priceAnchors.filter(a => a.region !== region)];
  // a monthly checkout would replace an existing subscription, so patrons only get one-time donations
  const isPatron = !!(opts.user.patron || opts.user.patronColor);
  const state: { freq: Freq; tier: number } = { freq: isPatron ? 'onetime' : 'monthly', tier: 1 };
  const example = checkout.example;
  const exampleMins = i18n.site.nbMinutes(example.minutes, numberFormat(example.minutes));
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
      <div class="freq" role="radiogroup"${isPatron ? ' hidden' : ''}>
        <i class="freq__pill"></i>
        <button role="radio" data-freq="monthly">${esc(i18n.patron.monthly)}</button>
        <button role="radio" data-freq="onetime">${esc(i18n.patron.onetime)}</button>
      </div>
      <div class="tiers" role="radiogroup"></div>
      <div class="donate__pay">
        <button class="button button-fat donate__card" data-icon="${licon.Wings}">${esc(i18n.patron.donate)}</button>
        <div class="donate__paypal" data-freq="monthly"></div>
        <div class="donate__paypal" data-freq="onetime"></div>
      </div>
      <div class="donate__links">
        <a class="donate__skip" href="/patron">${esc(i18n.recap.donateAnotherAmount)}</a>
        <button class="donate__skip donate__not-today">${esc(i18n.recap.donateNotToday)}</button>
      </div>
    </div>
    <div class="donate__after" hidden></div>
    <p class="anchor reveal" style="--d:1.8s">
      ${esc(i18n.recap.donateCharity)}
      ${trHtml(i18n.recap.donateUptimeCosts.asArray(raw(`<b>${esc(exampleMins)}</b>`), raw(`<b>${esc(money(example.amount))}</b>`)))}
      ${trHtml(i18n.recap.donateSameAs.asArray(raw('<span class="ticker"></span>')))}
    </p>
    <div class="impact reveal" style="--d:2.2s">
      <p class="small">${esc(i18n.recap.donateInThoseMinutes(exampleMins))}</p>
      <div class="impact__grid">
        ${impact(fiveMinutes.moves, i18n.recap.nbMoves)}
        ${impact(fiveMinutes.games, i18n.recap.nbNewGames)}
        ${impact(fiveMinutes.players, i18n.site.nbPlayers)}
        ${impact(fiveMinutes.analysed, i18n.recap.nbGamesAnalysedByStockfish)}
      </div>
    </div>`;
  countAll(sc, el, { dur: 2000 });

  const $in = <E extends Element = HTMLElement>(s: string) => el.querySelector<E>(s)!;
  const tiersEl = $in('.tiers');
  const card = $in<HTMLButtonElement>('.donate__card');
  const payPal = {
    monthly: $in('.donate__paypal[data-freq=monthly]'),
    onetime: $in('.donate__paypal[data-freq=onetime]'),
  };
  const donation = () => ({
    amount: checkout.tiers[state.freq][state.tier].amount,
    currency: checkout.currency,
    freq: state.freq,
  });
  const panel = $in('.donate__panel');
  const afterBox = $in('.donate__after');

  let tiersShown = false;
  const update = () => {
    const monthly = state.freq === 'monthly';
    const tiers = checkout.tiers[state.freq];
    tiersEl.classList.toggle('tiers--settled', tiersShown);
    tiersShown = true;
    $in('.freq').dataset.freq = state.freq;
    el.querySelectorAll<HTMLElement>('.freq button').forEach(b =>
      b.setAttribute('aria-checked', String(b.dataset.freq === state.freq)),
    );
    tiersEl.innerHTML = tiers
      .map(({ minutes: m, amount }, i) => {
        const on = i === state.tier;
        const uptime = monthly ? i18n.recap.donateMinutesOfUptimePerMonth : i18n.recap.donateMinutesOfUptime;
        return `<button role="radio" aria-checked="${on}" class="tier${on ? ' tier--on' : ''}" data-tier="${i}">
          <b>${esc(money(amount))}</b>
          <small>${esc(uptime(m, numberFormat(m)))}</small>
        </button>`;
      })
      .join('');
    const { minutes: m } = tiers[state.tier];
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
    payPal.monthly.hidden = !monthly;
    payPal.onetime.hidden = monthly;
  };
  update();

  const payPalReady = renderPayPal(payPal, donation);
  if (!payPalReady.monthly && !payPalReady.onetime) Object.values(payPal).forEach(c => c.remove());
  card.onclick = async () => {
    if (!hasStripe()) return location.assign(`/patron?freq=${state.freq}`);
    card.disabled = true;
    try {
      await stripeCheckout(checkout.stripePublicKey, donation());
    } finally {
      card.disabled = false;
    }
  };

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

  const after = (html: string) => {
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
  };
  const replayButton = `<button class="cta cta--ghost" data-act="replay">${esc(i18n.recap.donateReplay)} ↺</button>`;
  $in('.donate__not-today').onclick = () => {
    after(
      `<p class="donate__thanks">${esc(i18n.recap.thanksTitle)}</p>
       <p class="lead">${trHtml(i18n.recap.thanksHaveAGreat.asArray(raw(`<strong>${opts.recap!.year + 1}</strong>`)))}</p>
       <p class="small">${esc(i18n.recap.donateNoWorries)} 💜</p>
       <div class="donate__actions">
         ${replayButton}
         <button class="donate__skip" data-act="back">${esc(i18n.recap.donateChangedMyMind)}</button>
       </div>`,
    );
    sc.onDispose(bg.fireworks(6, 2000));
  };

  const ticker = $in('.ticker');
  let n = 0;
  const show = () => {
    const a = anchors[n++ % anchors.length];
    ticker.innerHTML = `<span class="ticker__item">${a.flag} <b>${esc(a.text())}</b></span>`;
    if (anchors.length > 1) sc.after(n === 1 ? 6000 : 4200, show);
  };
  show();
}
