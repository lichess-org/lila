package lila.stripe

import lila.db.dsl._
import lila.user.{ User, UserRepo }

import org.joda.time.DateTime

final class StripeApi(
    client: StripeClient,
    customerColl: Coll) {

  import Customer.BSONHandlers._

  def checkout(user: User, data: Checkout): Fu[StripeSubscription] =
    LichessPlan findUnder data.cents match {
      case None       => fufail(s"Invalid amount ${data.amount}")
      case Some(plan) => setUserPlan(user, plan.stripePlan, data.source)
    }

  def upgrade(user: User, plan: LichessPlan): Fu[StripeSubscription] =
    userCustomer(user) flatMap {
      case None => fufail(s"Can't upgrade non-existent customer ${user.id}")
      case Some(customer) =>
        customer.firstSubscription match {
          case None      => fufail(s"Can't upgrade non-existent subscription of ${user.id}")
          case Some(sub) => client.updateSubscription(sub, plan.stripePlan, none)
        }
    }

  def onCharge(charge: StripeCharge): Funit =
    customerColl.uno[Customer]($id(charge.customer)).flatMap {
      case None => fufail(s"Charged unknown customer $charge")
      case Some(cus) if cus.canLevelUp =>
        UserRepo byId cus.userId.value flatten s"Missing user for $cus" flatMap { user =>
          customerColl.updateField($id(cus.id), "lastLevelUp", DateTime.now) >>
            UserRepo.setPlan(user, user.plan.fold(lila.user.Plan.init)(_.incMonths)) >>-
            logger.info(s"Charged ${charge} ${cus}")
        }
      case Some(cus) => fufail(s"Too early to level up $charge $cus")
    }

  def onSubscriptionDeleted(sub: StripeSubscription): Funit =
    customerColl.uno[Customer]($id(sub.customer)).flatMap {
      case None => fufail(s"Deleted subscription of unknown customer $sub")
      case Some(cus) =>
        UserRepo byId cus.userId.value flatten s"Missing user for $cus" flatMap { user =>
          UserRepo.setPlan(user, user.plan.|(lila.user.Plan.init).disable) >>-
            logger.info(s"Unsubed ${user.id} ${sub}")
        }
    }

  def getEvent = client.getEvent _

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
        UserRepo.setPlan(user, lila.user.Plan.init) >>-
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
