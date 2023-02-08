package lila.plan

enum CustomerInfo:

  case Monthly(
      subscription: StripeSubscription,
      nextInvoice: StripeInvoice,
      paymentMethod: Option[StripePaymentMethod]
  )

  case OneTime(customer: StripeCustomer)
