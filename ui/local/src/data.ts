import { RoundData } from 'round';
//import { Player, GameData } from 'game';

/*interface RoundApi {
  socketReceive(typ: string, data: any): boolean;
  moveOn: MoveOn;
}*/

export const fakeData: RoundData = {
  game: {
    id: 'synthetic',
    variant: { key: 'standard', name: 'Standard', short: 'Std' },
    speed: 'classical',
    perf: 'classical',
    rated: false,
    fen: 'rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1',
    turns: 0,
    source: 'friend',
    status: { id: 20, name: 'started' },
    player: 'white',
  },
  player: {
    color: 'white',
    user: {
      id: 'anonymous',
      username: 'Anonymous',
      online: true,
      perfs: {},
    },
    rating: 1628,
    id: '7J3E',
    isGone: false,
    name: 'Anonymous',
    onGame: true,
    version: 0,
  },
  opponent: {
    color: 'black',
    user: {
      id: 'anonymous',
      username: 'Baby Howard',
      online: true,
      perfs: {},
    },
    id: '',
    isGone: false,
    name: 'Baby Howard',
    onGame: true,
    rating: 800,
    version: 0,
    image: '/assets/lifat/bots/images/coral.webp',
  },
  pref: {
    animationDuration: 300,
    coords: 1,
    resizeHandle: 1,
    replay: 2,
    autoQueen: 2,
    clockTenths: 1,
    moveEvent: 2,
    clockBar: true,
    clockSound: true,
    confirmResign: true,
    rookCastle: true,
    highlight: true,
    destination: true,
    enablePremove: true,
    showCaptured: true,
    blindfold: false,
    is3d: false,
    keyboardMove: false,
    voiceMove: false,
    ratings: true,
    submitMove: false,
  },
  steps: [{ ply: 0, san: '', uci: '', fen: 'rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1' }],
  /*correspondence: {
    daysPerTurn: 2,
    increment: 0,
    white: 0,
    black: 0,
    showBar: true,
  },*/
  takebackable: true,
  moretimeable: true,
};
