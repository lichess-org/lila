import { myUserId } from 'lib';
import { licon } from 'lib/licon';
import { pubsub } from 'lib/pubsub';
import { alert, domDialog, makeLinkPopups } from 'lib/view';
import * as xhr from 'lib/xhr';

const gamesAngle = document.querySelector<HTMLElement>('.games');
if (gamesAngle) gamesAngle.style.visibility = 'hidden'; // FOUC

export interface TrophyItem {
  cls: string;
  title: string;
  href?: string;
  icon?: string;
  imgSrc?: string;
  imgW?: number;
  imgH?: number;
  stacked?: boolean;
  badge?: boolean;
  primary?: boolean;
}

export async function initModule(data: { trophies?: TrophyItem[]; username?: string }): Promise<void> {
  makeLinkPopups($('.social_links'));
  makeLinkPopups($('.user-infos .bio'));

  tmpRandomTutorLink();
  if (data?.trophies) initTrophies(data.trophies, data.username);

  window.addEventListener('pageshow', (e: PageTransitionEvent) => {
    if (e.persisted && data?.trophies) initTrophies(data.trophies, data.username);
  });

  const loadNoteZone = () => {
    const $zone = $('.user-show .note-zone');
    $zone.find('textarea')[0]?.focus();
    if ($zone.hasClass('loaded')) return;
    $zone.addClass('loaded');
    $noteToggle.find('strong').text(String($zone.find('.note').length));
    $zone.find('.note-form button[type=submit]').on('click', function (this: HTMLButtonElement) {
      $(this)
        .parents('form')
        .each((_, form: HTMLFormElement) =>
          xhr
            .formToXhr(form, this)
            .then(html => $zone.replaceWith(html))
            .then(() => loadNoteZone())
            .catch(() => alert('Invalid note, is it too short or too long?')),
        );
      return false;
    });
  };

  const $noteToggle = $('.user-show .note-zone-toggle').on('click', () => {
    $('.user-show .note-zone').toggle();
    loadNoteZone();
  });
  if (location.search.includes('note')) $noteToggle.trigger('click');

  $('.user-show .claim_title_zone').each(function (this: HTMLElement) {
    const $zone = $(this);
    $zone.find('.actions a').on('click', function (this: HTMLAnchorElement) {
      xhr.text(this.href, { method: 'post' });
      $zone.remove();
      return false;
    });
  });

  $('.user-show .angles').each(function (this: HTMLElement) {
    const $angles = $(this),
      $content = $('.angle-content'),
      browseTo = (path: string) =>
        xhr.text(path).then(html => {
          $content.html(html);
          pubsub.emit('content-loaded', $content[0]); // TODO don't do this twice
          history.replaceState({}, '', path);
          site.asset.loadEsm('bits.infiniteScroll');
        });
    $angles.on('click', 'a', function (this: HTMLAnchorElement) {
      if ($('#games .to-search').hasClass('active')) return true;
      $angles.find('.active').removeClass('active');
      $(this).addClass('active');
      browseTo(this.href);
      return false;
    });
    $('.user-show').on('click', '#games a', function (this: HTMLAnchorElement) {
      if ($('#games .to-search').hasClass('active') || $(this).hasClass('to-search')) return true;
      $(this).addClass('active');
      browseTo(this.href);
      return false;
    });
  });
  setTimeout(() => {
    if (gamesAngle) gamesAngle.style.visibility = 'visible'; // FOUC
  });
}

function tmpRandomTutorLink() {
  const me = myUserId(),
    userId = $('main.page-menu').data('username').toLowerCase();
  if (!me || !userId || me !== userId) return;
  const getNbGames = (icon: string) => {
    const text = $(`.sub-ratings a[data-icon=${icon}] rating span:last-child`).text();
    return Number.parseInt(text.replaceAll(/\D/g, ''));
  };
  const enoughGames = [licon.Bullet, licon.FlameBlitz, licon.Rabbit, licon.Turtle].some(
    icon => getNbGames(icon) > 100,
  );
  if (!enoughGames) return;
  const buttonHtml = `
  <a href="/tutor" class="tutor-link">
    <img src="${site.asset.flairSrc('nature.octopus-howard')}" />
    <span><strong>Try out Tutor</strong><em>Compare to your peers!</em></span>
  </a>`;
  $(buttonHtml).insertBefore('.profile-side .insight');
}

let trophiesDialogOpen = false;

function initTrophies(items: TrophyItem[], username?: string) {
  const el = document.querySelector<HTMLElement>('.trophies');
  if (!el || !items.length) return;

  const allCups = dedup(items.filter(t => !t.badge));
  const cups = allCups.filter(t => t.primary !== false);
  const badges = items.filter(t => t.badge);
  const cupEls = cups.map(makeTrophy);
  const badgeEls = badges.map(makeTrophy);

  const measure = (nodes: HTMLElement[]) => {
    el.replaceChildren(...nodes);
    const r = nodes.map(n => ({
      node: n,
      width:
        n.getBoundingClientRect().width +
        parseFloat(window.getComputedStyle(n).marginLeft) +
        parseFloat(window.getComputedStyle(n).marginRight),
    }));
    el.replaceChildren();
    return r;
  };

  const allImgs: HTMLImageElement[] = [];
  for (const n of [...cupEls, ...badgeEls]) for (const img of n.querySelectorAll('img')) allImgs.push(img);

  const waits = allImgs.map(img =>
    img.complete
      ? Promise.resolve()
      : new Promise<void>(r => {
          img.addEventListener('load', () => r(), { once: true });
          img.addEventListener('error', () => r(), { once: true });
        }),
  );

  const render = () => {
    if (!el.clientWidth) return;
    const gap = parseFloat(window.getComputedStyle(el).columnGap) || 0;
    const moreBtn = makeMoreBtn(0, allCups, username);
    const bM = measure(badgeEls);
    const cM = measure(cupEls);
    const moreW = measure([moreBtn])[0].width;
    const bTotal = bM.reduce((s, m) => s + m.width + gap, 0) - gap;

    const tailWidth = (cnt: number) => {
      let w = 0;
      for (let i = cM.length - cnt; i < cM.length; i++) w += cM[i].width + gap;
      return w;
    };

    const layout = () => {
      const avail = el.clientWidth;
      if (!avail) return;
      let n = cM.length;
      if (tailWidth(n) + bTotal > avail) while (n && moreW + gap + tailWidth(n) + bTotal > avail) n--;
      const hidden = cM.length - n;
      el.replaceChildren();
      if (hidden) el.appendChild(makeMoreBtn(hidden, allCups, username));
      for (let i = cM.length - n; i < cM.length; i++) el.appendChild(cM[i].node);
      bM.forEach(m => el.appendChild(m.node));
    };

    layout();
    new ResizeObserver(layout).observe(el);
  };

  let rendered = false;
  const onReady = () => {
    if (rendered) return;
    rendered = true;
    render();
  };

  Promise.all([document.fonts.ready, ...waits]).then(onReady);
  setTimeout(onReady, 2000);
}

function dedup(items: TrophyItem[]): TrophyItem[] {
  const seen = new Set<string>();
  return items.filter(t => (seen.has(t.title) ? false : (seen.add(t.title), true)));
}

function makeTrophy(t: TrophyItem): HTMLElement {
  const el = document.createElement(t.href ? 'a' : 'span');
  el.className = t.cls;
  el.title = t.title;
  el.setAttribute('aria-label', t.title);
  if (t.href) (el as HTMLAnchorElement).href = t.href;
  if (t.icon) el.textContent = t.icon;
  if (t.imgSrc) {
    const img = document.createElement('img');
    img.src = t.imgSrc;
    if (t.imgW) img.width = t.imgW;
    if (t.imgH) img.height = t.imgH;
    el.appendChild(img);
  }
  if (t.stacked) {
    const wrap = document.createElement('span');
    wrap.className = 'stacked';
    wrap.appendChild(el);
    return wrap;
  }
  return el;
}

function makeMoreBtn(hidden: number, allCups: TrophyItem[], username?: string): HTMLElement {
  const button = document.createElement('button');
  button.type = 'button';
  button.className = 'more-trophies';
  button.textContent = `+${hidden}`;
  button.title = i18n.site.more;
  button.setAttribute('aria-label', i18n.site.more);
  button.addEventListener('click', () => openAllTrophies(allCups, username));
  return button;
}

function openAllTrophies(cups: TrophyItem[], username?: string) {
  if (trophiesDialogOpen) return;
  trophiesDialogOpen = true;

  const $grid = $(`<div class="all-trophies"/>`).append(
    $(`<h2 class="all-trophies__title"/>`).text(username ?? i18n.site.more),
    ...cups.map(t =>
      $(`<div class="all-trophies__item"/>`).append(
        makeTrophy(t),
        $(`<span class="all-trophies__name"/>`).text(t.title),
      ),
    ),
  );

  domDialog({
    class: 'all-trophies-dialog',
    cash: $grid,
    modal: true,
    show: true,
    onClose: () => {
      trophiesDialogOpen = false;
    },
  });
}
