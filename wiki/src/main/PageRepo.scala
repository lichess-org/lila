package lila.wiki

import lila.db.{ Repo, DbApi }
import lila.db.Types._

private[wiki] final class PageRepo(coll: ReactiveColl) extends Repo[String, Page](coll, Pages.json) {

  def clear: Funit = remove(select.all)
}
