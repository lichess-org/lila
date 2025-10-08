import { storedJsonProp } from 'lib/storage';
import { Game } from './game';
import type { GameData } from './interfaces';

const currentGameJson = storedJsonProp<GameData | null>('bot.current-game', () => null);

export const loadCurrentGame = (): Game | undefined => {
  const data = currentGameJson();
  return data ? new Game(data) : undefined;
};

export const saveCurrentGame = (game: Game | null) => {
  currentGameJson(game ? game.data : null);
};
