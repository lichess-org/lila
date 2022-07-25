package lila.forum

import chess.format.pgn.Pgn
import scala.concurrent.ExecutionContext
import scalatags.Text.all._

import lila.common.config
import lila.common.String.html.richText
import lila.game.{ Game, GameRepo, PgnDump }

final class ForumExpand(gameRepo: GameRepo, netDomain: config.NetDomain, pgnDump: PgnDump)(implicit
    ec: ExecutionContext
) {

  private val gameRegex =
    s"""$netDomain/(?:embed/)?(?:game/)?(\\w{8})(?:(?:/(white|black))|\\w{4}|)(#\\d+)?\\b""".r

  private val notGames =
    Set("training", "analysis", "insights", "practice", "features", "password", "streamer", "timeline")

  private object removeScheme {
    private val regex      = "^(?:https?://)?(.+)$".r
    def apply(url: String) = regex.replaceAllIn(url, m => Option(m group 1) | m.matched)
  }

  def one(text: String): Fu[Frag] =
    gameRegex
      .findAllMatchIn(text)
      .toList
      .flatMap { m =>
        Option(m group 1) filter { id =>
          !notGames(id)
        } map (m.matched -> _)
      }
      .map { case (matched, id) =>
        gameRepo.gameWithInitialFen(id) flatMap {
          _ ?? { g =>
            val url = removeScheme(matched)
            pgnDump(g.game, g.fen, pgnFlags) map { pgn => (url -> (g -> pgn)) } dmap some
          }
        }
      }
      .sequenceFu
      .map(_.flatten.toMap) map { matches =>
      def renderWith = (url: String, text: String) =>
        matches
          .get(url)
          .orElse(matches.get(removeScheme(url)))
          .fold[Frag](raw(url)) { case (_, pgn) =>
            div(cls := "lpv--autostart", attr("data-pgn") := pgn.toString, attr("data-showMoves") := true)
          }
          .render
      raw {
        lila.base.RawHtml.addLinks(text, expandImg = true, renderWith = renderWith.some)
      }
    }

  def many(texts: Seq[String]): Fu[Seq[Frag]] = texts.map(one).sequenceFu

  def manyPosts(posts: Seq[Post]): Fu[Seq[Post.WithFrag]] =
    many(posts.map(_.text)) map {
      _ zip posts map { case (body, post) =>
        Post.WithFrag(post, body)
      }
    }

  private def lichessPgnViewer(game: Game.WithInitialFen, pgn: Pgn): Frag =
    div(cls := "lpv", attr("data-pgn") := pgn.toString)

  private val pgnFlags = PgnDump.WithFlags(clocks = false, evals = false, opening = false)
}
