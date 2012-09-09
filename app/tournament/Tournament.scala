package lila
package tournament

import org.joda.time.{ DateTime, Duration }
import org.scala_tools.time.Imports._
import com.novus.salat.annotations.Key
import ornicar.scalalib.OrnicarRandom

case class Data(
    minutes: Int,
    minUsers: Int,
    createdAt: DateTime,
    createdBy: String,
    users: List[String]) {

  lazy val duration = new Duration(minutes * 60 * 1000)

  def missingUsers = minUsers - users.size

  def contains(username: String) = users contains username
}

sealed trait Tournament {

  def id: String
  def encode: RawTournament

  def showClock = "2 + 0"
}

case class Created(
    id: String,
    data: Data) extends Tournament {

  def encode = RawTournament.created(id, data)
}

case class RawTournament(
    @Key("_id") id: String,
    status: Int,
    data: Data) {

  def created: Option[Created] = (status == Status.Created.id) option Created(
    id = id,
    data = data)

  def any: Option[Tournament] = created
}

object RawTournament {

  def created(id: String, data: Data) = {
    new RawTournament(
      id = id,
      status = Status.Created.id,
      data = data)
  }
}

object Tournament {

  import lila.core.Form._

  def apply(
    createdBy: String,
    minutes: Int,
    minUsers: Int): Created = Created(
    id = OrnicarRandom nextString 8,
    data = Data(
      createdBy = createdBy,
      createdAt = DateTime.now,
      minutes = minutes,
      minUsers = minUsers,
      users = List(createdBy))
  )

  val minutes = 5 to 30 by 5
  val minuteDefault = 10
  val minuteChoices = options(minutes, "%d minute{s}")

  val minUsers = 5 to 30 by 5
  val minUserDefault = 10
  val minUserChoices = options(minUsers, "%d players{s}")
}
