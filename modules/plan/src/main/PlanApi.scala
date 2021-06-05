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
    monthlyGoalApi: MonthlyGoalApi,
    currencyApi: CurrencyApi,
    pricingApi: PlanPricingApi
)(implicit ec: scala.concurrent.ExecutionContext) {

  import BsonHandlers._
  import PatronHandlers._
  import ChargeHandlers._

  def switch(user: User, money: Money): Fu[StripeSubscription] =
    userCustomer(user) flatMap {
      case None => fufail(s"Can't switch non-existent customer ${user.id}")
      case Some(customer) =>
        customer.firstSubscription match {
          case None                                       => fufail(s"Can't switch non-existent subscription of ${user.id}")
          case Some(sub) if sub.item.price.money == money => fuccess(sub)
          case Some(sub)                                  => stripeClient.updateSubscription(sub, money)
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

  private val stripeGifts = cacheApi.notLoadingSync[PaymentIntentId, User.ID](64, "plan.stripeGift") {
    _.expireAfterWrite(1 hour).build()
  }

  // will be followed with `onStripeCharge` shortly
  def onCompletedSession(session: StripeCompletedSession): Funit =
    customerIdPatron(session.customer) flatMap {
      case None =>
        logger.warn(s"Completed Session of unknown patron $session")
        funit
      case Some(prevPatron) =>
        userRepo byId prevPatron.userId orFail s"Missing user for $prevPatron" flatMap { user =>
          session.giftTo match {
            case Some(giftTo) =>
              for {
                to <- userRepo byId giftTo orFail s"Missing patron gift dest $giftTo"
                _  <- gift(user, to, session.money)
              } yield stripeGifts.put(session.payment_intent.pp(to.id), to.id)
            case None =>
              userRepo byId prevPatron.userId orFail s"Missing user for $prevPatron" flatMap { user =>
                val patron = prevPatron
                  .copy(lastLevelUp = Some(DateTime.now))
                  .removePayPal
                  .expireInOneMonth(!session.freq.renew)
                patronColl.update.one($id(user.id), patron, upsert = true).void
              }
          }
        }
    }

  def onStripeCharge(stripeCharge: StripeCharge): Funit = for {
    patronOption <- customerIdPatron(stripeCharge.customer)
    giftTo       <- stripeCharge.payment_intent ?? stripeGifts.getIfPresent ?? userRepo.byId
    money = stripeCharge.amount toMoney stripeCharge.currency
    usd <- currencyApi toUsd money
    charge = Charge
      .make(
        userId = patronOption.map(_.userId),
        giftTo = giftTo.map(_.id),
        stripe = Charge.Stripe(stripeCharge.id, stripeCharge.customer).some,
        money = money,
        usd = usd | Usd(0)
      )
      .pp
    isLifetime <- pricingApi isLifetime money
    _          <- addCharge(charge, stripeCharge.country)
    _ <- giftTo.isEmpty ?? {
      patronOption match {
        case None =>
          logger.info(s"Charged anon customer $charge")
          funit
        case Some(patron) =>
          logger.info(s"Charged $charge $patron")
          userRepo byId patron.userId orFail s"Missing user for $patron" flatMap { user =>
            val p2 = patron.levelUpIfPossible
            patronColl.update.one($id(patron.id), p2) >>
              setDbUserPlanOnCharge(user, patron.canLevelUp) >> {
                isLifetime ?? setLifetime(user, none)
              }
          }
      }
    }
  } yield ()

  def onPaypalCharge(
      userId: Option[User.ID],
      email: Option[Patron.PayPal.Email],
      subId: Option[Patron.PayPal.SubId],
      money: Money,
      name: Option[String],
      txnId: Option[String],
      country: Option[Country],
      ip: String,
      key: String
  ): Funit = for {
    pricing    <- pricingApi pricingFor money.currency orFail s"Invalid paypal currency $money"
    usd        <- currencyApi toUsd money orFail s"Invalid paypal currency $money"
    isLifetime <- pricingApi isLifetime money
    _ <-
      if (key != payPalIpnKey.value) {
        logger.error(s"Invalid PayPal IPN key $key from $ip $userId $money")
        funit
      } else if (!pricing.valid(money)) {
        logger.info(s"Ignoring invalid paypal amount from $ip $userId $money $txnId")
        funit
      } else {
        val charge = Charge.make(
          userId = userId,
          giftTo = none, // TODO
          payPal = Charge
            .PayPal(
              name = name,
              email = email.map(_.value),
              txnId = txnId,
              subId = subId.map(_.value),
              ip = ip.some
            )
            .some,
          money = money,
          usd = usd
        )
        addCharge(charge, country) >>
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
                    setDbUserPlanOnCharge(user, levelUp = false)
                case Some(patron) =>
                  val p2 = patron
                    .copy(
                      payPal = payPal.some,
                      free = none
                    )
                    .levelUpIfPossible
                    .expireInOneMonth
                  patronColl.update.one($id(patron.id), p2) >>
                    setDbUserPlanOnCharge(user, patron.canLevelUp)
              } >> {
                isLifetime ?? setLifetime(user, none)
              } >>- logger.info(s"Charged ${user.username} with paypal: $money")
            }
          }
      }
  } yield ()

  private def setDbUserPlanOnCharge(user: User, levelUp: Boolean): Funit = {
    val plan =
      if (levelUp) user.plan.incMonths
      else user.plan.enable
    Bus.publish(lila.hub.actorApi.plan.MonthInc(user.id, plan.months), "plan")
    if (plan.months > 1) notifier.onRenew(user.copy(plan = plan))
    else notifier.onStart(user)
    setDbUserPlan(user, plan)
  }

  def onSubscriptionDeleted(sub: StripeSubscription): Funit =
    customerIdPatron(sub.customer) flatMap {
      _ ?? { patron =>
        if (patron.isLifetime) funit
        else
          userRepo byId patron.userId orFail s"Missing user for $patron" flatMap { user =>
            setDbUserPlan(user, user.plan.disable) >>
              patronColl.update.one($id(user.id), patron.removeStripe).void >>-
              notifier.onExpire(user) >>-
              logger.info(s"Unsubed ${user.username} $sub")
          }
      }
    }

  def getEvent = stripeClient.getEvent _

  def customerInfo(user: User, customer: StripeCustomer): Fu[Option[CustomerInfo]] =
    stripeClient.getNextInvoice(customer.id) zip
      customer.firstSubscription.??(stripeClient.getPaymentMethod) map {
        case (Some(nextInvoice), paymentMethod) =>
          customer.firstSubscription match {
            case Some(sub) => MonthlyCustomerInfo(sub, nextInvoice, paymentMethod).some
            case None =>
              logger.warn(s"Can't identify ${user.username} monthly subscription $customer")
              none
          }
        case (None, _) => OneTimeCustomerInfo(customer).some
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

  def setLifetime(user: User, gifter: Option[User]): Funit = {
    if (user.plan.isEmpty) Bus.publish(lila.hub.actorApi.plan.MonthInc(user.id, 0), "plan")
    userRepo.setPlan(
      user,
      user.plan.enable
    ) >> patronColl.update
      .one(
        $id(user.id),
        $set(
          "lastLevelUp" -> DateTime.now,
          "lifetime"    -> true,
          "free"        -> Patron.Free(DateTime.now, by = gifter.map(_.id))
        ),
        upsert = true
      )
      .void >>- lightUserApi.invalidate(user.id)
  }

  def freeMonth(user: User): Funit =
    patronColl.update
      .one(
        $id(user.id),
        $set(
          "lastLevelUp" -> DateTime.now,
          "lifetime"    -> false,
          "free"        -> Patron.Free(DateTime.now, by = none),
          "expiresAt"   -> DateTime.now.plusMonths(1)
        ),
        upsert = true
      )
      .void >> setDbUserPlanOnCharge(user, levelUp = false)

  def gift(from: User, to: User, money: Money): Funit =
    !to.isPatron ?? {
      for {
        isLifetime <- pricingApi isLifetime money
        patron     <- patronColl.byId[Patron](to.id)
        _ <- patronColl.update
          .one(
            $id(to.id),
            $set(
              "lastLevelUp" -> DateTime.now,
              "lifetime"    -> isLifetime,
              "free"        -> Patron.Free(DateTime.now, by = from.id.some),
              "expiresAt"   -> DateTime.now.plusMonths(1)
            ),
            upsert = true
          )
        plan = to.plan.incMonths
        _    = Bus.publish(lila.hub.actorApi.plan.MonthInc(to.id, plan.months), "plan")
        _    = notifier.onGift(from, to, isLifetime)
        _ <- setDbUserPlan(to, plan)
      } yield ()
    }

  def remove(user: User): Funit =
    userRepo.unsetPlan(user) >>
      patronColl.unsetField($id(user.id), "lifetime").void >>-
      lightUserApi.invalidate(user.id)

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
                GroupField("userId")("total" -> SumField("usd")),
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

  private def addCharge(charge: Charge, country: Option[Country]): Funit =
    monitorCharge(charge, country) >>
      chargeColl.insert.one(charge).void >>- {
        recentChargeUserIdsCache.invalidateUnit()
        monthlyGoalApi.get foreach { m =>
          Bus.publish(
            lila.hub.actorApi.plan.ChargeEvent(
              username = charge.userId.flatMap(lightUserApi.sync).fold("Anonymous")(_.name),
              cents = charge.usd.cents,
              percent = m.percent,
              DateTime.now
            ),
            "plan"
          )
          lila.mon.plan.goal.update(m.goal.cents)
          lila.mon.plan.current.update(m.current.cents)
          lila.mon.plan.percent.update(m.percent)
          if (charge.isPayPal) lila.mon.plan.paypal.record(charge.usd.cents)
          else if (charge.isStripe) lila.mon.plan.stripe.record(charge.usd.cents)
        }
      }

  private def monitorCharge(charge: Charge, country: Option[Country]): Funit = {
    lila.mon.plan.charge
      .countryCents(
        country = country.fold("unknown")(_.code),
        currency = charge.money.currency,
        service = charge.serviceName
      )
      .record(charge.usd.cents)
    charge.userId ?? { userId =>
      chargeColl.exists($doc("userId" -> userId)) map {
        case false => lila.mon.plan.charge.first(charge.serviceName).increment().unit
        case _     =>
      }
    }
  }

  private def setDbUserPlan(user: User, plan: lila.user.Plan): Funit =
    userRepo.setPlan(user, plan) >>- lightUserApi.invalidate(user.id)

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

  def makeCustomer(user: User, data: Checkout): Fu[StripeCustomer] =
    stripeClient.createCustomer(user, data) flatMap { customer =>
      saveStripeCustomer(user, customer.id) inject customer
    }

  def patronCustomer(patron: Patron): Fu[Option[StripeCustomer]] =
    patron.stripe.map(_.customerId) ?? stripeClient.getCustomer

  private def customerIdPatron(id: CustomerId): Fu[Option[Patron]] =
    patronColl.one[Patron]($doc("stripe.customerId" -> id))

  def userPatron(user: User): Fu[Option[Patron]] = patronColl.one[Patron]($id(user.id))

  def createSession(data: CreateStripeSession): Fu[StripeSession] =
    data.checkout.freq match {
      case Freq.Onetime => stripeClient.createOneTimeSession(data)
      case Freq.Monthly => stripeClient.createMonthlySession(data)
    }

  def createPaymentUpdateSession(sub: StripeSubscription, nextUrls: NextUrls): Fu[StripeSession] =
    stripeClient.createPaymentUpdateSession(sub, nextUrls)

  def updatePayment(sub: StripeSubscription, sessionId: String) =
    stripeClient.getSession(sessionId) flatMap {
      _ ?? { session =>
        stripeClient.setCustomerPaymentMethod(sub.customer, session.setup_intent.payment_method) zip
          stripeClient.setSubscriptionPaymentMethod(sub, session.setup_intent.payment_method) void
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
