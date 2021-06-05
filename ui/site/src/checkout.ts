import * as xhr from 'common/xhr';

export interface Pricing {
  currency: string;
  default: number;
  min: number;
  max: number;
  lifetime: number;
}

export default function (publicKey: string, pricing: Pricing) {
  const $checkout = $('div.plan_checkout');

  const hasLifetime = $('#freq_lifetime').prop('disabled');

  const getFreq = () => $checkout.find('group.freq input:checked').val();
  const getDest = () => $checkout.find('group.dest input:checked').val();

  const toggleInput = ($input: Cash, enable: boolean) =>
    $input.prop('disabled', !enable).toggleClass('disabled', !enable);

  // Other is selected but no amount specified
  // happens with backward button
  if (!$checkout.find('.amount_choice group.amount input:checked').data('amount'))
    $checkout.find('input.default').trigger('click');

  const selectAmountGroup = function () {
    const freq = getFreq();
    $checkout.find('.amount_fixed').toggleClass('none', freq != 'lifetime');
    $checkout.find('.amount_choice').toggleClass('none', freq == 'lifetime');
  };
  selectAmountGroup();

  $checkout.find('group.freq input').on('change', selectAmountGroup);

  $checkout.find('group.dest input').on('change', () => {
    const isGift = getDest() == 'gift';
    const $monthly = $('#freq_monthly');
    toggleInput($monthly, !isGift);
    $checkout.find('.gift').toggleClass('none', !isGift).find('input').val('');
    const $lifetime = $('#freq_lifetime');
    toggleInput($lifetime, isGift || !hasLifetime);
    $lifetime.toggleClass('lifetime-check', !isGift && hasLifetime);
    if (isGift) {
      if ($monthly.is(':checked')) $('#freq_onetime').trigger('click');
      $checkout.find('.gift input').trigger('focus');
    }
    toggleCheckout();
  });

  $checkout.find('group.amount .other label').on('click', function (this: HTMLLabelElement) {
    let amount: number;
    const raw: string = prompt(this.title) || '';
    try {
      amount = parseFloat(raw.replace(',', '.').replace(/[^0-9\.]/gim, ''));
    } catch (e) {
      return false;
    }
    if (!amount) {
      $(this).text($(this).data('trans-other'));
      $checkout.find('input.default').trigger('click');
      return false;
    }
    amount = Math.max(pricing.min, Math.min(pricing.max, amount));
    $(this).text(`${pricing.currency} ${amount}`);
    $(this).siblings('input').data('amount', amount);
  });

  const $userInput = $checkout.find('input.user-autocomplete');

  const getGiftDest = () => {
    const raw = ($userInput.val() as string).trim().toLowerCase();
    return raw.match(/^[a-z0-9][\w-]{2,29}$/) ? raw : null;
  };

  const toggleCheckout = () => {
    const giftDest = getGiftDest();
    const enabled = getDest() != 'gift' || !!giftDest;
    toggleInput($checkout.find('.service button'), enabled);
    let custom = $('body').data('user');
    if (enabled && giftDest) custom += ' ' + giftDest;
    $checkout.find('form.paypal_checkout:not(.monthly) input[name=custom]').val(custom);
  };

  toggleCheckout();
  $userInput.on('change', toggleCheckout).on('input', toggleCheckout);

  const getAmountToCharge = () => {
    const freq = getFreq(),
      amount =
        freq == 'lifetime' ? pricing.lifetime : parseFloat($checkout.find('group.amount input:checked').data('amount'));
    if (amount && amount >= pricing.min && amount <= pricing.max) return amount;
  };

  $checkout.find('button.paypal').on('click', function () {
    const freq = getFreq(),
      amount = getAmountToCharge();
    if (!amount) return;
    const $form = $checkout.find('form.paypal_checkout.' + freq);
    $form.find('input.amount').val('' + amount);
    ($form[0] as HTMLFormElement).submit();
    $checkout.find('.service').html(lichess.spinnerHtml);
  });

  const stripe = window.Stripe(publicKey);
  const showErrorThenReload = (error: string) => {
    alert(error);
    location.assign('/patron');
  };
  $checkout.find('button.stripe').on('click', function () {
    const freq = getFreq(),
      amount = getAmountToCharge();
    if (!amount) return;
    $checkout.find('.service').html(lichess.spinnerHtml);

    fetch('/patron/stripe/checkout', {
      ...xhr.defaultInit,
      headers: {
        ...xhr.jsonHeader,
        ...xhr.xhrHeader,
      },
      method: 'post',
      body: xhr.form({
        email: $checkout.data('email'),
        amount,
        freq,
        gift: $checkout.find('.gift input').val(),
      }),
    })
      .then(res => res.json())
      .then(data => {
        if (data.error) showErrorThenReload(data.error);
        else if (data.session?.id) {
          stripe
            .redirectToCheckout({
              sessionId: data.session.id,
            })
            .then(result => showErrorThenReload(result.error.message));
        } else location.assign('/patron');
      });
  });

  // Close Checkout on page navigation:
  $(window).on('popstate', function () {
    window.stripeHandler.close();
  });

  if (location.hash === '#onetime') $('#freq_onetime').trigger('click');
  if (location.hash === '#lifetime') $('#freq_lifetime').trigger('click');
  if (location.hash === '#gift') $('#dest_gift').trigger('click');
}
