lishogi.checkout = function (publicKey) {
  var $checkout = $('div.plan_checkout');
  var lifetime = {
    cents: parseInt($checkout.data('lifetime-cents')),
    usd: $checkout.data('lifetime-usd'),
  };
  var min = 100,
    max = 100 * 100000;

  if (location.hash === '#onetime') $('#freq_onetime').click();
  if (location.hash === '#lifetime') $('#freq_lifetime').click();

  var getFreq = function () {
    return $checkout.find('group.freq input:checked').val();
  };

  // Other is selected but no amount specified
  // happens with backward button
  if (!$checkout.find('.amount_choice group.amount input:checked').data('amount'))
    $checkout.find('#plan_monthly_1000').click();

  var selectAmountGroup = function () {
    var freq = getFreq();
    $checkout.find('.amount_fixed').toggle(freq == 'lifetime');
    $checkout.find('.amount_choice').toggle(freq != 'lifetime');
  };
  selectAmountGroup();

  $checkout.find('group.freq input').on('change', selectAmountGroup);

  $checkout.find('group.amount .other label').on('click', function () {
    var amount;
    var raw = prompt($(this).attr('title'));
    try {
      amount = parseFloat(raw.replace(',', '.').replace(/[^0-9\.]/gim, ''));
    } catch (e) {
      return false;
    }
    var cents = Math.round(amount * 100);
    if (!cents) {
      $(this).text($(this).data('trans-other'));
      $checkout.find('#plan_monthly_1000').click();
      return false;
    }
    if (cents < min) cents = min;
    else if (cents > max) cents = max;
    var usd = '$' + cents / 100;
    $(this).text(usd);
    $(this).siblings('input').data('amount', cents).data('usd', usd);
  });

  $checkout.find('button.paypal').on('click', function () {
    var freq = getFreq(),
      cents;
    if (freq == 'lifetime') {
      cents = lifetime.cents;
    } else {
      var cents = parseInt($checkout.find('group.amount input:checked').data('amount'));
    }
    if (!cents || cents < min || cents > max) return;
    var amount = cents / 100;
    var $form = $checkout.find('form.paypal_checkout.' + getFreq());
    $form.find('input.amount').val(amount);
    $form.submit();
    $checkout.find('.service').html(lishogi.spinnerHtml);
  });

  $('.confirm-policy input').change(function () {
    $('.service button').toggleClass('disabled');
  });

  //let stripe = Stripe(publicKey);
  //let showError = (error) => {
  //  // TODO: consider a more sophisticated error handling mechanism,
  //  //       for now, this should work just fine.
  //  alert(error);
  //};
  //$checkout.find("button.stripe").on("click", function () {
  //  var freq = getFreq(),
  //    amount;
  //  if (freq == "lifetime") {
  //    amount = lifetime.cents;
  //  } else {
  //    var $input = $checkout.find("group.amount input:checked");
  //    amount = parseInt($input.data("amount"));
  //  }
  //  if (amount < min || amount > max) return;
  //  $checkout.find(".service").html(lishogi.spinnerHtml);
  //
  //  $.ajax({
  //    url: "/patron/stripe-checkout",
  //    method: "post",
  //    data: {
  //      email: $checkout.data("email"),
  //      amount: amount,
  //      freq: freq,
  //    },
  //  }).then(
  //    (data) => {
  //      if (data.session && data.session.id) {
  //        stripe
  //          .redirectToCheckout({
  //            sessionId: data.session.id,
  //          })
  //          .then((result) => showError(result.error.message));
  //      } else {
  //        location.assign("/patron");
  //      }
  //    },
  //    (err) => {
  //      showError(err);
  //    }
  //  );
  //});

  // Close Checkout on page navigation:
  //$(window).on("popstate", function () {
  //  stripeHandler.close();
  //});
};
