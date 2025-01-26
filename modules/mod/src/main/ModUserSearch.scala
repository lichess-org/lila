package lila.mod

import play.api.data.*
import play.api.data.Forms.*

import lila.user.{ UserApi, UserRepo, WithPerfsAndEmails }

case class ModUserSearchResult(
    users: List[WithPerfsAndEmails],
    regexMatch: Boolean,
    exists: Boolean,
    lameNameMatch: Option[String]
)

final class ModUserSearch(userRepo: UserRepo, userApi: UserApi)(using Executor):

  def apply(query: String): Fu[ModUserSearchResult] = for
    users     <- EmailAddress.from(query).map(searchEmail).getOrElse(searchUsername(UserStr(query)))
    withPerfs <- userApi.withPerfsAndEmails(users)
    userStr  = UserStr.read(query)
    userName = userStr.map(_.into(UserName))
    exists <- userStr.so(userRepo.exists)
  yield ModUserSearchResult(
    users = withPerfs,
    regexMatch = lila.user.nameRules.newUsernameRegex.matches(query),
    exists = exists,
    lameNameMatch = userName.so(lila.common.LameName.explain)
  )

  private def searchUsername(username: UserStr) = userRepo.byId(username).map(_.toList)

  private def searchEmail(email: EmailAddress): Fu[List[User]] =
    val normalized = email.normalize
    userRepo.byEmail(normalized).flatMap { current =>
      userRepo.byPrevEmail(normalized).map(current.toList.:::)
    }

object ModUserSearch:
  val form = Form:
    single("q" -> lila.common.Form.trim(nonEmptyText))
