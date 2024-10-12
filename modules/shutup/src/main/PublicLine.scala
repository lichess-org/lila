package lila.shutup

import scala.util.Success

import lila.core.shutup.PublicSource as Source
import lila.db.dsl.given

case class PublicLine(text: String, from: Source, date: Instant)

object PublicLine:

  def make(text: String, from: Source): PublicLine =
    PublicLine(text.take(200), from, nowInstant)

  import reactivemongo.api.bson.*
  import lila.db.dsl.*
  private given BSONHandler[Source] = lila.db.dsl.tryHandler[Source](
    { case BSONString(v) =>
      v.split(':') match
        case Array("t", id) => Success(Source.Tournament(TourId(id)))
        case Array("s", id) => Success(Source.Simul(SimulId(id)))
        case Array("w", id) => Success(Source.Watcher(GameId(id)))
        case Array("u", id) => Success(Source.Study(StudyId(id)))
        case Array("e", id) => Success(Source.Team(TeamId(id)))
        case Array("i", id) => Success(Source.Swiss(SwissId(id)))
        case Array("f", id) => Success(Source.Forum(ForumPostId(id)))
        case Array("b", id) => Success(Source.Ublog(UblogPostId(id)))
        case _              => lila.db.BSON.handlerBadValue(s"Invalid PublicLine source $v")
    },
    x =>
      BSONString(x match
        case Source.Tournament(id)  => s"t:$id"
        case Source.Simul(id)       => s"s:$id"
        case Source.Study(id)       => s"u:$id"
        case Source.Watcher(gameId) => s"w:$gameId"
        case Source.Team(id)        => s"e:$id"
        case Source.Swiss(id)       => s"i:$id"
        case Source.Forum(id)       => s"f:$id"
        case Source.Ublog(id)       => s"b:$id"
      )
  )

  given BSONHandler[PublicLine] = Macros.handler
