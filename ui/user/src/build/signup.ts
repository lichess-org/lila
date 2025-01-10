import { spinnerHtml } from 'common/spinner';
import { debounce } from 'common/timings';

window.lishogi.ready.then(() => {
  const $form = $('#signup_form'),
    $exists = $form.find('.username-exists'),
    $username = $form
      .find<HTMLInputElement>('input[name="username"]')
      .on('change keyup paste', () => {
        $exists.hide();
        usernameCheck();
      });

  const usernameCheck = debounce(() => {
    const name = $username.val();
    if (name && name.length >= 3)
      window.lishogi.xhr
        .json('GET', '/api/player/autocomplete', {
          url: { term: name, exists: true },
        })
        .then(res => {
          $exists.toggle(res);
        });
  }, 300);

  $form.on('submit', () => {
    $form
      .find('button.submit')
      .prop('disabled', true)
      .removeAttr('data-icon')
      .addClass('frameless')
      .html(spinnerHtml);
  });
});
(window as any).signupSubmit = () => {
  const form = document.getElementById('signup_form') as HTMLFormElement;
  if (form.reportValidity()) form.submit();
};
