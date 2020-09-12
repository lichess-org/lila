package lila.study

import play.api.libs.ws.StandaloneWSClient

final private class PgnFetch(ws: StandaloneWSClient) {

  private type Pgn = String
  private val pgnContentType = "application/x-chess-pgn"

  // http://www.chessgames.com/perl/chessgame?gid=1427487
  // http://www.chessgames.com/perl/nph-chesspgn?text=1&gid=1427487
  // http://www.chessgames.com/pgn/boleslavsky_ufimtsev_1944.pgn?gid=1427487
  private val ChessbaseRegex = """chessgames\.com/.*[\?&]gid=(\d+)""".r.unanchored

  def fromUrl(url: String): Fu[Option[Pgn]] =
    url match {
      case ChessbaseRegex(id) => id.toIntOption ?? downloadChessbase
      case _                  => fuccess(none)
    }

  private def downloadChessbase(id: Int): Fu[Option[Pgn]] = {
    ws.url(s"""http://www.chessgames.com/pgn/any.pgn?gid=$id""").get().dmap { res =>
      res.header("Content-Type").contains(pgnContentType) option res.body
    }
  }
}
