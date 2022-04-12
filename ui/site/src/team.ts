import * as xhr from 'common/xhr';
import LichessChat from 'chat';

window.LichessChat = LichessChat;

interface TeamOpts {
  id: string;
  socketVersion: number;
  chat?: any;
}

export default function (opts: TeamOpts) {
  lichess.socket = new lichess.StrongSocket('/team/' + opts.id, opts.socketVersion);

  if (opts.chat) lichess.makeChat(opts.chat);

  $('#team-subscribe').on('change', function (this: HTMLInputElement) {
    $(this)
      .parents('form')
      .each(function (this: HTMLFormElement) {
        xhr.formToXhr(this);
      });
  });
}

$('button.explain').on('click', e => {
  let why = prompt('Please explain the reason for this action');
  why = why && why.trim();
  if (why && why.length > 3) $(e.target).parents('form').find('input[name="explain"]').val(why);
  else return false;
});
