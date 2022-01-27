package lila.mod

import play.api.data._
import play.api.data.Forms._

import lila.common.{ EmailAddress, IpAddress }
import lila.user.{ User, UserRepo }

final class UserSearch(
    securityApi: lila.security.SecurityApi,
    userRepo: UserRepo
)(implicit ec: scala.concurrent.ExecutionContext) {

  def apply(query: String): Fu[List[User.WithEmails]] =
    EmailAddress.from(query).map(searchEmail) orElse
      IpAddress.from(query).map(searchIp) getOrElse
      searchUsername(query) flatMap userRepo.withEmailsU

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

  val form = Form(
    single(
      "q" -> lila.common.Form.trim(nonEmptyText)
    )
  )
}
