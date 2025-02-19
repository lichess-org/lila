import * as xhr from 'common/xhr';
import { makeLinkPopups } from 'common/linkPopup';
import { alert } from 'common/dialog';
import { pubsub } from 'common/pubsub';
import * as licon from 'common/licon';
import { frag } from 'common';
import type { LocalEnv, LiteGame } from 'local';
import { status } from 'game';

let local: LocalEnv;
site.asset.loadEsm<LocalEnv>('local.db').then(l => {
  local = l;
  renderLocalGames();
});

const gamesAngle = document.querySelector<HTMLElement>('.games');
if (gamesAngle) gamesAngle.style.visibility = 'hidden'; // FOUC

export async function initModule(): Promise<void> {
  makeLinkPopups($('.social_links'));
  makeLinkPopups($('.user-infos .bio'));

  const loadNoteZone = () => {
    const $zone = $('.user-show .note-zone');
    $zone.find('textarea')[0]?.focus();
    if ($zone.hasClass('loaded')) return;
    $zone.addClass('loaded');
    $noteToggle.find('strong').text('' + $zone.find('.note').length);
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
          renderLocalGames();

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

async function renderLocalGames(page?: 'first' | 'last') {
  page;
  const games = gamesAngle?.querySelectorAll<HTMLElement>('article:not(.local)');
  if (!(gamesAngle && games?.length && local)) return;

  const dates = [...games].map(game => game.querySelector('time[datetime]')?.getAttribute('datetime'));
  let newContent = false;
  // if (page === 'first' && dates.length) {
  //   const begin = new Date(dates[0]!).getTime();
  //   const localGames = await local.db.byDate(undefined, begin);
  //   const head = games[0];
  //   for (const localGame of localGames) {
  //     if (document.getElementById(localGame.id)) continue;
  //     const gameEl = renderGame(localGame);
  //     newContent = true;
  //     gamesAngle.insertBefore(gameEl, head);
  //   }

  // }
  for (let i = 0; i < dates.length; i++) {
    if (!dates[i]) continue;
    const end = new Date(dates[i]!).getTime();
    const begin = dates.find((d, j) => d && j > i);
    const endDate = end /*&& page !== 'first'*/ ? new Date(end).getTime() : undefined;
    const beginDate = begin ? new Date(begin).getTime() : page === 'last' ? undefined : endDate;
    const localGames = await local.db.byDate(beginDate, endDate);
    console.log(localGames);

    for (const localGame of localGames) {
      if (document.getElementById(localGame.id)) continue;
      const gameEl = renderGame(localGame);
      newContent = true;
      gamesAngle.insertBefore(gameEl, games[i + 1] ?? null);
    }
  }
  if (newContent) pubsub.emit('content-loaded', gamesAngle);
}

// mirrored html generation in file://../../../app/views/game/widgets.scala
function renderGame(game: LiteGame) {
  return frag<HTMLElement>($html`
    <article id="${game.id}" class="game-row local">
      <a class="game-row__overlay" href="/local#id=${game.id}"></a>
      <div class="game-row__board">
        <span class="mini-board mini-board--init cg-wrap is2d"
          data-state="${game.fen},${game.turn},${game.lastMove}">
        </span>
      </div>
      <div class="game-row__infos">
        <div class="header" data-icon="${gameIcon(game.speed)}">
          <div class="header__text">
            <strong>${clockString(game)} • ${game.speed} • CASUAL</strong>
            <time class="timeago once" datetime="${new Date(game.createdAt)}"></time>
          </div>
        </div>
      <div class="versus">
        ${playerHtml(game, 'white')}
        <div class="swords" data-icon="${licon.Swords}"></div>
        ${playerHtml(game, 'black')}
      </div>
      <div class="result">
        ${resultHtml(game)}
      </div>
    </article>    
  `);
}

function gameIcon(speed: Speed) {
  switch (speed) {
    case 'ultraBullet':
      return licon.UltraBullet;
    case 'bullet':
      return licon.Bullet;
    case 'blitz':
      return licon.Fire;
    case 'rapid':
      return licon.Rabbit;
    case 'classical':
      return licon.Turtle;
    case 'correspondence':
      return licon.PaperAirplane;
  }
}

function clockString({ initial, increment }: LiteGame) {
  return initial === Infinity
    ? '∞'
    : `${initial === 15 ? '¼' : initial === 30 ? '½' : initial === 45 ? '¾' : initial}+${increment}`;
}

function playerHtml(game: LiteGame, color: Color) {
  // TODO fancify
  return $html`
    <div class="player ${color}">
      <span>${local.nameOf(game[color])}</span>
    </div>
  `;
}

function resultHtml(game: LiteGame) {
  if (game.status === status.started) return i18n.site.playingRightNow;
  return $html`
    ${game.winner ?? i18n.site.draw}
  `;
}
