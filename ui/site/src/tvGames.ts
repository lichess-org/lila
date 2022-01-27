import * as xhr from 'common/xhr';

interface ReplacementResponse {
  id: string;
  html: string;
}

const getId = (el: EleLoose) => el.getAttribute('href')?.substring(1, 9);

let isRequestPending = false;
const finishedIdQueue: string[] = [];

function requestReplacementGame() {
  // Make sure to only make one request at a time.
  // This avoids getting copies of the same game to replace two different finished games.
  if (isRequestPending) return;
  const oldId = finishedIdQueue.shift();
  if (!oldId) return;
  isRequestPending = true;

  // Use requestAnimationFrame to avoid requesting games in background tabs
  requestAnimationFrame(() => {
    const main = $('main.tv-games');
    const url = new URL(main.data('rel').replace('gameId', oldId));
    main.find('.mini-game').each((_i, el) => url.searchParams.append('exclude', getId(el)!));
    xhr
      .json(url.toString())
      .then((data: ReplacementResponse) => {
        main.find(`.mini-game[href^="/${oldId}"]`).replaceWith(data.html);
        if (data.html.includes('mini-game__result')) onFinish(data.id);
        lichess.contentLoaded();
      })
      .then(done, done);
  });
}

function done() {
  isRequestPending = false;
  requestReplacementGame();
}

function onFinish(id: string) {
  setTimeout(() => {
    finishedIdQueue.push(id);
    requestReplacementGame();
  }, 7000); // 7000 matches the rematch wait duration in /modules/tv/main/Tv.scala
}

lichess.load.then(() => {
  lichess.pubsub.on('socket.in.finish', ({ id }) => onFinish(id));
  $('main.tv-games')
    .find('.mini-game')
    .each((_i, el) => {
      if ($(el).find('.mini-game__result').length > 0) onFinish(getId(el)!);
    });
});
