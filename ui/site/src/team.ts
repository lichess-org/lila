import * as xhr from 'common/xhr';

interface TeamOpts {
  id: string;
  socketVersion: number;
  chat?: any;
}

export function initModule(opts: TeamOpts) {
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

$('.emoji-details').each(function (this: HTMLElement) {
  const details = this;
  const parent = $(details).parent();
  const close = () => details.removeAttribute('open');
  const onEmojiSelect = (i?: { id: string; src: string }) => {
    parent.find('input[name="flair"]').val(i?.id ?? '');
    parent.find('.uflair').remove();
    if (i) parent.find('.flair-container').append('<img class="uflair" src="' + i.src + '" />');
    close();
  };
  parent.find('.emoji-remove').on('click', e => {
    e.preventDefault();
    onEmojiSelect();
    $(e.target).remove();
  });
  $(details).on('toggle', () =>
    lichess.asset.loadEsm('flairPicker', {
      init: {
        element: details.querySelector('.flair-picker')!,
        close,
        onEmojiSelect,
      },
    }),
  );
});
