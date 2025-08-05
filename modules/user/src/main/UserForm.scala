package lila.user
import play.api.data.*
import play.api.data.Forms.*

import lila.common.Form.{
  cleanFewSymbolsAndNonEmptyText,
  cleanFewSymbolsText,
  cleanNonEmptyText,
  cleanTextWithSymbols,
  into,
  playerTitle
}
import lila.common.LameName
import lila.core.user.{ FlagCode, Profile }

final class UserForm:

  def username(user: User): Form[UserName] = Form(
    single(
      "username" -> cleanNonEmptyText
        .into[UserName]
        .verifying(
          "changeUsernameNotSame",
          name => name.id == user.username.id && name != user.username
        )
        .verifying(
          "usernameUnacceptable",
          name => !LameName.hasTitle(name.value) || LameName.hasTitle(user.username.value)
        )
    )
  ).fill(user.username)

  def usernameOf(user: User) = username(user).fill(user.username)

  val profile: Form[Profile] = Form:
    mapping(
      "flag" -> optional(text.into[FlagCode].verifying(Flags.codeSet contains _)),
      "location" -> optional(cleanFewSymbolsAndNonEmptyText(maxLength = 80)),
      "bio" -> optional(cleanFewSymbolsAndNonEmptyText(maxLength = 400, maxSymbols = 10)),
      "realName" -> optional(cleanFewSymbolsText(minLength = 1, maxLength = 100)),
      "fideRating" -> optional(number(min = 1400, max = 3000)),
      "uscfRating" -> optional(number(min = 100, max = 3000)),
      "ecfRating" -> optional(number(min = 0, max = 3000)),
      "rcfRating" -> optional(number(min = 0, max = 3000)),
      "cfcRating" -> optional(number(min = 200, max = 3000)),
      "dsbRating" -> optional(number(min = 0, max = 3000)),
      "links" -> optional(cleanFewSymbolsAndNonEmptyText(maxLength = 3000))
    )(Profile.apply)(unapply)

  def profileOf(user: User) = profile.fill(user.profileOrDefault)

  def flair(asMod: Boolean) = Form[Option[Flair]]:
    single(FlairApi.formPair(asMod))

object UserForm:

  val note = Form:
    mapping(
      "text" -> cleanTextWithSymbols(minLength = 3, maxLength = 2000),
      "noteType" -> text
    )((text, noteType) => NoteData(text, noteType == "mod" || noteType == "dox", noteType == "dox"))(_ =>
      none
    )

  val apiNote = Form:
    mapping(
      "text" -> cleanTextWithSymbols(minLength = 3, maxLength = 2000),
      "mod" -> boolean,
      "dox" -> default(boolean, false)
    )(NoteData.apply)(unapply)

  case class NoteData(text: String, mod: Boolean, dox: Boolean)

  val title = Form:
    single("title" -> optional(playerTitle.field))
