package lila.swiss

import org.joda.time.DateTime
import scala.concurrent.ExecutionContext

import lila.db.dsl._

final private class SwissOfficialSchedule(colls: SwissColls, cache: SwissCache)(implicit
    ec: ExecutionContext
) {
  import SwissOfficialSchedule._

  private val classical   = Config("Classical", 30, 0, 5, 5)
  private val rapid       = Config("Rapid", 10, 0, 7, 8)
  private val blitz       = Config("Blitz", 5, 0, 12, 12)
  private val superblitz  = Config("SuperBlitz", 3, 0, 15, 12)
  private val bullet      = Config("Bullet", 1, 0, 25, 15)
  private val hyperbullet = Config("HyperBullet", 0.5, 0, 25, 15)

  // length must divide 24 (schedule starts at 0AM)
  // so either 3, 4, 6, 8, 12
  private val schedule = Vector(
    classical,
    bullet,
    rapid,
    hyperbullet,
    blitz,
    superblitz
  )
  private def daySchedule =
    (0 to 23).toList.flatMap(i => schedule.lift(i % schedule.length))

  def generate: Funit = {
    val dayStart = DateTime.now.plusDays(3).withTimeAtStartOfDay
    daySchedule.zipWithIndex
      .map { case (config, hour) =>
        val startAt = dayStart plusHours hour
        colls.swiss.exists($doc("teamId" -> lichessTeamId, "startsAt" -> startAt)) flatMap {
          case true => fuFalse
          case _    => colls.swiss.insert.one(BsonHandlers.addFeaturable(makeSwiss(config, startAt))) inject true
        }
      }
      .sequenceFu
      .map { res =>
        if (res.exists(identity)) cache.featuredInTeam.invalidate(lichessTeamId)
      }
  }

  private def makeSwiss(config: Config, startAt: DateTime) =
    Swiss(
      _id = Swiss.makeId,
      name = config.name,
      clock = config.clock,
      variant = chess.variant.Standard,
      round = SwissRound.Number(0),
      nbPlayers = 0,
      nbOngoing = 0,
      createdAt = DateTime.now,
      createdBy = lila.user.User.lichessId,
      teamId = lichessTeamId,
      nextRoundAt = startAt.some,
      startsAt = startAt,
      finishedAt = none,
      winnerId = none,
      settings = Swiss.Settings(
        nbRounds = config.nbRounds,
        rated = true,
        description = none,
        position = none,
        roundInterval = SwissForm.autoInterval(config.clock),
        password = none,
        conditions = SwissCondition
          .All(nbRatedGame = SwissCondition.NbRatedGame(config.minGames).some, none, none, none),
        forbiddenPairings = ""
      )
    )
}

private object SwissOfficialSchedule {
  case class Config(
      name: String,
      clockMinutes: Double,
      clockSeconds: Int,
      nbRounds: Int,
      minGames: Int
  ) {
    def clock = chess.Clock.Config((clockMinutes * 60).toInt, clockSeconds)
  }
}
