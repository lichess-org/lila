package lila.fishnet

import org.joda.time.DateTime

import shogi.format.forsyth.Sfen
import shogi.format.usi.Usi
import shogi.variant.Variant
import lila.common.IpAddress

sealed trait Work {
  def _id: Work.Id
  def name: String
  def game: Work.Game
  def engine: String
  def tries: Int
  def lastTryByKey: Option[Client.Key]
  def acquired: Option[Work.Acquired]
  def createdAt: DateTime

  def skill: Client.Skill

  def id = _id

  def acquiredAt                   = acquired.map(_.date)
  def acquiredByKey                = acquired.map(_.clientKey)
  def isAcquiredBy(client: Client) = acquiredByKey contains client.key
  def isAcquired                   = acquired.isDefined
  def nonAcquired                  = !isAcquired
  def canAcquire(client: Client)   = lastTryByKey.fold(true)(client.key !=)
  def isStandard = game.variant.standard && game.initialSfen.fold(true)(_.initialOf(game.variant))

  def acquiredBefore(date: DateTime) = acquiredAt.??(_ isBefore date)
}

object Work {

  case class Id(value: String) extends AnyVal with StringValue

  case class Acquired(
      clientKey: Client.Key,
      userId: Client.UserId,
      date: DateTime
  ) {

    def ageInMillis = nowMillis - date.getMillis

    override def toString = s"by $userId at $date"
  }

  case class Game(
      id: String, // can be a study chapter ID, if studyId is set
      initialSfen: Option[Sfen],
      studyId: Option[String],
      variant: Variant,
      moves: String
  ) {
    def sfen               = initialSfen.getOrElse(variant.initialSfen)
    def usiList: List[Usi] = ~(Usi readList moves)
  }

  case class Sender(
      userId: Option[String],
      postGameStudy: Option[lila.analyse.Analysis.PostGameStudy],
      ip: Option[IpAddress],
      mod: Boolean,
      system: Boolean
  ) {

    override def toString =
      if (system) lila.user.User.lishogiId
      else userId orElse ip.map(_.value) getOrElse "unknown"
  }

  case class Clock(btime: Int, wtime: Int, inc: Int, byo: Int)

  case class Move(
      _id: Work.Id, // random
      ply: Int,
      game: Game,
      currentSfen: Sfen, // backup
      level: Int,
      engine: String,
      clock: Option[Work.Clock],
      tries: Int,
      lastTryByKey: Option[Client.Key],
      acquired: Option[Acquired],
      delayMillis: Option[Int],
      createdAt: DateTime
  ) extends Work {
    def skill = Client.Skill.Move

    def name = "move"

    def assignTo(client: Client) =
      copy(
        acquired = Acquired(
          clientKey = client.key,
          userId = client.userId,
          date = DateTime.now
        ).some,
        lastTryByKey = client.key.some,
        tries = tries + 1
      )

    def timeout = copy(acquired = none)
    def invalid = copy(acquired = none)

    def isOutOfTries = tries >= maxTries
    def hasLastTry   = tries + 1 == maxTries

    def sfenOnly = copy(
      game = game.copy(
        initialSfen = currentSfen.some,
        moves = ""
      )
    )

    def similar(to: Move) = game.id == to.game.id && ply == to.ply

    override def toString =
      s"id:$id game:${game.id} variant:${game.variant.key} sfen:${currentSfen} level:$level tries:$tries created:$createdAt acquired:$acquired"
  }

  case class Analysis(
      _id: Work.Id, // random
      sender: Sender,
      game: Game,
      engine: String,
      startPly: Int,
      tries: Int,
      lastTryByKey: Option[Client.Key],
      acquired: Option[Acquired],
      postGameStudies: Set[lila.analyse.Analysis.PostGameStudy], // set of studies to inform
      skipPositions: List[Int],
      puzzleWorthy: Boolean,
      createdAt: DateTime
  ) extends Work {

    def skill = Client.Skill.Analysis

    def name = "analysis"

    def assignTo(client: Client) =
      copy(
        acquired = Acquired(
          clientKey = client.key,
          userId = client.userId,
          date = DateTime.now
        ).some,
        lastTryByKey = client.key.some,
        tries = tries + 1
      )

    def timeout = copy(acquired = none)
    def invalid = copy(acquired = none)

    def isOutOfTries = tries >= maxTries

    def abort = copy(acquired = none)

    def nbMoves = game.moves.count(' ' ==) + 1

    override def toString = s"id:$id game:${game.id} tries:$tries requestedBy:$sender acquired:$acquired"
  }

  case class Puzzle(
      _id: Work.Id, // random
      game: Work.Game,
      engine: String,
      source: Puzzle.Source,
      tries: Int,
      lastTryByKey: Option[Client.Key],
      acquired: Option[Acquired],
      createdAt: DateTime,
      verifiable: Boolean
  ) extends Work {

    def skill = Client.Skill.Puzzle

    def name = "puzzle"

    def assignTo(client: Client) =
      copy(
        acquired = Acquired(
          clientKey = client.key,
          userId = client.userId,
          date = DateTime.now
        ).some,
        lastTryByKey = client.key.some,
        tries = tries + 1
      )

    def prepareToVerify = copy(verifiable = true, tries = 0)

    def timeout = copy(acquired = none)
    def invalid = copy(acquired = none)

    def isOutOfTries = tries >= maxTries

  }

  object Puzzle {
    case class Source(
        game: Option[Source.FromGame],
        user: Option[Source.FromUser]
    )
    object Source {
      case class FromGame(
          id: String
      )
      case class FromUser(
          submittedBy: String,
          author: Option[String]
      )
    }
  }

  def makeId = Id(lila.common.ThreadLocalRandom nextString 8)

  val maxTries = 3
}
