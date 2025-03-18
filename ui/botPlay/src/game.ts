import { randomId } from 'common/algo';
import { BotId } from 'local';

export interface Game {
  id: string;
  botId: BotId;
  sans: San[];
  pov: Color;
}

export const makeGame = (botId: BotId, pov: Color): Game => ({
  id: randomId(),
  botId,
  sans: [],
  pov,
});
