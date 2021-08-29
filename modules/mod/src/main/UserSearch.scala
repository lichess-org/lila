package lila.mod

import play.api.data._
import play.api.data.Forms._

import lila.common.{ EmailAddress, IpAddress }
import lila.user.{ User, UserRepo }

final class UserSearch(
    securityApi: lila.security.SecurityApi,
    userRepo: UserRepo
)(implicit ec: scala.concurrent.ExecutionContext) {

  def apply(query: UserSearch.Query): Fu[List[User.WithEmails]] =
    (~query.as match {
      case _ => // "exact"
        EmailAddress.from(query.q).map(searchEmail) orElse
          IpAddress.from(query.q).map(searchIp) getOrElse
          searchUsername(query.q)
    }) flatMap userRepo.withEmailsU

  private def searchIp(ip: IpAddress) =
    securityApi recentUserIdsByIp ip map (_.reverse) flatMap userRepo.usersFromSecondary

  private def searchUsername(username: String) = userRepo named username map (_.toList)

  private def searchEmail(email: EmailAddress): Fu[List[User]] = {
    val normalized = email.normalize
    userRepo.byEmail(normalized) flatMap { current =>
      userRepo.byPrevEmail(normalized) map current.toList.:::
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

  val form = Form(
    mapping(
      "q"  -> lila.common.Form.trim(nonEmptyText),
      "as" -> optional(nonEmptyText.verifying(asValues contains _))
    )(Query.apply)(Query.unapply)
  )
}
