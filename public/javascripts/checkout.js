lichess.checkout = function(publicKey) {

  var $checkout = $('div.plan_checkout');
  var $stripeForm = $checkout.find('form.stripe_checkout');
  var min = 100,
    max = 100 * 100000

  $checkout.find('group.radio .other label').on('click', function() {
    var amount;
    var raw = prompt("Please enter an amount in USD");
    try {
      amount = parseFloat(raw.replace(/[^0-9\.,]/gim, ''));
    } catch (e) {
      return false;
    }
    var cents = Math.round(amount * 100);
    if (!cents) {
      $(this).text('Other');
      $checkout.find('#plan_monthly_10').click();
      return false;
    }
    if (cents < min) cents = min;
    else if (cents > max) cents = max;
    var usd = '$' + (cents / 100);
    $(this).text(usd);
    $(this).siblings('input').data('amount', cents).data('usd', usd);
  });

  $checkout.find('button.paypal').on('click', function() {
    var $input = $checkout.find('group.radio input:checked');
    if ($input.attr('id') == 'plan_other')
      return alert("Sorry, PayPal doesn't work with custom amounts!");
    var usd = $input.data('usd');
    var $form = $checkout.find('form.paypal_checkout');
    $form.find('input.usd').val(usd);
    $form.submit();
    $checkout.find('.service').html(lichess.spinnerHtml);
  });

  $checkout.find('button.stripe').on('click', function() {
    var $input = $checkout.find('group.radio input:checked');
    var usd = $input.data('usd');
    var amount = parseInt($input.data('amount'));
    if (amount < min || amount > max) return;
    $stripeForm.find('.amount').val(amount);

    stripeHandler.open({
      description: usd + '/month',
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
