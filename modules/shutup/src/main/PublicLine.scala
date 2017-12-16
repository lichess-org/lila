package lila.shutup

import org.joda.time.DateTime
import lila.hub.actorApi.shutup.{ PublicSource => Source }

case class PublicLine(
    text: String,
    from: Option[Source],
    date: Option[DateTime]
)

object PublicLine {

  def make(text: String, from: Source): PublicLine =
    PublicLine(text, from.some, DateTime.now.some)

  import reactivemongo.bson._
  import lila.db.dsl._
  private implicit val SourceHandler = new BSONHandler[BSONString, Source] {
    def read(bs: BSONString): Source = bs.value split ':' match {
      case Array("t", id) => Source.Tournament(id)
      case Array("s", id) => Source.Simul(id)
      case Array("w", gameId) => Source.Watcher(gameId)
      case Array("u", id) => Source.Study(id)
      case a => sys error s"Invalid PublicLine source ${bs.value}"
    }
    def write(x: Source) = BSONString {
      x match {
        case Source.Tournament(id) => s"t:$id"
        case Source.Simul(id) => s"s:$id"
        case Source.Study(id) => s"u:$id"
        case Source.Watcher(gameId) => s"w:$gameId"
      }
    }
  }
  implicit val PublicLineBSONHandler = new BSONHandler[BSONValue, PublicLine] {
    private val objectHandler = Macros.handler[PublicLine]
    def read(bv: BSONValue): PublicLine = bv match {
      case doc: BSONDocument => objectHandler read doc
      case BSONString(text) => PublicLine(text, none, none)
      case a => sys error s"Invalid PublicLine $a"
    }
    def write(x: PublicLine) =
      if (x.from.isDefined) objectHandler write x
      else BSONString(x.text)
  }
}
