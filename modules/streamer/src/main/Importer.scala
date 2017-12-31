package lila.streamer

import com.typesafe.config.ConfigFactory
import reactivemongo.bson._
import scala.collection.JavaConversions._
import scala.util.{ Try, Success, Failure }

import lila.db.dsl._
import lila.user.UserRepo

private final class Importer(api: StreamerApi, flagColl: Coll) {

  import Streamer._

  def apply = flagColl.primitiveOne[String]($id("streamer"), "text") dmap (~_) flatMap { text =>
    val now = org.joda.time.DateTime.now
    validate(text)._1.map { s =>
      UserRepo named s.lichessName flatMap {
        _ ?? { user =>
          api.save(Streamer(
            _id = Id(s.lichessName.toLowerCase),
            listed = Listed(true),
            approved = Approved(true),
            autoFeatured = AutoFeatured(s.featured),
            chatEnabled = ChatEnabled(s.chat),
            picturePath = none,
            name = Name {
              s.streamerNameForDisplay.fold(user.realNameOrUsername)(removeTitle)
            },
            description = none,
            twitch = s.twitch option Twitch(s.streamerName, Live.empty),
            youTube = s.youtube option YouTube(s.streamerName, Live.empty),
            sorting = Sorting.empty,
            createdAt = now,
            updatedAt = now
          ))
        }
      }
    }.sequenceFu.void
  }

  private def removeTitle(name: String) = name.split(' ').toList match {
    case title :: rest if lila.user.User.titles.exists(_._1 == title) => rest mkString " "
    case _ => name
  }

  private def validate(text: String): (List[Importer.Streamer], List[Exception]) = Try {
    ConfigFactory.parseString(text).getConfigList("streamers").toList.map { c =>
      Try {
        Importer.Streamer(
          service = c getString "service" match {
            case s if s == "twitch" => Importer.Twitch
            case s if s == "youtube" => Importer.Youtube
            case s => sys error s"Invalid service name: $s"
          },
          streamerName = c getString "streamer_name",
          streamerNameForDisplay = Try(c getString "streamer_name_for_display").toOption,
          lichessName = lila.user.User.normalize(c getString "lichess_name"),
          featured = c.getBoolean("featured"),
          chat = c.getBoolean("chat")
        )
      }
    }.foldLeft(List.empty[Importer.Streamer] -> List.empty[Exception]) {
      case ((res, err), Success(r)) => (r :: res, err)
      case ((res, err), Failure(e: Exception)) =>
        lila.log("tv").warn("streamer", e)
        (res, e :: err)
      case (_, Failure(e)) => throw e
    }
  } match {
    case Failure(e: Exception) => (Nil, List(e))
    case Failure(e) => throw e
    case Success((x, y)) => (x.reverse, y.reverse)
  }
}

object Importer {

  sealed trait Service
  case object Twitch extends Service
  case object Youtube extends Service

  case class Streamer(
      service: Service,
      streamerName: String,
      streamerNameForDisplay: Option[String],
      lichessName: String,
      featured: Boolean,
      chat: Boolean
  ) {

    def showStreamerName = streamerNameForDisplay | streamerName

    def twitch = service == Twitch
    def youtube = service == Youtube
  }
}
