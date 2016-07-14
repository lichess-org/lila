package lila.plan

import lila.db.dsl._
import lila.user.{ User, UserRepo }

import org.joda.time.DateTime

final class PlanApi(
    stripeClient: StripeClient,
    patronColl: Coll,
    chargeColl: Coll,
    notifier: PlanNotifier,
    bus: lila.common.Bus) {

  import BsonHandlers._
  import PatronHandlers._
  import ChargeHandlers._

  def checkout(userOption: Option[User], data: Checkout): Funit =
    getOrMakePlan(data.cents) flatMap { plan =>
      userOption.fold(setAnonPlan(plan, data, renew = data.isMonthly)) { user =>
        setUserPlan(user, plan, data, renew = data.isMonthly)
      }
    } void

  def switch(user: User, cents: Cents): Fu[StripeSubscription] =
    userCustomer(user) flatMap {
      case None => fufail(s"Can't switch non-existent customer ${user.id}")
      case Some(customer) =>
        customer.firstSubscription match {
          case None                                 => fufail(s"Can't switch non-existent subscription of ${user.id}")
          case Some(sub) if sub.plan.cents == cents => fuccess(sub)
          case Some(sub) =>
            getOrMakePlan(cents) flatMap { plan =>
              stripeClient.updateSubscription(sub, plan, none)
            }
        }
    }

  def cancel(user: User): Funit =
    userCustomer(user) flatMap {
      case None => fufail(s"Can't cancel non-existent customer ${user.id}")
      case Some(customer) =>
        customer.firstSubscription match {
          case None => fufail(s"Can't cancel non-existent subscription of ${user.id}")
          case Some(sub) => stripeClient.cancelSubscription(sub) >>
            UserRepo.setPlan(user, user.plan.disable)
        }
    }

  def onStripeCharge(charge: StripeCharge): Funit =
    customerIdPatron(charge.customer) flatMap { patronOption =>
      chargeColl.insert(Charge.make(
        userId = patronOption.map(_.userId),
        stripe = Charge.Stripe(charge.id, charge.customer).some,
        cents = charge.amount)) >> {
        patronOption match {
          case None =>
            logger.info(s"Charged anon customer $charge")
            lila.mon.plan.amount(charge.amount.value)
            bus.publish(lila.hub.actorApi.plan.ChargeEvent(
              username = "Anonymous",
              amount = charge.amount.value), 'stripe)
            funit
          case Some(patron) if patron.canLevelUp =>
            UserRepo byId patron.userId flatten s"Missing user for $patron" flatMap { user =>
              patronColl.update($id(patron.id), patron.levelUpNow) >>
                UserRepo.setPlan(user, user.plan.incMonths) >>- {
                  logger.info(s"Charged $charge $patron")
                  lila.mon.plan.amount(charge.amount.value)
                  bus.publish(lila.hub.actorApi.plan.ChargeEvent(
                    username = user.username,
                    amount = charge.amount.value), 'stripe)
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
              lastLevelUp = DateTime.now
            ).expireInOneMonth) >>
              UserRepo.setPlan(user, lila.user.Plan.start) >>
              notifier.onStart(user)
            case Some(patron) if patron.canLevelUp =>
              patronColl.update($id(patron.id), patron.levelUpNow.expireInOneMonth) >>
                UserRepo.setPlan(user, user.plan.incMonths)
            case Some(patron) => fufail(s"Too early to level up with paypal $patron")
          } >>- {
            logger.info(s"Charged ${user.username} with paypal: $cents")
            lila.mon.plan.amount(cents.value)
          }
        }
      }
    }
  }

  def onSubscriptionDeleted(sub: StripeSubscription): Funit =
    customerIdPatron(sub.customer) flatMap {
      case None => fufail(s"Deleted subscription of unknown patron $sub")
      case Some(patron) =>
        UserRepo byId patron.userId flatten s"Missing user for $patron" flatMap { user =>
          UserRepo.setPlan(user, user.plan.disable) >>
            patronColl.update($id(user.id), patron.removeStripe).void >>-
            logger.info(s"Unsubed ${user.username} ${sub}")
        }
    }

  def getEvent = stripeClient.getEvent _

  def customerInfo(user: User, customer: StripeCustomer): Fu[Option[CustomerInfo]] =
    stripeClient.getNextInvoice(customer.id) zip
      stripeClient.getPastInvoices(customer.id) map {
        case (Some(nextInvoice), pastInvoices) =>
          customer.firstSubscription match {
            case Some(sub) => MonthlyCustomerInfo(sub, nextInvoice, pastInvoices).some
            case None =>
              logger.warn(s"Can't identify ${user.username} monthly subscription $customer")
              none
          }
        case (None, _) =>
          customer.firstSubscription match {
            case Some(sub) => OneTimeCustomerInfo(customer, sub).some
            case None =>
              logger.warn(s"Can't identify ${user.username} one-time subscription $customer")
              none
          }
      }

  import PlanApi.SyncResult.{ ReloadUser, Synced }

  def sync(user: User): Fu[PlanApi.SyncResult] = userPatron(user) flatMap {

    case None if user.plan.active =>
      logger.warn(s"sync: disable plan of non-patron")
      UserRepo.setPlan(user, user.plan.disable) inject ReloadUser

    case None => fuccess(Synced(none, none))

    case Some(patron) => (patron.stripe, patron.payPal) match {

      case (Some(stripe), _) => stripeClient.getCustomer(stripe.customerId) flatMap {
        case None =>
          logger.warn(s"sync: unset DB patron that's not in stripe")
          patronColl.update($id(patron.id), patron.removeStripe) >> sync(user)
        case Some(customer) if customer.firstSubscription.isEmpty =>
          logger.warn(s"sync: unset DB patron of customer without a subscription")
          patronColl.update($id(patron.id), patron.removeStripe) >> sync(user)
        case Some(customer) if customer.firstSubscription.isDefined && !user.plan.active =>
          logger.warn(s"sync: enable plan of customer with a subscription")
          UserRepo.setPlan(user, user.plan.enable) inject ReloadUser
        case customer => fuccess(Synced(patron.some, customer))
      }

      case (_, Some(paypal)) =>
        if (paypal.isExpired)
          patronColl.update($id(user.id), patron.removePayPal) >> sync(user)
        else if (!paypal.isExpired && !user.plan.active) {
          logger.warn(s"sync: enable plan of customer with paypal")
          UserRepo.setPlan(user, user.plan.enable) inject ReloadUser
        }
        else fuccess(Synced(patron.some, none))

      case (None, None) if user.plan.active =>
        logger.warn(s"sync: disable plan of patron with no paypal or stripe")
        UserRepo.setPlan(user, user.plan.disable) inject ReloadUser

      case _ => fuccess(Synced(patron.some, none))
    }
  }

  def recentChargeUserIds(nb: Int): Fu[List[String]] =
    chargeColl.primitive[String]($empty, sort = $doc("date" -> -1), "userId")

  def recentChargesOf(user: User): Fu[List[Charge]] =
    chargeColl.find($doc("userId" -> user.id)).sort($doc("date" -> -1)).list[Charge]()

  private def getOrMakePlan(cents: Cents): Fu[StripePlan] =
    stripeClient.getPlan(cents) getOrElse stripeClient.makePlan(cents)

  private def setAnonPlan(plan: StripePlan, data: Checkout, renew: Boolean): Fu[StripeSubscription] =
    stripeClient.createAnonCustomer(plan, data) map { customer =>
      logger.info(s"Subed anon $customer to ${plan} renew=$renew")
      customer.firstSubscription err s"Can't create anon $customer subscription to $plan"
    } flatMap { subscription =>
      if (renew) fuccess(subscription)
      else stripeClient dontRenewSubscription subscription
    }

  private def setUserPlan(user: User, plan: StripePlan, data: Checkout, renew: Boolean): Fu[StripeSubscription] =
    userCustomer(user) flatMap {
      case None => createCustomer(user, data, plan) map { customer =>
        customer.firstSubscription err s"Can't create ${user.username} subscription for customer $customer"
      }
      case Some(customer) => setCustomerPlan(customer, plan, data.source) flatMap { sub =>
        saveStripePatron(user, customer.id, data.isMonthly) inject sub
      }
    } flatMap { subscription =>
      logger.info(s"Subed user ${user.username} $subscription renew=$renew")
      if (renew) fuccess(subscription)
      else stripeClient dontRenewSubscription subscription
    }

  private def createCustomer(user: User, data: Checkout, plan: StripePlan): Fu[StripeCustomer] =
    stripeClient.createCustomer(user, data, plan) flatMap { customer =>
      saveStripePatron(user, customer.id, data.isMonthly) >>
        UserRepo.setPlan(user, lila.user.Plan.start) >>
        notifier.onStart(user) >>-
        logger.info(s"Create ${user.username} customer $customer") inject customer
    }

  private def saveStripePatron(user: User, customerId: CustomerId, renew: Boolean): Funit = userPatron(user) flatMap {
    case None => patronColl.insert(Patron(
      _id = Patron.UserId(user.id),
      stripe = Patron.Stripe(customerId).some,
      lastLevelUp = DateTime.now
    ).expireInOneMonth(!renew))
    case Some(patron) => patronColl.update(
      $id(patron.id),
      patron.copy(stripe = Patron.Stripe(customerId).some).expireInOneMonth(!renew))
  } void

  private def setCustomerPlan(customer: StripeCustomer, plan: StripePlan, source: Source): Fu[StripeSubscription] =
    customer.subscriptions.data.find(_.plan == plan) match {
      case Some(sub) => fuccess(sub)
      case None => customer.firstSubscription match {
        case None      => stripeClient.createSubscription(customer, plan, source)
        case Some(sub) => stripeClient.updateSubscription(sub, plan, source.some)
      }
    }

  private def userCustomerId(user: User): Fu[Option[CustomerId]] =
    userPatron(user) map {
      _.flatMap { _.stripe.map(_.customerId) }
    }

  private def userCustomer(user: User): Fu[Option[StripeCustomer]] =
    userCustomerId(user) flatMap {
      _ ?? stripeClient.getCustomer
    }

  private def customerIdPatron(id: CustomerId): Fu[Option[Patron]] =
    patronColl.uno[Patron](selectStripeCustomerId(id))

  private def selectStripeCustomerId(id: CustomerId): Bdoc =
    $doc("stripe.customerId" -> id)

  private def userPatron(user: User): Fu[Option[Patron]] =
    patronColl.uno[Patron]($id(user.id))
}

object PlanApi {

  sealed trait SyncResult
  object SyncResult {
    case object ReloadUser extends SyncResult
    case class Synced(patron: Option[Patron], customer: Option[StripeCustomer]) extends SyncResult
  }
}
