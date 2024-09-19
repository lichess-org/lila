import * as xhr from 'common/xhr';

const showError = (error: string) => alert(error);

export function initModule(stripePublicKey?: string): void {
  if (stripePublicKey) stripeStart(stripePublicKey);
  else payPalStart();
}

const changeForm = () => {
  const $change = $('.plan table.all .change');
  $change.find('a').on('click', function(this: HTMLLinkElement) {
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
          .then((result: any) => showError(result.error.message));
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
