package lila
package tournament

import org.joda.time.{ DateTime, Duration }
import org.scala_tools.time.Imports._
import com.novus.salat.annotations.Key
import ornicar.scalalib.OrnicarRandom

import user.User

case class Data(
  minutes: Int,
  minUsers: Int,
  createdAt: DateTime,
  createdBy: String,
  users: List[String])

sealed trait Tournament {

  val id: String
  val data: Data
  def encode: RawTournament

  import data._

  lazy val duration = new Duration(minutes * 60 * 1000)

  def missingUsers = minUsers - users.size

  def contains(username: String): Boolean = users contains username
  def contains(user: User): Boolean = contains(user.id)

  def showClock = "2 + 0"
}

case class Created(
    id: String,
    data: Data) extends Tournament {

  import data._

  def readyToStart = users.size >= minUsers

  def encode = new RawTournament(
    id = id,
    status = Status.Created.id,
    data = data)

  def join(user: User): Valid[Created] = contains(user).fold(
    !!("User %s is already part of the tournament" format user.id),
    withUsers(users :+ user.id).success
  )

  def withdraw(user: User): Valid[Created] = contains(user).fold(
    withUsers(users filterNot (user.id ==)).success,
    !!("User %s is not part of the tournament" format user.id)
  )

  def start = Started(id, data, DateTime.now, Nil)

  private def withUsers(x: List[String]) = copy(data = data.copy(users = x))
}

case class Started(
    id: String,
    data: Data,
    startedAt: DateTime,
    pairings: List[Pairing]) extends Tournament {

  def encode = new RawTournament(
    id = id,
    status = Status.Created.id,
    data = data,
    startedAt = startedAt.some,
    pairings = pairings map (_.encode))
}

case class Pairing(
    gameId: String,
    status: chess.Status,
    users: List[String]) {

  def encode: RawPairing = RawPairing(gameId, status.id, users)
}

case class RawPairing(
    g: String,
    s: Int,
    u: List[String]) {

  def decode: Option[Pairing] = chess.Status(s) map { status ⇒
    Pairing(g, status, u)
  }
}

case class RawTournament(
    @Key("_id") id: String,
    status: Int,
    data: Data,
    startedAt: Option[DateTime] = None,
    pairings: List[RawPairing] = Nil) {

  def created: Option[Created] = (status == Status.Created.id) option Created(
    id = id,
    data = data)

  def started: Option[Started] = for {
    stAt ← startedAt
    if status == Status.Created.id
  } yield Started(
    id = id,
    data = data,
    startedAt = stAt,
    decodePairings)

  def decodePairings = pairings map (_.decode) flatten

  def any: Option[Tournament] = created
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

  val minUsers = (2 to 4) ++ (5 to 30 by 5)
  val minUserDefault = 10
  val minUserChoices = options(minUsers, "%d players{s}")
}
