import * as xhr from 'common/xhr';
import flairPickerLoader from './exports/flairPicker';
import StrongSocket from 'common/socket';
import { makeChat } from 'chat';

interface TeamOpts {
  id: string;
  socketVersion: number;
  chat?: any;
}

export function initModule(opts: TeamOpts): void {
  site.socket = new StrongSocket('/team/' + opts.id, opts.socketVersion);

  if (opts.chat) makeChat(opts.chat);

  $('#team-subscribe').on('change', function(this: HTMLInputElement) {
    $(this)
      .parents('form')
      .each(function(this: HTMLFormElement) {
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

$('.emoji-details').each(function(this: HTMLElement) {
  flairPickerLoader(this);
});
