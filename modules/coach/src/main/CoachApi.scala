package lila.coach

import lila.db.dsl.{ *, given }
import lila.memo.PicfitApi
import lila.notify.NotifyApi
import lila.security.Granter
import lila.user.{ Me, User, UserRepo }

final class CoachApi(
    coachColl: Coll,
    userRepo: UserRepo,
    picfitApi: PicfitApi,
    cacheApi: lila.memo.CacheApi,
    notifyApi: NotifyApi
)(using Executor):

  import BsonHandlers.given

  def byId[U: UserIdOf](u: U): Fu[Option[Coach]] = coachColl.byId[Coach](u)

  def find(username: UserStr): Fu[Option[Coach.WithUser]] =
    userRepo byId username flatMapz find

  def canCoach = Granter.of(_.Coach)

  def find(user: User): Fu[Option[Coach.WithUser]] =
    canCoach(user).so:
      byId(user.id).dmap:
        _ map withUser(user)

  def findOrInit(using me: Me): Fu[Option[Coach.WithUser]] =
    val user = me.value
    canCoach(user).so:
      find(user) orElse {
        val c = Coach.WithUser(Coach make user, user)
        coachColl.insert.one(c.coach) inject c.some
      }

  def isListedCoach(user: User): Fu[Boolean] =
    canCoach(user).so:
      user.enabled.yes so user.marks.clean so coachColl.exists(
        $id(user.id) ++ $doc("listed" -> true)
      )

  def setSeenAt(user: User): Funit =
    canCoach(user).so:
      coachColl.update.one($id(user.id), $set("user.seenAt" -> nowInstant)).void

  def setRating(userPre: User): Funit =
    canCoach(userPre).so:
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
