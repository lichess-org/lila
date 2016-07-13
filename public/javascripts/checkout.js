lichess.checkout = function(publicKey) {

  var $checkout = $('div.plan_checkout');

  $checkout.find('button.stripe').on('click', function() {
    var $input = $checkout.find('group.radio input:checked');
    var amount = parseInt($input.data('amount'));
    var $form = $checkout.find('form.stripe_checkout');
    $form.find('.amount').val(amount);

    stripeHandler.open({
      description: $input.data('description'),
      amount: amount,
      panelLabel: $input.data('panel-label'),
      email: $checkout.data('email')
    });
  });

  $checkout.find('button.paypal').on('click', function() {
    var $input = $checkout.find('group.radio input:checked');
    var usd = $input.data('usd');
    var $form = $checkout.find('form.paypal_checkout');
    $form.find('input.usd').val(usd);
    $form.submit();
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
      $form.find('.token').val(token.id);
      $form.submit();
    }
  });
  // Close Checkout on page navigation:
  $(window).on('popstate', function() {
    stripeHandler.close();
  });
};
