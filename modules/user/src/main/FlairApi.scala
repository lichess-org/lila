package lila.user

import lila.core.user.{ FlairGet, FlairGetMap }

object FlairApi:

  private var db: Set[Flair] = Set.empty

  def exists(flair: Flair): Boolean = db.isEmpty || db(flair)

  def find(name: String): Option[Flair] = Flair(name).some.filter(exists)

  def formField(anyFlair: Boolean, asMod: Boolean): play.api.data.Mapping[Option[Flair]] =
    import play.api.data.Forms.*
    import lila.common.Form.into
    optional:
      text
        .into[Flair]
        .verifying(exists)
        .verifying(f => anyFlair || !adminFlairs(f) || asMod)

  def formPair(asMod: Boolean) =
    "flair" -> formField(anyFlair = false, asMod = asMod)

  val adminFlairs: Set[Flair] = Set(Flair("activity.lichess"))

final class FlairApi(lightUserApi: LightUserApi, getFile: lila.common.config.GetRelativeFile)(using Executor)(
    using scheduler: Scheduler
) extends lila.core.user.FlairApi:

  import FlairApi.*
  export FlairApi.{ find, formField, adminFlairs }

  given flairOf: FlairGet = id => lightUserApi.async(id).dmap(_.flatMap(_.flair))

  given flairsOf: FlairGetMap = ids =>
    lightUserApi
      .asyncMany(ids.distinct)
      .map: users =>
        val pairs = for
          uOpt <- users
          user <- uOpt
          flair <- user.flair
        yield user.id -> flair
        pairs.toMap

  private def refresh(): Unit =
    try refreshFrom(scala.io.Source.fromFile("public/flair/list.txt", "UTF-8"))
    catch
      case e: Exception =>
        logger.error("Cannot read flairs, trying alternative path", e)
        val pathname = getFile.exec("public/flair/list.txt").toPath.toString
        refreshFrom(scala.io.Source.fromFile(pathname, "UTF-8"))

  private def refreshFrom(source: scala.io.Source): Unit =
    try
      db = Flair.from(source.getLines.toSet)
      logger.info(s"Updated flair db with ${db.size} flairs")
    finally source.close()

  scheduler.scheduleOnce(11.seconds)(refresh())

  lila.common.Bus.sub[lila.core.net.AssetVersion.Changed]:
    case lila.core.net.AssetVersion.Changed(_) => refresh()
