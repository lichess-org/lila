import { SwissOpts, SwissData } from './interfaces';
import { ChatCtrl } from 'chat';

export default function(opts: SwissOpts): void {
  const li = window.lichess;
  const element = document.querySelector('main.swiss') as HTMLElement,
  data: SwissData = opts.data;
  li.socket = li.StrongSocket(
    '/swiss/' + cfg.data.id, cfg.data.socketVersion, {
      receive: function(t, d) {
        return tournament.socketReceive(t, d);
      }
    });
  cfg.socketSend = lichess.socket.send;
  cfg.element = element;
  cfg.$side = $('.tour__side').clone();
  cfg.$faq = $('.tour__faq').clone();
  tournament = LichessTournament.start(cfg);
}
