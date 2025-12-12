package lila.coach

import lila.core.perm.Granter
import lila.db.dsl.{ *, given }
import lila.memo.PicfitApi
import lila.rating.UserPerfsExt.bestStandardRating
import lila.memo.CacheApi.buildAsyncTimeout

final class CoachApi(
    coll: Coll,
    userRepo: lila.core.user.UserRepo,
    userApi: lila.core.user.UserApi,
    flagApi: lila.core.user.FlagApi,
    picfitApi: PicfitApi,
    cacheApi: lila.memo.CacheApi
)(using Executor, Scheduler):

  import BsonHandlers.given

  lila.common.Bus.sub[lila.core.user.UserDelete]: del =>
    coll.delete.one($id(del.id)).void

  def byId[U: UserIdOf](u: U): Fu[Option[Coach]] = coll.byId[Coach](u.id)

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
        coll.insert.one(c.coach).inject(c.some)
      })

  def isListedCoach(user: User): Fu[Boolean] =
    (canCoach(user) && user.enabled.yes && user.marks.clean).so:
      coll.secondary.exists($id(user.id) ++ $doc("listed" -> true))

  def setSeenAt(user: User): Funit =
    canCoach(user).so:
      coll.update.one($id(user.id), $set("user.seenAt" -> nowInstant)).void

  def updateRatingFromDb(user: User): Funit =
    canCoach(user).so:
      userApi.perfsOf(user).flatMap { perfs =>
        coll.update.one($id(perfs.id), $set("user.rating" -> perfs.bestStandardRating)).void
      }

  def update(c: Coach.WithUser, data: CoachProfileForm.Data): Funit =
    coll.update
      .one(
        $id(c.coach.id),
        data(c.coach),
        upsert = true
      )
      .void

  def uploadPicture(c: Coach.WithUser, picture: PicfitApi.FilePart): Funit =
    for
      pic <- picfitApi.uploadFile(picture, c.user.id, s"coach:${c.coach.id}".some, requestAutomod = false)
      _ <- coll.update.one($id(c.coach.id), $set("picture" -> pic.id))
    yield ()

  private val languagesCache = cacheApi.unit[Set[String]]:
    _.refreshAfterWrite(1.hour).buildAsyncTimeout(): _ =>
      coll.secondary.distinctEasy[String, Set]("languages", $empty)

  def allLanguages: Fu[Set[String]] = languagesCache.get {}

  private val countriesCache = cacheApi.unit[CountrySelection]:
    _.refreshAfterWrite(1.hour).buildAsyncTimeout(): _ =>
      import lila.core.user.FlagCode
      userRepo.coll.secondary
        .distinctEasy[FlagCode, Set](
          "profile.country",
          $doc("roles" -> lila.core.perm.Permission.Coach.dbKey, "enabled" -> true)
        )
        .map: codes =>
          flagApi.all
            .collect:
              case f if codes.contains(f.code) && !flagApi.nonCountries.contains(f.code) => f.code -> f.name
            .sortBy(_._2)
        .map(CountrySelection(_))

  def countrySelection: Fu[CountrySelection] = countriesCache.get {}
