import * as xhr from 'common/xhr';

interface ReplacementResponse {
  id: string;
  html: string;
}

const finishedIdsQueue: string[] = [];

function requestReplacementGame() {
  // Make sure to only make one request at a time.
  // This avoids getting copies of the same game to replace two different finished games.
  if (finishedIdsQueue.length !== 1) return;

  // Use requestAnimationFrame to avoid requesting games in background tabs
  requestAnimationFrame(() => {
    const url = new URL(`${window.location.pathname}/replacement`, window.location.origin);
    $('.mini-game').each((_i, el) => url.searchParams.append('exclude', el.dataset.live!));

    xhr
      .json(url.toString())
      .then((data: ReplacementResponse) => {
        const oldId = finishedIdsQueue.shift()!;
        $(`.mini-game[data-live="${oldId}"]`).parent().html(data.html);
        lichess.contentLoaded();
        requestReplacementGame();
      })
      .catch(() => {
        finishedIdsQueue.shift();
        requestReplacementGame();
      });
  });
}

lichess.load.then(() =>
  lichess.pubsub.on('socket.in.finish', ({ id }) => {
    setTimeout(() => {
      finishedIdsQueue.push(id);
      requestReplacementGame();
    }, 7000); // 7000 matches the rematch wait duration in /modules/tv/main/Tv.scala
  })
);
