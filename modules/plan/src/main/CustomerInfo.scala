package lila.plan

import org.joda.time.DateTime

case class CustomerInfo(
  currentPlan: StripePlan,
  nextInvoice: StripeInvoice,
  pastInvoices: List[StripeInvoice])
