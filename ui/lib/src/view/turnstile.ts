import { requestIdleCallbackSafe } from '@/common';

import { alert } from './dialogs';

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
          showError(`Captcha error: ${errorCode} - ${troubleshooting(errorCode)}`);
        },
        'expired-callback': () => {
          showError('Captcha expired, please try again');
        },
        'timeout-callback': () => {
          showError('Captcha timed out');
        },
        'unsupported-callback': () => {
          alert(
            'Unfortunately, your browser does not support the captcha required to log in. Please use a different browser or device to access your account.',
          );
          showError('Captcha is not supported in this browser. Please use a different browser or device.');
        },
      });
    });
  });
}

// https://developers.cloudflare.com/turnstile/troubleshooting/client-side-errors/error-codes/
function troubleshooting(code: string): string {
  switch (code) {
    case '110600': // Challenge timed out
    case '200100': // Clock or cache problem
      return 'Please check your system clock and clear your browser cache.';
    case '200500': // Iframe load error
      return 'Failed to load the captcha. Check if "challenges.cloudflare.com" is blocked.';
    case '110100': // Invalid sitekey
    case '110110': // Sitekey not found
    case '110200': // Domain not authorized
    case '400020': // Invalid sitekey
    case '400070': // Sitekey disabled
      return 'Please report this issue to the site administrator.';
    default:
      return 'An unknown error occurred. Please access https://browser-compat.turnstile.workers.dev/ for more information.';
  }
}
