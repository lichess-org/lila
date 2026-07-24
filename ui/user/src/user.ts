import { myUserId } from 'lib';
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

  const seen = new Set<string>();
  const allCups: TrophyItem[] = [];
  items.forEach(t => {
    if (t.badge) return;
    if (seen.has(t.title)) return;
    seen.add(t.title);
    allCups.push(t);
  });
  const cups = allCups.filter(t => t.primary !== false);
  const badges = items.filter(t => t.badge);

  const createEl = (t: TrophyItem): HTMLElement => {
    const tag = t.href ? 'a' : 'span';
    const el = document.createElement(tag);
    el.className = t.cls + (t.stacked ? ' stacked' : '');
    el.setAttribute('aria-label', t.title);
    if (t.href) (el as HTMLAnchorElement).href = t.href;
    if (t.icon) el.textContent = t.icon;
    else {
      const img = document.createElement('img');
      img.src = t.imgSrc!;
      if (t.imgW) img.width = t.imgW;
      if (t.imgH) img.height = t.imgH;
      el.appendChild(img);
    }
    return el;
  };

  const contentFits = () => {
    const children = Array.from(el.children);
    if (children.length < 2) return true;
    const first = children[0].getBoundingClientRect();
    const last = children[children.length - 1].getBoundingClientRect();
    return last.right - first.left <= el.clientWidth + 1;
  };

  const openDialog = () => {
    if (trophiesDialogOpen) return;
    trophiesDialogOpen = true;
    const grid = document.createElement('div');
    grid.className = 'all-trophies';
    const title = document.createElement('h2');
    title.className = 'all-trophies__title';
    title.textContent = username ?? '';
    grid.appendChild(title);
    allCups.forEach(t => {
      const item = document.createElement('div');
      item.className = 'all-trophies__item';
      item.appendChild(createEl(t));
      const name = document.createElement('span');
      name.className = 'all-trophies__name';
      name.textContent = t.title;
      item.appendChild(name);
      grid.appendChild(item);
    });
    domDialog({
      class: 'all-trophies-dialog',
      cash: $(grid),
      modal: true,
      show: true,
      onClose: () => {
        trophiesDialogOpen = false;
      },
    });
  };

  const render = () => {
    el.innerHTML = '';
    badges.forEach(t => el.appendChild(createEl(t)));
    cups.forEach(t => el.insertBefore(createEl(t), el.firstChild));

    if (!site.blindMode && cups.length > 0) {
      let hidden = 0;
      let moreBtn: HTMLElement | undefined;

      while (hidden < cups.length && !contentFits()) {
        const cupIdx = moreBtn ? 1 : 0;
        const cup = el.children[cupIdx];
        if (!cup || cup === moreBtn) break;

        el.removeChild(cup);
        hidden++;

        if (!moreBtn) {
          moreBtn = document.createElement('button');
          (moreBtn as HTMLButtonElement).type = 'button';
          moreBtn.className = 'more-trophies';
          moreBtn.setAttribute('aria-label', i18n.site.more);
          moreBtn.textContent = `+${hidden}`;
          moreBtn.addEventListener('click', openDialog);
          el.insertBefore(moreBtn, el.firstChild);
        } else {
          moreBtn.textContent = `+${hidden}`;
        }
      }
    }
  };

  render();
  el.classList.add('trophies-ready');
  window.addEventListener('resize', render);
  new ResizeObserver(render).observe(el);
}
