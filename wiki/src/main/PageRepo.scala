package lila.wiki

import lila.db.{ Repo, DbApi }
import lila.db.Implicits._

private[wiki] final class PageRepo(coll: ReactiveColl) extends Repo[Page](coll, Pages.json) {

  def clear: Funit = remove(select.all)
}
