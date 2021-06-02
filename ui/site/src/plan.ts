import * as xhr from 'common/xhr';

export default function (publicKey: string) {
  const stripe = window.Stripe(publicKey);
  $('.update-payment-method').on('click', () => {
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

  const $change = $('.plan table.all .change');
  $change.find('a').on('click', function (this: HTMLLinkElement) {
    const f = $(this).data('form');
    $change.find('form:not(.' + f + ')').hide();
    $change.find('form.' + f).toggle();
  });
}

const showError = (error: string) => alert(error);
