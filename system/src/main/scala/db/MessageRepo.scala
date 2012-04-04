package lila.system
package db

import model.Message

import com.mongodb.casbah.MongoCollection
import com.mongodb.casbah.Imports._
import scalaz.effects._
import org.apache.commons.lang3.StringEscapeUtils.escapeXml

class MessageRepo(collection: MongoCollection, val max: Int) {

  private val naturalOrder = DBObject("$natural" -> -1)
  private val urlRegex = """lichess\.org/([\w-]{8})[\w-]{4}""".r

  def add(text: String, username: String): Valid[IO[Message]] =
    if (username.isEmpty || username == "Anonymous")
      !!("Invalid username " + username)
    else escapeXml(text.trim take 140) |> { t ⇒
      if (t.isEmpty) !!("Empty message")
      else success {
        val t = urlRegex.replaceAllIn(
          text.trim.take(140),
          m ⇒ "lichess.org/" + (m group 1))
        io {
          collection += DBObject(
            "u" -> username,
            "t" -> t)
          Message(username, t)
        }
      }
    }

  val recent: IO[List[Message]] = io {
    collection.find(DBObject()).sort(naturalOrder).limit(max).toList map { obj ⇒
      for {
        u ← obj.getAs[String]("u")
        t ← obj.getAs[String]("t")
      } yield Message(u, t)
    } flatten
  }

  private def !!(msg: String) = failure(msg.wrapNel)
}
