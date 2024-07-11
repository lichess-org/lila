import * as xhr from 'common/xhr';
import flairPickerLoader from './exports/flairPicker';

interface TeamOpts {
  id: string;
  socketVersion: number;
  chat?: any;
}

export function initModule(opts: TeamOpts) {
  site.socket = new site.StrongSocket('/team/' + opts.id, opts.socketVersion);

  if (opts.chat) site.makeChat(opts.chat);

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

$('.emoji-details').each(function (this: HTMLElement) {
  flairPickerLoader(this);
});
