import * as xhr from 'common/xhr';
import flairPickerLoader from './flairPicker';
import { wsConnect } from 'common/socket';
import { makeChat } from 'chat';
import { prompt } from 'common/dialog';

interface TeamOpts {
  id: string;
  socketVersion: number;
  chat?: any;
}

export function initModule(opts: TeamOpts): void {
  wsConnect('/team/' + opts.id, opts.socketVersion);

  if (opts.chat) makeChat(opts.chat);

  $('#team-subscribe').on('change', function (this: HTMLInputElement) {
    $(this)
      .parents('form')
      .each(function (this: HTMLFormElement) {
        xhr.formToXhr(this);
      });
  });
}

$('button.explain').on('click', async e => {
  if (!e.isTrusted) return;
  e.preventDefault();
  const why = (await prompt('Please explain the reason for this action'))?.trim();
  if (why && why.length > 3) {
    $(e.target).parents('form').find('input[name="explain"]').val(why);
    (e.target as HTMLElement).click();
  }
});

$('.emoji-details').each(function (this: HTMLElement) {
  flairPickerLoader(this);
});
