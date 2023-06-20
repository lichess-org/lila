package lila.shutup

import scala.util.Success
import lila.db.dsl.given

import lila.hub.actorApi.shutup.{ PublicSource as Source }

case class PublicLine(
    text: String,
    from: Option[Source],
    date: Option[Instant]
)

object PublicLine:

  def make(text: String, from: Source): PublicLine =
    PublicLine(text, from.some, nowInstant.some)

  import reactivemongo.api.bson.*
  import lila.db.dsl.*
  private given BSONHandler[Source] = lila.db.dsl.tryHandler[Source](
    { case BSONString(v) =>
      v split ':' match {
        case Array("t", id) => Success(Source.Tournament(TourId(id)))
        case Array("s", id) => Success(Source.Simul(SimulId(id)))
        case Array("w", id) => Success(Source.Watcher(GameId(id)))
        case Array("u", id) => Success(Source.Study(StudyId(id)))
        case Array("e", id) => Success(Source.Team(TeamId(id)))
        case Array("i", id) => Success(Source.Swiss(SwissId(id)))
        case _              => lila.db.BSON.handlerBadValue(s"Invalid PublicLine source $v")
      }
    },
    x =>
      BSONString(x match {
        case Source.Tournament(id)  => s"t:$id"
        case Source.Simul(id)       => s"s:$id"
        case Source.Study(id)       => s"u:$id"
        case Source.Watcher(gameId) => s"w:$gameId"
        case Source.Team(id)        => s"e:$id"
        case Source.Swiss(id)       => s"i:$id"
      })
  )

  private val objectHandler = Macros.handler[PublicLine]

  given BSONHandler[PublicLine] = lila.db.dsl.tryHandler[PublicLine](
    {
      case doc: BSONDocument => objectHandler readTry doc
      case BSONString(text)  => Success(PublicLine(text, none, none))
      case a                 => lila.db.BSON.handlerBadValue(s"Invalid PublicLine $a")
    },
    x => if (x.from.isDefined) objectHandler.writeTry(x).get else BSONString(x.text)
  )
