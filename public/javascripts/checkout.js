lichess.checkout = function(publicKey) {

  var $form = $('.checkout_form');

  var handler = StripeCheckout.configure({
    key: publicKey,
    name: 'lichess.org',
    image: 'https://s3.amazonaws.com/stripe-uploads/acct_18J612Fj1uHKxNqMmerchant-icon-1465200826114-logo.512.png',
    locale: 'auto',
    allowRememberMe: false,
    zipCode: false,
    billingAddress: false,
    currency: 'usd',
    token: function(token) {
      $('.checkout_buttons').html(lichess.spinnerHtml);
      $form.find('.token').val(token.id);
      $form.submit();
    }
  });

  $('button.checkout').on('click', function(e) {
    var amount = parseInt($(this).data('cents'));
    $form.find('.cents').val(amount);
    handler.open({
      description: $(this).data('description'),
      amount: amount,
      panelLabel: $(this).data('panel-label'),
      email: $(this).data('email')
    });
    e.preventDefault();
  });

  // Close Checkout on page navigation:
  $(window).on('popstate', function() {
    handler.close();
  });
};
