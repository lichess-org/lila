import * as xhr from 'common/xhr';
import { RoundOpts, RoundData, NvuiPlugin } from './interfaces';
import MoveOn from './moveOn';
import { ChatCtrl } from 'chat';
import { TourPlayer } from 'game';
import { tourStandingCtrl, TourStandingCtrl } from './tourStanding';

interface RoundApi {
  socketReceive(typ: string, data: any): boolean;
  moveOn: MoveOn;
}

export default async function (opts: RoundOpts, roundMain: (opts: RoundOpts, nvui?: NvuiPlugin) => RoundApi) {
  const data = opts.data;
  if (data.tournament) $('body').data('tournament-id', data.tournament.id);
  lichess.socket = new lichess.StrongSocket(data.url.socket, data.player.version, {
    params: { userTv: data.userTv && data.userTv.id },
    receive(t: string, d: any) {
      round.socketReceive(t, d);
    },
    events: {
      tvSelect(o: any) {
        if (data.tv && data.tv.channel == o.channel) lichess.reload();
        else
          $('.tv-channels .' + o.channel + ' .champion').html(
            o.player
              ? [o.player.title, o.player.name, data.pref.ratings ? o.player.rating : '']
                  .filter(x => x)
                  .join('&nbsp')
              : 'Anonymous'
          );
      },
      endData() {
        xhr.text(`${data.tv ? '/tv' : ''}/${data.game.id}/${data.player.color}/sides`).then(html => {
          const $html = $(html),
            $meta = $html.find('.game__meta');
          $meta.length && $('.game__meta').replaceWith($meta);
          $('.crosstable').replaceWith($html.find('.crosstable'));
          startTournamentClock();
          lichess.contentLoaded();
        });
      },
      tourStanding(s: TourPlayer[]) {
        opts.chat?.plugin &&
          opts.chat?.instance?.then(chat => {
            (opts.chat!.plugin as TourStandingCtrl).set(s);
            chat.redraw();
          });
      },
    },
  });

  const startTournamentClock = () => {
    if (data.tournament)
      $('.game__tournament .clock').each(function (this: HTMLElement) {
        lichess.clockWidget(this, {
          time: parseFloat($(this).data('time')),
        });
      });
  };
  const getPresetGroup = (d: RoundData) => {
    if (d.player.spectator) return;
    if (d.steps.length < 4) return 'start';
    else if (d.game.status.id >= 30) return 'end';
    return;
  };
  opts.element = document.querySelector('.round__app') as HTMLElement;
  opts.socketSend = lichess.socket.send;

  const round: RoundApi = roundMain(
    opts,
    lichess.blindMode ? await lichess.loadEsm<NvuiPlugin>('round.nvui') : undefined
  );
  const chatOpts = opts.chat;
  if (chatOpts) {
    if (data.tournament?.top) {
      chatOpts.plugin = tourStandingCtrl(data.tournament.top, data.tournament.team, opts.i18n.standing);
      chatOpts.alwaysEnabled = true;
    } else if (!data.simul && !data.swiss) {
      chatOpts.preset = getPresetGroup(data);
      chatOpts.parseMoves = true;
    }
    if (chatOpts.noteId && (chatOpts.noteAge || 0) < 10) chatOpts.noteText = '';
    chatOpts.instance = lichess.makeChat(chatOpts) as Promise<ChatCtrl>;
    if (!data.tournament && !data.simul && !data.swiss)
      opts.onChange = (d: RoundData) =>
        chatOpts.instance!.then(chat => chat.preset.setGroup(getPresetGroup(d)));
  }
  startTournamentClock();
  $('.round__now-playing .move-on input')
    .on('change', round.moveOn.toggle)
    .prop('checked', round.moveOn.get())
    .on('click', 'a', () => {
      lichess.unload.expected = true;
      return true;
    });
  if (location.pathname.lastIndexOf('/round-next/', 0) === 0)
    history.replaceState(null, '', '/' + data.game.id);
  $('#zentog').on('click', () => lichess.pubsub.emit('zen'));
  lichess.storage.make('reload-round-tabs').listen(lichess.reload);

  if (!data.player.spectator && location.hostname != (document as any)['Location'.toLowerCase()].hostname) {
    alert(`Games cannot be played through a web proxy. Please use ${location.hostname} instead.`);
    lichess.socket.destroy();
  }
}
