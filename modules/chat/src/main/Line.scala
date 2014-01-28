package lila.chat

import chess.Color

sealed trait Line {
  def text: String
  def author: String
}

case class UserLine(username: String, text: String) extends Line {
  def author = username
}
case class PlayerLine(color: Color, text: String) extends Line {
  def author = color.name
}

object Line {

  import lila.db.BSON
  import reactivemongo.bson.{ BSONHandler, BSONString }

  implicit val userLineBSONHandler = new BSONHandler[BSONString, UserLine] {

    def read(bsonStr: BSONString) = {
      val str = bsonStr.value
      val username = str takeWhile (' '!=)
      UserLine(username = username, text = str drop (username.size + 1))
    }

    def write(x: UserLine) = BSONString(s"${x.username} ${x.text}")
  }

  implicit val lineBSONHandler = new BSONHandler[BSONString, Line] {

    def read(bsonStr: BSONString) = if (bsonStr.value(1) != ' ') userLineBSONHandler read bsonStr else {
      val str = bsonStr.value
      PlayerLine(color = Color(str.head) err s"Invalid player line $str", text = str drop 2)
    }

    def write(x: Line) = x match {
      case PlayerLine(color, text) ⇒ BSONString(s"${color.letter} $text")
      case e: UserLine             ⇒ userLineBSONHandler write e
    }
  }
}
