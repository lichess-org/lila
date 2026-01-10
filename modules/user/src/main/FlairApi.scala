package lila.user

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

  scheduler.scheduleOnce(18.seconds)(refresh())

  lila.common.Bus.sub[lila.core.net.AssetVersion.Changed]:
    case lila.core.net.AssetVersion.Changed(_) => refresh()
