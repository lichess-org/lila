package lila.tournament

import org.joda.time.DateTime
import reactivemongo.api.ReadPreference
import scala.concurrent.duration._

import lila.db.dsl._
import lila.user.User
import lila.memo.CacheApi._

final class TournamentShieldApi(
    tournamentRepo: TournamentRepo,
    cacheApi: lila.memo.CacheApi
)(implicit ec: scala.concurrent.ExecutionContext) {

  import TournamentShield._
  import BSONHandlers._

  def active(u: User): Fu[List[Award]] =
    cache.getUnit dmap {
      _.value.values.flatMap(_.headOption.filter(_.owner.value == u.id)).toList
    }

  def history(maxPerCateg: Option[Int]): Fu[History] =
    cache.getUnit dmap { h =>
      maxPerCateg.fold(h)(h.take)
    }

  def byCategKey(k: String): Fu[Option[(Category, List[Award])]] =
    Category.byKey(k) ?? { categ =>
      cache.getUnit dmap {
        _.value get categ map {
          categ -> _
        }
      }
    }

  def currentOwner(tour: Tournament): Fu[Option[OwnerId]] =
    tour.isShield ?? {
      Category.of(tour) ?? { cat =>
        history(none).map(_.current(cat).map(_.owner))
      }
    }

  private[tournament] def clear(): Unit = cache.invalidateUnit().unit

  private[tournament] def clearAfterMarking(userId: User.ID): Funit = cache.getUnit map { hist =>
    import cats.implicits._
    if (hist.value.exists(_._2.exists(_.owner.value === userId))) clear()
  }

  private val cache = cacheApi.unit[History] {
    _.refreshAfterWrite(1 day)
      .buildAsyncFuture { _ =>
        tournamentRepo.coll
          .find(
            $doc(
              "schedule.freq" -> scheduleFreqHandler.writeTry(Schedule.Freq.Shield).get,
              "status"        -> statusBSONHandler.writeTry(Status.Finished).get
            )
          )
          .sort($sort asc "startsAt")
          .cursor[Tournament](ReadPreference.secondaryPreferred)
          .list() map { tours =>
          for {
            tour   <- tours
            categ  <- Category of tour
            winner <- tour.winnerId
          } yield Award(
            categ = categ,
            owner = OwnerId(winner),
            date = tour.finishesAt,
            tourId = tour.id
          )
        } map {
          _.foldLeft(Map.empty[Category, List[Award]]) { case (hist, entry) =>
            hist + (entry.categ -> hist.get(entry.categ).fold(List(entry))(entry :: _))
          }
        } dmap History.apply
      }
  }
}

object TournamentShield {

  case class OwnerId(value: String) extends AnyVal

  case class Award(
      categ: Category,
      owner: OwnerId,
      date: DateTime,
      tourId: Tournament.ID
  )
  // newer entry first
  case class History(value: Map[Category, List[Award]]) {

    def sorted: List[(Category, List[Award])] =
      Category.all map { categ =>
        categ -> ~(value get categ)
      }

    def userIds: List[User.ID] = value.values.flatMap(_.map(_.owner.value)).toList

    def current(cat: Category): Option[Award] = value get cat flatMap (_.headOption)

    def take(max: Int) =
      copy(
        value = value.view.mapValues(_ take max).toMap
      )
  }

  private type SpeedOrVariant = Either[Schedule.Speed, chess.variant.Variant]

  sealed abstract class Category(
      val of: SpeedOrVariant,
      val iconChar: Char
  ) {
    def key  = of.fold(_.key, _.key)
    def name = of.fold(_.name, _.name)
    def matches(tour: Tournament) =
      if (tour.variant.standard) ~(for {
        tourSpeed  <- tour.schedule.map(_.speed)
        categSpeed <- of.left.toOption
      } yield tourSpeed == categSpeed)
      else of.toOption.has(tour.variant)
  }

  object Category {

    case object UltraBullet
        extends Category(
          of = Left(Schedule.Speed.UltraBullet),
          iconChar = ''
        )

    case object HyperBullet
        extends Category(
          of = Left(Schedule.Speed.HyperBullet),
          iconChar = ''
        )

    case object Bullet
        extends Category(
          of = Left(Schedule.Speed.Bullet),
          iconChar = ''
        )

    case object SuperBlitz
        extends Category(
          of = Left(Schedule.Speed.SuperBlitz),
          iconChar = ''
        )

    case object Blitz
        extends Category(
          of = Left(Schedule.Speed.Blitz),
          iconChar = ''
        )

    case object Rapid
        extends Category(
          of = Left(Schedule.Speed.Rapid),
          iconChar = ''
        )

    case object Classical
        extends Category(
          of = Left(Schedule.Speed.Classical),
          iconChar = ''
        )

    case object Chess960
        extends Category(
          of = Right(chess.variant.Chess960),
          iconChar = ''
        )

    case object KingOfTheHill
        extends Category(
          of = Right(chess.variant.KingOfTheHill),
          iconChar = ''
        )

    case object Antichess
        extends Category(
          of = Right(chess.variant.Antichess),
          iconChar = ''
        )

    case object Atomic
        extends Category(
          of = Right(chess.variant.Atomic),
          iconChar = ''
        )

    case object ThreeCheck
        extends Category(
          of = Right(chess.variant.ThreeCheck),
          iconChar = ''
        )

    case object Horde
        extends Category(
          of = Right(chess.variant.Horde),
          iconChar = ''
        )

    case object RacingKings
        extends Category(
          of = Right(chess.variant.RacingKings),
          iconChar = ''
        )

    case object Crazyhouse
        extends Category(
          of = Right(chess.variant.Crazyhouse),
          iconChar = ''
        )

    val all: List[Category] = List(
      Bullet,
      SuperBlitz,
      Blitz,
      Rapid,
      Classical,
      HyperBullet,
      UltraBullet,
      Crazyhouse,
      Chess960,
      KingOfTheHill,
      ThreeCheck,
      Antichess,
      Atomic,
      Horde,
      RacingKings
    )

    def of(t: Tournament): Option[Category] = all.find(_ matches t)

    def byKey(k: String): Option[Category] = all.find(_.key == k)
  }

  def spotlight(name: String) =
    Spotlight(
      iconFont = "".some,
      headline = s"Battle for the $name Shield",
      description =
        s"""This [Shield trophy](https://lichess.org/blog/Wh36WiQAAMMApuRb/introducing-shield-tournaments) is unique.
The winner keeps it for one month,
then must defend it during the next $name Shield tournament!""",
      homepageHours = 6.some
    )
}
