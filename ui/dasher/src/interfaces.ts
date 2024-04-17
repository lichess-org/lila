import { LangsData } from './langs';
import { BackgroundData } from './background';
import { BoardData } from './board';
import { ThemeData } from './theme';
import { PieceData } from './piece';

export interface DasherData {
  user?: LightUser;
  lang: LangsData;
  sound: {
    list: string[];
  };
  background: BackgroundData;
  board: BoardData;
  theme: ThemeData;
  piece: PieceData;
  coach: boolean;
  streamer: boolean;
  i18n: I18nDict;
}

export type Mode = 'links' | 'langs' | 'sound' | 'background' | 'board' | 'theme' | 'piece';

export interface DasherOpts {
  playing: boolean;
  zenable: boolean;
}
