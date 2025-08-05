package lila.swiss

import chess.Clock.{ IncrementSeconds, LimitSeconds }

import lila.db.dsl.{ *, given }
import lila.gathering.Condition.NbRatedGame

final private class SwissOfficialSchedule(mongo: SwissMongo, cache: SwissCache)(using
    Executor
):
  import SwissOfficialSchedule.*

  private val classical = Config("Classical", 30, IncrementSeconds(0), 5, 5)
  private val rapid = Config("Rapid", 10, IncrementSeconds(0), 7, 8)
  private val blitz = Config("Blitz", 5, IncrementSeconds(0), 10, 12)
  private val superblitz = Config("SuperBlitz", 3, IncrementSeconds(0), 12, 12)
  private val bullet = Config("Bullet", 1, IncrementSeconds(0), 15, 15)
  private val hyperbullet = Config("HyperBullet", 0.5, IncrementSeconds(0), 20, 15)

  private val classicalInc = Config("Classical Increment", 25, IncrementSeconds(3), 5, 5)
  private val rapidInc = Config("Rapid Increment", 7, IncrementSeconds(2), 7, 8)
  private val blitzInc = Config("Blitz Increment", 5, IncrementSeconds(2), 10, 12)
  private val superblitzInc = Config("SuperBlitz Increment", 3, IncrementSeconds(1), 12, 12)
  private val bulletInc = Config("Bullet Increment", 1, IncrementSeconds(1), 20, 15)
  private val hyperbulletInc = Config("HyperBullet Increment", 0, IncrementSeconds(1), 20, 15)

  // length must divide 48 (schedule starts at 0AM)
  // so either 3, 4, 6, 8, 12, 24
  private val schedule = Vector(
    classical,
    blitzInc,
    bullet,
    superblitzInc,
    rapid,
    classicalInc,
    hyperbullet,
    bulletInc,
    blitz,
    hyperbulletInc,
    superblitz,
    rapidInc
  )
  private def daySchedule = (0 to 47).toList.flatMap(i => schedule.lift(i % schedule.length))

  def generate: Funit =
    val dayStart = nowInstant.plusDays(3).withTimeAtStartOfDay
    daySchedule
      .mapWithIndex { (config, position) =>
        val hour = position / 2
        val minute = (position % 2) * 30
        val startAt = dayStart.plusHours(hour).plusMinutes(minute)
        mongo.swiss
          .exists($doc("teamId" -> lichessTeamId, "startsAt" -> startAt))
          .flatMap:
            if _ then fuFalse
            else mongo.swiss.insert.one(BsonHandlers.addFeaturable(makeSwiss(config, startAt))).inject(true)
      }
      .parallel
      .map { res =>
        if res.exists(identity) then cache.featuredInTeam.invalidate(lichessTeamId)
      }

  private def makeSwiss(config: Config, startAt: Instant) =
    Swiss(
      id = Swiss.makeId,
      name = config.name,
      clock = config.clock,
      variant = chess.variant.Standard,
      round = SwissRoundNumber(0),
      nbPlayers = 0,
      nbOngoing = 0,
      createdAt = nowInstant,
      createdBy = UserId.lichess,
      teamId = lichessTeamId,
      nextRoundAt = startAt.some,
      startsAt = startAt,
      finishedAt = none,
      winnerId = none,
      settings = Swiss.Settings(
        nbRounds = config.nbRounds,
        rated = chess.Rated.Yes,
        description = none,
        position = none,
        roundInterval = SwissForm.autoInterval(config.clock),
        password = none,
        conditions = SwissCondition.All.empty.copy(nbRatedGame = NbRatedGame(config.minGames).some),
        forbiddenPairings = "",
        manualPairings = ""
      )
    )

private object SwissOfficialSchedule:
  case class Config(
      name: String,
      clockMinutes: Double,
      clockSeconds: IncrementSeconds,
      nbRounds: Int,
      minGames: Int
  ):
    def clock = chess.Clock.Config(LimitSeconds((clockMinutes * 60).toInt), clockSeconds)
