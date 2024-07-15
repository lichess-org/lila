package lila.coach

import lila.core.perm.Granter
import lila.db.dsl.{ *, given }
import lila.memo.PicfitApi
import lila.rating.UserPerfsExt.bestStandardRating

final class CoachApi(
    coachColl: Coll,
    userRepo: lila.core.user.UserRepo,
    userApi: lila.core.user.UserApi,
    flagApi: lila.core.user.FlagApi,
    picfitApi: PicfitApi,
    cacheApi: lila.memo.CacheApi
)(using Executor):

  import BsonHandlers.given

  def byId[U: UserIdOf](u: U): Fu[Option[Coach]] = coachColl.byId[Coach](u)

  def find(username: UserStr): Fu[Option[Coach.WithUser]] =
    userApi.byId(username).flatMapz(find)

  def canCoach = Granter.ofUser(_.Coach)

  def find(user: User): Fu[Option[Coach.WithUser]] =
    canCoach(user).so:
      byId(user.id).flatMapz: coach =>
        userApi.withPerfs(user).dmap(coach.withUser).dmap(some)

  def findOrInit(using me: Me): Fu[Option[Coach.WithUser]] =
    val user = me.value
    canCoach(user).so:
      find(user).orElse(userApi.withPerfs(user).flatMap { user =>
        val c = Coach.make(user).withUser(user)
        coachColl.insert.one(c.coach).inject(c.some)
      })

  def isListedCoach(user: User): Fu[Boolean] =
    canCoach(user).so:
      user.enabled.yes
        .so(user.marks.clean)
        .so(
          coachColl.exists(
            $id(user.id) ++ $doc("listed" -> true)
          )
        )

  def setSeenAt(user: User): Funit =
    canCoach(user).so:
      coachColl.update.one($id(user.id), $set("user.seenAt" -> nowInstant)).void

  def updateRatingFromDb(user: User): Funit =
    canCoach(user).so:
      userApi.perfsOf(user).flatMap { perfs =>
        coachColl.update.one($id(perfs.id), $set("user.rating" -> perfs.bestStandardRating)).void
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
      .uploadFile(s"coach:${c.coach.id}", picture, userId = c.user.id)
      .flatMap { pic =>
        coachColl.update.one($id(c.coach.id), $set("picture" -> pic.id)).void
      }

  private val languagesCache = cacheApi.unit[Set[String]]:
    _.refreshAfterWrite(1 hour).buildAsyncFuture: _ =>
      coachColl.secondaryPreferred.distinctEasy[String, Set]("languages", $empty)

  def allLanguages: Fu[Set[String]] = languagesCache.get {}

  private val countriesCache = cacheApi.unit[CountrySelection]:
    _.refreshAfterWrite(1 hour).buildAsyncFuture: _ =>
      import lila.core.user.Flag
      userRepo.coll.secondaryPreferred
        .distinctEasy[Flag.Code, Set](
          "profile.country",
          $doc("roles" -> lila.core.perm.Permission.Coach.dbKey, "enabled" -> true)
        )
        .map: codes =>
          ("all", "All countries") :: flagApi.all
            .collect:
              case f if codes.contains(f.code) && !flagApi.nonCountries.contains(f.code) => f.code -> f.name
            .sortBy(_._2)
        .map(CountrySelection(_))

  def countrySelection: Fu[CountrySelection] = countriesCache.get {}
