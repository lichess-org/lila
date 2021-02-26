import { formToXhr } from 'common/xhr';

lichess.load.then(() => {
  if ($('.nav-tree').length) location.hash = location.hash || '#help-root';

  $('select.appeal-presets').on('change', (e: Event) => $('#form3-text').val((e.target as HTMLTextAreaElement).value));

  $('form.appeal__actions__slack').on('submit', (e: Event) => {
    const form = e.target as HTMLFormElement;
    formToXhr(form);
    $(form).find('button').text('Sent!').attr('disabled', 'true');
    return false;
  });
});
