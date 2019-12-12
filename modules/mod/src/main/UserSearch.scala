package lila.mod

import play.api.data._
import play.api.data.Forms._
import play.api.data.validation.Constraints
import reactivemongo.api.ReadPreference

import lila.common.{ EmailAddress, IpAddress }
import lila.db.dsl._
import lila.user.{ User, UserRepo }
import User.{ BSONFields => F }

final class UserSearch(
    securityApi: lila.security.SecurityApi,
    emailValidator: lila.security.EmailAddressValidator,
    userColl: Coll
) {

  def apply(query: UserSearch.Query): Fu[List[User.WithEmails]] = (~query.as match {
    case _ => // "exact"
      EmailAddress.from(query.q).map(searchEmail) orElse
        IpAddress.from(query.q).map(searchIp) getOrElse
        searchUsername(query.q)
  }) flatMap UserRepo.withEmailsU

  private def searchIp(ip: IpAddress) =
    securityApi recentUserIdsByIp ip map (_.reverse) flatMap UserRepo.usersFromSecondary

  private def searchUsername(username: String) = UserRepo named username map (_.toList)

  private def searchEmail(email: EmailAddress): Fu[List[User]] = {
    val normalized = email.normalize
    UserRepo.byEmail(normalized) flatMap { current =>
      UserRepo.byPrevEmail(normalized) map current.toList.:::
    }
  }
}

object UserSearch {

  val asChoices = List(
    "exact" -> "Exact match over all users"
  )
  val asValues = asChoices.map(_._1)

  case class Query(q: String, as: Option[String])

  def exact(q: String) = Query(q, none)

  val form = Form(mapping(
    "q" -> nonEmptyText,
    "as" -> optional(nonEmptyText.verifying(asValues contains _))
  )(Query.apply)(Query.unapply))
}
