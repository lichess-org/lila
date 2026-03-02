import type { RoundData, RoundOpts } from './interfaces';
import { attributesModule, classModule, init } from 'snabbdom';
import menuHover from 'lib/menuHover';
import RoundController from './ctrl';
import { main as view } from './view/main';
import { text as xhrText } from 'lib/xhr';
import type MoveOn from './moveOn';
import type { TourPlayer } from 'lib/game';
import { tourStandingCtrl, type TourStandingCtrl } from './tourStanding';
import { wsConnect, wsDestroy } from 'lib/socket';
import { storage } from 'lib/storage';
import { setClockWidget } from 'lib/game/clock/clockWidget';
import standaloneChat from 'lib/chat/standalone';
import { pubsub } from 'lib/pubsub';
import { myUserId } from 'lib';
import { alert } from 'lib/view';

const patch = init([classModule, attributesModule]);

export async function initModule(opts: RoundOpts): Promise<RoundController> {
  await site.asset.loadPieces;
  return opts.data.local ? app(opts) : boot(opts, app);
}

async function app(opts: RoundOpts): Promise<RoundController> {
  const ctrl = new RoundController(opts, redraw);
  const blueprint = view(ctrl);
  const el = (opts.element ?? document.querySelector('.round__app')) as HTMLElement;

  let vnode = patch(el, blueprint);

  window.addEventListener('resize', () => {
    redraw(); // col1 / col2+ transition
    ctrl.autoScroll();
  });

  if (ctrl.isPlaying()) menuHover();

  site.sound.preloadBoardSounds();

  return ctrl;

  function redraw() {
    vnode = patch(vnode, view(ctrl));
  }
}

async function boot(
  opts: RoundOpts,
  roundMain: (opts: RoundOpts) => Promise<RoundController>,
): Promise<RoundController> {
  const data = opts.data;
  if (data.tournament) document.body.dataset.tournamentId = data.tournament.id;
  const socketUrl = opts.data.player.spectator
    ? `/watch/${data.game.id}/${data.player.color}/v6`
    : `/play/${data.game.id}${data.player.id}/v6`;
  opts.socketSend = wsConnect(socketUrl, data.player.version, {
    options: { reloadOnResume: true },
    params: { userTv: data.userTv && data.userTv.id },
    receive(t: string, d: any) {
      round.socketReceive(t, d);
    },
    events: {
      tvSelect(o: {
        channel: string;
        gameId: string;
        color: Color;
        player?: { title?: string; name: string; rating?: number };
      }) {
        if (data.tv && data.tv.channel === o.channel) site.reload();
        else
          $('.tv-channels .' + o.channel + ' .champion').html(
            o.player
              ? [o.player.title, o.player.name, data.pref.ratings ? o.player.rating : '']
                  .filter(x => x)
                  .join('&nbsp')
              : 'Anonymous',
          );
      },
      endData() {
        xhrText(`${data.tv ? '/tv' : ''}/${data.game.id}/${data.player.color}/sides`).then(html => {
          const $html = $(html),
            $meta = $html.find('.game__meta');
          $meta.length && $('.game__meta').replaceWith($meta);
          $('.crosstable').replaceWith($html.find('.crosstable'));
          startTournamentClock();
          pubsub.emit('content-loaded');
        });
      },
      tourStanding(s: TourPlayer[]) {
        const chat = opts.chat?.plugin && opts.chat?.instance;
        if (chat) {
          (opts.chat!.plugin as TourStandingCtrl).set(s);
          chat.redraw();
        }
      },
    },
  }).send;

  const startTournamentClock = () => {
    if (data.tournament)
      $('.game__tournament .clock').each(function (this: HTMLElement) {
        setClockWidget(this, {
          time: parseFloat(this.dataset.time!),
        });
      });
  };
  const getPresetGroup = (d: RoundData) => {
    if (d.player.spectator) return;
    if (d.steps.length < 4) return 'start';
    else if (d.game.status.id >= 30) return 'end';
    return;
  };
  const ctrl = await roundMain(opts);
  const round: RoundApi = { socketReceive: ctrl.socket.receive, moveOn: ctrl.moveOn };
  const chatOpts = opts.chat;
  if (chatOpts) {
    if (data.tournament?.top) {
      chatOpts.plugin = tourStandingCtrl(data.tournament.top, data.tournament.team, i18n.site.standings);
    } else if (!data.simul && !data.swiss) {
      chatOpts.preset = getPresetGroup(data);
      chatOpts.enhance = { plies: true };
    }
    if (chatOpts.noteId && (chatOpts.noteAge || 0) < 10) chatOpts.noteText = '';
    chatOpts.instance = standaloneChat(chatOpts);
    if (!data.tournament && !data.simul && !data.swiss) {
      opts.onChange = (d: RoundData) => chatOpts.instance!.preset.setGroup(getPresetGroup(d));
      if (myUserId())
        chatOpts.instance.listenToIncoming(line => {
          if (
            line.u === 'lichess' &&
            (startsWithPrefix(line.t, 'warning') || startsWithPrefix(line.t, 'reminder'))
          )
            alert(line.t);
        });
    }
  }
  startTournamentClock();
  $('#round-toggle-autoswitch')
    .on('change', round.moveOn.toggle)
    .prop('checked', round.moveOn.get())
    .on('click', 'a', () => {
      site.unload.expected = true;
      return true;
    });
  if (location.pathname.lastIndexOf('/round-next/', 0) === 0)
    history.replaceState(null, '', '/' + data.game.id);
  $('#zentog').on('click', () => pubsub.emit('zen'));
  storage.make('reload-round-tabs').listen(site.reload);

  if (!data.player.spectator && location.hostname != (document as any)['Location'.toLowerCase()].hostname) {
    alert(`Games cannot be played through a web proxy. Please use ${location.hostname} instead.`);
    wsDestroy();
  }
  return ctrl;
}

const startsWithPrefix = (t: string, prefix: string) =>
  t.toLowerCase().startsWith(`${prefix}, ${myUserId()}`);

interface RoundApi {
  socketReceive(typ: string, data: any): boolean;
  moveOn: MoveOn;
}
