package lila.user

import scala.util.Try

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

  private[user] object badFlairs:
    private type Pair = (UserId, Flair)
    private var found: Set[Pair] = Set.empty
    def add(userId: UserId, flair: Flair): Unit = found += (userId -> flair)
    def flush(): Set[Pair] =
      val res = found
      found = Set.empty
      res

final class FlairApi(getFile: lila.common.config.GetRelativeFile)(using Executor)(using
    scheduler: Scheduler
) extends lila.core.user.FlairApi:

  import FlairApi.*
  export FlairApi.{ find, formField, adminFlairs }

  private def refresh(): Unit =
    val path1 = "public/flair/list.txt"
    val path2 = getFile.exec(path1).toPath.toString
    Try(refreshFrom(path1))
      .orElse(Try(refreshFrom(path2)))
      .recover:
        case e: Exception => throw Exception(s"Cannot read flairs from either $path1 or $path2", e)

  private def refreshFrom(path: String): Unit =
    val source = scala.io.Source.fromFile(path, "UTF-8")
    try
      db = Flair.from(source.getLines.toSet)
      logger.info(s"Updated flair db with ${db.size} flairs")
    finally source.close()

  scheduler.scheduleOnce(18.seconds)(refresh())

  lila.common.Bus.sub[lila.core.net.AssetVersion.Changed]:
    case lila.core.net.AssetVersion.Changed(_) => refresh()
