import * as xhr from 'common/xhr';

const showError = (error: string) => alert(error);

const changeForm = () => {
  const $change = $('.plan table.all .change');
  $change.find('a').on('click', function (this: HTMLLinkElement) {
    const f = this.dataset.form!;
    $change.find('form:not(.' + f + ')').hide();
    $change.find('form.' + f).toggle();
  });
};

export function stripeStart(publicKey: string) {
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

export function payPalStart() {
  changeForm();
}

(window as any).plan = { payPalStart, stripeStart };
