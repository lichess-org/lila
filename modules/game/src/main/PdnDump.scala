package lidraughts.game

import draughts.format.Forsyth
import draughts.format.pdn.{ Pdn, Tag, Tags, TagType, Parser, ParsedPdn }
import draughts.format.{ pdn => draughtsPdn }
import draughts.{ Centis, Color }

import lidraughts.common.LightUser

final class PdnDump(
    netBaseUrl: String,
    getLightUser: LightUser.GetterSync
) {

  import PdnDump._

  def apply(game: Game, initialFen: Option[String], flags: WithFlags): Pdn = {
    val imported = game.pdnImport.flatMap { pdni =>
      Parser.full(pdni.pdn).toOption
    }
    val ts = tags(game, initialFen, imported)
    val fenSituation = ts.fen.map(_.value) flatMap Forsyth.<<<
    val moves2 = fenSituation.??(_.situation.color.black).fold(".." +: game.pdnMoves, game.pdnMoves)
    val turns = makeTurns(
      moves2,
      fenSituation.map(_.fullMoveNumber) | 1,
      flags.clocks ?? ~game.bothClockStates,
      game.startColor
    )
    Pdn(ts, turns)
  }

  private val fileR = """[\s,]""".r

  def filename(game: Game): String = gameLightUsers(game) match {
    case (wu, bu) => fileR.replaceAllIn(
      "lidraughts_pdn_%s_%s_vs_%s.%s.pdn".format(
        Tag.UTCDate.format.print(game.createdAt),
        player(game.whitePlayer, wu),
        player(game.blackPlayer, bu),
        game.id
      ), "_"
    )
  }

  private def gameUrl(id: String) = s"$netBaseUrl/$id"

  private def gameLightUsers(game: Game): (Option[LightUser], Option[LightUser]) =
    (game.whitePlayer.userId ?? getLightUser) -> (game.blackPlayer.userId ?? getLightUser)

  private def rating(p: Player) = p.rating.fold("?")(_.toString)

  private def player(p: Player, u: Option[LightUser]) =
    p.aiLevel.fold(u.fold(p.name | lidraughts.user.User.anonymous)(_.name))("lidraughts AI level " + _)

  private val customStartPosition: Set[draughts.variant.Variant] = Set()

  private def eventOf(game: Game) = {
    val perf = game.perfType.fold("Standard")(_.name)
    game.tournamentId.map { id =>
      s"${game.mode} $perf tournament https://lidraughts.org/tournament/$id"
    } orElse game.simulId.map { id =>
      s"$perf simul https://lidraughts.org/simul/$id"
    } getOrElse {
      s"${game.mode} $perf game"
    }
  }

  private def ratingDiffTag(p: Player, tag: Tag.type => TagType) =
    p.ratingDiff.map { rd => Tag(tag(Tag), s"${if (rd >= 0) "+" else ""}$rd") }

  def tags(
    game: Game,
    initialFen: Option[String],
    imported: Option[ParsedPdn]
  ): Tags = gameLightUsers(game) match {
    case (wu, bu) => Tags {
      val importedDate = imported.flatMap(_.tags(_.Date))
      List[Option[Tag]](
        Tag(_.Event, imported.flatMap(_.tags(_.Event)) | { if (game.imported) "Import" else eventOf(game) }).some,
        Tag(_.Site, gameUrl(game.id)).some,
        Tag(_.Date, importedDate | Tag.UTCDate.format.print(game.createdAt)).some,
        Tag(_.Round, imported.flatMap(_.tags(_.Round)) | "-").some,
        Tag(_.White, player(game.whitePlayer, wu)).some,
        Tag(_.Black, player(game.blackPlayer, bu)).some,
        Tag(_.Result, result(game)).some,
        importedDate.isEmpty option Tag(_.UTCDate, imported.flatMap(_.tags(_.UTCDate)) | Tag.UTCDate.format.print(game.createdAt)),
        importedDate.isEmpty option Tag(_.UTCTime, imported.flatMap(_.tags(_.UTCTime)) | Tag.UTCTime.format.print(game.createdAt)),
        Tag(_.WhiteElo, rating(game.whitePlayer)).some,
        Tag(_.BlackElo, rating(game.blackPlayer)).some,
        ratingDiffTag(game.whitePlayer, _.WhiteRatingDiff),
        ratingDiffTag(game.blackPlayer, _.BlackRatingDiff),
        wu.flatMap(_.title).map { t => Tag(_.WhiteTitle, t) },
        bu.flatMap(_.title).map { t => Tag(_.BlackTitle, t) },
        Tag(_.GameType, game.variant.gameType).some,
        Tag.timeControl(game.clock.map(_.config)).some,
        Tag(_.ECO, game.opening.fold("?")(_.opening.eco)).some,
        Tag(_.Opening, game.opening.fold("?")(_.opening.name)).some,
        Tag(_.Termination, {
          import draughts.Status._
          game.status match {
            case Created | Started => "Unterminated"
            case Aborted | NoStart => "Abandoned"
            case Timeout | Outoftime => "Time forfeit"
            case Resign | Draw | Stalemate | Mate | VariantEnd => "Normal"
            case Cheat => "Rules infraction"
            case UnknownFinish => "Unknown"
          }
        }).some
      ).flatten ::: customStartPosition(game.variant).??(List(
          Tag(_.FEN, initialFen | "?")
        //Tag("SetUp", "1")
        ))
    }
  }

  private def makeTurns(moves: Seq[String], from: Int, clocks: Vector[Centis], startColor: Color): List[draughtsPdn.Turn] =
    (moves grouped 2).zipWithIndex.toList map {
      case (moves, index) =>
        val clockOffset = startColor.fold(0, 1)
        draughtsPdn.Turn(
          number = index + from,
          white = moves.headOption filter (".." !=) map { san =>
            draughtsPdn.Move(
              san = san,
              secondsLeft = clocks lift (index * 2 - clockOffset) map (_.roundSeconds)
            )
          },
          black = moves lift 1 map { san =>
            draughtsPdn.Move(
              san = san,
              secondsLeft = clocks lift (index * 2 + 1 - clockOffset) map (_.roundSeconds)
            )
          }
        )
    } filterNot (_.isEmpty)
}

object PdnDump {

  case class WithFlags(
      clocks: Boolean = true
  )

  def result(game: Game) =
    if (game.finished) Color.showResult(game.winnerColor)
    else "*"
}
