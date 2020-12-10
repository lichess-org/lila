package lila.app
package templating

import chess.format.Forsyth
import chess.{ Status => S, Color, Clock, Mode }
import controllers.routes
import play.api.i18n.Lang

import lila.api.Context
import lila.app.ui.ScalatagsTemplate._
import lila.game.{ Game, Namer, Player, Pov }
import lila.i18n.{ I18nKeys => trans, defaultLang }
import lila.user.{ Title, User }

trait GameHelper { self: I18nHelper with UserHelper with AiHelper with StringHelper with ShogigroundHelper =>

  private val dataLive     = attr("data-live")
  private val dataColor    = attr("data-color")
  private val dataFen      = attr("data-fen")
  private val dataLastmove = attr("data-lastmove")

  def netBaseUrl: String
  def cdnUrl(path: String): String

  def povOpenGraph(pov: Pov) =
    lila.app.ui.OpenGraph(
      image = cdnUrl(routes.Page.notSupported().url).some, // routes.Export.gameThumbnail(pov.gameId).url
      title = titleGame(pov.game),
      url = s"$netBaseUrl${routes.Round.watcher(pov.gameId, pov.color.name).url}",
      description = describePov(pov)
    )

  def titleGame(g: Game) = {
    val speed   = chess.Speed(g.clock.map(_.config)).name
    val variant = g.variant.exotic ?? s" ${g.variant.name}"
    s"$speed$variant Shogi • ${playerText(g.whitePlayer)} vs ${playerText(g.blackPlayer)}"
  }

  def describePov(pov: Pov) = {
    import pov._
    val p1 = playerText(player, withRating = true)
    val p2 = playerText(opponent, withRating = true)
    val speedAndClock =
      if (game.imported) "imported"
      else
        game.clock.fold(chess.Speed.Correspondence.name) { c =>
          s"${chess.Speed(c.config).name} (${c.config.show})"
        }
    val mode = game.mode.name
    val variant =
      if (game.variant == chess.variant.FromPosition) "position setup shogi"
      else if (game.variant.exotic) game.variant.name
      else "shogi"
    import chess.Status._
    val result = (game.winner, game.loser, game.status) match {
      case (Some(w), _, Mate)                               => s"${playerText(w)} won by checkmate"
      case (_, Some(l), Resign | Timeout | Cheat | NoStart) => s"${playerText(l)} resigned"
      case (_, Some(l), Outoftime)                          => s"${playerText(l)} forfeits by time"
      case (Some(w), _, UnknownFinish)                      => s"${playerText(w)} won"
      case (Some(w), _, Stalemate)                          => s"${playerText(w)} won by stalemate"
      case (Some(w), _, Impasse)                            => s"${playerText(w)} won by impasse"
      case (_, Some(l), PerpetualCheck)                     => s"${playerText(l)} lost due to perpetual check"
      case (_, _, Draw | UnknownFinish)                     => "Game is a draw"
      case (_, _, Aborted)                                  => "Game has been aborted"
      case (_, _, VariantEnd) =>
        game.variant match {
          case _ => "Perpetual check"
        }
      case _ => "Game is still being played"
    }
    val moves = s"${game.chess.fullMoveNumber} moves"
    s"$p1 plays $p2 in a $mode $speedAndClock game of $variant. $result after $moves. Click to replay, analyse, and discuss the game!"
  }

  def variantName(variant: chess.variant.Variant)(implicit lang: Lang) =
    variant match {
      case chess.variant.Standard     => trans.standard.txt()
      case chess.variant.FromPosition => trans.fromPosition.txt()
      case v                          => v.name
    }

  def variantNameNoCtx(variant: chess.variant.Variant) = variantName(variant)(defaultLang)

  def shortClockName(clock: Option[Clock.Config])(implicit lang: Lang): Frag =
    clock.fold[Frag](trans.unlimited())(shortClockName)

  def shortClockName(clock: Clock.Config): Frag = raw(clock.show)

  def modeName(mode: Mode)(implicit lang: Lang): String =
    mode match {
      case Mode.Casual => trans.casual.txt()
      case Mode.Rated  => trans.rated.txt()
    }

  def modeNameNoCtx(mode: Mode): String = modeName(mode)(defaultLang)

  def playerUsername(player: Player, withRating: Boolean = true, withTitle: Boolean = true): Frag =
    player.aiLevel.fold[Frag](
      player.userId.flatMap(lightUser).fold[Frag](lila.user.User.anonymous) { user =>
        val title = user.title ifTrue withTitle map { t =>
          frag(
            span(
              cls := "title",
              (Title(t) == Title.BOT) option dataBotAttr,
              st.title := Title titleName Title(t)
            )(t),
            " "
          )
        }
        if (withRating) frag(title, user.name, " ", "(", lila.game.Namer ratingString player, ")")
        else frag(title, user.name)
      }
    ) { level =>
      raw(s"A.I. level $level")
    }

  def playerText(player: Player, withRating: Boolean = false) =
    Namer.playerTextBlocking(player, withRating)(lightUser)

  def gameVsText(game: Game, withRatings: Boolean = false): String =
    Namer.gameVsTextBlocking(game, withRatings)(lightUser)

  val berserkIconSpan = iconTag("`")
  val statusIconSpan  = i(cls := "status")

  def playerLink(
      player: Player,
      cssClass: Option[String] = None,
      withOnline: Boolean = true,
      withRating: Boolean = true,
      withDiff: Boolean = true,
      engine: Boolean = false,
      withStatus: Boolean = false,
      withBerserk: Boolean = false,
      mod: Boolean = false,
      link: Boolean = true
  )(implicit lang: Lang): Frag = {
    val statusIcon =
      if (withStatus) statusIconSpan.some
      else if (withBerserk && player.berserk) berserkIconSpan.some
      else none
    player.userId.flatMap(lightUser) match {
      case None =>
        val klass = cssClass.??(" " + _)
        span(cls := s"user-link$klass")(
          (player.aiLevel, player.name) match {
            case (Some(level), _) => aiNameFrag(level, withRating)
            case (_, Some(name))  => name
            case _                => User.anonymous
          },
          statusIcon
        )
      case Some(user) =>
        frag(
          (if (link) a else span)(
            cls := userClass(user.id, cssClass, withOnline),
            href := s"${routes.User show user.name}${if (mod) "?mod" else ""}"
          )(
            withOnline option frag(lineIcon(user), " "),
            playerUsername(player, withRating),
            (player.ratingDiff ifTrue withDiff) map { d =>
              frag(" ", showRatingDiff(d))
            },
            engine option span(
              cls := "engine_mark",
              title := trans.thisAccountViolatedTos.txt()
            )
          ),
          statusIcon
        )
    }
  }

  def gameEndStatus(game: Game)(implicit lang: Lang): String =
    game.status match {
      case S.Aborted => trans.gameAborted.txt()
      case S.Mate    => trans.checkmate.txt()
      case S.Resign =>
        game.loser match {
          case Some(p) if p.color.white => trans.blackResigned.txt() // swapped
          case _                        => trans.whiteResigned.txt()
        }
      case S.UnknownFinish => trans.finished.txt()
      case S.Stalemate     => trans.stalemate.txt()
      case S.Impasse       => "Impasse"
      case S.PerpetualCheck=> "Perpetual Check"
      case S.Timeout =>
        game.loser match {
          case Some(p) if p.color.white => trans.blackLeftTheGame.txt() // swapped
          case Some(_)                  => trans.whiteLeftTheGame.txt()
          case None                     => trans.draw.txt()
        }
      case S.Draw      => trans.draw.txt()
      case S.Outoftime => trans.timeOut.txt()
      case S.NoStart => {
        val color = if(game.loser.fold(Color.black)(_.color).name.capitalize == "White") "Sente" else "Gote" // swapped   
        s"$color didn't move"
      }
      case S.Cheat => "Cheat detected"
      case S.VariantEnd =>
        game.variant match {
          case chess.variant.KingOfTheHill => trans.kingInTheCenter.txt()
          case chess.variant.ThreeCheck    => trans.threeChecks.txt()
          case chess.variant.RacingKings   => trans.raceFinished.txt()
          case _                           => trans.variantEnding.txt()
        }
      case _ => ""
    }

  private def gameTitle(game: Game, color: Color): String = {
    val u1 = playerText(game player color, withRating = true)
    val u2 = playerText(game opponent color, withRating = true)
    val clock = game.clock ?? { c =>
      " • " + c.config.show
    }
    val variant = game.variant.exotic ?? s" • ${game.variant.name}"
    s"$u1 vs $u2$clock$variant"
  }

  // whiteUsername 1-0 blackUsername
  def gameSummary(whiteUserId: String, blackUserId: String, finished: Boolean, result: Option[Boolean]) = {
    val res = if (finished) chess.Color.showResult(result map Color.apply) else "*"
    s"${usernameOrId(whiteUserId)} $res ${usernameOrId(blackUserId)}"
  }

  def gameResult(game: Game) =
    if (game.finished) chess.Color.showResult(game.winnerColor)
    else "*"

  def gameLink(
      game: Game,
      color: Color,
      ownerLink: Boolean = false,
      tv: Boolean = false
  )(implicit ctx: Context): String = {
    val owner = ownerLink ?? ctx.me.flatMap(game.player)
    if (tv) routes.Tv.index()
    else
      owner.fold(routes.Round.watcher(game.id, color.name)) { o =>
        routes.Round.player(game fullIdOf o.color)
      }
  }.toString

  def gameLink(pov: Pov)(implicit ctx: Context): String = gameLink(pov.game, pov.color)

  def gameFen(
      pov: Pov,
      ownerLink: Boolean = false,
      tv: Boolean = false,
      withTitle: Boolean = true,
      withLink: Boolean = true,
      withLive: Boolean = true
  )(implicit ctx: Context): Frag = {
    val game     = pov.game
    val isLive   = withLive && game.isBeingPlayed
    val cssClass = isLive ?? ("live mini-board-" + game.id)
    val variant  = game.variant.key
    val tag      = if (withLink) a else span
    tag(
      href := withLink.option(gameLink(game, pov.color, ownerLink, tv)),
      title := withTitle.option(gameTitle(game, pov.color)),
      cls := s"mini-board mini-board-${game.id} cg-wrap parse-fen is2d $cssClass $variant",
      dataLive := isLive.option(game.id),
      dataColor := pov.color.name,
      dataFen := Forsyth.exportBoard(game.board),
      dataLastmove := ~game.lastMoveKeys
    )(cgWrapContent)
  }

  def gameFenNoCtx(pov: Pov, tv: Boolean = false, blank: Boolean = false): Frag = {
    val isLive  = pov.game.isBeingPlayed
    val variant = pov.game.variant.key
    a(
      href := (if (tv) routes.Tv.index() else routes.Round.watcher(pov.gameId, pov.color.name)),
      title := gameTitle(pov.game, pov.color),
      cls := List(
        s"mini-board mini-board-${pov.gameId} cg-wrap parse-fen is2d $variant" -> true,
        s"live mini-board-${pov.gameId}"                                       -> isLive
      ),
      dataLive := isLive.option(pov.gameId),
      dataColor := pov.color.name,
      dataFen := Forsyth.exportBoard(pov.game.board),
      dataLastmove := ~pov.game.lastMoveKeys,
      target := blank.option("_blank")
    )(cgWrapContent)
  }

  def challengeTitle(c: lila.challenge.Challenge) = {
    val speed = c.clock.map(_.config).fold(chess.Speed.Correspondence.name) { clock =>
      s"${chess.Speed(clock).name} (${clock.show})"
    }
    val variant = c.variant.exotic ?? s" ${c.variant.name}"
    val challenger = c.challengerUser.fold(User.anonymous) { reg =>
      s"${usernameOrId(reg.id)} (${reg.rating.show})"
    }
    val players =
      if (c.isOpen) "Open challenge"
      else
        c.destUser.fold(s"Challenge from $challenger") { dest =>
          s"$challenger challenges ${usernameOrId(dest.id)} (${dest.rating.show})"
        }
    s"$speed$variant ${c.mode.name} Shogi • $players"
  }

  def challengeOpenGraph(c: lila.challenge.Challenge) =
    lila.app.ui.OpenGraph(
      title = challengeTitle(c),
      url = s"$netBaseUrl${routes.Round.watcher(c.id, chess.White.name).url}",
      description = "Join the challenge or watch the game here."
    )
}
