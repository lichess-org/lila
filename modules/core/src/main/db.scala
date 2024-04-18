package lila.core
package db

import reactivemongo.api.bson.collection.BSONCollection
import alleycats.Zero

trait AsyncCollFailingSilently:
  def apply[A](f: BSONCollection => Fu[A])(using Zero[A]): Fu[A]
