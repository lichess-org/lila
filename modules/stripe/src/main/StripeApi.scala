package lila.stripe

import lila.db.dsl._
import lila.user.{ User, UserRepo }

import org.joda.time.DateTime

final class StripeApi(
    client: StripeClient,
    customerColl: Coll,
    bus: lila.common.Bus) {

  import Customer.BSONHandlers._

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
    customerColl.uno[Customer]($id(charge.customer)).flatMap {
      case None => fufail(s"Charged unknown customer $charge")
      case Some(cus) if cus.canLevelUp =>
        UserRepo byId cus.userId.value flatten s"Missing user for $cus" flatMap { user =>
          customerColl.updateField($id(cus.id), "lastLevelUp", DateTime.now) >>
            UserRepo.setPlan(user, user.plan.incMonths) >>- {
              logger.info(s"Charged ${charge} ${cus}")
              lila.mon.stripe.amount(charge.amount)
              bus.publish(lila.hub.actorApi.stripe.ChargeEvent(
                username = user.username,
                amount = charge.amount), 'stripe)
            }
        }
      case Some(cus) => fufail(s"Too early to level up $charge $cus")
    }

  def onSubscriptionDeleted(sub: StripeSubscription): Funit =
    customerColl.uno[Customer]($id(sub.customer)).flatMap {
      case None => fufail(s"Deleted subscription of unknown customer $sub")
      case Some(cus) =>
        UserRepo byId cus.userId.value flatten s"Missing user for $cus" flatMap { user =>
          UserRepo.setPlan(user, user.plan.disable) >>-
            logger.info(s"Unsubed ${user.id} ${sub}")
        }
    }

  def getEvent = client.getEvent _

  def customerInfo(user: User): Fu[Option[CustomerInfo]] =
    customerColl.uno[Customer]($doc("userId" -> user.id)) flatMap {
      _ ?? { c =>
        client.getCustomer(c.id) zip client.getNextInvoice(c.id) zip client.getPastInvoices(c.id) map {
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

  def sync(user: User): Fu[Boolean] =
    customerColl.uno[Customer]($doc("userId" -> user.id)) flatMap {
      case None if !user.plan.isEmpty =>
        logger.warn(s"sync: disable plan of non-customer")
        UserRepo.setPlan(user, user.plan.disable) inject true
      case Some(c) => client.getCustomer(c.id) flatMap {
        case None =>
          logger.warn(s"sync: remove DB customer that's not in stripe")
          customerColl.remove($id(c.id)) >> UserRepo.setPlan(user, user.plan.disable) inject true
        case Some(customer) if customer.firstSubscription.isEmpty && user.plan.active =>
          logger.warn(s"sync: disable plan of customer without a subscription")
          UserRepo.setPlan(user, user.plan.disable) inject true
        case Some(customer) if customer.firstSubscription.isDefined && !user.plan.active =>
          logger.warn(s"sync: enable plan of customer with a subscription")
          UserRepo.setPlan(user, user.plan.enable) inject true
        case _ => fuccess(false)
      }
      case _ => fuccess(false)
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
      customerColl.insert(Customer(
        _id = Customer.Id(customer.id),
        userId = Customer.UserId(user.id),
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

  private def userCustomer(user: User): Fu[Option[StripeCustomer]] =
    customerColl.primitiveOne[Customer.Id]($doc("userId" -> user.id), "_id") flatMap {
      _ ?? client.getCustomer
    }
}
