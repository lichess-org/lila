package lila.chat

import lila.user.User

import chess.Color

sealed trait Line {
  def text: String
  def author: String
  def deleted: Boolean
  def isSystem = author == systemUserId
  def isHuman = !isSystem
  def humanAuthor = isHuman option author
}

case class UserLine(
    username: String,
    title: Option[String],
    text: String,
    troll: Boolean,
    deleted: Boolean
) extends Line {

  def author = username

  def userId = User normalize username

  def delete = copy(deleted = true)

  def isVisible = !troll && !deleted
}
case class PlayerLine(
    color: Color,
    text: String
) extends Line {
  def deleted = false
  def author = color.name
}

object Line {

  val textMaxSize = 140
  val titleSep = '~'

  import reactivemongo.bson.{ BSONHandler, BSONString }

  private val invalidLine = UserLine("", None, "[invalid character]", troll = false, deleted = true)

  private[chat] implicit val userLineBSONHandler = new BSONHandler[BSONString, UserLine] {
    def read(bsonStr: BSONString) = strToUserLine(bsonStr.value) getOrElse invalidLine
    def write(x: UserLine) = BSONString(userLineToStr(x))
  }

  private[chat] implicit val lineBSONHandler = new BSONHandler[BSONString, Line] {
    def read(bsonStr: BSONString) = strToLine(bsonStr.value) getOrElse invalidLine
    def write(x: Line) = BSONString(lineToStr(x))
  }

  private val UserLineRegex = """(?s)([\w-~]{2,}+)([ !?])(.++)""".r
  private def strToUserLine(str: String): Option[UserLine] = str match {
    case UserLineRegex(username, sep, text) =>
      val troll = sep == "!"
      val deleted = sep == "?"
      username split titleSep match {
        case Array(title, name) => UserLine(name, Some(title), text, troll = troll, deleted = deleted).some
        case _ => UserLine(username, None, text, troll = troll, deleted = deleted).some
      }
    case _ => none
  }
  def userLineToStr(x: UserLine): String = {
    val sep = if (x.troll) "!"
    else if (x.deleted) "?"
    else " "
    val tit = x.title.??(_ + titleSep)
    s"$tit${x.username}$sep${x.text}"
  }

  def strToLine(str: String): Option[Line] = strToUserLine(str) orElse {
    str.headOption flatMap Color.apply map { color =>
      PlayerLine(color, str drop 2)
    }
  }
  def lineToStr(x: Line) = x match {
    case u: UserLine => userLineToStr(u)
    case p: PlayerLine => s"${p.color.letter} ${p.text}"
  }
}
