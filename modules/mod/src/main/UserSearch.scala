package lila.mod

import lila.common.{ Email, IpAddress }
import lila.user.{ User, UserRepo }

final class UserSearch(
    securityApi: lila.security.Api,
    emailAddress: lila.security.EmailAddress
) {

  def apply(query: String): Fu[List[User]] =
    if (query.isEmpty) fuccess(Nil)
    else Email.from(query).map(searchEmail) orElse
      IpAddress.from(query).map(searchIp) getOrElse
      searchUsername(query)

  private def searchIp(ip: IpAddress) =
    securityApi recentUserIdsByIp ip map (_.reverse) flatMap UserRepo.usersFromSecondary

  private def searchUsername(username: String) = UserRepo named username map (_.toList)

  private def searchEmail(email: Email) = emailAddress.validate(email) ?? { fixed =>
    UserRepo byEmail fixed map (_.toList)
  }
}
