package lila.mod

import lila.user.{ User, UserRepo }

final class UserSearch(
  securityApi: lila.security.Api,
emailAddress: lila.security.EmailAddress) {

  // http://stackoverflow.com/questions/106179/regular-expression-to-match-hostname-or-ip-address
  private val ipv4Pattern = """^(([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\.){3}([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])$""".r.pattern

  // ipv6 in standard form
  private val ipv6Pattern = """^((0|[1-9a-f][0-9a-f]{0,3}):){7}(0|[1-9a-f][0-9a-f]{0,3})""".r.pattern

  // from playframework
  private val emailPattern =
    """^[a-zA-Z0-9\.!#$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$""".r.pattern

  def apply(query: String): Fu[List[User]] =
    if (query.isEmpty) fuccess(Nil)
    else if (emailPattern.matcher(query).matches) searchEmail(query)
    else if (ipv4Pattern.matcher(query).matches) searchIp(query)
    else if (ipv6Pattern.matcher(query).matches) searchIp(query)
    else searchUsername(query)

  private def searchIp(ip: String) =
    securityApi recentUserIdsByIp ip map (_.reverse) flatMap UserRepo.byOrderedIds

  private def searchUsername(username: String) = UserRepo named username map (_.toList)

  private def searchEmail(email: String) = emailAddress.validate(email) ?? { fixed =>
    UserRepo byEmail fixed map (_.toList)
  }
}
