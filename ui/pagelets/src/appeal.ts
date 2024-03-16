import { formToXhr } from 'common/xhr';

site.load.then(() => {
  if ($('.nav-tree').length) location.hash = location.hash || '#help-root';

  $('select.appeal-presets').on('change', function (this: HTMLSelectElement, e: Event) {
    $(this)
      .parents('form')
      .find('textarea')
      .val((e.target as HTMLTextAreaElement).value);
  });

  $('form.appeal__actions__slack').on('submit', (e: Event) => {
    const form = e.target as HTMLFormElement;
    formToXhr(form);
    $(form).find('button').text('Sent!').attr('disabled', 'true');
    return false;
  });
});
