package lila.mod

import play.api.data.*
import play.api.data.Forms.*

import lila.common.{ EmailAddress, IpAddress }
import lila.user.{ User, UserRepo }

final class UserSearch(
    securityApi: lila.security.SecurityApi,
    userRepo: UserRepo
)(using Executor):

  def apply(query: String): Fu[List[User.WithEmails]] =
    EmailAddress.from(query).map(searchEmail) orElse
      IpAddress.from(query).map(searchIp) getOrElse
      searchUsername(UserStr(query)) flatMap userRepo.withEmails

  private def searchIp(ip: IpAddress) =
    securityApi recentUserIdsByIp ip map (_.reverse) flatMap userRepo.usersFromSecondary

  private def searchUsername(username: UserStr) = userRepo byId username map (_.toList)

  private def searchEmail(email: EmailAddress): Fu[List[User]] =
    val normalized = email.normalize
    userRepo.byEmail(normalized) flatMap { current =>
      userRepo.byPrevEmail(normalized) map current.toList.:::
    }

object UserSearch:

  val form = Form(
    single("q" -> lila.common.Form.trim(nonEmptyText))
  )
