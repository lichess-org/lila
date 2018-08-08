package lidraughts.study

import play.api.libs.ws.WS
import play.api.Play.current

private final class PdnFetch {

  private type Pdn = String
  private val pdnContentType = "application/x-draughts-pdn"

  // http://www.chessgames.com/perl/chessgame?gid=1427487
  // http://www.chessgames.com/perl/nph-chesspgn?text=1&gid=1427487
  // http://www.chessgames.com/pgn/boleslavsky_ufimtsev_1944.pgn?gid=1427487
  private val ChessbaseRegex = """.*chessgames\.com/.*[\?&]gid=(\d+).*""".r

  def fromUrl(url: String): Fu[Option[Pdn]] = url match {
    //case ChessbaseRegex(id) => parseIntOption(id) ?? downloadChessbase
    case _ => fuccess(none)
  }

  private def downloadChessbase(id: Int): Fu[Option[Pdn]] = {
    WS.url(s"""http://www.chessgames.com/pgn/any.pgn?gid=$id""").get().map { res =>
      res.header("Content-Type").contains(pdnContentType) option res.body
    }
  }
}
