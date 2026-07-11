import { myUserId } from 'lib';
import { licon } from 'lib/licon';
import { pubsub } from 'lib/pubsub';
import { alert, domDialog, makeLinkPopups } from 'lib/view';
import * as xhr from 'lib/xhr';

const gamesAngle = document.querySelector<HTMLElement>('.games');
if (gamesAngle) gamesAngle.style.visibility = 'hidden'; // FOUC

export async function initModule(): Promise<void> {
  makeLinkPopups($('.social_links'));
  makeLinkPopups($('.user-infos .bio'));

  tmpRandomTutorLink();
  fitTrophies();

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

const trophySelector = '.trophy, .shield-trophy, .revol_trophy';
const badgeSelector = '.icon3d';

const trophyName = (el: HTMLElement): string => el.getAttribute('aria-label') || el.title || el.className;
const isCup = (el: HTMLElement): boolean => !el.matches(badgeSelector);

function fitTrophies() {
  const box = document.querySelector<HTMLElement>('.user-show');
  if (box) new ResizeObserver(layoutTrophies).observe(box);
}

function layoutTrophies() {
  const header = document.querySelector<HTMLElement>('.user-show__header');
  const trophies = header?.querySelector<HTMLElement>('.trophies');
  const title = header?.querySelector<HTMLElement>('h1');
  if (!trophies || !title) return;

  trophies.querySelector('.more-trophies')?.remove();
  const items = [...trophies.querySelectorAll<HTMLElement>(trophySelector)];
  for (const el of items) el.style.removeProperty('display');

  const hide = (el: HTMLElement) => (el.style.display = 'none');
  const isVisible = (el: HTMLElement) => el.style.display !== 'none';
  const nameRight = () => title.getBoundingClientRect().right + 48;

  const seen = new Set<string>();
  for (const el of items) {
    if (seen.has(trophyName(el))) hide(el);
    else seen.add(trophyName(el));
  }

  for (const el of items.filter(isVisible)) {
    if (el.getBoundingClientRect().left >= nameRight()) break;
    hide(el);
  }

  const username = title.querySelector<HTMLElement>('.user-link[data-href]')?.textContent?.trim();
  const cupCount = items.filter(isCup).length;
  trophies.prepend(moreButton(cupCount, () => showAllTrophies(trophies, username)));
}

function moreButton(count: number, onClick: () => void): HTMLButtonElement {
  const button = document.createElement('button');
  button.type = 'button';
  button.className = 'more-trophies';
  button.textContent = `+${count}`;
  button.title = i18n.site.more;
  button.setAttribute('aria-label', i18n.site.more);
  button.addEventListener('click', onClick);
  return button;
}

let trophiesDialogOpen = false;

function showAllTrophies(trophies: HTMLElement, username?: string) {
  if (trophiesDialogOpen) return;
  trophiesDialogOpen = true;

  const heading = document.createElement('h2');
  heading.className = 'all-trophies__title';
  heading.textContent = username ?? i18n.site.more;

  const grid = document.createElement('div');
  grid.className = 'all-trophies';
  grid.append(heading);
  for (const el of trophies.querySelectorAll<HTMLElement>(trophySelector)) {
    if (isCup(el)) grid.append(trophyCard(el));
  }

  domDialog({
    class: 'all-trophies-dialog',
    append: [{ node: grid }],
    modal: true,
    show: true,
    onClose: () => (trophiesDialogOpen = false),
  });
}

function trophyCard(cup: HTMLElement): HTMLElement {
  const icon = cup.cloneNode(true) as HTMLElement;
  icon.removeAttribute('style');
  icon.querySelectorAll('img').forEach(img => img.removeAttribute('style'));

  const cell = document.createElement('div');
  cell.className = 'all-trophies__item';
  cell.append(icon);

  const label = cup.getAttribute('aria-label') || cup.title;
  if (label) {
    const name = document.createElement('span');
    name.className = 'all-trophies__name';
    name.textContent = label;
    cell.append(name);
  }
  return cell;
}
