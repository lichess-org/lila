import type { LobbyData, NowPlayingUpdate } from './interfaces';

type NowPlayingState = Pick<LobbyData, 'nowPlaying' | 'nbNowPlaying' | 'nbMyTurn'>;

export function updateNowPlayingData(data: NowPlayingState, update: NowPlayingUpdate): void {
  data.nowPlaying = update.nowPlaying;
  data.nbNowPlaying = update.nbNowPlaying;
  data.nbMyTurn = update.nbMyTurn;
}
