import { makeChat } from 'chat';
import type { ChatCtrl, ChatOpts } from 'chat/interfaces';
import type { TourPlayer } from 'game';
import type RoundController from './ctrl';
import type { RoundData, RoundOpts } from './interfaces';
import { type TourStandingCtrl, tourStandingCtrl } from './tour-standing';

export function boot(
  opts: RoundOpts,
  start: (element: HTMLElement, opts: RoundOpts) => RoundController,
): RoundController {
  const li = window.lishogi,
    element = document.querySelector('.round__app') as HTMLElement,
    data: RoundData = opts.data;

  let ctrl: RoundController, chat: ChatCtrl | undefined;
  if (data.tournament) $('body').data('tournament-id', data.tournament.id);
  const socketUrl = opts.data.player.spectator
    ? `/watch/${data.game.id}/${data.player.color}/v6`
    : `/play/${data.game.id}${data.player.id}/v6`;
  li.socket = new li.StrongSocket(socketUrl, data.player.version, {
    options: { name: 'round' },
    params: { userTv: data.userTv && data.userTv.id },
    receive(t: string, d: any) {
      ctrl.socket.receive(t, d);
    },
    events: {
      tvSelect(o: any) {
        if (data.tv && data.tv.channel == o.channel) li.reload();
        else
          $('.tv-channels .' + o.channel + ' .champion').html(
            o.player
              ? [o.player.title, o.player.name, o.player.rating].filter(x => x).join('&nbsp')
              : 'Anonymous',
          );
      },
      endData() {
        console.log('enddata boot'); // todo
        window.lishogi.xhr
          .text('GET', `${data.tv ? '/tv' : ''}/${data.game.id}/${data.player.color}/sides`)
          .then(html => {
            const $html = $(html),
              $meta = $html.find('.game__meta');
            $meta.length && $('.game__meta').replaceWith($meta);
            $('.crosstable').replaceWith($html.find('.crosstable'));
            startTournamentClock();
            li.pubsub.emit('content_loaded');
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
          time: Number.parseFloat($(this).data('time')),
        });
      });
  }
  function getPresetGroup(d: RoundData) {
    if (d.player.spectator) return;
    if (d.steps.length < 4) return 'start';
    else if (d.game.status.id >= 30) return 'end';
    return;
  }
  opts.klasses = Array.from(element.classList);
  opts.socketSend = li.socket.send;
  if (!data.tournament && !data.simul)
    opts.onChange = (d: RoundData) => {
      if (chat) chat.preset.setGroup(getPresetGroup(d));
    };

  ctrl = start(element, opts);
  const chatOpts = opts.chat;
  if (chatOpts) {
    if (data.tournament?.top) {
      chatOpts.plugin = tourStandingCtrl(data.tournament.top, data.tournament.team);
      chatOpts.alwaysEnabled = true;
    } else if (!data.simul) {
      chatOpts.preset = getPresetGroup(data);
      chatOpts.parseMoves = true;
    }
    if (chatOpts.noteId && (chatOpts.noteAge || 0) < 10) chatOpts.noteText = '';
    chat = makeChat(chatOpts as ChatOpts);
  }
  startTournamentClock();
  $('.round__now-playing .move-on input')
    .on('change', ctrl.moveOn.toggle)
    .prop('checked', ctrl.moveOn.get())
    .on('click', 'a', () => {
      window.lishogi.properReload = true;
      return true;
    });
  if (location.pathname.lastIndexOf('/round-next/', 0) === 0)
    history.replaceState(null, '', '/' + data.game.id);
  if (location.pathname.length === 9 && data.player.id)
    history.replaceState(null, '', '/' + data.game.id + data.player.id);
  $('#zentog').on('click', () => li.pubsub.emit('zen'));
  li.storage.make('reload-round-tabs').listen(li.reload);

  return ctrl;
}
