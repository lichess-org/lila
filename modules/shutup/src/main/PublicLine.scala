package lila.shutup

import org.joda.time.DateTime
import scala.util.Success

import lila.hub.actorApi.shutup.{ PublicSource => Source }

case class PublicLine(
    text: String,
    from: Option[Source],
    date: Option[DateTime]
)

object PublicLine {

  def make(text: String, from: Source): PublicLine =
    PublicLine(text, from.some, DateTime.now.some)

  import reactivemongo.api.bson._
  import lila.db.dsl._
  implicit private val SourceHandler = lila.db.dsl.quickHandler[Source](
    { case BSONString(v) =>
      v split ':' match {
        case Array("t", id)     => Source.Tournament(id)
        case Array("s", id)     => Source.Simul(id)
        case Array("w", gameId) => Source.Watcher(gameId)
        case Array("u", id)     => Source.Study(id)
        case Array("e", id)     => Source.Team(id)
        case Array(_, source)   => Source.Unknown(source)
        case _                  => Source.Unknown(v.take(32))
      }
    },
    x =>
      BSONString(x match {
        case Source.Tournament(id)  => s"t:$id"
        case Source.Simul(id)       => s"s:$id"
        case Source.Study(id)       => s"u:$id"
        case Source.Watcher(gameId) => s"w:$gameId"
        case Source.Team(id)        => s"e:$id"
        case Source.Unknown(source) => s"i:$source"
      })
  )

  private val objectHandler = Macros.handler[PublicLine]

  implicit val PublicLineBSONHandler = lila.db.dsl.tryHandler[PublicLine](
    {
      case doc: BSONDocument => objectHandler readTry doc
      case BSONString(text)  => Success(PublicLine(text, none, none))
      case a                 => lila.db.BSON.handlerBadValue(s"Invalid PublicLine $a")
    },
    x => if (x.from.isDefined) objectHandler.writeTry(x).get else BSONString(x.text)
  )
}
