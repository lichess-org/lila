import * as xhr from 'common/xhr';

export default function (publicKey: string) {
  var $checkout = $('div.plan_checkout');
  var lifetime = {
    cents: parseInt($checkout.data('lifetime-cents')),
    usd: $checkout.data('lifetime-usd'),
  };
  var min = 100,
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
    var freq = getFreq();
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
    var cents = Math.round(amount * 100);
    if (!cents) {
      $(this).text($(this).data('trans-other'));
      $checkout.find('#plan_monthly_1000').trigger('click');
      return false;
    }
    if (cents < min) cents = min;
    else if (cents > max) cents = max;
    var usd = '$' + cents / 100;
    $(this).text(usd);
    $(this).siblings('input').data('amount', cents).data('usd', usd);
  });

  $checkout.find('button.paypal').on('click', function () {
    let freq = getFreq(),
      cents: number;
    if (freq == 'lifetime') {
      cents = lifetime.cents;
    } else {
      cents = parseInt($checkout.find('group.amount input:checked').data('amount'));
    }
    if (!cents || cents < min || cents > max) return;
    var amount = cents / 100;
    var $form = $checkout.find('form.paypal_checkout.' + getFreq());
    $form.find('input.amount').val('' + amount);
    ($form[0] as HTMLFormElement).submit();
    $checkout.find('.service').html(lichess.spinnerHtml);
  });

  let stripe = window.Stripe(publicKey);
  let showError = (error: string) => {
    // TODO: consider a more sophisticated error handling mechanism,
    //       for now, this should work just fine.
    alert(error);
  };
  $checkout.find('button.stripe').on('click', function () {
    let freq = getFreq(),
      amount: number;
    if (freq == 'lifetime') {
      amount = lifetime.cents;
    } else {
      var $input = $checkout.find('group.amount input:checked');
      amount = parseInt($input.data('amount'));
    }
    if (amount < min || amount > max) return;
    $checkout.find('.service').html(lichess.spinnerHtml);

    xhr
      .json('/patron/stripe-checkout', {
        method: 'post',
        body: xhr.form({
          email: $checkout.data('email'),
          amount,
          freq,
        }),
      })
      .then(data => {
        if (data.session && data.session.id) {
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
