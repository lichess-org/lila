package lila.chat

import chess.Color
import reactivemongo.api.bson.*

case class UserLine(username: UserName, text: String, troll: Boolean, deleted: Boolean) extends Line:

  def author = username.value

  def userId = username.id
  def userIdMaybe = userId.some

  def delete = copy(deleted = true)

  def isVisible = !troll && !deleted

  def isLichess = userId.is(UserId.lichess)

object UserLine:
  private[chat] given BSONHandler[UserLine] = BSONStringHandler.as[UserLine](
    v => Line.strToUserLine(v).getOrElse(Line.invalidLine),
    Line.userLineToStr
  )

case class PlayerLine(color: Color, text: String) extends Line:
  def deleted = false
  def author = color.name
  def troll = false
  def flair = false
  def userIdMaybe = none

object Line:

  val textMaxSize = 140

  private[chat] val invalidLine =
    UserLine(UserName(""), "[invalid character]", troll = false, deleted = true)

  private[chat] given lineHandler: BSONHandler[lila.core.chat.Line] =
    BSONStringHandler.as[lila.core.chat.Line](
      v => strToLine(v).getOrElse(invalidLine),
      lineToStr
    )

  private val baseChar = " "
  private val trollChar = "!"
  private val deletedChar = "?"
  private val patronChar = "&" // BC for DB data
  private val flairChar = ":" // BC for DB data
  private val patronFlairChar = ";" // BC for DB data
  private val titleSep = '~' // BC for DB data
  private[chat] val separatorChars = // keep historical BC fields for DB data
    List(baseChar, trollChar, deletedChar, patronChar, flairChar, patronFlairChar)
  private val UserLineRegex = {
    """(?s)([\w-~]{2,}+)([""" + separatorChars.mkString("") + """])(.++)"""
  }.r
  private[chat] def strToUserLine(str: String): Option[UserLine] = str match
    case UserLineRegex(username, sep, text) =>
      val troll = sep == trollChar
      val deleted = sep == deletedChar
      val name = username.split(titleSep) match
        case Array(_, name) => UserName(name)
        case _ => UserName(username)
      UserLine(name, text, troll = troll, deleted = deleted).some
    case _ => none
  def userLineToStr(x: UserLine): String =
    val sep =
      if x.troll then trollChar
      else if x.deleted then deletedChar
      else " "
    s"${x.username}$sep${x.text}"

  def strToLine(str: String): Option[Line] =
    strToUserLine(str).orElse:
      str.headOption.flatMap(Color.apply).map {
        PlayerLine(_, str.drop(2))
      }
  def lineToStr(x: Line) =
    x match
      case u: UserLine => userLineToStr(u)
      case p: PlayerLine => s"${p.color.letter} ${p.text}"
