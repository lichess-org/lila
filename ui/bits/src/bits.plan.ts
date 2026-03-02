import * as xhr from 'lib/xhr';
import { alert } from 'lib/view';
import { log } from 'lib/permalog';

const showError = (error: string) => alert(error);

export function initModule(opts?: { stripePublicKey: string }): void {
  if (opts?.stripePublicKey) stripeStart(opts.stripePublicKey);
  else payPalStart();
}

const changeForm = () => {
  const $change = $('.plan table.all .change');
  $change.find('a').on('click', function (this: HTMLLinkElement) {
    const f = this.dataset.form!;
    $change.find('form:not(.' + f + ')').hide();
    $change.find('form.' + f).toggle();
  });
};

export function stripeStart(publicKey: string): void {
  $('.update-payment-method').on('click', () => {
    const stripe = window.Stripe(publicKey);
    xhr.json('/patron/stripe/update-payment', { method: 'post' }).then(data => {
      if (data.session?.id) {
        stripe
          .redirectToCheckout({
            sessionId: data.session.id,
          })
          .then((result: any) => showError(result.error.message))
          .catch((e: unknown) => {
            log('Stripe.redirectToCheckout', e);
            if (e instanceof Error) showError(e.message);
            else if (typeof e === 'string') showError(e);
          });
      } else {
        location.assign('/patron');
      }
    }, showError);
  });

  changeForm();
}

export function payPalStart(): void {
  changeForm();
}
