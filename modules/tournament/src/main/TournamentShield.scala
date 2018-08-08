package lidraughts.tournament

import org.joda.time.DateTime
import scala.concurrent.duration._
import reactivemongo.api.ReadPreference

import lidraughts.db.dsl._
import lidraughts.rating.PerfType
import lidraughts.user.User

final class TournamentShieldApi(
    coll: Coll,
    asyncCache: lidraughts.memo.AsyncCache.Builder
) {

  import TournamentShield._
  import BSONHandlers._

  def active(u: User): Fu[List[Award]] = cache.get map {
    _.value.values.flatMap(_.headOption.filter(_.owner.value == u.id)).toList
  }

  def history: Fu[History] = cache.get

  def currentOwner(tour: Tournament): Fu[Option[OwnerId]] = tour.isShield ?? {
    Category.of(tour) ?? { cat =>
      history.map(_.current(cat).map(_.owner))
    }
  }

  private[tournament] def clear = cache.refresh

  private val cache = asyncCache.single[History](
    name = "tournament.shield",
    expireAfter = _.ExpireAfterWrite(1 day),
    f = coll.find($doc(
      "schedule.freq" -> scheduleFreqHandler.write(Schedule.Freq.Shield),
      "status" -> statusBSONHandler.write(Status.Finished)
    )).sort($sort asc "startsAt").list[Tournament](none, ReadPreference.secondaryPreferred) map { tours =>
      for {
        tour <- tours
        categ <- Category of tour
        winner <- tour.winnerId
      } yield Award(
        categ = categ,
        owner = OwnerId(winner),
        date = tour.finishesAt,
        tourId = tour.id
      )
    } map {
      _.foldLeft(Map.empty[Category, List[Award]]) {
        case (hist, entry) => hist + (entry.categ -> hist.get(entry.categ).fold(List(entry))(entry :: _))
      }
    } map History.apply
  )
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

    def sorted: List[(Category, List[Award])] = Category.all map { categ =>
      categ -> ~(value get categ)
    }

    def userIds: List[User.ID] = value.values.flatMap(_.map(_.owner.value)).toList

    def current(cat: Category): Option[Award] = value get cat flatMap (_.headOption)
  }

  private type SpeedOrVariant = Either[Schedule.Speed, draughts.variant.Variant]

  sealed abstract class Category(
      val of: SpeedOrVariant,
      val iconChar: Char
  ) {
    def key = of.fold(_.name, _.key)
    def name = of.fold(_.toString, _.name)
    def matches(tour: Tournament) =
      if (tour.variant.standard) ~(for {
        tourSpeed <- tour.schedule.map(_.speed)
        categSpeed <- of.left.toOption
      } yield tourSpeed == categSpeed)
      else of.right.toOption.has(tour.variant)
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

    case object Frisian extends Category(
      of = Right(draughts.variant.Frisian),
      iconChar = '''
    )

    val all: List[Category] = List(Bullet, SuperBlitz, Blitz, Rapid, Classical, HyperBullet, UltraBullet, Frisian)

    def of(t: Tournament) = all.find(_ matches t)
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
