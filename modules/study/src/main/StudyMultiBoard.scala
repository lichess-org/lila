package lidraughts.study

import play.api.libs.json._
import reactivemongo.bson._

import draughts.Color
import draughts.format.pdn.{ Tag, Tags }
import draughts.format.{ FEN, Uci }

import BSONHandlers._
import JsonView._
import lidraughts.db.dsl._
import lidraughts.common.MaxPerPage
import lidraughts.common.paginator.{ Paginator, PaginatorJson }
import lidraughts.db.paginator.Adapter

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
      val root = doc.getAs[Node.Root]("root").err("Preview missing root")
      val node =
        if (players.isDefined) root.lastMainlineNode
        else root
      ChapterPreview(
        id = doc.getAs[Chapter.Id]("_id") err "Preview missing id",
        name = doc.getAs[Chapter.Name]("name") err "Preview missing name",
        players = players,
        orientation = doc.getAs[Bdoc]("setup") flatMap { setup =>
          setup.getAs[Color]("orientation")
        } getOrElse Color.White,
        fen = node.fen,
        lastMove = node.moveOption.map(_.uci),
        playing = tags.flatMap(_(_.Result)) has "*"
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

  private implicit val previewWriter: Writes[ChapterPreview] = Json.writes[ChapterPreview]
}

object StudyMultiBoard {

  case class ChapterPreview(
      id: Chapter.Id,
      name: Chapter.Name,
      players: Option[ChapterPreview.Players],
      orientation: Color,
      fen: FEN,
      lastMove: Option[Uci],
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
