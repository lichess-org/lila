package lila.tournament

import org.joda.time.DateTime
import scala.concurrent.duration._

import lila.db.dsl._
import lila.rating.PerfType
import lila.user.User

final class TournamentShieldApi(
    coll: Coll,
    asyncCache: lila.memo.AsyncCache.Builder
) {

  import TournamentShield._
  import BSONHandlers._

  def apply(u: User): Fu[List[Owner]] = cache.get map {
    _.filter(_.userId == u.id)
  }

  def apply(c: Category): Fu[Option[Owner]] = cache.get map {
    _.find(_.categ == c)
  }

  def apply: Fu[List[Owner]] = cache.get

  private[tournament] def clear = cache.refresh

  private val cache = asyncCache.single[List[Owner]](
    name = "tournament.shield",
    findAll,
    expireAfter = _.ExpireAfterWrite(1 day)
  )

  private def findAll: Fu[List[Owner]] =
    coll.find($doc(
      "schedule.freq" -> scheduleFreqHandler.write(Schedule.Freq.Shield),
      "status" -> statusBSONHandler.write(Status.Finished),
      "startsAt" $gt DateTime.now.minusMonths(1).minusDays(1)
    )).sort($sort desc "startsAt").list[Tournament]() map { tours =>
      Category.all.flatMap { categ =>
        for {
          tour <- tours find categ.matches
          winner <- tour.winnerId
        } yield Owner(
          categ = categ,
          userId = winner,
          since = tour.finishesAt,
          tourId = tour.id
        )
      }
    }
}

object TournamentShield {

  case class Owner(
      categ: Category,
      userId: User.ID,
      since: DateTime,
      tourId: Tournament.ID
  )

  sealed abstract class Category(
      val of: Either[Schedule.Speed, chess.variant.Variant],
      val iconChar: Char
  ) {
    def key = of.fold(_.name, _.key)
    def name = of.fold(_.toString, _.name)
    def matches(tour: Tournament) = of.fold(
      speed => tour.schedule.exists(_.speed == speed),
      variant => tour.variant == variant
    )
  }

  object Category {

    case object UltraBullet extends Category(
      of = Left(Schedule.Speed.UltraBullet),
      iconChar = '{'
    )

    case object HyperBullet extends Category(
      of = Left(Schedule.Speed.HyperBullet),
      iconChar = 'T'
    )

    case object Bullet extends Category(
      of = Left(Schedule.Speed.Bullet),
      iconChar = 'T'
    )

    case object SuperBlitz extends Category(
      of = Left(Schedule.Speed.SuperBlitz),
      iconChar = ')'
    )

    case object Blitz extends Category(
      of = Left(Schedule.Speed.Blitz),
      iconChar = ')'
    )

    case object Rapid extends Category(
      of = Left(Schedule.Speed.Rapid),
      iconChar = '#'
    )

    case object Classical extends Category(
      of = Left(Schedule.Speed.Classical),
      iconChar = '+'
    )

    case object Chess960 extends Category(
      of = Right(chess.variant.Chess960),
      iconChar = '''
    )

    case object KingOfTheHill extends Category(
      of = Right(chess.variant.KingOfTheHill),
      iconChar = '('
    )

    case object Antichess extends Category(
      of = Right(chess.variant.Antichess),
      iconChar = '@'
    )

    case object Atomic extends Category(
      of = Right(chess.variant.Atomic),
      iconChar = '>'
    )

    case object ThreeCheck extends Category(
      of = Right(chess.variant.ThreeCheck),
      iconChar = '.'
    )

    case object Horde extends Category(
      of = Right(chess.variant.Horde),
      iconChar = '_'
    )

    case object RacingKings extends Category(
      of = Right(chess.variant.RacingKings),
      iconChar = ''
    )

    case object Crazyhouse extends Category(
      of = Right(chess.variant.Crazyhouse),
      iconChar = ''
    )

    val all: List[Category] = List(UltraBullet, HyperBullet, Bullet, SuperBlitz, Blitz, Rapid, Classical, Crazyhouse, Chess960, KingOfTheHill, ThreeCheck, Antichess, Atomic, Horde, RacingKings)
  }

  def spotlight(name: String) = Spotlight(
    iconFont = "5".some,
    headline = s"Battle for the $name Shield",
    description = s"""This Shield trophy is unique.
The winner keeps it for one month,
then must defend it during the next $name Shield tournament!""",
    homepageHours = 6.some
  )
}
