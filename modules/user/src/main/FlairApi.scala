package lila.user

object FlairApi:

  private var db: Set[Flair] = Set.empty

  def exists(flair: Flair): Boolean = db.isEmpty || db(flair)

  private type GetterType          = UserId => Fu[Option[Flair]]
  opaque type Getter <: GetterType = GetterType
  object Getter extends TotalWrapper[Getter, GetterType]

  type FlairMap = Map[UserId, Flair]

  def formField(anyFlair: Boolean = false)(using by: Me): play.api.data.Mapping[Option[Flair]] =
    import play.api.data.Forms.*
    import lila.common.Form.into
    optional:
      text
        .into[Flair]
        .verifying(exists)
        .verifying(f => anyFlair || !adminFlairs(f) || by.isAdmin)

  def formPair(anyFlair: Boolean = false)(using by: Me) = "flair" -> formField(anyFlair)

  val adminFlairs: Set[Flair] = Set(Flair("activity.lichess"))

final class FlairApi(lightUserApi: LightUserApi)(using Executor)(using scheduler: akka.actor.Scheduler):

  import FlairApi.*

  val getter = Getter: id =>
    lightUserApi.async(id).dmap(_.flatMap(_.flair))

  def flairsOf(ids: List[UserId]): Fu[Map[UserId, Flair]] =
    lightUserApi.asyncMany(ids.distinct) map: users =>
      val pairs = for
        uOpt  <- users
        user  <- uOpt
        flair <- user.flair
      yield user.id -> flair
      pairs.toMap

  private def refresh(): Unit =
    val source = scala.io.Source.fromFile("public/flair/list.txt", "UTF-8")
    try
      db = Flair from source.getLines.toSet
      logger.info(s"Updated flair db with ${db.size} flairs")
    finally source.close()

  scheduler.scheduleOnce(11 seconds)(refresh())

  lila.common.Bus.subscribeFun("assetVersion"):
    case lila.common.AssetVersion.Changed(_) => refresh()
