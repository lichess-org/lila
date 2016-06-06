package lila.stripe

import lila.user.User

final class StripeApi(client: StripeClient) {

  def checkout(user: User, data: Checkout): Funit =
    ???
  // (user.plan.customerId ?? client.customerExists) flatMap {
  //   case false => client.createCustomer(user, data.source
  // }
}
