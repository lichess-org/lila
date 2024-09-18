import { RoundData, RoundOpts, NvuiPlugin } from './interfaces';
import { attributesModule, classModule, init } from 'snabbdom';
import menuHover from 'common/menuHover';
import RoundController from './ctrl';
import { main as view } from './view/main';
import * as xhr from 'common/xhr';
import MoveOn from './moveOn';
import { TourPlayer } from 'game';
import { tourStandingCtrl, TourStandingCtrl } from './tourStanding';
import StrongSocket from 'common/socket';
import { storage } from 'common/storage';
import { setClockWidget } from 'common/clock';
import { makeChat } from 'chat';
import { pubsub } from 'common/pubsub';

const patch = init([classModule, attributesModule]);

export async function initModule(opts: RoundOpts): Promise<RoundController> {
  return opts.local ? app(opts) : boot(opts, app);
}

async function app(opts: RoundOpts): Promise<RoundController> {
  const nvui = site.blindMode ? await site.asset.loadEsm<NvuiPlugin>('round.nvui') : undefined;
  const ctrl = new RoundController(opts, redraw, nvui);

  const blueprint = view(ctrl);
  const el = (opts.element ?? document.querySelector('.round__app')) as HTMLElement;

  let vnode = patch(el, blueprint);

  window.addEventListener('resize', redraw); // col1 / col2+ transition

  if (ctrl.isPlaying()) menuHover();

  site.sound.preloadBoardSounds();

  return ctrl;

  function redraw() {
    vnode = patch(vnode, view(ctrl));
  }
}

async function boot(
  opts: RoundOpts,
  roundMain: (opts: RoundOpts) => Promise<RoundController>): Promise<RoundController> {

  const data = opts.data;
  if (data.tournament) document.body.dataset.tournamentId = data.tournament.id;
  const socketUrl = opts.data.player.spectator
    ? `/watch/${data.game.id}/${data.player.color}/v6`
    : `/play/${data.game.id}${data.player.id}/v6`;
  site.socket = new StrongSocket(socketUrl, data.player.version, {
    params: { userTv: data.userTv && data.userTv.id },
    receive(t: string, d: any) {
      round.socketReceive(t, d);
    },
    events: {
      tvSelect(o: any) {
        if (data.tv && data.tv.channel == o.channel) site.reload();
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
        xhr.text(`${data.tv ? '/tv' : ''}/${data.game.id}/${data.player.color}/sides`).then(html => {
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
  });
  opts.socketSend = site.socket.send;

  const startTournamentClock = () => {
    if (data.tournament)
      $('.game__tournament .clock').each(function(this: HTMLElement) {
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
      chatOpts.plugin = tourStandingCtrl(data.tournament.top, data.tournament.team, opts.i18n.standing);
      chatOpts.alwaysEnabled = true;
    } else if (!data.simul && !data.swiss) {
      chatOpts.preset = getPresetGroup(data);
      chatOpts.enhance = { plies: true };
    }
    if (chatOpts.noteId && (chatOpts.noteAge || 0) < 10) chatOpts.noteText = '';
    chatOpts.instance = makeChat(chatOpts);
    if (!data.tournament && !data.simul && !data.swiss)
      opts.onChange = (d: RoundData) =>
        chatOpts.instance!.preset.setGroup(getPresetGroup(d));
  }
  startTournamentClock();
  $('.round__now-playing .move-on input')
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
    site.socket.destroy();
  }
  return ctrl;
}

interface RoundApi {
  socketReceive(typ: string, data: any): boolean;
  moveOn: MoveOn;
}
