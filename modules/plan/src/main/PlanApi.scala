package lila.plan

import lila.db.dsl._
import lila.user.{ User, UserRepo }

import org.joda.time.DateTime
import reactivemongo.api.collections.bson.BSONBatchCommands.AggregationFramework._
import reactivemongo.api.ReadPreference
import scala.concurrent.duration._

final class PlanApi(
    stripeClient: StripeClient,
    patronColl: Coll,
    chargeColl: Coll,
    notifier: PlanNotifier,
    lightUserApi: lila.user.LightUserApi,
    bus: lila.common.Bus,
    asyncCache: lila.memo.AsyncCache.Builder,
    payPalIpnKey: PayPalIpnKey,
    monthlyGoalApi: MonthlyGoalApi
) {

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
          case None => fufail(s"Can't switch non-existent subscription of ${user.id}")
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
        cents = stripeCharge.amount
      )
      addCharge(charge) >> {
        patronOption match {
          case None =>
            logger.info(s"Charged anon customer $charge")
            funit
          case Some(patron) =>
            logger.info(s"Charged $charge $patron")
            UserRepo byId patron.userId flatten s"Missing user for $patron" flatMap { user =>
              val p2 = patron.copy(
                stripe = Patron.Stripe(stripeCharge.customer).some
              ).levelUpIfPossible
              patronColl.update($id(patron.id), p2) >>
                setDbUserPlanOnCharge(user, patron)
            }
        }
      }
    }

  def onPaypalCharge(
    userId: Option[User.ID],
    email: Option[Patron.PayPal.Email],
    subId: Option[Patron.PayPal.SubId],
    cents: Cents,
    name: Option[String],
    txnId: Option[String],
    ip: String,
    key: PayPalIpnKey
  ): Funit =
    if (key != payPalIpnKey) {
      logger.error(s"Invalid PayPal IPN key $key from $ip $userId $cents")
      funit
    } else if (cents.value < 100) {
      logger.info(s"Ignoring small paypal charge from $ip $userId $cents $txnId")
      funit
    } else {
      val charge = Charge.make(
        userId = userId,
        payPal = Charge.PayPal(
          name = name,
          email = email.map(_.value),
          txnId = txnId,
          subId = subId.map(_.value),
          ip = ip.some
        ).some,
        cents = cents
      )
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
                notifier.onStart(user)
              case Some(patron) =>
                val p2 = patron.copy(
                  payPal = payPal.some
                ).levelUpIfPossible.expireInOneMonth
                patronColl.update($id(patron.id), p2) >>
                  setDbUserPlanOnCharge(user, patron)
            } >>- logger.info(s"Charged ${user.username} with paypal: $cents")
          }
        }
    }

  private def setDbUserPlanOnCharge(user: User, patron: Patron): Funit = {
    val plan =
      if (patron.canLevelUp) user.plan.incMonths
      else user.plan.enable
    bus.publish(lila.hub.actorApi.plan.MonthInc(user.id, plan.months), 'plan)
    setDbUserPlan(user, plan)
  }

  def onSubscriptionDeleted(sub: StripeSubscription): Funit =
    customerIdPatron(sub.customer) flatMap {
      case None =>
        logger.warn(s"Deleted subscription of unknown patron $sub")
        funit
      case Some(patron) if patron.isLifetime =>
        logger.info(s"Ignore sub end for lifetime patron $patron")
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
      logger.warn(s"${user.username} sync: disable plan of non-patron")
      setDbUserPlan(user, user.plan.disable) inject ReloadUser

    case None => fuccess(Synced(none, none))

    case Some(patron) if patron.isLifetime => fuccess(Synced(patron.some, none))

    case Some(patron) => (patron.stripe, patron.payPal) match {

      case (Some(stripe), _) => stripeClient.getCustomer(stripe.customerId) flatMap {
        case None =>
          logger.warn(s"${user.username} sync: unset DB patron that's not in stripe")
          patronColl.update($id(patron.id), patron.removeStripe) >> sync(user)
        case Some(customer) if customer.firstSubscription.isEmpty =>
          logger.warn(s"${user.username} sync: unset DB patron of customer without a subscription")
          patronColl.update($id(patron.id), patron.removeStripe) >> sync(user)
        case Some(customer) if customer.firstSubscription.isDefined && !user.plan.active =>
          logger.warn(s"${user.username} sync: enable plan of customer with a subscription")
          setDbUserPlan(user, user.plan.enable) inject ReloadUser
        case customer => fuccess(Synced(patron.some, customer))
      }

      case (_, Some(paypal)) =>
        if (!user.plan.active) {
          logger.warn(s"${user.username} sync: enable plan of customer with paypal")
          setDbUserPlan(user, user.plan.enable) inject ReloadUser
        } else fuccess(Synced(patron.some, none))

      case (None, None) if user.plan.active =>
        logger.warn(s"${user.username} sync: disable plan of patron with no paypal or stripe")
        setDbUserPlan(user, user.plan.disable) inject ReloadUser

      case _ => fuccess(Synced(patron.some, none))
    }
  }

  def isLifetime(user: User): Fu[Boolean] = userPatron(user) map {
    _.exists(_.isLifetime)
  }

  def setLifetime(user: User): Funit = isLifetime(user) flatMap {
    case true => funit
    case _ => UserRepo.setPlan(user, lila.user.Plan(
      months = user.plan.months | 1,
      active = true,
      since = user.plan.since orElse DateTime.now.some
    )) >> patronColl.update(
      $id(user.id),
      $set(
        "lastLevelUp" -> DateTime.now,
        "lifetime" -> true
      )
    ).void >>- lightUserApi.invalidate(user.id)
  }

  private val recentChargeUserIdsNb = 50
  private val recentChargeUserIdsCache = asyncCache.single[List[User.ID]](
    name = "plan.recentChargeUserIds",
    f = chargeColl.primitive[User.ID](
      $empty, sort = $doc("date" -> -1), nb = recentChargeUserIdsNb * 3 / 2, "userId"
    ) flatMap filterUserIds map (_ take recentChargeUserIdsNb),
    expireAfter = _.ExpireAfterWrite(1 hour)
  )

  def recentChargeUserIds: Fu[List[User.ID]] = recentChargeUserIdsCache.get

  def recentChargesOf(user: User): Fu[List[Charge]] =
    chargeColl.find($doc("userId" -> user.id)).sort($doc("date" -> -1)).list[Charge]()

  private val topPatronUserIdsNb = 120
  private val topPatronUserIdsCache = asyncCache.single[List[User.ID]](
    name = "plan.topPatronUserIds",
    f = chargeColl.aggregateList(
      Match($doc("userId" $exists true)), List(
        GroupField("userId")("total" -> SumField("cents")),
        Sort(Descending("total")),
        Limit(topPatronUserIdsNb * 3 / 2)
      ),
      maxDocs = topPatronUserIdsNb * 2,
      readPreference = ReadPreference.secondaryPreferred
    ).map {
        _.flatMap { _.getAs[User.ID]("_id") }
      } flatMap filterUserIds map (_ take topPatronUserIdsNb),
    expireAfter = _.ExpireAfterWrite(1 hour)
  )

  def topPatronUserIds: Fu[List[User.ID]] = topPatronUserIdsCache.get

  private def filterUserIds(ids: List[User.ID]): Fu[List[User.ID]] = {
    val dedup = ids.distinct
    UserRepo.filterByEnabledPatrons(dedup) map { enableds =>
      dedup filter enableds.contains
    }
  }

  private def addCharge(charge: Charge): Funit =
    chargeColl.insert(charge).void >>- {
      recentChargeUserIdsCache.refresh
      topPatronUserIdsCache.refresh
      monthlyGoalApi.get foreach { m =>
        bus.publish(lila.hub.actorApi.plan.ChargeEvent(
          username = charge.userId.flatMap(lightUserApi.sync).fold("Anonymous")(_.name),
          amount = charge.cents.value,
          percent = m.percent,
          DateTime.now
        ), 'plan)
        lila.mon.plan.goal(m.goal.value)
        lila.mon.plan.current(m.current.value)
        lila.mon.plan.percent(m.percent)
        if (charge.isPayPal) {
          lila.mon.plan.amount.paypal(charge.cents.value)
          lila.mon.plan.count.paypal()
        } else if (charge.isStripe) {
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
    UserRepo.setPlan(user, plan) >>- lightUserApi.invalidate(user.id)

  private def createCustomer(user: User, data: Checkout, plan: StripePlan): Fu[StripeCustomer] =
    stripeClient.createCustomer(user, data, plan) flatMap { customer =>
      saveStripePatron(user, customer.id, data.freq) >>
        setDbUserPlan(user, lila.user.Plan.start) >>
        notifier.onStart(user) >>-
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
      patron.copy(
        stripe = Patron.Stripe(customerId).some
      ).removePayPal.expireInOneMonth(!freq.renew)
    )
  } void

  private def setCustomerPlan(customer: StripeCustomer, plan: StripePlan, source: Source): Fu[StripeSubscription] =
    customer.subscriptions.data.find(_.plan == plan) match {
      case Some(sub) => fuccess(sub)
      case None => customer.firstSubscription match {
        case None => stripeClient.createSubscription(customer, plan, source)
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
