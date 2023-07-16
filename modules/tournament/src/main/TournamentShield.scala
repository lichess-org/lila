package lila.tournament

import chess.variant.Variant.given
import lila.common.licon
import lila.db.dsl.*
import lila.user.User
import lila.memo.CacheApi.*

final class TournamentShieldApi(
    tournamentRepo: TournamentRepo,
    cacheApi: lila.memo.CacheApi
)(using Executor):

  import TournamentShield.*
  import BSONHandlers.given

  def active(u: User): Fu[List[Award]] =
    cache.getUnit.dmap:
      _.value.values.flatMap(_.headOption.filter(_.owner == u.id)).toList

  def history(maxPerCateg: Option[Int]): Fu[History] =
    cache.getUnit.dmap: h =>
      maxPerCateg.fold(h)(h.take)

  def byCategKey(k: String): Fu[Option[(Category, List[Award])]] =
    Category.byKey.get(k) so { categ =>
      cache.getUnit.dmap:
        _.value get categ map {
          categ -> _
        }
    }

  def currentOwner(tour: Tournament): Fu[Option[UserId]] =
    tour.isShield.so:
      Category.of(tour) so { cat =>
        history(none).map(_.current(cat).map(_.owner))
      }

  private[tournament] def clear(): Unit = cache.invalidateUnit()

  private[tournament] def clearAfterMarking(userId: UserId): Funit = cache.getUnit.map: hist =>
    if hist.value.exists(_._2.exists(_.owner == userId))
    then clear()

  private val cache = cacheApi.unit[History]:
    _.refreshAfterWrite(1 day).buildAsyncFuture: _ =>
      tournamentRepo.coll
        .find:
          $doc(
            "schedule.freq" -> (Schedule.Freq.Shield: Schedule.Freq),
            "status"        -> (Status.Finished: Status)
          )
        .sort($sort asc "startsAt")
        .cursor[Tournament](ReadPref.priTemp)
        .listAll()
        .map: tours =>
          for
            tour   <- tours
            categ  <- Category of tour
            winner <- tour.winnerId
          yield Award(
            categ = categ,
            owner = winner,
            date = tour.finishesAt,
            tourId = tour.id
          )
        .map:
          _.foldLeft(Map.empty[Category, List[Award]]): (hist, entry) =>
            hist + (entry.categ -> hist.get(entry.categ).fold(List(entry))(entry :: _))
        .dmap(History.apply)

object TournamentShield:

  case class Award(
      categ: Category,
      owner: UserId,
      date: Instant,
      tourId: TourId
  )
  // newer entry first
  case class History(value: Map[Category, List[Award]]):

    def sorted: List[(Category, List[Award])] =
      Category.list.map: categ =>
        categ -> ~(value get categ)

    def userIds: List[UserId] = value.values.flatMap(_.map(_.owner)).toList

    def current(cat: Category): Option[Award] = value get cat flatMap (_.headOption)

    def take(max: Int) =
      copy(value = value.view.mapValues(_ take max).toMap)

  private type SpeedOrVariant = Either[Schedule.Speed, chess.variant.Variant]

  enum Category(val of: SpeedOrVariant, val icon: licon.Icon):
    def key  = of.fold(_.key, _.key.value)
    def name = of.fold(_.name, _.name)
    def matches(tour: Tournament) =
      if tour.variant.standard then
        ~(for
          tourSpeed  <- tour.schedule.map(_.speed)
          categSpeed <- of.left.toOption
        yield tourSpeed == categSpeed)
      else of.toOption.has(tour.variant)

    case Bullet        extends Category(Left(Schedule.Speed.Bullet), licon.Bullet)
    case SuperBlitz    extends Category(Left(Schedule.Speed.SuperBlitz), licon.FlameBlitz)
    case Blitz         extends Category(Left(Schedule.Speed.Blitz), licon.FlameBlitz)
    case Rapid         extends Category(Left(Schedule.Speed.Rapid), licon.Rabbit)
    case Classical     extends Category(Left(Schedule.Speed.Classical), licon.Turtle)
    case HyperBullet   extends Category(Left(Schedule.Speed.HyperBullet), licon.Bullet)
    case UltraBullet   extends Category(Left(Schedule.Speed.UltraBullet), licon.UltraBullet)
    case Chess960      extends Category(Right(chess.variant.Chess960), licon.DieSix)
    case Crazyhouse    extends Category(Right(chess.variant.Crazyhouse), licon.Crazyhouse)
    case KingOfTheHill extends Category(Right(chess.variant.KingOfTheHill), licon.FlagKingHill)
    case ThreeCheck    extends Category(Right(chess.variant.ThreeCheck), licon.ThreeCheckStack)
    case Antichess     extends Category(Right(chess.variant.Antichess), licon.Antichess)
    case Atomic        extends Category(Right(chess.variant.Atomic), licon.Atom)
    case Horde         extends Category(Right(chess.variant.Horde), licon.Keypad)
    case RacingKings   extends Category(Right(chess.variant.RacingKings), licon.FlagRacingKings)

  object Category:
    val list                                = values.toList
    val byKey                               = values.mapBy(_.key)
    def of(t: Tournament): Option[Category] = list.find(_ matches t)

  def spotlight(name: String) =
    Spotlight(
      iconFont = licon.Shield.some,
      headline = s"Battle for the $name Shield",
      description =
        s"""This [Shield trophy](https://lichess.org/blog/Wh36WiQAAMMApuRb/introducing-shield-tournaments) is unique.
The winner keeps it for one month,
then must defend it during the next $name Shield tournament!""",
      homepageHours = 6.some
    )
