package lila.plan

import lila.db.dsl._
import lila.memo._
import lila.user.{ User, UserRepo }

import org.joda.time.DateTime
import reactivemongo.api.collections.bson.BSONBatchCommands.AggregationFramework._
import scala.concurrent.duration._

final class PlanApi(
    stripeClient: StripeClient,
    patronColl: Coll,
    chargeColl: Coll,
    tracking: PlanTracking,
    notifier: PlanNotifier,
    lightUserApi: lila.user.LightUserApi,
    bus: lila.common.Bus,
    payPalIpnKey: PayPalIpnKey,
    monthlyGoalApi: MonthlyGoalApi) {

  import BsonHandlers._
  import PatronHandlers._
  import ChargeHandlers._

  def checkout(userOption: Option[User], data: Checkout): Funit =
    getOrMakePlan(data.cents, data.freq) flatMap { plan =>
      userOption.fold(anonCheckout(plan, data)) { user =>
        userCheckout(user, plan, data)
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
            getOrMakePlan(cents, Freq.Monthly) flatMap { plan =>
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
            setDbUserPlan(user, user.plan.disable) >>
            patronColl.update($id(user.id), $unset("stripe", "payPal", "expiresAt")).void >>-
            logger.info(s"Canceled subscription $sub of ${user.username}")
        }
    }

  def onStripeCharge(stripeCharge: StripeCharge): Funit =
    customerIdPatron(stripeCharge.customer) flatMap { patronOption =>
      val charge = Charge.make(
        userId = patronOption.map(_.userId),
        stripe = Charge.Stripe(stripeCharge.id, stripeCharge.customer).some,
        cents = stripeCharge.amount)
      addCharge(charge) >> {
        patronOption match {
          case None =>
            logger.info(s"Charged anon customer $charge")
            funit
          case Some(patron) =>
            logger.info(s"Charged $charge $patron")
            stripeClient getCustomer stripeCharge.customer foreach {
              _ foreach { customer => tracking.charge(charge, renew = customer.renew) }
            }
            UserRepo byId patron.userId flatten s"Missing user for $patron" flatMap { user =>
              val p2 = patron.copy(
                stripe = Patron.Stripe(stripeCharge.customer).some
              ).levelUpIfPossible
              patronColl.update($id(patron.id), p2) >>
                setDbUserPlan(user,
                  if (patron.canLevelUp) user.plan.incMonths
                  else user.plan.enable)
            }
        }
      }
    }

  def onPaypalCharge(
    userId: Option[String],
    email: Option[Patron.PayPal.Email],
    subId: Option[Patron.PayPal.SubId],
    cents: Cents,
    name: Option[String],
    txnId: Option[String],
    ip: String,
    key: PayPalIpnKey): Funit =
    if (key != payPalIpnKey) {
      logger.error(s"Invalid PayPal IPN key $key from $ip $userId $cents")
      funit
    }
    else (cents.value >= 100) ?? {
      val charge = Charge.make(
        userId = userId,
        payPal = Charge.PayPal(
          name = name,
          email = email.map(_.value),
          txnId = txnId,
          subId = subId.map(_.value),
          ip = ip.some).some,
        cents = cents)
      tracking.charge(charge, renew = subId.isDefined)
      addCharge(charge) >>
        (userId ?? UserRepo.named) flatMap { userOption =>
          userOption ?? { user =>
            val payPal = Patron.PayPal(email, subId, DateTime.now)
            userPatron(user).flatMap {
              case None => patronColl.insert(Patron(
                _id = Patron.UserId(user.id),
                payPal = payPal.some,
                lastLevelUp = DateTime.now
              ).expireInOneMonth) >>
                setDbUserPlan(user, lila.user.Plan.start) >>
                notifier.onStart(user) >>-
                tracking.newDonation(user, cents, renew = subId.isDefined)
              case Some(patron) =>
                if (subId.isDefined) tracking.upgrade(user, cents)
                else tracking.reDonation(user, cents)
                val p2 = patron.copy(
                  payPal = payPal.some
                ).levelUpIfPossible.expireInOneMonth
                patronColl.update($id(patron.id), p2) >>
                  setDbUserPlan(user,
                    if (patron.canLevelUp) user.plan.incMonths
                    else user.plan.enable)
            } >>- logger.info(s"Charged ${user.username} with paypal: $cents")
          }
        }
    }

  def onSubscriptionDeleted(sub: StripeSubscription): Funit =
    customerIdPatron(sub.customer) flatMap {
      case None =>
        logger.warn(s"Deleted subscription of unknown patron $sub")
        funit
      case Some(patron) =>
        UserRepo byId patron.userId flatten s"Missing user for $patron" flatMap { user =>
          setDbUserPlan(user, user.plan.disable) >>
            patronColl.update($id(user.id), patron.removeStripe).void >>
            notifier.onExpire(user) >>-
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
      setDbUserPlan(user, user.plan.disable) inject ReloadUser

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
          setDbUserPlan(user, user.plan.enable) inject ReloadUser
        case customer => fuccess(Synced(patron.some, customer))
      }

      case (_, Some(paypal)) =>
        if (!user.plan.active) {
          logger.warn(s"sync: enable plan of customer with paypal")
          setDbUserPlan(user, user.plan.enable) inject ReloadUser
        }
        else fuccess(Synced(patron.some, none))

      case (None, None) if user.plan.active =>
        logger.warn(s"sync: disable plan of patron with no paypal or stripe")
        setDbUserPlan(user, user.plan.disable) inject ReloadUser

      case _ => fuccess(Synced(patron.some, none))
    }
  }

  private val recentChargeUserIdsCache = AsyncCache[Int, List[User.ID]](
    f = nb => chargeColl.primitive[User.ID](
      $empty, sort = $doc("date" -> -1), nb = nb, "userId"
    ) flatMap filterUserIds,
    timeToLive = 1 hour)

  def recentChargeUserIds(nb: Int): Fu[List[User.ID]] = recentChargeUserIdsCache(nb)

  def recentChargesOf(user: User): Fu[List[Charge]] =
    chargeColl.find($doc("userId" -> user.id)).sort($doc("date" -> -1)).list[Charge]()

  private val topPatronUserIdsCache = AsyncCache[Int, List[User.ID]](
    f = nb => chargeColl.aggregate(
      Match($doc("userId" $exists true)), List(
        GroupField("userId")("total" -> SumField("cents")),
        Sort(Descending("total")),
        Limit(nb))).map {
        _.firstBatch.flatMap { _.getAs[User.ID]("_id") }
      } flatMap filterUserIds,
    timeToLive = 1 hour)

  def topPatronUserIds(nb: Int): Fu[List[User.ID]] = topPatronUserIdsCache(nb)

  private def filterUserIds(ids: List[User.ID]): Fu[List[User.ID]] = {
    val dedup = ids.distinct
    UserRepo.filterByEnabled(dedup) map { enableds =>
      val set = enableds.toSet
      dedup filter set.contains
    }
  }

  private def addCharge(charge: Charge): Funit =
    chargeColl.insert(charge) >>
      recentChargeUserIdsCache.clear >>
      topPatronUserIdsCache.clear >>- {
        monthlyGoalApi.get foreach { m =>
          bus.publish(lila.hub.actorApi.plan.ChargeEvent(
            username = charge.userId.flatMap(lightUserApi.get).fold("Anonymous")(_.name),
            amount = charge.cents.value,
            percent = m.percent), 'plan)
          lila.mon.plan.goal(m.goal.value)
          lila.mon.plan.current(m.current.value)
          lila.mon.plan.percent(m.percent)
          if (charge.isPayPal) {
            lila.mon.plan.amount.paypal(charge.cents.value)
            lila.mon.plan.count.paypal()
          }
          else if (charge.isStripe) {
            lila.mon.plan.amount.stripe(charge.cents.value)
            lila.mon.plan.count.stripe()
          }
        }
      }

  private def getOrMakePlan(cents: Cents, freq: Freq): Fu[StripePlan] =
    stripeClient.getPlan(cents, freq) getOrElse stripeClient.makePlan(cents, freq)

  private def anonCheckout(plan: StripePlan, data: Checkout): Funit =
    stripeClient.createAnonCustomer(plan, data) map { customer =>
      logger.info(s"Subed anon $customer to ${plan} freq=${data.freq}")
      customer.firstSubscription err s"Can't create anon $customer subscription to $plan"
    } flatMap { subscription =>
      if (data.freq.renew) funit
      else stripeClient dontRenewSubscription subscription void
    }

  private def userCheckout(user: User, plan: StripePlan, data: Checkout): Funit =
    userCustomer(user) flatMap {
      case None => createCustomer(user, data, plan) map { customer =>
        customer.firstSubscription err s"Can't create ${user.username} subscription for customer $customer"
      } flatMap withNewSubscription(user, data)
      case Some(customer) =>
        // tracking: user did one-time before, goes for monthly now
        if (!customer.renew && data.freq.renew) tracking.upgrade(user, plan.amount)
        // tracking: one-time
        if (!data.freq.renew) tracking.reDonation(user, plan.amount)

        // user has a monthly going on and is making an extra one-time
        // let's not change the user plan to one-time, or else
        // it would only cancel the monthly
        if (customer.renew && !data.freq.renew) stripeClient.addOneTime(customer, data.amount)
        // or else, set this new plan to the customer
        else setCustomerPlan(customer, plan, data.source) flatMap { sub =>
          saveStripePatron(user, customer.id, data.freq) inject sub
        } flatMap withNewSubscription(user, data)
    }

  private def withNewSubscription(user: User, data: Checkout)(subscription: StripeSubscription): Funit = {
    logger.info(s"Subed user ${user.username} $subscription freq=${data.freq}")
    if (data.freq.renew) funit
    else stripeClient dontRenewSubscription subscription void
  }

  private def setDbUserPlan(user: User, plan: lila.user.Plan): Funit =
    UserRepo.setPlan(user, plan) >> lightUserApi.invalidate(user.id)

  private def createCustomer(user: User, data: Checkout, plan: StripePlan): Fu[StripeCustomer] =
    stripeClient.createCustomer(user, data, plan) flatMap { customer =>
      saveStripePatron(user, customer.id, data.freq) >>
        setDbUserPlan(user, lila.user.Plan.start) >>
        notifier.onStart(user) >>-
        tracking.newDonation(user, plan.amount, renew = data.freq.renew) >>-
        logger.info(s"Create ${user.username} customer $customer") inject customer
    }

  private def saveStripePatron(user: User, customerId: CustomerId, freq: Freq): Funit = userPatron(user) flatMap {
    case None => patronColl.insert(Patron(
      _id = Patron.UserId(user.id),
      stripe = Patron.Stripe(customerId).some,
      lastLevelUp = DateTime.now
    ).expireInOneMonth(!freq.renew))
    case Some(patron) => patronColl.update(
      $id(patron.id),
      patron.copy(stripe = Patron.Stripe(customerId).some).expireInOneMonth(!freq.renew))
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

  def patronCustomer(patron: Patron): Fu[Option[StripeCustomer]] =
    patron.stripe.map(_.customerId) ?? stripeClient.getCustomer

  private def customerIdPatron(id: CustomerId): Fu[Option[Patron]] =
    patronColl.uno[Patron]($doc("stripe.customerId" -> id))

  def userPatron(user: User): Fu[Option[Patron]] = patronColl.uno[Patron]($id(user.id))
}

object PlanApi {

  sealed trait SyncResult
  object SyncResult {
    case object ReloadUser extends SyncResult
    case class Synced(patron: Option[Patron], customer: Option[StripeCustomer]) extends SyncResult
  }
}
