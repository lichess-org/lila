package lila.db

import dsl._

import lila.common.config.CollName

final class AsyncColl(val name: CollName, resolve: () => Fu[Coll])(implicit
    ec: scala.concurrent.ExecutionContext
) {

  def get: Fu[Coll] = resolve()

  def apply[A](f: Coll => Fu[A]) = get flatMap f

  def map[A](f: Coll => A): Fu[A] = get map f
}
