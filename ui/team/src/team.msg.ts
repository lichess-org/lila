import * as xhr from 'lib/xhr';

export function initModule(): void {
  $('#team-subscribe').on('change', function (this: HTMLInputElement) {
    $(this)
      .parents('form')
      .each(function (this: HTMLFormElement) {
        void xhr.formToXhr(this);
      });
  });
}
