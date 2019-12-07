package lila.db

import reactivemongo.api._
import reactivemongo.api.bson._

import dsl._

final class AsyncColl(resolve: () => Fu[Coll]) {

  def get: Fu[Coll] = resolve()

  def apply[A](f: Coll => Fu[A]) = get flatMap f

  def map[A](f: Coll => A) = get map f

  def find(selector: Bdoc) = get.map(_.find(selector, none))

  def find(selector: Bdoc, proj: Bdoc) = get.map(_.find(selector, proj.some))
}
