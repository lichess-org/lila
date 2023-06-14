package lila.coach

import lila.db.dsl.{ *, given }
import lila.memo.PicfitApi
import lila.notify.NotifyApi
import lila.security.Granter
import lila.user.{ Holder, User, UserRepo }

final class CoachApi(
    coachColl: Coll,
    userRepo: UserRepo,
    picfitApi: PicfitApi,
    cacheApi: lila.memo.CacheApi,
    notifyApi: NotifyApi
)(using Executor):

  import BsonHandlers.given

  def byId[U](u: U)(using idOf: UserIdOf[U]): Fu[Option[Coach]] = coachColl.byId[Coach](idOf(u))

  def find(username: UserStr): Fu[Option[Coach.WithUser]] =
    userRepo byId username flatMapz find

  def find(user: User): Fu[Option[Coach.WithUser]] =
    Granter(_.Coach)(user).so:
      byId(user.id) dmap {
        _ map withUser(user)
      }

  def findOrInit(coach: Holder): Fu[Option[Coach.WithUser]] =
    Granter.is(_.Coach)(coach) so {
      find(coach.user) orElse {
        val c = Coach.WithUser(Coach make coach.user, coach.user)
        coachColl.insert.one(c.coach) inject c.some
      }
    }

  def isListedCoach(user: User): Fu[Boolean] =
    Granter(_.Coach)(user) so user.enabled.yes so user.marks.clean so coachColl.exists(
      $id(user.id) ++ $doc("listed" -> true)
    )

  def setSeenAt(user: User): Funit =
    Granter(_.Coach)(user) so coachColl.update.one($id(user.id), $set("user.seenAt" -> nowInstant)).void

  def setRating(userPre: User): Funit =
    Granter(_.Coach)(userPre).so:
      userRepo.byId(userPre.id) flatMapz { user =>
        coachColl.update.one($id(user.id), $set("user.rating" -> user.perfs.bestStandardRating)).void
      }

  def update(c: Coach.WithUser, data: CoachProfileForm.Data): Funit =
    coachColl.update
      .one(
        $id(c.coach.id),
        data(c.coach),
        upsert = true
      )
      .void

  def uploadPicture(c: Coach.WithUser, picture: PicfitApi.FilePart): Funit =
    picfitApi
      .uploadFile(s"coach:${c.coach.id}", picture, userId = c.user.id) flatMap { pic =>
      coachColl.update.one($id(c.coach.id), $set("picture" -> pic.id)).void
    }

  private val languagesCache = cacheApi.unit[Set[String]]:
    _.refreshAfterWrite(1 hour).buildAsyncFuture: _ =>
      coachColl.secondaryPreferred.distinctEasy[String, Set]("languages", $empty)

  def allLanguages: Fu[Set[String]] = languagesCache.get {}

  private val countriesCache = cacheApi.unit[Set[String]]:
    _.refreshAfterWrite(1 hour).buildAsyncFuture: _ =>
      userRepo.coll.secondaryPreferred
        .distinctEasy[String, Set](
          "profile.country",
          $doc("roles" -> lila.security.Permission.Coach.dbKey, "enabled" -> true)
        )
  def allCountries: Fu[Set[String]] = countriesCache.get {}

  private def withUser(user: User)(coach: Coach) = Coach.WithUser(coach, user)
