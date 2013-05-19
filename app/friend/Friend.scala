package lila
package friend

import com.novus.salat.annotations.Key
import org.joda.time.DateTime

import user.User

case class Friend(
    @Key("_id") id: String, 
    users: List[String],
    date: DateTime) {

  def contains(userId: String): Boolean = users contains userId
  def contains(user: User): Boolean = contains(user.id)
}

object Friend {

  def makeId(u1: String, u2: String) = List(u1, u2).sorted mkString "@" 

  def apply(u1: String, u2: String): Friend = new Friend(
    id = makeId(u1, u2),
    users = List(u1, u2), 
    date = DateTime.now)
}
