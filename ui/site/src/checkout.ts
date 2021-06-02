import * as xhr from 'common/xhr';

export default function (publicKey: string) {
  const $checkout = $('div.plan_checkout');
  const lifetime = {
    cents: parseInt($checkout.data('lifetime-cents')),
    usd: $checkout.data('lifetime-usd'),
  };
  const min = 100,
    max = 100 * 100000;

  if (location.hash === '#onetime') $('#freq_onetime').trigger('click');
  if (location.hash === '#lifetime') $('#freq_lifetime').trigger('click');

  const getFreq = function () {
    return $checkout.find('group.freq input:checked').val();
  };

  // Other is selected but no amount specified
  // happens with backward button
  if (!$checkout.find('.amount_choice group.amount input:checked').data('amount'))
    $checkout.find('#plan_monthly_1000').trigger('click');

  const selectAmountGroup = function () {
    const freq = getFreq();
    $checkout.find('.amount_fixed').toggleClass('none', freq != 'lifetime');
    $checkout.find('.amount_choice').toggleClass('none', freq == 'lifetime');
  };
  selectAmountGroup();

  $checkout.find('group.freq input').on('change', selectAmountGroup);

  $checkout.find('group.amount .other label').on('click', function (this: HTMLLabelElement) {
    let amount: number;
    const raw: string = prompt(this.title) || '';
    try {
      amount = parseFloat(raw.replace(',', '.').replace(/[^0-9\.]/gim, ''));
    } catch (e) {
      return false;
    }
    let cents = Math.round(amount * 100);
    if (!cents) {
      $(this).text($(this).data('trans-other'));
      $checkout.find('#plan_monthly_1000').trigger('click');
      return false;
    }
    if (cents < min) cents = min;
    else if (cents > max) cents = max;
    const usd = '$' + cents / 100;
    $(this).text(usd);
    $(this).siblings('input').data('amount', cents).data('usd', usd);
  });

  $checkout.find('button.paypal').on('click', function () {
    const freq = getFreq(),
      cents =
        freq == 'lifetime' ? lifetime.cents : parseInt($checkout.find('group.amount input:checked').data('amount'));
    if (!cents || cents < min || cents > max) return;
    const amount = cents / 100;
    const $form = $checkout.find('form.paypal_checkout.' + freq);
    $form.find('input.amount').val('' + amount);
    ($form[0] as HTMLFormElement).submit();
    $checkout.find('.service').html(lichess.spinnerHtml);
  });

  const stripe = window.Stripe(publicKey);
  const showError = (error: string) => alert(error);
  $checkout.find('button.stripe').on('click', function () {
    const freq = getFreq(),
      amount =
        freq == 'lifetime' ? lifetime.cents : parseInt($checkout.find('group.amount input:checked').data('amount'));
    if (amount < min || amount > max) return;
    $checkout.find('.service').html(lichess.spinnerHtml);

    xhr
      .json('/patron/stripe/checkout', {
        method: 'post',
        body: xhr.form({
          email: $checkout.data('email'),
          amount,
          freq,
        }),
      })
      .then(data => {
        if (data.session?.id) {
          stripe
            .redirectToCheckout({
              sessionId: data.session.id,
            })
            .then(result => showError(result.error.message));
        } else {
          location.assign('/patron');
        }
      }, showError);
  });

  // Close Checkout on page navigation:
  $(window).on('popstate', function () {
    window.stripeHandler.close();
  });
}
