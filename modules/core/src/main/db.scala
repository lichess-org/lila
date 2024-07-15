package lila.core
package db

import alleycats.Zero
import reactivemongo.api.bson.collection.BSONCollection

trait AsyncCollFailingSilently:
  def apply[A](f: BSONCollection => Fu[A])(using Zero[A]): Fu[A]
