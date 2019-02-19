package lila.mod

import lila.common.{ EmailAddress, IpAddress }
import lila.user.{ User, UserRepo }

final class UserSearch(
    securityApi: lila.security.SecurityApi,
    emailValidator: lila.security.EmailAddressValidator
) {

  def apply(query: String): Fu[List[User]] =
    if (query.isEmpty) fuccess(Nil)
    else EmailAddress.from(query).map(searchEmail) orElse
      IpAddress.from(query).map(searchIp) getOrElse
      (searchUsername(query) zip searchFingerHash(query) map Function.tupled(_ ++ _)) // list concatenation, in case a fingerhash is also someone's username

  private def searchIp(ip: IpAddress) =
    securityApi recentUserIdsByIp ip map (_.reverse) flatMap UserRepo.usersFromSecondary

  private def searchFingerHash(fh: String): Fu[List[User]] =
    (fh.size == 8) ?? {
      securityApi recentUserIdsByFingerHash lila.security.FingerHash(fh) map (_.reverse) flatMap UserRepo.usersFromSecondary
    }

  private def searchUsername(username: String) = UserRepo named username map (_.toList)

  private def searchEmail(email: EmailAddress): Fu[List[User]] = {
    val normalized = email.normalize
    UserRepo.byEmail(normalized) flatMap { current =>
      UserRepo.byPrevEmail(normalized) map current.toList.:::
    }
  }
}
