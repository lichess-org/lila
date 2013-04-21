package lila.db
package api

import Implicits._

import reactivemongo.bson._
import reactivemongo.api.Cursor
import play.modules.reactivemongo.json.ImplicitBSONHandlers._
import play.api.libs.json._
import play.api.libs.iteratee._

object $enumerate {

  def bulk[A](cursor: Cursor[A], size: Int)(op: List[A] ⇒ Funit): Funit =
    cursor.enumerateBulks(size) run {
      Iteratee.foreach((objs: Iterator[A]) ⇒
        op(objs.toList)
      )(execontext)
    }
}
