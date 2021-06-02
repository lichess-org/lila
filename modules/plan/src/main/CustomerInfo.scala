package lila.plan

sealed trait CustomerInfo

case class MonthlyCustomerInfo(
    subscription: StripeSubscription,
    nextInvoice: StripeInvoice,
    pastInvoices: List[StripeInvoice],
    paymentMethod: Option[StripePaymentMethod]
) extends CustomerInfo

case class OneTimeCustomerInfo(
    customer: StripeCustomer
) extends CustomerInfo
