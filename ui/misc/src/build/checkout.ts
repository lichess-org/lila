import { spinnerHtml } from 'common/spinner';

window.lishogi.ready.then(() => {
  const $checkout = $('div.plan_checkout'),
    lifetime = {
      cents: parseInt($checkout.data('lifetime-cents')),
      usd: $checkout.data('lifetime-usd'),
    };
  const min = 100,
    max = 100 * 100000;

  if (location.hash === '#onetime') $('#freq_onetime').click();
  if (location.hash === '#lifetime') $('#freq_lifetime').click();

  const getFreq = () => {
    return $checkout.find('group.freq input:checked').val();
  };

  // Other is selected but no amount specified
  // happens with backward button
  if (!$checkout.find('.amount_choice group.amount input:checked').data('amount'))
    $checkout.find('#plan_monthly_1000').click();

  const selectAmountGroup = () => {
    const freq = getFreq();
    $checkout.find('.amount_fixed').toggle(freq == 'lifetime');
    $checkout.find('.amount_choice').toggle(freq != 'lifetime');
  };
  selectAmountGroup();

  $checkout.find('group.freq input').on('change', selectAmountGroup);

  $checkout.find('group.amount .other label').on('click', function () {
    const raw = prompt($(this).attr('title'));
    let amount;
    try {
      amount = parseFloat(raw!.replace(',', '.').replace(/[^0-9\.]/gim, ''));
    } catch (e) {
      return false;
    }
    let cents = Math.round(amount * 100);
    if (!cents) {
      $(this).text($(this).data('trans-other'));
      $checkout.find('#plan_monthly_1000').click();
      return false;
    }
    if (cents < min) cents = min;
    else if (cents > max) cents = max;
    const usd = '$' + cents / 100;
    $(this).text(usd);
    $(this).siblings('input').data('amount', cents).data('usd', usd);

    return true;
  });

  $checkout.find('button.paypal').on('click', function () {
    const freq = getFreq();
    let cents;
    if (freq == 'lifetime') {
      cents = lifetime.cents;
    } else {
      cents = parseInt($checkout.find('group.amount input:checked').data('amount'));
    }
    if (!cents || cents < min || cents > max) return;
    const amount = cents / 100,
      $form = $checkout.find('form.paypal_checkout.' + getFreq());
    $form.find('input.amount').val(amount);
    $form.submit();
    $checkout.find('.service').html(spinnerHtml);
  });

  $('.confirm-policy input').change(function () {
    $('.service button').toggleClass('disabled');
  });
});
