import * as xhr from 'common/xhr';

interface ReplacementResponse {
  id: string;
  html: string;
}

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
    const url = new URL(`${window.location.pathname}/replacement`, window.location.origin);
    $('.mini-game').each((_i, el) => url.searchParams.append('exclude', el.dataset.live!));
    xhr
      .json(url.toString())
      .then((data: ReplacementResponse) => {
        $(`.mini-game[data-live="${oldId}"]`).parent().html(data.html);
        lichess.contentLoaded();
      })
      .then(done, done);
  });
}

function done() {
  isRequestPending = false;
  requestReplacementGame();
}

lichess.load.then(() =>
  lichess.pubsub.on('socket.in.finish', ({ id }) => {
    setTimeout(() => {
      finishedIdQueue.push(id);
      requestReplacementGame();
    }, 7000); // 7000 matches the rematch wait duration in /modules/tv/main/Tv.scala
  })
);
