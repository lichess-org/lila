import * as xhr from 'lib/xhr';

// Donations from the recap use the same endpoints as /patron, always charged in USD.
// The page loads Stripe.js and both PayPal SDKs, see modules/recap/src/main/ui/RecapUi.scala

export type Freq = 'monthly' | 'onetime';

export interface Donation {
  amount: number; // USD
  freq: Freq;
}

const checkoutUrl = (service: 'stripe' | 'paypal') => `/patron/${service}/checkout?currency=USD`;
const thanksUrl = '/patron/thanks';

const post = async (service: 'stripe' | 'paypal', d: Donation) => {
  const res = await xhr.jsonAnyResponse(checkoutUrl(service), {
    method: 'post',
    body: xhr.form({ amount: d.amount, freq: d.freq }),
  });
  return res.json();
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
