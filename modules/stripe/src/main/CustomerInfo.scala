package lila.stripe

import org.joda.time.DateTime

case class CustomerInfo(
  currentPlan: LichessPlan,
  nextInvoice: StripeInvoice,
  pastInvoices: List[StripeInvoice])
