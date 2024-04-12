package lila.mod

import play.api.data.*
import play.api.data.Forms.*

import lila.user.{ UserApi, UserRepo, WithPerfsAndEmails }

final class ModUserSearch(userRepo: UserRepo, userApi: UserApi)(using Executor):

  def apply(query: String): Fu[List[WithPerfsAndEmails]] =
    EmailAddress
      .from(query)
      .map(searchEmail)
      .getOrElse(searchUsername(UserStr(query)))
      .flatMap(userApi.withPerfsAndEmails)

  private def searchUsername(username: UserStr) = userRepo.byId(username).map(_.toList)

  private def searchEmail(email: EmailAddress): Fu[List[User]] =
    val normalized = email.normalize
    userRepo.byEmail(normalized).flatMap { current =>
      userRepo.byPrevEmail(normalized).map(current.toList.:::)
    }

object ModUserSearch:
  val form = Form:
    single("q" -> lila.common.Form.trim(nonEmptyText))
