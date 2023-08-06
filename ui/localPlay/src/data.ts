import { /*RoundOpts,*/ RoundData } from 'round';
//import { Player, GameData } from 'game';

/*interface RoundApi {
  socketReceive(typ: string, data: any): boolean;
  moveOn: MoveOn;
}*/

const data: RoundData = {
  game: {
    id: 'x7hgwoir',
    variant: { key: 'standard', name: 'Standard', short: 'Std' },
    speed: 'correspondence',
    perf: 'correspondence',
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
      username: 'Anonymous',
      online: true,
      perfs: {},
    },
    id: '',
    isGone: false,
    name: 'Anonymous',
    onGame: true,
    rating: 1500,
    version: 0,
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
    keyboardMove: true,
    voiceMove: true,
    ratings: true,
    submitMove: false,
  },
  steps: [{ ply: 0, san: '', uci: '', fen: 'rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1' }],
  correspondence: {
    daysPerTurn: 2,
    increment: 0,
    white: 0,
    black: 0,
    showBar: true,
  },
  takebackable: true,
  moretimeable: true,
};
gah();
function gah() {
  const socketUrl = /*opts.data.player.spectator
    ? `/watch/${data.game.id}/${data.player.color}/v6`
    :*/ `/play/${data.game.id}${data.player.id}/v6`;
  lichess.socket = new lichess.StrongSocket(socketUrl, data.player.version, {
    params: { userTv: false },
    receive(t: string, d: any) {
      t, d;
      //round.socketReceive(t, d);
    },
    events: {},
  });

  if (location.pathname.lastIndexOf('/round-next/', 0) === 0) history.replaceState(null, '', '/' + data.game.id);
  $('#zentog').on('click', () => lichess.pubsub.emit('zen'));
  lichess.storage.make('reload-round-tabs').listen(lichess.reload);

  if (!data.player.spectator && location.hostname != (document as any)['Location'.toLowerCase()].hostname) {
    alert(`Games cannot be played through a web proxy. Please use ${location.hostname} instead.`);
    lichess.socket.destroy();
  }
}

export function makeRounds() {
  const opts /*: RoundOpts*/ = {
    element: document.querySelector('.round__app') as HTMLElement,
    data,
    socketSend: lichess.socket.send,
  };
  lichess.loadEsm('round', { init: opts });
}
