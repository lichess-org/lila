package lila.fishnet

import org.joda.time.DateTime

import chess.format.FEN
import chess.variant.Variant

sealed trait Work {
  def _id: Work.Id
  def game: Work.Game
  def tries: Int
  def acquired: Option[Work.Acquired]
  def createdAt: DateTime

  def skill: Client.Skill

  def id = _id

  def acquiredAt = acquired.map(_.date)
  def acquiredByKey = acquired.map(_.clientKey)
  def isAcquiredBy(client: Client) = acquiredByKey contains client.key
  def isAcquired = acquired.isDefined
  def nonAcquired = !isAcquired

  def acquiredBefore(date: DateTime) = acquiredAt.??(_ isBefore date)
}

object Work {

  case class Id(value: String) extends AnyVal with StringValue

  case class Acquired(
      clientKey: Client.Key,
      date: DateTime) {
    override def toString = s"by $clientKey at $date"
  }

  case class Game(
      id: String,
      initialFen: Option[FEN],
      variant: Variant,
      moves: String) {

    def moveList = moves.split(' ').toList
  }

  case class Sender(
      userId: Option[String],
      ip: Option[String],
      mod: Boolean,
      system: Boolean) {

    override def toString = if (system) "lichess" else userId orElse ip getOrElse "unknown"
  }

  case class Move(
      _id: Work.Id, // random
      game: Game,
      currentFen: FEN,
      level: Int,
      tries: Int,
      acquired: Option[Acquired],
      createdAt: DateTime) extends Work {

    def skill = Client.Skill.Move

    def assignTo(client: Client) = copy(
      acquired = Acquired(clientKey = client.key, date = DateTime.now).some,
      tries = tries + 1)

    def timeout = copy(acquired = none)
    def invalid = copy(acquired = none)

    def isOutOfTries = tries >= 3

    def similar(to: Move) = game.id == to.game.id && currentFen == to.currentFen

    override def toString = s"id:$id game:${game.id} level:$level tries:$tries currentFen:$currentFen acquired:$acquired"
  }

  case class Analysis(
      _id: Work.Id, // random
      sender: Sender,
      game: Game,
      startPly: Int,
      nbPly: Int,
      tries: Int,
      acquired: Option[Acquired],
      createdAt: DateTime) extends Work {

    def skill = Client.Skill.Analysis

    def assignTo(client: Client) = copy(
      acquired = Acquired(clientKey = client.key, date = DateTime.now).some,
      tries = tries + 1)

    def timeout = copy(acquired = none)
    def invalid = copy(acquired = none)

    def isOutOfTries = tries >= 2

    def abort = copy(acquired = none)

    override def toString = s"id:$id game:${game.id} tries:$tries requestedBy:$sender acquired:$acquired"
  }

  def makeId = Id(scala.util.Random.alphanumeric take 8 mkString)
}
