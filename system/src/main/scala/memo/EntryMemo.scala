package lila.system
package memo

import com.mongodb.casbah.MongoCollection
import com.mongodb.casbah.Imports._
import scalaz.effects._

final class EntryMemo(getId: () => IO[Option[Int]]) {

  private var privateId: Int = _

  refresh.unsafePerformIO

  def refresh = for {
    idOption ← getId()
  } yield {
    privateId = idOption err "No last entry found"
  }

  def ++ : IO[Int] = io {
    privateId = privateId + 1
    privateId
  }

  def id: Int = privateId
}
