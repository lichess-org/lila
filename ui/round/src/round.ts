import { attributesModule, classModule, init } from 'snabbdom';

import { myUserId } from 'lib';
import standaloneChat from 'lib/chat/standalone';
import { finished, type TourPlayer } from 'lib/game';
import { setClockWidget } from 'lib/game/clock/clockWidget';
import menuHover from 'lib/menuHover';
import { pubsub } from 'lib/pubsub';
import { wsConnect, wsDestroy } from 'lib/socket';
import { storage } from 'lib/storage';
import { alert } from 'lib/view';
import { text as xhrText } from 'lib/xhr';

import RoundController from './ctrl';
import type { RoundData, RoundOpts } from './interfaces';
import type MoveOn from './moveOn';
import { tourStandingCtrl, type TourStandingCtrl } from './tourStanding';
import { main as view } from './view/main';

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

  function redraw() {
    vnode = patch(vnode, view(ctrl));
  }

  window.addEventListener('resize', () => {
    redraw(); // col1 / col2+ transition
    ctrl.autoScroll();
  });

  if (ctrl.isPlaying()) menuHover();

  site.sound.preloadBoardSounds();

  return ctrl;
}

async function boot(
  opts: RoundOpts,
  roundMain: (opts: RoundOpts) => Promise<RoundController>,
): Promise<RoundController> {
  const { data, chat } = opts;

  if (data.tournament) document.body.dataset.tournamentId = data.tournament.id;
  const socketUrl = data.player.spectator
    ? `/watch/${data.game.id}/${data.player.color}/v6`
    : `/play/${data.game.id}${data.player.id}/v6`;

  opts.socketSend = wsConnect(socketUrl, data.player.version, {
    options: { reloadOnResume: true },
    params: { userTv: data.userTv && data.userTv.id },
    receive(t: string, d: any) {
      round.socketReceive(t, d);
    },
    events: {
      tvSelect({ channel, player }: TVOptions) {
        if (data.tv && data.tv.channel === channel) site.reload();
        else
          $(`.tv-channels .${channel} .champion`).html(
            player
              ? [player.title, player.name, data.pref.ratings ? player.rating : '']
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
        const chatInstance = chat?.plugin && chat?.instance;
        if (chatInstance) {
          (chat.plugin as TourStandingCtrl).set(s);
          chatInstance.redraw();
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
    if (d.player.spectator) return undefined;
    if (finished(d)) return 'end';
    if (d.steps.length < 6) return 'start';
    return undefined;
  };

  const ctrl = await roundMain(opts);
  const round: RoundApi = { socketReceive: ctrl.socket.receive, moveOn: ctrl.moveOn };

  if (chat) {
    if (data.tournament?.top) {
      chat.plugin = tourStandingCtrl(data.tournament.top, data.tournament.team, i18n.site.standings);
    } else if (!data.simul && !data.swiss) {
      chat.preset = getPresetGroup(data);
      chat.enhance = { plies: true };
    }
    if (chat.noteId && (chat.noteAge || 0) < 10) chat.noteText = '';
    chat.instance = standaloneChat(chat);
    if (!data.tournament && !data.simul && !data.swiss) {
      opts.onChange = (d: RoundData) => chat.instance!.preset.setGroup(getPresetGroup(d));
      if (myUserId())
        chat.instance.listenToIncoming(line => {
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

  if (location.pathname.lastIndexOf('/round-next/', 0) === 0) {
    history.replaceState(null, '', '/' + data.game.id);
  }

  $('#zentog').on('click', () => pubsub.emit('zen'));
  storage.make('reload-round-tabs').listen(site.reload);

  if (!data.player.spectator && location.hostname !== (document as any)['Location'.toLowerCase()].hostname) {
    alert(`Games cannot be played through a web proxy. Please use ${location.hostname} instead.`);
    wsDestroy();
  }
  return ctrl;
}

const startsWithPrefix = (t: string, prefix: string) =>
  t.toLowerCase().startsWith(`${prefix}, ${myUserId()}`);

type RoundApi = {
  socketReceive: (typ: string, data: any) => boolean;
  moveOn: MoveOn;
};

type TVOptions = {
  channel: string;
  gameId: string;
  color: Color;
  player?: { title?: string; name: string; rating?: number };
};
