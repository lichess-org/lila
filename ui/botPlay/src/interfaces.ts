import { BotInfo } from 'local';

export interface BotOpts {
  bots: BotInfo[];
}

export interface Game {
  opponent: BotInfo;
  moves: San[];
}
