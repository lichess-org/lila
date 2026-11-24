import { text as xhrText } from 'lib/xhr';

export function registerFormHandler() {
  $('form.insight-refresh').on('submit', function (this: HTMLFormElement) {
    xhrText(this.action, { method: 'post' }).then(() => site.reload());

    $(this).replaceWith($(this).find('.crunching').removeClass('none'));

    return false;
  });
}

site.load.then(registerFormHandler);
