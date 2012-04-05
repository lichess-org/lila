package lila.system
package memo

import com.mongodb.casbah.MongoCollection
import com.mongodb.casbah.Imports._
import scalaz.effects._

final class EntryMemo(getId: () ⇒ IO[Option[Int]]) {

  private var privateId: Int = _

  refresh.unsafePerformIO

  def refresh = for {
    idOption ← getId() except (_ ⇒ io(none))
  } yield {
    privateId = idOption | 0
  }

  def ++ : IO[Int] = io {
    privateId = privateId + 1
    privateId
  }

  def id: Int = privateId
}
