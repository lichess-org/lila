import { currencyFormat, roundToCurrency } from 'lib/i18n';
import * as xhr from 'lib/xhr';

import type { CheckoutOpts } from './interfaces';

// Donations from the recap use the same endpoints as /patron, in the visitor's currency.
// The page loads Stripe.js and both PayPal SDKs, see modules/recap/src/main/ui/RecapUi.scala

export type Freq = 'monthly' | 'onetime';

export interface Donation {
  amount: number; // in `currency`
  currency: string;
  freq: Freq;
}

export const usdCheckout: CheckoutOpts = {
  stripePublicKey: '',
  currency: 'USD',
  usdRate: 1,
  stripe: true,
  payPal: true,
};

// a USD amount in the visitor's currency, rounded to 2 significant figures so it reads naturally
export const localAmount = (usd: number, { currency, usdRate }: CheckoutOpts): number => {
  if (currency === 'USD') return usd;
  const n = usd * usdRate;
  const step = Math.pow(10, Math.floor(Math.log10(n)) - 1);
  return roundToCurrency(Math.round(n / step) * step, currency);
};

// whole amounts without decimals: €7, but €6.90
export const formatMoney = (n: number, currency: string): string =>
  currencyFormat(n, currency, n % 1 ? {} : { minimumFractionDigits: 0, maximumFractionDigits: 0 });

const checkoutUrl = (service: 'stripe' | 'paypal', currency: string) =>
  `/patron/${service}/checkout?${new URLSearchParams({ currency })}`;
const thanksUrl = '/patron/thanks';

// on a network failure or a non-JSON error page, send the visitor to /patron to donate from there
const post = async (service: 'stripe' | 'paypal', d: Donation) => {
  try {
    const res = await xhr.jsonAnyResponse(checkoutUrl(service, d.currency), {
      method: 'post',
      body: xhr.form({ amount: d.amount, freq: d.freq }),
    });
    return await res.json();
  } catch {
    location.assign('/patron');
    return new Promise<never>(() => {}); // the page is navigating away
  }
};

const fail = (error: string) => {
  alert(error);
};

export const hasStripe = (): boolean => !!window.Stripe;

export async function stripeCheckout(publicKey: string, d: Donation): Promise<void> {
  const data = await post('stripe', d);
  if (data.error) return fail(data.error);
  // no session: the server updated an existing subscription instead
  if (!data.session?.id) return location.assign('/patron');
  const result = await window.Stripe(publicKey).redirectToCheckout({ sessionId: data.session.id });
  if (result?.error) fail(result.error.message);
}

interface PayPalApproval {
  orderID: string;
  subscriptionID?: string;
}

interface PayPalButtonsOpts {
  style: typeof payPalStyle;
  fundingSource: string;
  createOrder?: () => Promise<string | undefined>;
  createSubscription?: () => Promise<string | undefined>;
  onApprove: (data: PayPalApproval) => Promise<void>;
}

interface PayPalSdk {
  FUNDING: { PAYPAL: string };
  Buttons(opts: PayPalButtonsOpts): { render(el: HTMLElement): Promise<void> };
}

const payPalStyle = {
  layout: 'horizontal',
  color: 'gold',
  shape: 'pill',
  label: 'paypal',
  tagline: false,
  height: 48,
};

// renders PayPal's own buttons; getDonation is read when the visitor clicks, so it follows their choices
export function renderPayPal(
  containers: Record<Freq, HTMLElement>,
  getDonation: () => Donation,
): Record<Freq, boolean> {
  const order = window.paypalOrder as PayPalSdk | undefined;
  const subscription = window.paypalSubscription as PayPalSdk | undefined;
  order
    ?.Buttons({
      style: payPalStyle,
      fundingSource: order.FUNDING.PAYPAL, // only the PayPal logo button, no PayPal Credit or card
      createOrder: async () => {
        const data = await post('paypal', getDonation());
        if (data.error) fail(data.error);
        return data.order?.id;
      },
      onApprove: async data => {
        await xhr.json(`/patron/paypal/capture/${data.orderID}`, { method: 'POST' });
        location.assign(thanksUrl);
      },
    })
    .render(containers.onetime);
  subscription
    ?.Buttons({
      style: payPalStyle,
      fundingSource: subscription.FUNDING.PAYPAL,
      createSubscription: async () => {
        const data = await post('paypal', getDonation());
        if (data.error) fail(data.error);
        return data.subscription?.id;
      },
      onApprove: async data => {
        await xhr.json(`/patron/paypal/capture/${data.orderID}?sub=${data.subscriptionID ?? ''}`, {
          method: 'POST',
        });
        location.assign(thanksUrl);
      },
    })
    .render(containers.monthly);
  return { onetime: !!order, monthly: !!subscription };
}
