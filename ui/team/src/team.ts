import flairPickerLoader from 'bits/flairPicker';

import standaloneChat from 'lib/chat/standalone';
import { wsConnect } from 'lib/socket';
import { prompt } from 'lib/view';

interface TeamOpts {
  id: string;
  socketVersion: number;
  chat?: any;
}

export function initModule(opts: TeamOpts): void {
  wsConnect('/team/' + opts.id, opts.socketVersion);

  if (opts.chat) standaloneChat(opts.chat);
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

$('.emoji-details').each(async function (this: HTMLElement) {
  await flairPickerLoader(this);
});
