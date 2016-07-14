lichess.checkout = function(publicKey) {

  var $checkout = $('div.plan_checkout');
  var $stripeForm = $checkout.find('form.stripe_checkout');
  var min = 100,
    max = 100 * 100000

  var getFreq = function() {
    return $checkout.find('group.freq input:checked').val();
  };

  // Other is selected but no amount specified
  // happens with backward button
  if (!$checkout.find('group.amount input:checked').data('amount'))
    $checkout.find('#plan_monthly_1000').click();

  $checkout.find('group.amount .other label').on('click', function() {
    var amount;
    var raw = prompt("Please enter an amount in USD");
    try {
      amount = parseFloat(raw.replace(',', '.').replace(/[^0-9\.]/gim, ''));
    } catch (e) {
      return false;
    }
    var cents = Math.round(amount * 100);
    if (!cents) {
      $(this).text('Other');
      $checkout.find('#plan_monthly_1000').click();
      return false;
    }
    if (cents < min) cents = min;
    else if (cents > max) cents = max;
    var usd = '$' + (cents / 100);
    $(this).text(usd);
    $(this).siblings('input').data('amount', cents).data('usd', usd);
  });

  $checkout.find('button.paypal').on('click', function() {
    var cents = parseInt($checkout.find('group.amount input:checked').data('amount'));
    if (!cents || cents < min || cents > max) return;
    var amount = cents / 100;
    var $form = $checkout.find('form.paypal_checkout.' + getFreq());
    $form.find('input.amount').val(amount);
    $form.submit();
    $checkout.find('.service').html(lichess.spinnerHtml);
  });

  $checkout.find('button.stripe').on('click', function() {
    var $input = $checkout.find('group.amount input:checked');
    var usd = $input.data('usd');
    var amount = parseInt($input.data('amount'));
    var freq = getFreq();
    if (amount < min || amount > max) return;
    $stripeForm.find('.amount').val(amount);
    $stripeForm.find('.freq').val(freq);
    var desc = freq === 'monthly' ? usd + '/month' : usd + ' one-time';

    stripeHandler.open({
      description: desc,
      amount: amount,
      panelLabel: '{{amount}}',
      email: $checkout.data('email')
    });
  });

  var stripeHandler = StripeCheckout.configure({
    key: publicKey,
    name: 'lichess.org',
    image: 'https://s3.amazonaws.com/stripe-uploads/acct_18J612Fj1uHKxNqMmerchant-icon-1465200826114-logo.512.png',
    locale: 'auto',
    allowRememberMe: false,
    zipCode: false,
    billingAddress: false,
    currency: 'usd',
    token: function(token) {
      $checkout.find('.service').html(lichess.spinnerHtml);
      $stripeForm.find('.token').val(token.id);
      $stripeForm.find('.email').val(token.email);
      $stripeForm.submit();
    }
  });
  // Close Checkout on page navigation:
  $(window).on('popstate', function() {
    stripeHandler.close();
  });
};
