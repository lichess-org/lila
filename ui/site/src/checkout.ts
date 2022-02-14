import * as xhr from 'common/xhr';
import contactEmail from './component/contactEmail';

export interface Pricing {
  currency: string;
  default: number;
  min: number;
  max: number;
  lifetime: number;
}

const $checkout = $('div.plan_checkout');
const getFreq = () => $checkout.find('group.freq input:checked').val();
const getDest = () => $checkout.find('group.dest input:checked').val();
const showErrorThenReload = (error: string) => {
  alert(error);
  location.assign('/patron');
};

export default function (stripePublicKey: string, pricing: Pricing) {
  contactEmail();

  const hasLifetime = $('#freq_lifetime').prop('disabled');

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
    return raw != $('body').data('user') && raw.match(/^[a-z0-9][\w-]{2,29}$/) ? raw : null;
  };

  const toggleCheckout = () => {
    const giftDest = getGiftDest();
    const enabled = getDest() != 'gift' || !!giftDest;
    toggleInput($checkout.find('.service button'), enabled);
  };

  toggleCheckout();
  $userInput.on('change', toggleCheckout).on('input', toggleCheckout);

  const getAmountToCharge = () => {
    const freq = getFreq(),
      amount =
        freq == 'lifetime' ? pricing.lifetime : parseFloat($checkout.find('group.amount input:checked').data('amount'));
    if (amount && amount >= pricing.min && amount <= pricing.max) return amount;
  };

  const $currencyForm = $('form.currency');
  $('.currency-toggle').one('click', () => $currencyForm.toggleClass('none'));
  $currencyForm.find('select').on('change', () => {
    ['freq', 'dest'].forEach(name =>
      $('<input type="hidden">')
        .attr('name', name)
        .val($(`input[name=${name}]:checked`).val())
        .appendTo($currencyForm)
    );
    ($currencyForm[0] as HTMLFormElement).submit();
  });

  const queryParams = new URLSearchParams(location.search);
  for (const name of ['dest', 'freq']) {
    if (queryParams.has(name))
      $(`input[name=${name}][value=${queryParams.get(name)?.replace(/[^a-z_-]/gi, '')}]`).trigger('click');
  }

  payPalStart($checkout, pricing, getAmountToCharge);
  stripeStart($checkout, stripePublicKey, pricing, getAmountToCharge);
}

const xhrFormData = ($checkout: Cash, amount: number) =>
  xhr.form({
    email: $checkout.data('email'),
    amount,
    freq: getFreq(),
    gift: $checkout.find('.gift input').val(),
  });

function payPalStart($checkout: Cash, pricing: Pricing, getAmount: () => number | undefined) {
  (window.paypal as any)
    .Buttons({
      style: {
        layout: 'horizontal',
        color: 'blue',
        shape: 'rect',
        label: 'paypal',
        height: 55,
      },
      createOrder: (_data: any, _actions: any) => {
        const amount = getAmount();
        if (!amount) return;
        return xhr
          .json(`/patron/paypal/checkout?currency=${pricing.currency}`, {
            method: 'post',
            body: xhrFormData($checkout, amount),
          })
          .then(data => {
            if (data.error) showErrorThenReload(data.error);
            else if (data.order?.id) return data.order.id;
            else location.assign('/patron');
          });
      },
      onApprove: (data: any, _actions: any) => {
        xhr.json('/patron/paypal/capture/' + data.orderID, { method: 'POST' }).then(() => {
          lichess.reload();
        });
      },
    })
    .render('#paypal-button-container');
}

function stripeStart($checkout: Cash, publicKey: string, pricing: Pricing, getAmount: () => number | undefined) {
  const stripe = window.Stripe(publicKey);
  $checkout.find('.service .stripe').on('click', function () {
    const amount = getAmount();
    if (!amount) return;
    $checkout.find('.service').html(lichess.spinnerHtml);

    fetch(`/patron/stripe/checkout?currency=${pricing.currency}`, {
      ...xhr.defaultInit,
      headers: {
        ...xhr.jsonHeader,
        ...xhr.xhrHeader,
      },
      method: 'post',
      body: xhrFormData($checkout, amount),
    })
      .then(res => res.json())
      .then(data => {
        if (data.error) showErrorThenReload(data.error);
        else if (data.session?.id) {
          stripe
            .redirectToCheckout({
              sessionId: data.session.id,
            })
            .then((result: any) => showErrorThenReload(result.error.message));
        } else location.assign('/patron');
      });
  });

  // Close Checkout on page navigation:
  $(window).on('popstate', function () {
    window.stripeHandler.close();
  });
}
