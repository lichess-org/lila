package lila.plan

import play.api.i18n.Lang
import reactivemongo.api.*
import cats.syntax.all.*

import lila.common.config.Secret
import lila.common.{ Bus, IpAddress, EmailAddress }
import lila.db.dsl.{ *, given }
import lila.memo.CacheApi.*
import lila.user.{ Me, User, UserRepo }

final class PlanApi(
    stripeClient: StripeClient,
    payPalClient: PayPalClient,
    mongo: PlanMongo,
    notifier: PlanNotifier,
    userRepo: UserRepo,
    lightUserApi: lila.user.LightUserApi,
    cacheApi: lila.memo.CacheApi,
    mongoCache: lila.memo.MongoCache.Api,
    payPalIpnKey: Secret,
    monthlyGoalApi: MonthlyGoalApi,
    currencyApi: CurrencyApi,
    pricingApi: PlanPricingApi,
    ip2proxy: lila.security.Ip2Proxy
)(using Executor):

  import BsonHandlers.given
  import BsonHandlers.PatronHandlers.given
  import BsonHandlers.ChargeHandlers.given

  def switch(user: User, money: Money): Fu[StripeSubscription] =
    stripe.userCustomer(user) flatMap {
      case None => fufail(s"Can't switch non-existent customer ${user.id}")
      case Some(customer) =>
        customer.firstSubscription match
          case None => fufail(s"Can't switch non-existent subscription of ${user.id}")
          case Some(sub) if sub.item.price.money == money => fuccess(sub)
          case Some(sub)                                  => stripeClient.updateSubscription(sub, money)
    }

  def cancel(user: User): Funit =
    def onCancel =
      isLifetime(user).flatMap { lifetime =>
        !lifetime so setDbUserPlan(user.mapPlan(_.disable))
      } >>
        mongo.patron.update
          .one($id(user.id), $unset("stripe", "payPal", "payPalCheckout", "expiresAt"))
          .void >>-
        logger.info(s"Canceled subscription of ${user.username}")
    stripe.userCustomer(user) flatMap {
      case Some(customer) =>
        customer.firstSubscription match
          case None => fufail(s"Can't cancel non-existent subscription of ${user.id}")
          case Some(sub) =>
            stripeClient.cancelSubscription(sub) >> onCancel
      case None =>
        payPal.userSubscription(user) flatMap {
          case None      => fufail(s"Can't cancel non-existent customer ${user.id}")
          case Some(sub) => payPalClient.cancelSubscription(sub) >> onCancel
        }
    }

  object stripe:

    def getEvent = stripeClient.getEvent

    def onCharge(stripeCharge: StripeCharge): Funit = for {
      patronOption <- customerIdPatron(stripeCharge.customer)
      giftTo       <- stripeCharge.giftTo so userRepo.byId
      money = stripeCharge.amount toMoney stripeCharge.currency
      usd   <- currencyApi toUsd money
      proxy <- stripeCharge.ip.so(ip => ip2proxy(ip).dmap(some))
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
      _ <- patronOption match
        case None =>
          logger.info(s"Charged anon customer $charge")
          funit
        case Some(prevPatron) =>
          logger.info(s"Charged proxy:${proxy.flatMap(_.value)} $charge $prevPatron")
          userRepo byId prevPatron.userId orFail s"Missing user for $prevPatron" flatMap { user =>
            giftTo match
              case Some(to) => gift(user, to, money)
              case None =>
                stripeClient.getCustomer(stripeCharge.customer) flatMap { customer =>
                  val freq = if (customer.exists(_.renew)) Freq.Monthly else Freq.Onetime
                  val patron = prevPatron
                    .copy(lastLevelUp = prevPatron.lastLevelUp orElse nowInstant.some)
                    .levelUpIfPossible
                    .expireInOneMonth(freq == Freq.Onetime)
                  mongo.patron.update.one($id(prevPatron.id), patron) >>
                    setDbUserPlanOnCharge(user, prevPatron.canLevelUp) >> {
                      isLifetime so setLifetime(user)
                    }
                }
          }
    } yield ()

    def onSubscriptionDeleted(sub: StripeSubscription): Funit =
      customerIdPatron(sub.customer) flatMapz { patron =>
        if (patron.isLifetime) funit
        else
          userRepo byId patron.userId orFail s"Missing user for $patron" flatMap { user =>
            setDbUserPlan(user.mapPlan(_.disable)) >>
              mongo.patron.update.one($id(user.id), patron.removeStripe).void >>-
              notifier.onExpire(user) >>-
              logger.info(s"Unsubbed ${user.username} $sub")
          }
      }

    def customerInfo(user: User, customer: StripeCustomer): Fu[Option[CustomerInfo]] =
      stripeClient.getNextInvoice(customer.id) zip
        customer.firstSubscription.so(stripeClient.getPaymentMethod) map {
          case (Some(nextInvoice), paymentMethod) =>
            customer.firstSubscription match
              case Some(sub) => CustomerInfo.Monthly(sub, nextInvoice, paymentMethod).some
              case None =>
                logger.warn(s"Can't identify ${user.username} monthly subscription $customer")
                none
          case (None, _) => CustomerInfo.OneTime(customer).some
        }

    private def saveCustomer(user: User, customerId: StripeCustomerId): Funit =
      userPatron(user) flatMap { patronOpt =>
        val patron = patronOpt
          .getOrElse(Patron(_id = user.id))
          .copy(stripe = Patron.Stripe(customerId).some)
        mongo.patron.update.one($id(user.id), patron, upsert = true).void
      }

    def userCustomerId(user: User): Fu[Option[StripeCustomerId]] =
      userPatron(user) map {
        _.flatMap { _.stripe.map(_.customerId) }
      }

    def userCustomer(user: User): Fu[Option[StripeCustomer]] =
      userCustomerId(user) flatMapz stripeClient.getCustomer

    def makeCustomer(user: User, data: PlanCheckout): Fu[StripeCustomer] =
      stripeClient.createCustomer(user, data) flatMap { customer =>
        saveCustomer(user, customer.id) inject customer
      }

    def patronCustomer(patron: Patron): Fu[Option[StripeCustomer]] =
      patron.stripe.map(_.customerId) so stripeClient.getCustomer

    def createSession(data: CreateStripeSession)(using Lang)(using me: Me): Fu[StripeSession] =
      canUse(data.ip, data.checkout.freq) flatMap { can =>
        if can.yes then
          data.checkout.freq match
            case Freq.Onetime => stripeClient.createOneTimeSession(data)
            case Freq.Monthly => stripeClient.createMonthlySession(data)
        else
          logger.warn(s"${me.username} ${data.ip} ${data.customerId} can't use stripe for ${data.checkout}")
          fufail(StripeClient.CantUseException)
      }

    def createPaymentUpdateSession(sub: StripeSubscription, nextUrls: NextUrls): Fu[StripeSession] =
      stripeClient.createPaymentUpdateSession(sub, nextUrls)

    def updatePaymentMethod(sub: StripeSubscription, sessionId: String) =
      stripeClient.getSession(sessionId) flatMapz { session =>
        stripeClient.setCustomerPaymentMethod(sub.customer, session.setup_intent.payment_method) zip
          stripeClient.setSubscriptionPaymentMethod(sub, session.setup_intent.payment_method) void
      }

    def canUse(ip: IpAddress, freq: Freq)(using me: Me): Fu[StripeCanUse] = ip2proxy(ip) flatMap { proxy =>
      if (!proxy.is) fuccess(StripeCanUse.Yes)
      else
        val maxPerWeek = {
          val verifiedBonus  = me.isVerified so 50
          val nbGamesBonus   = math.sqrt(me.count.game / 50)
          val seniorityBonus = math.sqrt(daysBetween(me.createdAt, nowInstant) / 30d)
          verifiedBonus + nbGamesBonus + seniorityBonus
        }.toInt.atLeast(1).atMost(50)
        freq match
          case Freq.Monthly => // prevent several subscriptions in a row
            StripeCanUse from mongo.charge
              .countSel:
                $doc(
                  "userId" -> me.userId,
                  "date" $gt nowInstant.minusWeeks(1),
                  "stripe" $exists true,
                  "giftTo" $exists false
                )
              .map(_ < maxPerWeek)
          case Freq.Onetime => // prevents mass gifting or one-time donations
            StripeCanUse from mongo.charge
              .countSel:
                $doc("userId" -> me.userId, "date" $gt nowInstant.minusWeeks(1), "stripe" $exists true)
              .map(_ < maxPerWeek)
    }

    private def customerIdPatron(id: StripeCustomerId): Fu[Option[Patron]] =
      mongo.patron.one[Patron]($doc("stripe.customerId" -> id))
  end stripe

  object payPal:

    def getEvent = payPalClient.getEvent

    def onLegacyCharge(ipn: PlanForm.Ipn, ip: IpAddress, key: String): Funit = for {
      money      <- ipn.money.fold[Fu[Money]](fufail(s"Invalid paypal charge ${ipn.txnId}"))(fuccess)
      pricing    <- pricingApi pricingFor money.currency orFail s"Invalid paypal currency $money"
      usd        <- currencyApi toUsd money orFail s"Invalid paypal currency $money"
      isLifetime <- pricingApi isLifetime money
      giftTo     <- ipn.giftTo so userRepo.byId
      _ <-
        if (key != payPalIpnKey.value)
          logger.error(s"Invalid PayPal IPN key $key from $ip ${ipn.userId} $money")
          funit
        else if (!pricing.valid(money))
          logger.info(s"Ignoring invalid paypal amount from $ip ${ipn.userId} $money ${ipn.txnId}")
          funit
        else
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
            (ipn.userId so userRepo.byId) flatMapz { user =>
              giftTo match
                case Some(to) => gift(user, to, money)
                case None =>
                  val payPal =
                    Patron.PayPalLegacy(
                      ipn.email,
                      ipn.subId,
                      nowInstant
                    )
                  userPatron(user).flatMap {
                    case None =>
                      mongo.patron.insert.one(
                        Patron(
                          _id = user.id,
                          payPal = payPal.some,
                          lastLevelUp = Some(nowInstant)
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
                      mongo.patron.update.one($id(patron.id), p2) >>
                        setDbUserPlanOnCharge(user, patron.canLevelUp)
                  } >> {
                    isLifetime so setLifetime(user)
                  } >>- logger.info(s"Charged ${user.username} with paypal: $money")
            }
    } yield ()

    def userSubscriptionId(user: User): Fu[Option[PayPalSubscriptionId]] =
      userPatron(user) map {
        _.flatMap { _.payPalCheckout.flatMap(_.subscriptionId) }
      }

    def userSubscription(user: User): Fu[Option[PayPalSubscription]] =
      userSubscriptionId(user) flatMapz payPalClient.getSubscription

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
      giftTo     <- order.giftTo so userRepo.byId
      _ <-
        if (!pricing.valid(money))
          logger.info(s"Ignoring invalid paypal amount from $ip ${order.userId} $money ${orderId}")
          funit
        else
          val charge = Charge.make(
            userId = order.userId,
            giftTo = giftTo.map(_.id),
            payPalCheckout = Patron.PayPalCheckout(order.id, order.payer.id, none).some,
            money = money,
            usd = usd
          )
          addCharge(charge, order.country) >>
            (order.userId so userRepo.byId) flatMapz { user =>
              giftTo match
                case Some(to) => gift(user, to, money)
                case None =>
                  def newPayPalCheckout = Patron.PayPalCheckout(order.id, order.payer.id, none)
                  userPatron(user).flatMap {
                    case None =>
                      mongo.patron.insert.one(
                        Patron(
                          _id = user.id,
                          payPalCheckout = newPayPalCheckout.some,
                          lastLevelUp = Some(nowInstant)
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
                      mongo.patron.update.one($id(patron.id), p2) >>
                        setDbUserPlanOnCharge(user, patron.canLevelUp)
                  } >> {
                    isLifetime so setLifetime(user)
                  } >>- logger.info(s"Charged ${user.username} with paypal: $money")
            }
    } yield ()

    def captureSubscription(
        orderId: PayPalOrderId,
        subId: PayPalSubscriptionId,
        user: User,
        ip: IpAddress
    ) = for
      order <- payPalClient.getOrder(orderId) orFail s"Missing paypal order for id $orderId"
      sub   <- payPalClient.getSubscription(subId) orFail s"Missing paypal subscription for order $order"
      money = sub.capturedMoney
      pricing <- pricingApi pricingFor money.currency orFail s"Invalid paypal currency $money"
      usd     <- currencyApi toUsd money orFail s"Invalid paypal currency $money"
      _ <-
        if (!pricing.valid(money))
          logger.info(s"Ignoring invalid paypal amount from $ip ${order.userId} $money $orderId")
          funit
        else
          val charge = Charge.make(
            userId = user.id.some,
            giftTo = None,
            payPalCheckout = Patron.PayPalCheckout(order.id, order.payer.id, sub.id.some).some,
            money = money,
            usd = usd
          )
          addSubscriptionCharge(charge, user, order.country)
    yield ()

    def subscriptionUser(id: PayPalSubscriptionId): Fu[Option[User]] =
      subscriptionIdPatron(id) flatMap { _.map(_.id) so userRepo.byId }

    // only used for automatically renewing subscription charges
    def onCaptureCompleted(capture: PayPalCapture) =
      capture.subscriptionId.map { subId =>
        for
          user <- userRepo.byId(capture.userId) orFail s"Missing user for paypal capture $capture"
          // look for previous charge
          previous <- mongo.charge
            // hit the userId index
            .find($doc("userId" -> user.id, "payPalCheckout.subscriptionId" -> subId))
            .sort($doc("date" -> -1))
            .one[Charge]
            // avoid duplicating the initial charge
            .map(_.filter(_.date.isBefore(nowInstant.minusMinutes(3))))
          _ <- previous so { prev =>
            logger.info(s"Renewing paypal checkout subscription with $capture")
            addSubscriptionCharge(prev.copyAsNew, user, none)
          }
        yield ()
      } | funit

    private def addSubscriptionCharge(charge: Charge, user: User, country: Option[Country]) = for
      _ <- addCharge(charge, country)
      _ <- userPatron(user).flatMap {
        case None =>
          mongo.patron.insert.one(
            Patron(
              _id = user.id,
              payPalCheckout = charge.payPalCheckout,
              lastLevelUp = Some(nowInstant)
            ).expireInOneMonth
          ) >>
            setDbUserPlanOnCharge(user, levelUp = false)
        case Some(patron) =>
          val p2 = patron
            .copy(
              payPalCheckout = charge.payPalCheckout,
              stripe = none,
              free = none
            )
            .levelUpIfPossible
            .expireInOneMonth
          mongo.patron.update.one($id(patron.id), p2) >>
            setDbUserPlanOnCharge(user, patron.canLevelUp)
      }
      isLifetime <- pricingApi isLifetime charge.money
      _          <- isLifetime so setLifetime(user)
    yield logger.info(s"Charged ${user.username} with paypal checkout: $charge")

    private def subscriptionIdPatron(id: PayPalSubscriptionId): Fu[Option[Patron]] =
      mongo.patron.one[Patron]($doc("payPalCheckout.subscriptionId" -> id))
  end payPal

  private def setDbUserPlanOnCharge(from: User, levelUp: Boolean): Funit =
    val user = from.mapPlan(p => if (levelUp) p.incMonths else p.enable)
    notifier.onCharge(user)
    setDbUserPlan(user)

  import PlanApi.SyncResult.{ ReloadUser, Synced }

  def sync(user: User): Fu[PlanApi.SyncResult] =
    userPatron(user) flatMap {

      case None if user.plan.active =>
        logger.warn(s"${user.username} sync: disable plan of non-patron")
        setDbUserPlan(user.mapPlan(_.disable)) inject ReloadUser

      case None => fuccess(Synced(none, none, none))

      case Some(patron) =>
        (patron.stripe, patron.payPalCheckout, patron.payPal) match

          case (Some(stripe), _, _) =>
            stripeClient.getCustomer(stripe.customerId) flatMap {
              case None =>
                logger.warn(s"${user.username} sync: unset DB patron that's not in stripe")
                mongo.patron.update.one($id(patron.id), patron.removeStripe) >> sync(user)
              case Some(customer) if customer.firstSubscription.exists(_.isActive) && !user.plan.active =>
                logger.warn(s"${user.username} sync: enable plan of customer with a stripe subscription")
                setDbUserPlan(user.mapPlan(_.enable)) inject ReloadUser
              case customer => fuccess(Synced(patron.some, customer, none))
            }

          case (_, Some(Patron.PayPalCheckout(_, _, Some(subId))), _) =>
            payPalClient.getSubscription(subId) flatMap {
              case None =>
                logger.warn(s"${user.username} sync: unset DB patron that's not in paypal")
                mongo.patron.update.one($id(patron.id), patron.removePayPalCheckout) >> sync(user)
              case Some(subscription) if subscription.isActive && !user.plan.active =>
                logger.warn(s"${user.username} sync: enable plan of customer with a payPal subscription")
                setDbUserPlan(user.mapPlan(_.enable)) inject ReloadUser
              case subscription => fuccess(Synced(patron.some, none, subscription))
            }

          case (_, _, Some(_)) =>
            if (!user.plan.active)
              logger.warn(s"${user.username} sync: enable plan of customer with paypal")
              setDbUserPlan(user.mapPlan(_.enable)) inject ReloadUser
            else fuccess(Synced(patron.some, none, none))

          case (None, None, None) if patron.isLifetime => fuccess(Synced(patron.some, none, none))

          case (None, None, None) if user.plan.active && patron.free.isEmpty =>
            logger.warn(s"${user.username} sync: disable plan of patron with no paypal or stripe")
            setDbUserPlan(user.mapPlan(_.disable)) inject ReloadUser

          case _ => fuccess(Synced(patron.some, none, none))
    }

  def isLifetime(user: User): Fu[Boolean] =
    userPatron(user) map {
      _.exists(_.isLifetime)
    }

  def setLifetime(user: User): Funit =
    if (user.plan.isEmpty) Bus.publish(lila.hub.actorApi.plan.MonthInc(user.id, 0), "plan")
    userRepo.setPlan(
      user,
      user.plan.enable
    ) >> mongo.patron.update
      .one(
        $id(user.id),
        $set(
          "lastLevelUp" -> nowInstant,
          "lifetime"    -> true,
          "free"        -> Patron.Free(nowInstant, by = none)
        ),
        upsert = true
      )
      .void >>- lightUserApi.invalidate(user.id)

  def freeMonth(user: User): Funit =
    mongo.patron.update
      .one(
        $id(user.id),
        $set(
          "lastLevelUp" -> nowInstant,
          "lifetime"    -> false,
          "free"        -> Patron.Free(nowInstant, by = none),
          "expiresAt"   -> nowInstant.plusMonths(1)
        ),
        upsert = true
      )
      .void >> setDbUserPlanOnCharge(user, levelUp = false)

  def gift(from: User, to: User, money: Money): Funit =
    for {
      toPatronOpt <- userPatron(to)
      isLifetime  <- fuccess(toPatronOpt.exists(_.isLifetime)) >>| (pricingApi isLifetime money)
      _ <- mongo.patron.update
        .one(
          $id(to.id),
          $set(
            "lastLevelUp" -> nowInstant,
            "lifetime"    -> isLifetime,
            "free"        -> Patron.Free(nowInstant, by = from.id.some),
            "expiresAt"   -> (!isLifetime option nowInstant.plusMonths(1))
          ),
          upsert = true
        )
      newTo = to.mapPlan(p => if (toPatronOpt.exists(_.canLevelUp)) p.incMonths else p.enable)
      _ <- setDbUserPlan(newTo)
    } yield notifier.onGift(from, newTo, isLifetime)

  def recentGiftFrom(from: User): Fu[Option[Patron]] =
    mongo.patron
      .find(
        $doc(
          "free.by" -> from.id,
          "free.at" $gt nowInstant.minusMinutes(2)
        )
      )
      .sort($sort desc "free.at")
      .one[Patron]

  def remove(user: User): Funit =
    userRepo.unsetPlan(user) >>
      mongo.patron.unsetField($id(user.id), "lifetime").void >>-
      lightUserApi.invalidate(user.id)

  private val recentChargeUserIdsNb = 100
  private val recentChargeUserIdsCache = cacheApi.unit[List[UserId]] {
    _.refreshAfterWrite(30 minutes)
      .buildAsyncFuture { _ =>
        mongo.charge.primitive[UserId](
          $empty,
          sort = $doc("date" -> -1),
          nb = recentChargeUserIdsNb * 3 / 2,
          "userId"
        ) flatMap filterUserIds dmap (_ take recentChargeUserIdsNb)
      }
  }

  def recentChargeUserIds: Fu[List[UserId]] = recentChargeUserIdsCache.getUnit

  def recentChargesOf(user: User): Fu[List[Charge]] =
    mongo.charge
      .find(
        $or(
          $doc("userId" -> user.id),
          $doc("giftTo" -> user.id)
        )
      )
      .sort($doc("date" -> -1))
      .cursor[Charge]()
      .list(200)

  def giftsFrom(user: User): Fu[List[Charge.Gift]] =
    mongo.charge
      .find($doc("userId" -> user.id, "giftTo" $exists true))
      .sort($doc("date" -> -1))
      .cursor[Charge]()
      .list(200)
      .map(_.flatMap(_.toGift))

  private[plan] def onEmailChange(userId: UserId, email: EmailAddress): Funit =
    userRepo enabledById userId flatMapz { user =>
      stripe.userCustomer(user) flatMap {
        _.filterNot(_.email.has(email.value)) so {
          stripeClient.setCustomerEmail(_, email)
        }
      }
    }

  private val topPatronUserIdsNb = 300
  private val topPatronUserIdsCache = mongoCache.unit[List[UserId]](
    "patron:top",
    59 minutes
  ) { loader =>
    _.refreshAfterWrite(60 minutes)
      .buildAsyncFuture {
        loader { _ =>
          mongo.charge
            .aggregateList(
              maxDocs = topPatronUserIdsNb * 2,
              readPreference = ReadPreference.secondaryPreferred
            ) { framework =>
              import framework.*
              Match($doc("userId" $exists true)) -> List(
                GroupField("userId")("total" -> SumField("usd")),
                Sort(Descending("total")),
                Limit(topPatronUserIdsNb * 3 / 2)
              )
            }
            .dmap {
              _.flatMap { _.getAsOpt[UserId]("_id") }
            } flatMap filterUserIds dmap (_ take topPatronUserIdsNb)
        }
      }
  }

  def topPatronUserIds: Fu[List[UserId]] = topPatronUserIdsCache.get {}

  private def filterUserIds(ids: List[UserId]): Fu[List[UserId]] =
    val dedup = ids.distinct
    userRepo.filterByEnabledPatrons(dedup) map { enableds =>
      dedup filter enableds.contains
    }

  private def addCharge(charge: Charge, country: Option[Country]): Funit =
    mongo.charge.insert.one(charge).void >>- {
      recentChargeUserIdsCache.invalidateUnit()
    } >> monitorCharge(charge, country)

  private def monitorCharge(charge: Charge, country: Option[Country]): Funit =
    lila.mon.plan.charge
      .countryCents(
        country = country.fold("unknown")(_.value),
        currency = charge.money.currency,
        service = charge.serviceName,
        gift = charge.giftTo.isDefined
      )
      .record(charge.usd.cents)
    charge.userId.so { userId =>
      mongo.charge.countSel($doc("userId" -> userId)) map {
        case 1 => lila.mon.plan.charge.first(charge.serviceName).increment().unit
        case _ =>
      }
    } >>
      monthlyGoalApi.get.map { m =>
        Bus.publish(
          lila.hub.actorApi.plan.ChargeEvent(
            username = charge.userId.map(lightUserApi.syncFallback).fold(UserName("Anonymous"))(_.name),
            cents = charge.usd.cents,
            percent = m.percent,
            nowInstant
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

  private def setDbUserPlan(user: User): Funit =
    userRepo.setPlan(user, user.plan) >>- lightUserApi.invalidate(user.id)

  def userPatron(user: User): Fu[Option[Patron]] = mongo.patron.one[Patron]($id(user.id))

object PlanApi:

  enum SyncResult:
    case ReloadUser
    case Synced(
        patron: Option[Patron],
        stripeCustomer: Option[StripeCustomer],
        payPalSubscription: Option[PayPalSubscription]
    )
