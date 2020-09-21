package lila.plan

import org.joda.time.DateTime
import reactivemongo.api._
import scala.concurrent.duration._

import lila.common.config.Secret
import lila.common.Bus
import lila.db.dsl._
import lila.memo.CacheApi._
import lila.user.{ User, UserRepo }

final class PlanApi(
    stripeClient: StripeClient,
    patronColl: Coll,
    chargeColl: Coll,
    notifier: PlanNotifier,
    userRepo: UserRepo,
    lightUserApi: lila.user.LightUserApi,
    cacheApi: lila.memo.CacheApi,
    mongoCache: lila.memo.MongoCache.Api,
    payPalIpnKey: Secret,
    monthlyGoalApi: MonthlyGoalApi
)(implicit ec: scala.concurrent.ExecutionContext) {

  import BsonHandlers._
  import PatronHandlers._
  import ChargeHandlers._

  def switch(user: User, cents: Cents): Fu[StripeSubscription] =
    userCustomer(user) flatMap {
      case None => fufail(s"Can't switch non-existent customer ${user.id}")
      case Some(customer) =>
        customer.firstSubscription match {
          case None                                 => fufail(s"Can't switch non-existent subscription of ${user.id}")
          case Some(sub) if sub.plan.cents == cents => fuccess(sub)
          case Some(sub) =>
            getOrMakePlan(cents, Freq.Monthly) flatMap { plan =>
              stripeClient.updateSubscription(sub, plan)
            }
        }
    }

  def cancel(user: User): Funit =
    userCustomer(user) flatMap {
      case None => fufail(s"Can't cancel non-existent customer ${user.id}")
      case Some(customer) =>
        customer.firstSubscription match {
          case None => fufail(s"Can't cancel non-existent subscription of ${user.id}")
          case Some(sub) =>
            stripeClient.cancelSubscription(sub) >>
              isLifetime(user).flatMap { lifetime =>
                !lifetime ?? setDbUserPlan(user, user.plan.disable)
              } >>
              patronColl.update.one($id(user.id), $unset("stripe", "payPal", "expiresAt")).void >>-
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
            userRepo byId patron.userId orFail s"Missing user for $patron" flatMap { user =>
              val p2 = patron
                .copy(
                  stripe = Patron.Stripe(stripeCharge.customer).some,
                  free = none
                )
                .levelUpIfPossible
              patronColl.update.one($id(patron.id), p2) >>
                setDbUserPlanOnCharge(user, p2) >> {
                  stripeCharge.lifetimeWorthy ?? setLifetime(user)
                }
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
      key: String
  ): Funit =
    if (key != payPalIpnKey.value) {
      logger.error(s"Invalid PayPal IPN key $key from $ip $userId $cents")
      funit
    } else if (cents.value < 100) {
      logger.info(s"Ignoring small paypal charge from $ip $userId $cents $txnId")
      funit
    } else {
      val charge = Charge.make(
        userId = userId,
        payPal = Charge
          .PayPal(
            name = name,
            email = email.map(_.value),
            txnId = txnId,
            subId = subId.map(_.value),
            ip = ip.some
          )
          .some,
        cents = cents
      )
      addCharge(charge) >>
        (userId ?? userRepo.named) flatMap { userOption =>
          userOption ?? { user =>
            val payPal = Patron.PayPal(email, subId, DateTime.now)
            userPatron(user).flatMap {
              case None =>
                patronColl.insert.one(
                  Patron(
                    _id = Patron.UserId(user.id),
                    payPal = payPal.some,
                    lastLevelUp = Some(DateTime.now)
                  ).expireInOneMonth
                ) >>
                  setDbUserPlan(user, lila.user.Plan.start) >>
                  notifier.onStart(user)
              case Some(patron) =>
                val p2 = patron
                  .copy(
                    payPal = payPal.some,
                    free = none
                  )
                  .levelUpIfPossible
                  .expireInOneMonth
                patronColl.update.one($id(patron.id), p2) >>
                  setDbUserPlanOnCharge(user, p2)
            } >> {
              charge.lifetimeWorthy ?? setLifetime(user)
            } >>- logger.info(s"Charged ${user.username} with paypal: $cents")
          }
        }
    }

  private def setDbUserPlanOnCharge(user: User, patron: Patron): Funit = {
    val plan =
      if (patron.canLevelUp) user.plan.incMonths
      else user.plan.enable
    Bus.publish(lila.hub.actorApi.plan.MonthInc(user.id, plan.months), "plan")
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
        userRepo byId patron.userId orFail s"Missing user for $patron" flatMap { user =>
          setDbUserPlan(user, user.plan.disable) >>
            patronColl.update.one($id(user.id), patron.removeStripe).void >>
            notifier.onExpire(user) >>-
            logger.info(s"Unsubed ${user.username} $sub")
        }
    }

  def onCompletedSession(completedSession: StripeCompletedSession): Funit =
    customerIdPatron(completedSession.customer) flatMap {
      case None =>
        logger.warn(s"Completed Session of unknown patron $completedSession")
        funit
      case Some(patron) =>
        userRepo byId patron.userId orFail s"Missing user for $patron" flatMap { user =>
          saveStripePatron(
            user,
            completedSession.customer,
            if (completedSession.mode == "subscription") Freq.Monthly else Freq.Onetime
          )
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
          OneTimeCustomerInfo(customer).some
      }

  import PlanApi.SyncResult.{ ReloadUser, Synced }

  def sync(user: User): Fu[PlanApi.SyncResult] =
    userPatron(user) flatMap {

      case None if user.plan.active =>
        logger.warn(s"${user.username} sync: disable plan of non-patron")
        setDbUserPlan(user, user.plan.disable) inject ReloadUser

      case None => fuccess(Synced(none, none))

      case Some(patron) =>
        (patron.stripe, patron.payPal) match {

          case (Some(stripe), _) =>
            stripeClient.getCustomer(stripe.customerId) flatMap {
              case None =>
                logger.warn(s"${user.username} sync: unset DB patron that's not in stripe")
                patronColl.update.one($id(patron.id), patron.removeStripe) >> sync(user)
              case Some(customer) if customer.firstSubscription.exists(_.isActive) && !user.plan.active =>
                logger.warn(s"${user.username} sync: enable plan of customer with a subscription")
                setDbUserPlan(user, user.plan.enable) inject ReloadUser
              case customer => fuccess(Synced(patron.some, customer))
            }

          case (_, Some(_)) =>
            if (!user.plan.active) {
              logger.warn(s"${user.username} sync: enable plan of customer with paypal")
              setDbUserPlan(user, user.plan.enable) inject ReloadUser
            } else fuccess(Synced(patron.some, none))

          case (None, None) if patron.isLifetime => fuccess(Synced(patron.some, none))

          case (None, None) if user.plan.active && patron.free.isEmpty =>
            logger.warn(s"${user.username} sync: disable plan of patron with no paypal or stripe")
            setDbUserPlan(user, user.plan.disable) inject ReloadUser

          case _ => fuccess(Synced(patron.some, none))
        }
    }

  def isLifetime(user: User): Fu[Boolean] =
    userPatron(user) map {
      _.exists(_.isLifetime)
    }

  def setLifetime(user: User): Funit =
    userRepo.setPlan(
      user,
      lila.user.Plan(
        months = user.plan.months | 1,
        active = true,
        since = user.plan.since orElse DateTime.now.some
      )
    ) >> patronColl.update
      .one(
        $id(user.id),
        $set(
          "lastLevelUp" -> DateTime.now,
          "lifetime"    -> true,
          "free"        -> Patron.Free(DateTime.now)
        ),
        upsert = true
      )
      .void >>- lightUserApi.invalidate(user.id)

  def giveMonth(user: User): Funit =
    userRepo.setPlan(
      user,
      lila.user.Plan(
        months = user.plan.months | 1,
        active = true,
        since = user.plan.since orElse DateTime.now.some
      )
    ) >> patronColl.update
      .one(
        $id(user.id),
        $set(
          "lastLevelUp" -> DateTime.now,
          "lifetime"    -> false,
          "free"        -> Patron.Free(DateTime.now),
          "expiresAt"   -> DateTime.now.plusMonths(1).plusDays(1)
        ),
        upsert = true
      )
      .void >>- lightUserApi.invalidate(user.id)

  private val recentChargeUserIdsNb = 100
  private val recentChargeUserIdsCache = cacheApi.unit[List[User.ID]] {
    _.refreshAfterWrite(30 minutes)
      .buildAsyncFuture { _ =>
        chargeColl.primitive[User.ID](
          $empty,
          sort = $doc("date" -> -1),
          nb = recentChargeUserIdsNb * 3 / 2,
          "userId"
        ) flatMap filterUserIds dmap (_ take recentChargeUserIdsNb)
      }
  }

  def recentChargeUserIds: Fu[List[User.ID]] = recentChargeUserIdsCache.getUnit

  def recentChargesOf(user: User): Fu[List[Charge]] =
    chargeColl.find($doc("userId" -> user.id)).sort($doc("date" -> -1)).cursor[Charge]().list()

  private val topPatronUserIdsNb = 300
  private val topPatronUserIdsCache = mongoCache.unit[List[User.ID]](
    "patron:top",
    59 minutes
  ) { loader =>
    _.refreshAfterWrite(60 minutes)
      .buildAsyncFuture {
        loader { _ =>
          chargeColl
            .aggregateList(
              maxDocs = topPatronUserIdsNb * 2,
              readPreference = ReadPreference.secondaryPreferred
            ) { framework =>
              import framework._
              Match($doc("userId" $exists true)) -> List(
                GroupField("userId")("total" -> SumField("cents")),
                Sort(Descending("total")),
                Limit(topPatronUserIdsNb * 3 / 2)
              )
            }
            .dmap {
              _.flatMap { _.getAsOpt[User.ID]("_id") }
            } flatMap filterUserIds dmap (_ take topPatronUserIdsNb)
        }
      }
  }

  def topPatronUserIds: Fu[List[User.ID]] = topPatronUserIdsCache.get {}

  private def filterUserIds(ids: List[User.ID]): Fu[List[User.ID]] = {
    val dedup = ids.distinct
    userRepo.filterByEnabledPatrons(dedup) map { enableds =>
      dedup filter enableds.contains
    }
  }

  private def addCharge(charge: Charge): Funit =
    chargeColl.insert.one(charge).void >>- {
      recentChargeUserIdsCache.invalidateUnit()
      monthlyGoalApi.get foreach { m =>
        Bus.publish(
          lila.hub.actorApi.plan.ChargeEvent(
            username = charge.userId.flatMap(lightUserApi.sync).fold("Anonymous")(_.name),
            amount = charge.cents.value,
            percent = m.percent,
            DateTime.now
          ),
          "plan"
        )
        lila.mon.plan.goal.update(m.goal.value)
        lila.mon.plan.current.update(m.current.value)
        lila.mon.plan.percent.update(m.percent)
        if (charge.isPayPal) lila.mon.plan.paypal.record(charge.cents.value)
        else if (charge.isStripe) lila.mon.plan.stripe.record(charge.cents.value)
      }
    }

  private def getOrMakePlan(cents: Cents, freq: Freq): Fu[StripePlan] =
    stripeClient.getPlan(cents, freq) getOrElse stripeClient.makePlan(cents, freq)

  private def setDbUserPlan(user: User, plan: lila.user.Plan): Funit =
    userRepo.setPlan(user, plan) >>- lightUserApi.invalidate(user.id)

  private def saveStripePatron(user: User, customerId: CustomerId, freq: Freq): Funit =
    userPatron(user) flatMap { patronOpt =>
      val patron = patronOpt
        .getOrElse(Patron(_id = Patron.UserId(user.id)))
        .copy(
          stripe = Patron.Stripe(customerId).some,
          lastLevelUp = Some(DateTime.now)
        )
        .removePayPal
        .expireInOneMonth(!freq.renew)
      patronColl.update.one($id(user.id), patron, upsert = true).void
    }

  private def saveStripeCustomer(user: User, customerId: CustomerId): Funit =
    userPatron(user) flatMap { patronOpt =>
      val patron = patronOpt
        .getOrElse(Patron(_id = Patron.UserId(user.id)))
        .copy(stripe = Patron.Stripe(customerId).some)
      patronColl.update.one($id(user.id), patron, upsert = true).void
    }

  def userCustomerId(user: User): Fu[Option[CustomerId]] =
    userPatron(user) map {
      _.flatMap { _.stripe.map(_.customerId) }
    }

  def userCustomer(user: User): Fu[Option[StripeCustomer]] =
    userCustomerId(user) flatMap {
      _ ?? stripeClient.getCustomer
    }

  def getOrMakeCustomer(user: User, data: Checkout): Fu[StripeCustomer] =
    userCustomer(user) getOrElse makeCustomer(user, data)

  def makeCustomer(user: User, data: Checkout): Fu[StripeCustomer] =
    stripeClient.createCustomer(user, data) flatMap { customer =>
      saveStripeCustomer(user, customer.id) inject customer
    }

  def getOrMakeCustomerId(user: User, data: Checkout): Fu[CustomerId] =
    getOrMakeCustomer(user, data).map(_.id)

  def patronCustomer(patron: Patron): Fu[Option[StripeCustomer]] =
    patron.stripe.map(_.customerId) ?? stripeClient.getCustomer

  private def customerIdPatron(id: CustomerId): Fu[Option[Patron]] =
    patronColl.one[Patron]($doc("stripe.customerId" -> id))

  def userPatron(user: User): Fu[Option[Patron]] = patronColl.one[Patron]($id(user.id))

  def createSession(data: CreateStripeSession): Fu[StripeSession] =
    data.checkout.freq match {
      case Freq.Onetime =>
        stripeClient.createOneTimeSession(data)
      case Freq.Monthly =>
        getOrMakePlan(data.checkout.cents, data.checkout.freq) flatMap { plan =>
          stripeClient.createMonthlySession(data, plan)
        }
    }

}

object PlanApi {

  sealed trait SyncResult
  object SyncResult {
    case object ReloadUser                                                      extends SyncResult
    case class Synced(patron: Option[Patron], customer: Option[StripeCustomer]) extends SyncResult
  }
}
