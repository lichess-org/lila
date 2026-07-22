import { frag, myUserId } from 'lib';
import { licon } from 'lib/licon';
import { pubsub } from 'lib/pubsub';
import { alert, domDialog, makeLinkPopups } from 'lib/view';
import * as xhr from 'lib/xhr';

const gamesAngle = document.querySelector<HTMLElement>('.games');
if (gamesAngle) gamesAngle.style.visibility = 'hidden';

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
          pubsub.emit('content-loaded', $content[0]);
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
    if (gamesAngle) gamesAngle.style.visibility = 'visible';
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

  const render = () => {
    el.innerHTML = '';
    badgeEls.forEach(e => el.appendChild(e));
    cupEls.forEach(e => el.insertBefore(e, el.firstChild));
    if (el.scrollWidth > el.clientWidth && !site.blindMode) {
      let hidden = 0;
      while (hidden < cupEls.length && el.scrollWidth > el.clientWidth) {
        el.removeChild(el.children[el.children.length - badgeEls.length - 1]);
        hidden++;
      }
      if (hidden) el.insertBefore(makeMoreBtn(hidden, allCups, username), el.firstChild);
    }
  };

  new ResizeObserver(render).observe(el);
  setTimeout(render, 2000);
}

function dedup(items: TrophyItem[]): TrophyItem[] {
  const seen = new Set<string>();
  return items.filter(t => {
    if (seen.has(t.title)) return false;
    seen.add(t.title);
    return true;
  });
}

function trophyHtml(t: TrophyItem): string {
  const tag = t.href ? 'a' : 'span';
  const inner = t.icon ?? `<img src="${t.imgSrc}"${t.imgW ? ` width="${t.imgW}"` : ''}${t.imgH ? ` height="${t.imgH}"` : ''}>`;
  return `<${tag} class="${t.cls}" title="${t.title}" aria-label="${t.title}"${t.href ? ` href="${t.href}"` : ''}>${inner}</${tag}>`;
}

const makeTrophy = (t: TrophyItem) => frag<HTMLElement>(trophyHtml(t));

function makeMoreBtn(hidden: number, allCups: TrophyItem[], username?: string): HTMLElement {
  const btn = frag<HTMLButtonElement>(`<button type="button" class="more-trophies" title="${i18n.site.more}" aria-label="${i18n.site.more}">+${hidden}</button>`);
  btn.addEventListener('click', () => openAllTrophies(allCups, username));
  return btn;
}

function openAllTrophies(cups: TrophyItem[], username?: string) {
  if (trophiesDialogOpen) return;
  trophiesDialogOpen = true;
  const grid = frag(`<div class="all-trophies"><h2 class="all-trophies__title">${username ?? ''}</h2>${cups.map(t => `<div class="all-trophies__item">${trophyHtml(t)}<span class="all-trophies__name">${t.title}</span></div>`).join('')}</div>`);
  domDialog({
    class: 'all-trophies-dialog',
    cash: $(grid),
    modal: true,
    show: true,
    onClose: () => { trophiesDialogOpen = false; },
  });
}
