import { ChatCtrl } from 'chat';
import { TourPlayer } from 'game';
import { RoundData, RoundOpts } from './interfaces';
import { RoundApi, RoundMain } from './main';
import { TourStandingCtrl, tourStandingCtrl } from './tourStanding';

export default function (opts: RoundOpts): void {
  const li = window.lishogi;
  const element = document.querySelector('.round__app') as HTMLElement,
    data: RoundData = opts.data;
  let round: RoundApi, chat: ChatCtrl | undefined;
  if (data.tournament) $('body').data('tournament-id', data.tournament.id);
  li.socket = li.StrongSocket(data.url.socket, data.player.version, {
    options: { name: 'round' },
    params: { userTv: data.userTv && data.userTv.id },
    receive(t: string, d: any) {
      round.socketReceive(t, d);
    },
    events: {
      tvSelect(o: any) {
        if (data.tv && data.tv.channel == o.channel) li.reload();
        else
          $('.tv-channels .' + o.channel + ' .champion').html(
            o.player ? [o.player.title, o.player.name, o.player.rating].filter(x => x).join('&nbsp') : 'Anonymous'
          );
      },
      end() {
        $.ajax({
          url: [data.tv ? '/tv' : '', data.game.id, data.player.color, 'sides'].join('/'),
          success: function (html) {
            const $html = $(html),
              $meta = $html.find('.game__meta');
            $meta.length && $('.game__meta').replaceWith($meta);
            $('.crosstable').replaceWith($html.find('.crosstable'));
            startTournamentClock();
            li.pubsub.emit('content_loaded');
          },
        });
      },
      tourStanding(s: TourPlayer[]) {
        if (opts.chat && opts.chat.plugin && chat) {
          (opts.chat.plugin as TourStandingCtrl).set(s);
          chat.redraw();
        }
      },
    },
  });

  function startTournamentClock() {
    if (data.tournament)
      $('.game__tournament .clock').each(function (this: HTMLElement) {
        $(this).clock({
          time: parseFloat($(this).data('time')),
        });
      });
  }
  function getPresetGroup(d: RoundData) {
    if (d.player.spectator) return;
    if (d.steps.length < 4) return 'start';
    else if (d.game.status.id >= 30) return 'end';
    return;
  }
  opts.element = element;
  opts.klasses = Array.from(element.classList);
  opts.socketSend = li.socket.send;
  if (!data.tournament && !data.simul)
    opts.onChange = (d: RoundData) => {
      if (chat) chat.preset.setGroup(getPresetGroup(d));
    };

  round = (window['LishogiRound'] as RoundMain).app(opts);
  const chatOpts = opts.chat;
  if (chatOpts) {
    if (data.tournament?.top) {
      chatOpts.plugin = tourStandingCtrl(data.tournament.top, data.tournament.team, opts.i18n.standing);
      chatOpts.alwaysEnabled = true;
    } else if (!data.simul) {
      chatOpts.preset = getPresetGroup(data);
      chatOpts.parseMoves = true;
    }
    if (chatOpts.noteId && (chatOpts.noteAge || 0) < 10) chatOpts.noteText = '';
    li.makeChat(chatOpts, c => {
      chat = c;
    });
  }
  startTournamentClock();
  $('.round__now-playing .move-on input')
    .change(round.moveOn.toggle)
    .prop('checked', round.moveOn.get())
    .on('click', 'a', function () {
      li.hasToReload = true;
      return true;
    });
  if (location.pathname.lastIndexOf('/round-next/', 0) === 0) history.replaceState(null, '', '/' + data.game.id);
  if (location.pathname.length === 9 && data.player.id)
    history.replaceState(null, '', '/' + data.game.id + data.player.id);
  $('#zentog').click(() => li.pubsub.emit('zen'));
  li.storage.make('reload-round-tabs').listen(li.reload);
}
