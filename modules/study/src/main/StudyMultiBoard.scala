package lila.study

import play.api.libs.json._
import reactivemongo.bson._

import chess.{ Color, Centis }
import chess.format.pgn.{ Tag, Tags }
import chess.format.{ FEN, Uci }

import BSONHandlers._
import JsonView._
import lila.db.dsl._
import lila.common.MaxPerPage
import lila.common.paginator.{ Paginator, PaginatorJson }
import lila.db.paginator.Adapter

final class StudyMultiBoard(
    chapterColl: Coll,
    maxPerPage: MaxPerPage
) {

  import StudyMultiBoard._

  def json(study: Study, page: Int, playing: Boolean): Fu[JsObject] = get(study, page, playing) map { p =>
    PaginatorJson(p)
  }

  private def get(study: Study, page: Int, playing: Boolean): Fu[Paginator[ChapterPreview]] = Paginator(
    adapter = new Adapter[ChapterPreview](
      collection = chapterColl,
      selector = $doc("studyId" -> study.id) ++ playing.??(playingSelector),
      projection = projection,
      sort = $sort asc "order"
    ),
    currentPage = page,
    maxPerPage = maxPerPage
  )

  private val playingSelector = $doc("tags" -> "Result:*")

  private val projection = $doc(
    "name" -> true,
    "tags" -> true,
    "root" -> true,
    "setup.orientation" -> true
  )

  private implicit val previewBSONReader = new BSONDocumentReader[ChapterPreview] {
    def read(doc: BSONDocument) = {
      val tags = doc.getAs[Tags]("tags")
      val players = tags flatMap ChapterPreview.players
      val playing = tags.flatMap(_(_.Result)) has "*"
      val root = doc.getAs[Node.Root]("root") err "Preview missing root"
      val node = (players.isDefined ?? root.mainline.lastOption) getOrElse root
      val parentClock = node.moveOption.isDefined ?? {
        root.mainline.lift(root.mainline.size - 2).flatMap(_.clock)
      }
      ChapterPreview(
        id = doc.getAs[Chapter.Id]("_id") err "Preview missing id",
        name = doc.getAs[Chapter.Name]("name") err "Preview missing name",
        players = players,
        orientation = doc.getAs[Bdoc]("setup") flatMap { setup =>
          setup.getAs[Color]("orientation")
        } getOrElse Color.White,
        node = node,
        parentClock = parentClock,
        playing = playing
      )
    }
  }

  private implicit val previewPlayerWriter: Writes[ChapterPreview.Player] = Writes[ChapterPreview.Player] { p =>
    Json.obj("name" -> p.name)
      .add("title" -> p.title)
      .add("rating" -> p.rating)
  }

  private implicit val previewPlayersWriter: Writes[ChapterPreview.Players] = Writes[ChapterPreview.Players] { players =>
    Json.obj("white" -> players.white, "black" -> players.black)
  }

  import lila.tree.Node.clockWrites

  private implicit val nodeWriter: Writes[RootOrNode] = Writes[RootOrNode] { n =>
    Json.obj("fen" -> n.fen).add("move" -> n.moveOption.map(_.uci)).add("clock" -> n.clock)
  }

  private implicit val previewWriter: Writes[ChapterPreview] = Json.writes[ChapterPreview]
}

object StudyMultiBoard {

  case class ChapterPreview(
      id: Chapter.Id,
      name: Chapter.Name,
      players: Option[ChapterPreview.Players],
      orientation: Color,
      node: RootOrNode,
      parentClock: Option[Centis],
      playing: Boolean
  )

  object ChapterPreview {

    case class Player(name: String, title: Option[String], rating: Option[Int])

    type Players = Color.Map[Player]

    def players(tags: Tags): Option[Players] = for {
      wName <- tags(_.White)
      bName <- tags(_.Black)
    } yield Color.Map(
      white = Player(wName, tags(_.WhiteTitle), tags(_.WhiteElo) flatMap parseIntOption),
      black = Player(bName, tags(_.BlackTitle), tags(_.BlackElo) flatMap parseIntOption)
    )
  }
}
