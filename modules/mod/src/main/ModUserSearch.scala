package lila.mod

import play.api.data.*
import play.api.data.Forms.*

import lila.user.{ UserApi, UserRepo, WithPerfsAndEmails }
import lila.core.userId.{ UserName, UserSearch }

case class ModUserSearchResult(
    users: List[WithPerfsAndEmails],
    regexMatch: Boolean,
    exists: Boolean,
    lameNameMatch: Option[String]
)

final class ModUserSearch(userRepo: UserRepo, userApi: UserApi)(using Executor):

  def apply(query: String): Fu[ModUserSearchResult] = for
    ids       <- EmailAddress.from(query).fold(searchIds(query))(searchEmailIds)
    withPerfs <- userApi.withPerfsAndEmails(ids)
    exists    <- UserStr.read(query).so(userRepo.exists)
  yield ModUserSearchResult(
    users = withPerfs,
    regexMatch = lila.user.nameRules.newUsernameRegex.matches(query),
    exists = exists,
    lameNameMatch = lila.common.LameName.explain(UserName(query))
  )

  private def searchIds(query: String): Fu[List[UserId]] =
    UserStr.read(query) match
      case Some(id) => userRepo.byId(id).map(_.map(_.id).toList)
      case None     => UserSearch.read(query).so(userRepo.userIdsLike(_, 10))

  private def searchEmailIds(email: EmailAddress): Fu[List[UserId]] =
    searchEmail(email).map(_.map(_.id))

  private def searchEmail(email: EmailAddress): Fu[List[User]] =
    val normalized = email.normalize
    userRepo.byEmail(normalized).flatMap { current =>
      userRepo.byPrevEmail(normalized).map(current.toList.:::)
    }

object ModUserSearch:
  val form = Form:
    single("q" -> lila.common.Form.trim(nonEmptyText))
