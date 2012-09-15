package lila
package tournament

import org.joda.time.{ DateTime, Duration }
import org.scala_tools.time.Imports._
import com.novus.salat.annotations.Key
import com.mongodb.casbah.query.Imports.DBObject
import ornicar.scalalib.Random
import scalaz.NonEmptyList

import user.User

case class Data(
  name: String,
  minutes: Int,
  minUsers: Int,
  createdAt: DateTime,
  createdBy: String,
  users: List[String])

sealed trait Tournament {

  val id: String
  val data: Data
  def encode: RawTournament
  def pairings: List[Pairing]

  def numerotedPairings: Seq[(Int, Pairing)] = (pairings.size to 1 by -1) zip pairings

  def name = data.name
  def nameT = name + " tournament"

  def minutes = data.minutes
  lazy val duration = new Duration(minutes * 60 * 1000)

  def users = data.users
  def nbUsers = users.size
  def minUsers = data.minUsers
  def contains(username: String): Boolean = users contains username
  def contains(user: User): Boolean = contains(user.id)
  def contains(user: Option[User]): Boolean =
    user.fold(u ⇒ contains(u.id), false)
  def missingUsers = minUsers - users.size

  def showClock = "2+0"
  def createdBy = data.createdBy
  def createdAt = data.createdAt
}

sealed trait StartedOrFinished extends Tournament {

  def startedAt: DateTime

  def standing: Standing
  def rankedStanding = (1 to standing.size) zip standing

  def winner = standing.headOption
  def winnerUserId = winner map (_.username)

  def encode(status: Status) = new RawTournament(
    id = id,
    status = status.id,
    data = data,
    startedAt = startedAt.some,
    pairings = pairings map (_.encode))

  def finishedAt = startedAt + duration
}

case class Created(
    id: String,
    data: Data) extends Tournament {

  import data._

  def readyToStart = users.size >= minUsers

  def pairings = Nil
  lazy val standing = Standing of this

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
    pairings: List[Pairing]) extends StartedOrFinished {

  lazy val standing = Standing of this

  def addPairings(ps: NonEmptyList[Pairing]) =
    copy(pairings = ps.list ::: pairings)

  def updatePairing(gameId: String, f: Pairing ⇒ Pairing) = copy(
    pairings = pairings map { p ⇒ (p.gameId == gameId).fold(f(p), p) }
  )

  def readyToFinish = remainingSeconds == 0

  def remainingSeconds: Int = math.max(0, finishedAt.getSeconds - nowSeconds).toInt

  def finish = Finished(
    id = id,
    data = data,
    startedAt = startedAt,
    pairings = pairings,
    standing = standing)

  def encode = encode(Status.Started)
}

case class Finished(
    id: String,
    data: Data,
    startedAt: DateTime,
    pairings: List[Pairing],
    standing: Standing) extends StartedOrFinished {

  def encode = encode(Status.Finished) withStanding standing
}

case class RawTournament(
    @Key("_id") id: String,
    status: Int,
    data: Data,
    startedAt: Option[DateTime] = None,
    pairings: List[RawPairing] = Nil,
    standing: List[Player] = Nil) {

  def created: Option[Created] = (status == Status.Created.id) option Created(
    id = id,
    data = data)

  def started: Option[Started] = for {
    stAt ← startedAt
    if status == Status.Started.id
  } yield Started(
    id = id,
    data = data,
    startedAt = stAt,
    decodePairings)

  def finished: Option[Finished] = for {
    stAt ← startedAt
    if status == Status.Finished.id
  } yield Finished(
    id = id,
    data = data,
    startedAt = stAt,
    decodePairings,
    standing = standing)

  def decodePairings = pairings map (_.decode) flatten

  def any: Option[Tournament] = Status(status) flatMap {
    case Status.Created  ⇒ created
    case Status.Started  ⇒ started
    case Status.Finished ⇒ finished
  }

  def withStanding(s: Standing) = copy(standing = s)
}

object Tournament {

  import lila.core.Form._

  def apply(
    createdBy: String,
    minutes: Int,
    minUsers: Int): Created = Created(
    id = Random nextString 8,
    data = Data(
      name = RandomName(),
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
  val minUserChoices = options(minUsers, "%d player{s}")
}
