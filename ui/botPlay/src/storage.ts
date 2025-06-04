import { storedJsonProp } from 'lib/storage';
import { Game } from './game';

const currentGameJson = storedJsonProp<any | null>('bot.current-game', () => null);

export const loadCurrentGame = (): Game | undefined => {
  const json = currentGameJson();
  return json ? fromJson(json) : undefined;
};

export const saveCurrentGame = (game: Game | null) => {
  currentGameJson(game ? toJson(game) : null);
};

const toJson = (game: Game) => ({
  id: game.id,
  botId: game.botId,
  pov: game.pov,
  clockConfig: game.clockConfig,
  initialFen: game.initialFen,
  moves: game.moves,
});

const fromJson = (o: any) => new Game(o.botId, o.pov, o.clockConfig, o.initialFen, o.moves, o.id);
