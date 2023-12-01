package lila.user

import scala.io.Source

object UserFlairApi:

  private var db: Set[UserFlair] = Set.empty

  def exists(flair: UserFlair): Boolean = db.isEmpty || db(flair)

  private type GetterType          = UserId => Fu[Option[UserFlair]]
  opaque type Getter <: GetterType = GetterType
  object Getter extends TotalWrapper[Getter, GetterType]

  type FlairMap = Map[UserId, UserFlair]

final class UserFlairApi(
    lightUserApi: LightUserApi
)(using Executor)(using scheduler: akka.actor.Scheduler):

  import UserFlairApi.*

  val getter = Getter: id =>
    lightUserApi.async(id).dmap(_.flatMap(_.flair))

  def flairsOf(ids: List[UserId]): Fu[Map[UserId, UserFlair]] =
    lightUserApi.asyncMany(ids.distinct) map: users =>
      val pairs = for
        uOpt  <- users
        user  <- uOpt
        flair <- user.flair
      yield user.id -> flair
      pairs.toMap

  export lightUserApi.preloadMany

  private def refresh: Unit =
    val source = Source.fromFile("public/flair/list.txt", "UTF-8")
    try
      db = UserFlair from source.getLines.toSet
    finally
      source.close()

  scheduler.scheduleWithFixedDelay(5 seconds, 7 minutes): () =>
    refresh
