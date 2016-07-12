package lila.stripe

import lila.db.dsl._
import lila.user.{ User, UserRepo }

import org.joda.time.DateTime

final class StripeApi(
    client: StripeClient,
    patronColl: Coll,
    chargeColl: Coll,
    bus: lila.common.Bus) {

  import Patron.BSONHandlers._
  import Charge.BSONHandlers._

  def checkout(user: User, data: Checkout): Fu[StripeSubscription] =
    LichessPlan findUnder data.cents match {
      case None       => fufail(s"Invalid amount ${data.amount}")
      case Some(plan) => setUserPlan(user, plan.stripePlan, data.source)
    }

  def switch(user: User, plan: LichessPlan): Fu[StripeSubscription] =
    userCustomer(user) flatMap {
      case None => fufail(s"Can't switch non-existent customer ${user.id}")
      case Some(customer) =>
        customer.firstSubscription match {
          case None      => fufail(s"Can't switch non-existent subscription of ${user.id}")
          case Some(sub) => client.updateSubscription(sub, plan.stripePlan, none)
        }
    }

  def cancel(user: User): Funit =
    userCustomer(user) flatMap {
      case None => fufail(s"Can't cancel non-existent customer ${user.id}")
      case Some(customer) =>
        customer.firstSubscription match {
          case None => fufail(s"Can't cancel non-existent subscription of ${user.id}")
          case Some(sub) => client.cancelSubscription(sub) >>
            UserRepo.setPlan(user, user.plan.disable)
        }
    }

  def onCharge(charge: StripeCharge): Funit =
    customerIdPatron(charge.customer) flatMap { patronOption =>
      chargeColl.insert(Charge.make(
        userId = patronOption.map(_.userId.value),
        stripe = Charge.Stripe(charge.customer.value).some,
        cents = charge.cents)) >> {
        patronOption match {
          case None => fufail(s"Charged unknown customer $charge")
          case Some(patron) if patron.canLevelUp =>
            UserRepo byId patron.userId.value flatten s"Missing user for $patron" flatMap { user =>
              patronColl.updateField($id(patron.id), "lastLevelUp", DateTime.now) >>
                UserRepo.setPlan(user, user.plan.incMonths) >>- {
                  logger.info(s"Charged $charge $patron")
                  lila.mon.stripe.amount(charge.amount)
                  bus.publish(lila.hub.actorApi.stripe.ChargeEvent(
                    username = user.username,
                    amount = charge.amount), 'stripe)
                }
            }
          case Some(patron) => fufail(s"Too early to level up $charge $patron")
        }
      }
    }

  def onPaypalCharge(userId: String, email: Option[Patron.PayPal.Email], subId: Option[Patron.PayPal.SubId], cents: Cents): Funit = (cents.value >= 500) ?? {
    chargeColl.insert(Charge.make(
      userId = userId.some,
      payPal = Charge.PayPal(email = email.map(_.value), subId = subId.map(_.value)).some,
      cents = cents)) >> {
      UserRepo named userId flatMap {
        _ ?? { user =>
          userPatron(user).flatMap {
            case None => patronColl.insert(Patron(
              _id = Patron.UserId(user.id),
              payPal = Patron.PayPal(email, subId, DateTime.now).some,
              lastLevelUp = DateTime.now)) >>
              UserRepo.setPlan(user, lila.user.Plan.start)
            case Some(patron) if patron.canLevelUp =>
              patronColl.updateField($id(patron.id), "lastLevelUp", DateTime.now) >>
                UserRepo.setPlan(user, user.plan.incMonths)
            case Some(patron) => fufail(s"Too early to level up with paypal $patron")
          } >>-
            logger.info(s"Charged ${user.id} with paypal: $cents")
        }
      }
    }
  }

  def onSubscriptionDeleted(sub: StripeSubscription): Funit =
    customerIdPatron(sub.customer) flatMap {
      case None => fufail(s"Deleted subscription of unknown customer $sub")
      case Some(patron) =>
        UserRepo byId patron.userId.value flatten s"Missing user for $patron" flatMap { user =>
          UserRepo.setPlan(user, user.plan.disable) >>-
            logger.info(s"Unsubed ${user.id} ${sub}")
        }
    }

  def getEvent = client.getEvent _

  def customerInfo(user: User): Fu[Option[CustomerInfo]] =
    userCustomerId(user) flatMap {
      _ ?? { customerId =>
        client.getCustomer(customerId) zip
          client.getNextInvoice(customerId) zip
          client.getPastInvoices(customerId) map {
            case ((Some(customer), Some(nextInvoice)), pastInvoices) =>
              customer.plan flatMap LichessPlan.byStripePlan match {
                case Some(plan) => CustomerInfo(plan, nextInvoice, pastInvoices).some
                case None =>
                  logger.warn(s"Can't identify ${user.id} plan $customer")
                  none
              }
            case fail =>
              logger.warn(s"Can't fetch ${user.id} customer info $fail")
              none
          }
      }
    }

  def sync(user: User): Fu[Boolean] = userPatron(user) flatMap {

    case None if !user.plan.isEmpty =>
      logger.warn(s"sync: disable plan of non-patron")
      UserRepo.setPlan(user, user.plan.disable) inject true

    case None => fuccess(false)

    case Some(patron) => (patron.stripe, patron.payPal) match {

      case (Some(stripe), _) => client.getCustomer(stripe.customerId) flatMap {
        case None =>
          logger.warn(s"sync: unset DB patron that's not in stripe")
          patronColl.unsetField($id(user.id), "stripe") >> sync(user)
        case Some(customer) if customer.firstSubscription.isEmpty =>
          logger.warn(s"sync: unset DB patron of customer without a subscription")
          patronColl.unsetField($id(user.id), "stripe") >> sync(user)
        case Some(customer) if customer.firstSubscription.isDefined && !user.plan.active =>
          logger.warn(s"sync: enable plan of customer with a subscription")
          UserRepo.setPlan(user, user.plan.enable) inject true
        case _ => fuccess(false)
      }

      case (_, Some(paypal)) =>
        if (paypal.isExpired && user.plan.active) {
          logger.warn(s"sync: disable plan of patron with expired paypal")
          UserRepo.setPlan(user, user.plan.disable) inject true
        }
        else if (!paypal.isExpired && !user.plan.active) {
          logger.warn(s"sync: enable plan of customer with paypal")
          UserRepo.setPlan(user, user.plan.enable) inject true
        }
        else fuccess(false)

      case (None, None) if user.plan.active =>
        logger.warn(s"sync: disable plan of patron with no paypal or stripe")
        UserRepo.setPlan(user, user.plan.disable) inject true

      case _ => fuccess(false)
    }
  }

  private def setUserPlan(user: User, plan: StripePlan, source: Source): Fu[StripeSubscription] =
    userCustomer(user) flatMap {
      case None => createCustomer(user, plan, source) map { customer =>
        customer.firstSubscription err s"Can't create ${user.id} subscription"
      }
      case Some(customer) => setCustomerPlan(customer, plan, source)
    }

  private def createCustomer(user: User, plan: StripePlan, source: Source): Fu[StripeCustomer] =
    client.createCustomer(user, plan, source) flatMap { customer =>
      patronColl.insert(Patron(
        _id = Patron.UserId(user.id),
        stripe = Patron.Stripe(customer.id).some,
        lastLevelUp = DateTime.now)) >>
        UserRepo.setPlan(user, lila.user.Plan.start) >>-
        logger.info(s"Subed ${user.id} ${plan}") inject customer
    }

  private def setCustomerPlan(customer: StripeCustomer, plan: StripePlan, source: Source): Fu[StripeSubscription] =
    customer.subscriptions.data.find(_.plan == plan) match {
      case Some(sub) => fuccess(sub)
      case None => customer.firstSubscription match {
        case None      => client.createSubscription(customer, plan, source)
        case Some(sub) => client.updateSubscription(sub, plan, source.some)
      }
    }

  private def userCustomerId(user: User): Fu[Option[CustomerId]] =
    patronColl.primitiveOne[CustomerId]($id(user.id), "stripe.customerId")

  private def userCustomer(user: User): Fu[Option[StripeCustomer]] =
    userCustomerId(user) flatMap {
      _ ?? client.getCustomer
    }

  private def customerIdPatron(id: CustomerId): Fu[Option[Patron]] =
    patronColl.uno[Patron](selectStripeCustomerId(id))

  private def selectStripeCustomerId(id: CustomerId): Bdoc =
    $doc("stripe.customerId" -> id)

  private def userPatron(user: User): Fu[Option[Patron]] =
    patronColl.uno[Patron]($id(user.id))
}
