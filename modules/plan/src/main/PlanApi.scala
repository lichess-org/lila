package lila.plan

import com.softwaremill.tagging._
import org.joda.time.DateTime
import play.api.i18n.Lang
import reactivemongo.api._
import scala.concurrent.duration._

import lila.common.config.Secret
import lila.common.{ Bus, IpAddress }
import lila.db.dsl._
import lila.memo.CacheApi._
import lila.user.{ User, UserRepo }

final class PlanApi(
    stripeClient: StripeClient,
    payPalClient: PayPalClient,
    patronColl: Coll @@ PatronColl,
    chargeColl: Coll @@ ChargeColl,
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
    stripe.userCustomer(user) flatMap {
      case None => fufail(s"Can't switch non-existent customer ${user.id}")
      case Some(customer) =>
        customer.firstSubscription match {
          case None => fufail(s"Can't switch non-existent subscription of ${user.id}")
          case Some(sub) if sub.item.price.money == money => fuccess(sub)
          case Some(sub)                                  => stripeClient.updateSubscription(sub, money)
        }
    }

  def cancel(user: User): Funit = {
    def onCancel =
      isLifetime(user).flatMap { lifetime =>
        !lifetime ?? setDbUserPlan(user.mapPlan(_.disable))
      } >>
        patronColl.update
          .one($id(user.id), $unset("stripe", "payPal", "payPalCheckout", "expiresAt"))
          .void >>-
        logger.info(s"Canceled subscription of ${user.username}")
    stripe.userCustomer(user) flatMap {
      case Some(customer) =>
        customer.firstSubscription match {
          case None => fufail(s"Can't cancel non-existent subscription of ${user.id}")
          case Some(sub) =>
            stripeClient.cancelSubscription(sub) >> onCancel
        }
      case None =>
        payPal.userSubscription(user) flatMap {
          case None      => fufail(s"Can't cancel non-existent customer ${user.id}")
          case Some(sub) => payPalClient.cancelSubscription(sub) >> onCancel
        }
    }
  }

  object stripe {

    def getEvent = stripeClient.getEvent _

    def onCharge(stripeCharge: StripeCharge): Funit = for {
      patronOption <- customerIdPatron(stripeCharge.customer)
      giftTo       <- stripeCharge.giftTo ?? userRepo.named
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
      isLifetime <- pricingApi isLifetime money
      _          <- addCharge(charge, stripeCharge.country)
      _ <- patronOption match {
        case None =>
          logger.info(s"Charged anon customer $charge")
          funit
        case Some(prevPatron) =>
          logger.info(s"Charged $charge $prevPatron")
          userRepo byId prevPatron.userId orFail s"Missing user for $prevPatron" flatMap { user =>
            giftTo match {
              case Some(to) => gift(user, to, money)
              case None =>
                stripeClient.getCustomer(stripeCharge.customer) flatMap { customer =>
                  val freq = if (customer.exists(_.renew)) Freq.Monthly else Freq.Onetime
                  val patron = prevPatron
                    .copy(lastLevelUp = prevPatron.lastLevelUp orElse DateTime.now.some)
                    .levelUpIfPossible
                    .expireInOneMonth(freq == Freq.Onetime)
                  patronColl.update.one($id(prevPatron.id), patron) >>
                    setDbUserPlanOnCharge(user, prevPatron.canLevelUp) >> {
                      isLifetime ?? setLifetime(user)
                    }
                }
            }
          }
      }
    } yield ()

    def onSubscriptionDeleted(sub: StripeSubscription): Funit =
      customerIdPatron(sub.customer) flatMap {
        _ ?? { patron =>
          if (patron.isLifetime) funit
          else
            userRepo byId patron.userId orFail s"Missing user for $patron" flatMap { user =>
              setDbUserPlan(user.mapPlan(_.disable)) >>
                patronColl.update.one($id(user.id), patron.removeStripe).void >>-
                notifier.onExpire(user) >>-
                logger.info(s"Unsubed ${user.username} $sub")
            }
        }
      }

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

    private def saveCustomer(user: User, customerId: StripeCustomerId): Funit =
      userPatron(user) flatMap { patronOpt =>
        val patron = patronOpt
          .getOrElse(Patron(_id = Patron.UserId(user.id)))
          .copy(stripe = Patron.Stripe(customerId).some)
        patronColl.update.one($id(user.id), patron, upsert = true).void
      }

    def userCustomerId(user: User): Fu[Option[StripeCustomerId]] =
      userPatron(user) map {
        _.flatMap { _.stripe.map(_.customerId) }
      }

    def userCustomer(user: User): Fu[Option[StripeCustomer]] =
      userCustomerId(user) flatMap {
        _ ?? stripeClient.getCustomer
      }

    def makeCustomer(user: User, data: PlanCheckout): Fu[StripeCustomer] =
      stripeClient.createCustomer(user, data) flatMap { customer =>
        saveCustomer(user, customer.id) inject customer
      }

    def patronCustomer(patron: Patron): Fu[Option[StripeCustomer]] =
      patron.stripe.map(_.customerId) ?? stripeClient.getCustomer

    def createSession(data: CreateStripeSession)(implicit lang: Lang): Fu[StripeSession] =
      data.checkout.freq match {
        case Freq.Onetime => stripeClient.createOneTimeSession(data)
        case Freq.Monthly => stripeClient.createMonthlySession(data)
      }

    def createPaymentUpdateSession(sub: StripeSubscription, nextUrls: NextUrls): Fu[StripeSession] =
      stripeClient.createPaymentUpdateSession(sub, nextUrls)

    def updatePaymentMethod(sub: StripeSubscription, sessionId: String) =
      stripeClient.getSession(sessionId) flatMap {
        _ ?? { session =>
          stripeClient.setCustomerPaymentMethod(sub.customer, session.setup_intent.payment_method) zip
            stripeClient.setSubscriptionPaymentMethod(sub, session.setup_intent.payment_method) void
        }
      }

    private def customerIdPatron(id: StripeCustomerId): Fu[Option[Patron]] =
      patronColl.one[Patron]($doc("stripe.customerId" -> id))
  }

  object payPal {

    def getEvent = payPalClient.getEvent _

    def onLegacyCharge(ipn: PlanForm.Ipn, ip: IpAddress, key: String): Funit = for {
      money      <- ipn.money.fold[Fu[Money]](fufail(s"Invalid paypal charge ${ipn.txnId}"))(fuccess)
      pricing    <- pricingApi pricingFor money.currency orFail s"Invalid paypal currency $money"
      usd        <- currencyApi toUsd money orFail s"Invalid paypal currency $money"
      isLifetime <- pricingApi isLifetime money
      giftTo     <- ipn.giftTo ?? userRepo.byId
      _ <-
        if (key != payPalIpnKey.value) {
          logger.error(s"Invalid PayPal IPN key $key from $ip ${ipn.userId} $money")
          funit
        } else if (!pricing.valid(money)) {
          logger.info(s"Ignoring invalid paypal amount from $ip ${ipn.userId} $money ${ipn.txnId}")
          funit
        } else {
          val charge = Charge.make(
            userId = ipn.userId,
            giftTo = giftTo.map(_.id),
            payPal = Charge
              .PayPalLegacy(
                name = ipn.name,
                email = ipn.email,
                txnId = ipn.txnId,
                subId = ipn.subId,
                ip = ip.value.some
              )
              .some,
            money = money,
            usd = usd
          )
          addCharge(charge, ipn.country) >>
            (ipn.userId ?? userRepo.named) flatMap {
              _ ?? { user =>
                giftTo match {
                  case Some(to) => gift(user, to, money)
                  case None =>
                    val payPal =
                      Patron.PayPalLegacy(
                        ipn.email map Patron.PayPalLegacy.Email,
                        ipn.subId map Patron.PayPalLegacy.SubId,
                        DateTime.now
                      )
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
                      isLifetime ?? setLifetime(user)
                    } >>- logger.info(s"Charged ${user.username} with paypal: $money")
                }
              }
            }
        }
    } yield ()

    def userSubscriptionId(user: User): Fu[Option[PayPalSubscriptionId]] =
      userPatron(user) map {
        _.flatMap { _.payPalCheckout.flatMap(_.subscriptionId) }
      }

    def userSubscription(user: User): Fu[Option[PayPalSubscription]] =
      userSubscriptionId(user) flatMap {
        _ ?? payPalClient.getSubscription
      }

    def createOrder(checkout: PlanCheckout, user: User, giftTo: Option[lila.user.User]) =
      for {
        isLifetime <- pricingApi.isLifetime(checkout.money)
        order      <- payPalClient.createOrder(CreatePayPalOrder(checkout, user, giftTo, isLifetime))
      } yield order

    def createSubscription(checkout: PlanCheckout, user: User) =
      payPalClient.createSubscription(checkout, user)

    def captureOrder(orderId: PayPalOrderId, ip: IpAddress) = for {
      order      <- payPalClient.captureOrder(orderId)
      money      <- order.capturedMoney.fold[Fu[Money]](fufail(s"Invalid paypal capture $order"))(fuccess)
      pricing    <- pricingApi pricingFor money.currency orFail s"Invalid paypal currency $money"
      usd        <- currencyApi toUsd money orFail s"Invalid paypal currency $money"
      isLifetime <- pricingApi isLifetime money
      giftTo     <- order.giftTo ?? userRepo.named
      _ <-
        if (!pricing.valid(money)) {
          logger.info(s"Ignoring invalid paypal amount from $ip ${order.userId} $money ${orderId}")
          funit
        } else {
          val charge = Charge.make(
            userId = order.userId,
            giftTo = giftTo.map(_.id),
            payPalCheckout = Charge.PayPalCheckout(order.id, order.payer.id, none).some,
            money = money,
            usd = usd
          )
          addCharge(charge, order.country) >>
            (order.userId ?? userRepo.named) flatMap {
              _ ?? { user =>
                giftTo match {
                  case Some(to) => gift(user, to, money)
                  case None =>
                    def newPayPalCheckout = Patron.PayPalCheckout(order.payer.id, none)
                    userPatron(user).flatMap {
                      case None =>
                        patronColl.insert.one(
                          Patron(
                            _id = Patron.UserId(user.id),
                            payPalCheckout = newPayPalCheckout.some,
                            lastLevelUp = Some(DateTime.now)
                          ).expireInOneMonth
                        ) >>
                          setDbUserPlanOnCharge(user, levelUp = false)
                      case Some(patron) =>
                        val p2 = patron
                          .copy(
                            payPalCheckout = patron.payPalCheckout orElse newPayPalCheckout.some,
                            free = none
                          )
                          .levelUpIfPossible
                          .expireInOneMonth
                        patronColl.update.one($id(patron.id), p2) >>
                          setDbUserPlanOnCharge(user, patron.canLevelUp)
                    } >> {
                      isLifetime ?? setLifetime(user)
                    } >>- logger.info(s"Charged ${user.username} with paypal: $money")
                }
              }
            }
        }
    } yield ()

    def captureSubscription(
        orderId: PayPalOrderId,
        subId: PayPalSubscriptionId,
        user: User,
        ip: IpAddress
    ) = for {
      order <- payPalClient.getOrder(orderId) orFail s"Missing paypal order for id $orderId"
      sub   <- payPalClient.getSubscription(subId) orFail s"Missing paypal subscription for order $order"
      money = sub.capturedMoney
      pricing    <- pricingApi pricingFor money.currency orFail s"Invalid paypal currency $money"
      usd        <- currencyApi toUsd money orFail s"Invalid paypal currency $money"
      isLifetime <- pricingApi isLifetime money
      _ <-
        if (!pricing.valid(money)) {
          logger.info(s"Ignoring invalid paypal amount from $ip ${order.userId} $money $orderId")
          funit
        } else {
          val charge = Charge.make(
            userId = user.id.some,
            giftTo = None,
            payPalCheckout = Charge.PayPalCheckout(order.id, order.payer.id, sub.id.some).some,
            money = money,
            usd = usd
          )
          addCharge(charge, order.country) >> {
            val payPalCheckout = Patron.PayPalCheckout(order.payer.id, subId.some)
            userPatron(user).flatMap {
              case None =>
                patronColl.insert.one(
                  Patron(
                    _id = Patron.UserId(user.id),
                    payPalCheckout = payPalCheckout.some,
                    lastLevelUp = Some(DateTime.now)
                  ).expireInOneMonth
                ) >>
                  setDbUserPlanOnCharge(user, levelUp = false)
              case Some(patron) =>
                val p2 = patron
                  .copy(
                    payPalCheckout = payPalCheckout.some,
                    stripe = none,
                    free = none
                  )
                  .levelUpIfPossible
                  .expireInOneMonth
                patronColl.update.one($id(patron.id), p2) >>
                  setDbUserPlanOnCharge(user, patron.canLevelUp)
            } >> {
              isLifetime ?? setLifetime(user)
            } >>- logger.info(s"Charged ${user.username} with paypal checkout: $money")
          }
        }
    } yield ()

    def subscriptionUser(id: PayPalSubscriptionId): Fu[Option[User]] =
      subscriptionIdPatron(id) flatMap { _.map(_.id.value) ?? userRepo.byId }

    def onCaptureCompleted(capture: PayPalCapture) =
      logger.info(s"Charged $capture")

    private def subscriptionIdPatron(id: PayPalSubscriptionId): Fu[Option[Patron]] =
      patronColl.one[Patron]($doc("payPalCheckout.subscriptionId" -> id))
  }

  private def setDbUserPlanOnCharge(from: User, levelUp: Boolean): Funit = {
    val user = from.mapPlan(p => if (levelUp) p.incMonths else p.enable)
    notifier.onCharge(user)
    setDbUserPlan(user)
  }

  import PlanApi.SyncResult.{ ReloadUser, Synced }

  def sync(user: User): Fu[PlanApi.SyncResult] =
    userPatron(user) flatMap {

      case None if user.plan.active =>
        logger.warn(s"${user.username} sync: disable plan of non-patron")
        setDbUserPlan(user.mapPlan(_.disable)) inject ReloadUser

      case None => fuccess(Synced(none, none, none))

      case Some(patron) =>
        (patron.stripe, patron.payPalCheckout, patron.payPal) match {

          case (Some(stripe), _, _) =>
            stripeClient.getCustomer(stripe.customerId) flatMap {
              case None =>
                logger.warn(s"${user.username} sync: unset DB patron that's not in stripe")
                patronColl.update.one($id(patron.id), patron.removeStripe) >> sync(user)
              case Some(customer) if customer.firstSubscription.exists(_.isActive) && !user.plan.active =>
                logger.warn(s"${user.username} sync: enable plan of customer with a stripe subscription")
                setDbUserPlan(user.mapPlan(_.enable)) inject ReloadUser
              case customer => fuccess(Synced(patron.some, customer, none))
            }

          case (_, Some(Patron.PayPalCheckout(_, Some(subId))), _) =>
            payPalClient.getSubscription(subId) flatMap {
              case None =>
                logger.warn(s"${user.username} sync: unset DB patron that's not in paypal")
                patronColl.update.one($id(patron.id), patron.removePayPalCheckout) >> sync(user)
              case Some(subscription) if subscription.isActive && !user.plan.active =>
                logger.warn(s"${user.username} sync: enable plan of customer with a payPal subscription")
                setDbUserPlan(user.mapPlan(_.enable)) inject ReloadUser
              case subscription => fuccess(Synced(patron.some, none, subscription))
            }

          case (_, _, Some(_)) =>
            if (!user.plan.active) {
              logger.warn(s"${user.username} sync: enable plan of customer with paypal")
              setDbUserPlan(user.mapPlan(_.enable)) inject ReloadUser
            } else fuccess(Synced(patron.some, none, none))

          case (None, None, None) if patron.isLifetime => fuccess(Synced(patron.some, none, none))

          case (None, None, None) if user.plan.active && patron.free.isEmpty =>
            logger.warn(s"${user.username} sync: disable plan of patron with no paypal or stripe")
            setDbUserPlan(user.mapPlan(_.disable)) inject ReloadUser

          case _ => fuccess(Synced(patron.some, none, none))
        }
    }

  def isLifetime(user: User): Fu[Boolean] =
    userPatron(user) map {
      _.exists(_.isLifetime)
    }

  def setLifetime(user: User): Funit = {
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
          "free"        -> Patron.Free(DateTime.now, by = none)
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
    for {
      toPatronOpt <- userPatron(to)
      isLifetime  <- fuccess(toPatronOpt.exists(_.isLifetime)) >>| (pricingApi isLifetime money)
      _ <- patronColl.update
        .one(
          $id(to.id),
          $set(
            "lastLevelUp" -> DateTime.now,
            "lifetime"    -> isLifetime,
            "free"        -> Patron.Free(DateTime.now, by = from.id.some),
            "expiresAt"   -> (!isLifetime option DateTime.now.plusMonths(1))
          ),
          upsert = true
        )
      newTo = to.mapPlan(p => if (toPatronOpt.exists(_.canLevelUp)) p.incMonths else p.enable)
      _ <- setDbUserPlan(newTo)
    } yield {
      notifier.onGift(from, newTo, isLifetime)
    }

  def recentGiftFrom(from: User): Fu[Option[Patron]] =
    patronColl
      .find(
        $doc(
          "free.by" -> from.id,
          "free.at" $gt DateTime.now.minusMinutes(2)
        )
      )
      .sort($sort desc "free.at")
      .one[Patron]

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
    chargeColl
      .find(
        $or(
          $doc("userId" -> user.id),
          $doc("giftTo" -> user.id)
        )
      )
      .sort($doc("date" -> -1))
      .cursor[Charge]()
      .list()

  def giftsFrom(user: User): Fu[List[Charge.Gift]] =
    chargeColl
      .find($doc("userId" -> user.id, "giftTo" $exists true))
      .sort($doc("date" -> -1))
      .cursor[Charge]()
      .list()
      .map(_.flatMap(_.toGift))

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
    chargeColl.insert.one(charge).void >>- {
      recentChargeUserIdsCache.invalidateUnit()
    } >> monitorCharge(charge, country)

  private def monitorCharge(charge: Charge, country: Option[Country]): Funit = {
    lila.mon.plan.charge
      .countryCents(
        country = country.fold("unknown")(_.code),
        currency = charge.money.currency,
        service = charge.serviceName,
        gift = charge.giftTo.isDefined
      )
      .record(charge.usd.cents)
    charge.userId.?? { userId =>
      chargeColl.countSel($doc("userId" -> userId)) map {
        case 1 => lila.mon.plan.charge.first(charge.serviceName).increment().unit
        case _ =>
      }
    } >>
      monthlyGoalApi.get.map { m =>
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
        if (charge.isPayPalLegacy) lila.mon.plan.paypalLegacy.amount.record(charge.usd.cents)
        else if (charge.isPayPalCheckout) lila.mon.plan.paypalCheckout.amount.record(charge.usd.cents)
        else if (charge.isStripe) lila.mon.plan.stripe.record(charge.usd.cents)
      }.void
  }

  private def setDbUserPlan(user: User): Funit =
    userRepo.setPlan(user, user.plan) >>- lightUserApi.invalidate(user.id)

  def userPatron(user: User): Fu[Option[Patron]] = patronColl.one[Patron]($id(user.id))
}

object PlanApi {

  sealed trait SyncResult
  object SyncResult {
    case object ReloadUser extends SyncResult
    case class Synced(
        patron: Option[Patron],
        stripeCustomer: Option[StripeCustomer],
        payPalSubscription: Option[PayPalSubscription]
    ) extends SyncResult
  }
}
