import { RoundOpts, RoundData } from './interfaces';
import { RoundApi, RoundMain } from './main';
import { ChatCtrl } from 'chat';
import { tourStandingCtrl, TourStandingCtrl, TourPlayer } from './tourStanding';

export default function(opts: RoundOpts): void {
  const li = window.lichess;
  const element = document.querySelector('.round__app') as HTMLElement,
  data: RoundData = opts.data;
  let round: RoundApi, chat: ChatCtrl | undefined;
  if (data.tournament) $('body').data('tournament-id', data.tournament.id);
  li.socket = li.StrongSocket(
    data.url.socket,
    data.player.version, {
      options: { name: 'round' },
      params: { userTv: data.userTv && data.userTv.id },
      receive(t: string, d: any) { round.socketReceive(t, d); },
      events: {
        tvSelect(o: any) {
          if (data.tv && data.tv.channel == o.channel) li.reload();
          else $('.tv-channels .' + o.channel + ' .champion').html(
            o.player ? [
              o.player.title,
              o.player.name,
              '(' + o.player.rating + ')'
            ].filter(x => x).join('&nbsp') : 'Anonymous');
        },
        end() {
          $.ajax({
            url: [(data.tv ? '/tv' : ''), data.game.id, data.player.color, 'sides'].join('/'),
            success: function(html) {
              const $html = $(html), $meta = $html.find('.game__meta');
              $meta.length && $('.game__meta').replaceWith($meta);
              $('.crosstable').replaceWith($html.find('.crosstable'));
              li.pubsub.emit('content_loaded');
            }
          });
        },
        tourStanding(s: TourPlayer[]) {
          if (opts.chat && opts.chat.plugin && chat) {
            (opts.chat.plugin as TourStandingCtrl).set(s);
            chat.redraw();
          }
        }
      }
    });

  function startTournamentClock() {
    $('.game__tournament .clock').each(function(this: HTMLElement) {
      $(this).clock({
        time: parseFloat($(this).data('time'))
      });
    });
  };
  function getPresetGroup(d: RoundData) {
    if (d.player.spectator) return;
    if (d.steps.length < 4) return 'start';
    else if (d.game.status.id >= 30) return 'end';
    return;
  };
  opts.element = element;
  opts.socketSend = li.socket.send;
  if (!opts.tour && !data.simul) opts.onChange = (d: RoundData) => {
    if (chat) chat.preset.setGroup(getPresetGroup(d));
  };

  round = (window['LichessRound'] as RoundMain).app(opts);
  if (opts.chat) {
    if (opts.tour) {
      opts.chat.plugin = tourStandingCtrl(opts.tour, opts.i18n.standing);
      opts.chat.alwaysEnabled = true;
    } else if (!data.simul) {
      opts.chat.preset = getPresetGroup(opts.data);
      opts.chat.parseMoves = true;
    }
    li.makeChat(opts.chat, function(c) {
      chat = c;
    });
  }
  startTournamentClock();
  $('.round__now-playing .move-on input')
  .change(round.moveOn.toggle)
  .prop('checked', round.moveOn.get())
  .on('click', 'a', function() {
    li.hasToReload = true;
    return true;
  });
  if (location.pathname.lastIndexOf('/round-next/', 0) === 0)
  history.replaceState(null, '', '/' + data.game.id);
  $('#zentog').click(() => li.pubsub.emit('zen'));
  li.storage.make('reload-round-tabs').listen(li.reload);
}
