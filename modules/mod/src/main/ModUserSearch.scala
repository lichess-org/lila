package lila.mod

import play.api.data.*
import play.api.data.Forms.*
import play.api.libs.json.*

import lila.user.{ UserApi, UserRepo, WithPerfsAndEmails, JsonView }
import lila.core.user.Emails

case class ModUserSearchResult(
    users: List[WithPerfsAndEmails],
    regexMatch: Boolean,
    exists: Boolean,
    lameNameMatch: Option[String]
)

final class ModUserSearch(userRepo: UserRepo, userApi: UserApi, jsonView: JsonView)(using Executor):

  def apply(query: String): Fu[ModUserSearchResult] = for
    users <- EmailAddress.from(query).map(searchEmail).getOrElse(searchUsername(UserStr(query)))
    withPerfs <- userApi.withPerfsAndEmails(users)
    userStr = UserStr.read(query)
    userName = userStr.map(_.into(UserName))
    exists <- userStr.so(userRepo.existsSec)
  yield ModUserSearchResult(
    users = withPerfs,
    regexMatch = lila.user.nameRules.newUsernameRegex.matches(query),
    exists = exists,
    lameNameMatch = userName.so(lila.common.LameName.explain)
  )

  def apiSearch(regex: String): Fu[JsObject] =
    for
      ids <- userRepo.idLikeCanBeVeryExpensive(regex.toLowerCase)
      withPerfs <- userApi.withPerfsAndEmails(ids)
      jsons = withPerfs.map(userJson)
    yield Json.obj("users" -> jsons)

  private def userJson(u: WithPerfsAndEmails): JsObject =
    import lila.common.Json.given
    import JsonView.given
    given Writes[Emails] = Json.writes
    import u.user.*
    jsonView.base(user, perfs.some) ++ Json
      .obj("createdAt" -> user.createdAt)
      .add("profile" -> user.profile)
      .add("seenAt" -> user.seenAt)
      .add("playTime" -> user.playTime)
      .add("emails" -> u.emails.some)
      .add("marks" -> user.marks.value.map(_.key).some)

  private def searchUsername(username: UserStr) = userRepo.byId(username).map(_.toList)

  private def searchEmail(email: EmailAddress): Fu[List[User]] =
    val normalized = email.normalize
    userRepo.byEmail(normalized).flatMap { current =>
      userRepo.byPrevEmail(normalized).map(current.toList.:::)
    }

object ModUserSearch:
  val form = Form:
    single:
      "q" -> lila.common.Form.trim(nonEmptyText).transform(_.replace(">", "").replace("<", ""), identity)
