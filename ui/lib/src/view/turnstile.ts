import { requestIdleCallbackSafe } from '@/common';

const selector = '.cf-turnstile';

export default function turnstile($form: Cash): void {
  $form.find(selector).each(function (this: HTMLDivElement) {
    const turnstileDiv = this;
    $form.find('.submit').prop('disabled', true);
    turnstileDiv.innerHTML = '';
    const options = Object.assign({}, turnstileDiv.dataset);
    const showError = (message: string | false) => {
      const $err = $form.find('.cf-turnstile-error');
      if (message) $err.html(`<p>${message}</p>`).removeClass('none');
      else $err.addClass('none');
    };
    requestIdleCallbackSafe(() => {
      window.turnstile.render(selector, {
        ...options,
        appearance: 'interaction-only',
        callback: () => {
          $form.find('.submit').prop('disabled', false);
          showError(false);
        },
        'error-callback': (errorCode: string) => {
          showError('Captcha error: ' + errorCode);
        },
        'expired-callback': () => {
          showError('Captcha expired, please try again');
        },
        'timeout-callback': () => {
          showError('Captcha timed out');
        },
      });
    });
  });
}
