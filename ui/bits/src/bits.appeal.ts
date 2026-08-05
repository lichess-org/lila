import { formToXhr } from 'lib/xhr';

export function initModule(): void {
  if ($('.nav-tree').length) location.hash = location.hash || '#help-root';

  $('.appeal-presets button').on('click', function (this: HTMLSelectElement, e: Event) {
    $(this)
      .parents('form')
      .find('#form3-text')
      .val((e.target as HTMLButtonElement).value);
  });

  $('form.appeal__actions__zulip').on('submit', (e: SubmitEvent) => {
    const form = e.target as HTMLFormElement;
    formToXhr(form);
    $(form).find('button').text('Sent!').attr('disabled', 'true');
    return false;
  });

  $('form select[name="months"]').on('click', function (this: HTMLSelectElement) {
    if (this.value) (this.parentElement as HTMLFormElement).submit();
  });
}
