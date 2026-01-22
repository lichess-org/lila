package lila.shutup

import scala.util.{ Success, Failure, Try }

import lila.core.chat.PublicSource as Source
import lila.core.id.RelayRoundId

object PublicSource:

  import reactivemongo.api.bson.*
  given BSONHandler[Source] = lila.db.dsl.tryHandler[Source](
    { case BSONString(v) => shortNotation.read(v) },
    x => BSONString(shortNotation.write(x))
  )

  object shortNotation:
    def read(str: String): Try[Source] = str.split(':') match
      case Array("t", id) => Success(Source.Tournament(TourId(id)))
      case Array("s", id) => Success(Source.Simul(SimulId(id)))
      case Array("w", id) => Success(Source.Watcher(GameId(id)))
      case Array("u", id) => Success(Source.Study(StudyId(id)))
      case Array("e", id) => Success(Source.Team(TeamId(id)))
      case Array("i", id) => Success(Source.Swiss(SwissId(id)))
      case Array("f", id) => Success(Source.Forum(ForumPostId(id)))
      case Array("b", id) => Success(Source.Ublog(UblogPostId(id)))
      case Array("r", id) => Success(Source.Relay(RelayRoundId(id)))
      case _ => Failure(IllegalArgumentException(s"Invalid PublicLine shortNotation $str"))
    def write(s: Source): String = s match
      case Source.Tournament(id) => s"t:$id"
      case Source.Simul(id) => s"s:$id"
      case Source.Study(id) => s"u:$id"
      case Source.Watcher(gameId) => s"w:$gameId"
      case Source.Team(id) => s"e:$id"
      case Source.Swiss(id) => s"i:$id"
      case Source.Forum(id) => s"f:$id"
      case Source.Ublog(id) => s"b:$id"
      case Source.Relay(id) => s"r:$id"
      case Source.Player(gameId) => s"_:$gameId" // should not happen
