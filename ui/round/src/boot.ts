import { RoundOpts, RoundData } from './interfaces';
import { RoundApi, RoundMain } from './main';
import { tourStandingCtrl, TourStandingData } from './tourStanding';

const li = window.lichess;

export default function(opts: RoundOpts, element: HTMLElement): void {
  const data = opts.data;
  li.openInMobileApp(data.game.id);
  let round: RoundApi, chat: any;
  if (data.tournament) $('body').data('tournament-id', data.tournament.id);
  li.socket = li.StrongSocket(
    data.url.socket,
    data.player.version, {
      options: { name: 'round' },
      params: { userTv: data.userTv && data.userTv.id },
      receive(t: string, d: any) { round.socketReceive(t, d); },
      events: {
        crowd(e: { watchers: number }) {
          $watchers.watchers("set", e.watchers);
        },
        tvSelect(o: any) {
          if (data.tv && data.tv.channel == o.channel) li.reload();
          else $('#tv_channels a.' + o.channel + ' span').html(
            o.player ? [
              o.player.title,
              o.player.name,
              '(' + o.player.rating + ')'
            ].filter(x => x).join('&nbsp') : 'Anonymous');
        },
        end() {
          $.ajax({
            url: '/' + (data.tv ? ['tv', data.tv.channel, data.game.id, data.player.color, 'sides'] : [data.game.id, data.player.color, 'sides', data.player.spectator ? 'watcher' : 'player']).join('/'),
            success: function(html) {
              const $html = $(html);
              $('#site_header div.side').replaceWith($html.find('.side'));
              $('#lichess div.crosstable').replaceWith($html.find('.crosstable'));
              li.pubsub.emit('content_loaded')();
              startTournamentClock();
            }
          });
        },
        tourStanding(data: TourStandingData) {
          console.log(data);
        }
      }
    });

  function startTournamentClock() {
    $("div.game_tournament div.clock").each(function(this: HTMLElement) {
      $(this).clock({
        time: parseFloat($(this).data("time"))
      });
    });
  };
  function getPresetGroup(d: RoundData) {
    if (d.player.spectator) return null;
    if (d.steps.length < 4) return 'start';
    else if (d.game.status.id >= 30) return 'end';
    return null;
  };
  opts.element = element.querySelector('.round') as HTMLElement;
  opts.socketSend = li.socket.send;
  if (!opts.tour) opts.onChange = (d: RoundData) => {
    if (chat) chat.preset.setGroup(getPresetGroup(d));
  };
  opts.crosstableEl = element.querySelector('.crosstable') as HTMLElement;

  let $watchers: JQuery;
  function letsGo() {
    round = (window['LichessRound'] as RoundMain).app(opts);
    if (opts.chat) {
      if (opts.tour) {
        opts.chat.plugin = tourStandingCtrl(opts.tour, opts.i18n.standing);
        console.log(opts);
        opts.chat.alwaysEnabled = true;
      } else {
        opts.chat.preset = getPresetGroup(opts.data);
        opts.chat.parseMoves = true;
      }
      li.makeChat('chat', opts.chat, function(c) {
        chat = c;
      });
    }
    $watchers = $('#site_header div.watchers').watchers();
    startTournamentClock();
    $('#now_playing').find('.move_on input').change(function() {
      round.moveOn.toggle();
    }).prop('checked', round.moveOn.get()).on('click', 'a', function() {
      li.hasToReload = true;
      return true;
    });
    $('#now_playing').find('.zen input').change(function() {
      li.loadCss('/assets/stylesheets/zen.css');
      $('body').toggleClass('zen');
    });
    if (location.pathname.lastIndexOf('/round-next/', 0) === 0)
      history.replaceState(null, '', '/' + data.game.id);
    if (!data.player.spectator && data.game.status.id < 25) li.topMenuIntent();
  };
  if (window.navigator.userAgent.indexOf('Trident/') > -1) setTimeout(letsGo, 150);
  else letsGo();
}
