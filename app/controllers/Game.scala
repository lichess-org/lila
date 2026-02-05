package controllers

import play.api.mvc.*

import java.time.format.DateTimeFormatter

import lila.api.GameApiV2
import lila.app.{ *, given }
import lila.core.id.GameAnyId
import chess.Ply
import chess.format.SimpleFen
import chess.format.Uci

final class Game(env: Env, apiC: => Api) extends LilaController(env):

  def bookmark(gameId: GameId, ply: Ply) = AuthOrScopedBody(_.Web.Mobile) { _ ?=> me ?=>
    val fen = getAs[SimpleFen]("fen")
    val uci = get("uci")
    lila.log("FEN").info(fen.toString())
    lila.log("UCI").info(uci.toString())
    env.bookmark.api
      .toggle(env.round.gameProxy.updateIfPresent)(gameId, me, getBoolOpt("v"), ply, fen, uci)
      .inject(NoContent)
  }

  def delete(gameId: GameId) = Auth { _ ?=> me ?=>
    Found(env.game.gameRepo.game(gameId)): game =>
      if game.pgnImport.flatMap(_.user).exists(me.is(_)) then
        for
          _ <- env.bookmark.api.removeByGameId(game.id)
          _ <- env.game.gameRepo.remove(game.id)
          _ <- env.analyse.analysisRepo.remove(game.id)
          _ <- env.game.cached.clearNbImportedByCache(me)
        yield Redirect(routes.User.show(me.username))
      else Redirect(routes.Round.watcher(game.id, game.naturalOrientation))
  }

  def exportOne(id: GameAnyId) = AnonOrScoped():
    exportGame(id.gameId)

  private[controllers] def exportGame(gameId: GameId)(using Context): Fu[Result] =
    Found(env.round.proxyRepo.gameIfPresentOrFetch(gameId)): game =>
      val config = GameApiV2.OneConfig(
        format = GameApiV2.Format.byRequest,
        imported = getBool("imported"),
        flags = requestPgnFlags(extended = true)
      )
      for
        content <- env.api.gameApiV2.exportOne(game, config)
        filename <- env.api.gameApiV2.filename(game, config.format)
      yield Ok(content)
        .asAttachment(filename)
        .withHeaders(headersForApiOrApp*)
        .as(gameContentType(config))

  def exportByUser(username: UserStr) = OpenOrScoped()(handleExport(username))
  def apiExportByUser(username: UserStr) = OpenOrScoped()(handleExport(username))

  private def handleExport(username: UserStr)(using ctx: Context) =
    meOrFetch(username).flatMap:
      _.filter(u => u.enabled.yes || ctx.is(u) || isGrantedOpt(_.GamesModView)).so: user =>
        val format = GameApiV2.Format.byRequest
        WithVs: vs =>
          env.security.ipTrust
            .throttle(MaxPerSecond:
              if ctx.is(UserId.explorer) then env.web.settings.apiExplorerGamesPerSecond.get()
              else if ctx.is(user) then 60
              else if ctx.isOAuth then 30 // bonus for oauth logged in only (not for CSRF)
              else 25)
            .flatMap: perSecond =>
              val finished = getBoolOpt("finished") | true
              val config = GameApiV2.ByUserConfig(
                user = user,
                format = format,
                vs = vs,
                since = getTimestamp("since"),
                until = getTimestamp("until"),
                max = getIntAs[Max]("max").map(_.atLeast(1)),
                rated = getBoolOpt("rated"),
                perfKey = get("perfType").orZero.split(",").flatMap { PerfKey(_) }.toSet,
                color = get("color").flatMap(Color.fromName),
                analysed = getBoolOpt("analysed"),
                flags = requestPgnFlags(extended = false),
                sort =
                  if get("sort").has("dateAsc") then GameApiV2.GameSort.DateAsc
                  else GameApiV2.GameSort.DateDesc,
                perSecond = perSecond,
                ongoing = getBool("ongoing") || !finished,
                finished = finished
              )
              if ctx.is(UserId.explorer) then
                Ok.chunked(env.api.gameApiV2.exportByUser(config))
                  .noProxyBuffer
                  .as(gameContentType(config))
              else
                apiC
                  .GlobalConcurrencyLimitPerIpAndUserOption(user.some)(
                    env.api.gameApiV2.exportByUser(config)
                  ): source =>
                    Ok.chunked(source)
                      .asAttachmentStream:
                        s"lichess_${user.username}_${fileDate}.${format.toString.toLowerCase}"
                      .as(gameContentType(config))

  private def fileDate = DateTimeFormatter.ofPattern("yyyy-MM-dd").print(nowInstant)

  def apiExportByUserImportedGames() = AuthOrScoped() { ctx ?=> me ?=>
    apiC.GlobalConcurrencyLimitPerIpAndUserOption(me.some)(
      env.api.gameApiV2.exportUserImportedGames(me)
    ): source =>
      Ok.chunked(source)
        .asAttachmentStream(s"lichess_${me.username}_$fileDate.imported.pgn")
        .as(pgnContentType)
  }

  def apiExportByUserBookmarks() = Scoped() { ctx ?=> me ?=>
    val config = GameApiV2.BookmarkConfig(
      user = me.userId,
      format = GameApiV2.Format.byRequest,
      since = getTimestamp("since"),
      until = getTimestamp("until"),
      max = getIntAs[Max]("max").map(_.atLeast(1)),
      flags = requestPgnFlags(extended = false),
      sort =
        if get("sort").has("dateAsc") then GameApiV2.GameSort.DateAsc
        else GameApiV2.GameSort.DateDesc,
      perSecond = MaxPerSecond(30)
    )
    apiC.GlobalConcurrencyLimitPerIpAndUserOption(me.some)(
      env.api.gameApiV2.exportUserBookmarks(config)
    ): source =>
      Ok.chunked(source)
        .asAttachmentStream:
          s"lichess_${me.username}_$fileDate.bookmarks.${config.format.toString.toLowerCase}"
        .as(gameContentType(config))
  }

  def exportByIds = AnonOrScopedBody(parse.tolerantText)(): ctx ?=>
    val (limit, perSec) = if ctx.me.exists(_.isVerifiedOrChallengeAdmin) then (600, 100) else (300, 30)
    val config = GameApiV2.ByIdsConfig(
      ids = GameId.from(ctx.body.body.split(',').view.take(limit).toSeq),
      format = GameApiV2.Format.byRequest,
      flags = requestPgnFlags(extended = false),
      perSecond = MaxPerSecond(perSec),
      playerFile = get("players")
    )
    apiC.GlobalConcurrencyLimitPerIP
      .download(req.ipAddress)(env.api.gameApiV2.exportByIds(config)): source =>
        Ok.chunked(source).as(gameContentType(config)).noProxyBuffer

  private def WithVs(f: Option[lila.user.User] => Fu[Result])(using Context): Fu[Result] =
    getUserStr("vs").fold(f(none)): name =>
      meOrFetch(name).flatMap:
        _.fold[Fu[Result]](notFoundJson(s"No such opponent: $name")): user =>
          f(user.some)

  private[controllers] def requestPgnFlags(extended: Boolean)(using RequestHeader, Option[Me]) =
    lila.game.PgnDump.WithFlags(
      moves = getBoolOpt("moves") | true,
      tags = getBoolOpt("tags") | true,
      clocks = getBoolOpt("clocks") | extended,
      evals = getBoolOpt("evals") | extended,
      opening = getBoolOpt("opening") | extended,
      literate = getBool("literate"),
      pgnInJson = getBool("pgnInJson"),
      delayMoves = delayMovesFromReq,
      lastFen = getBool("lastFen"),
      accuracy = getBool("accuracy"),
      division = getBoolOpt("division") | extended,
      bookmark = getBool("withBookmarked")
    )

  private[controllers] def delayMovesFromReq(using RequestHeader)(using me: Option[Me]) =
    val trusted = get("key").exists(env.web.settings.noDelaySecret.get().value.contains) ||
      me.exists(_.is(UserId.ttt))
    !trusted

  private[controllers] def gameContentType(config: GameApiV2.Config) =
    config.format match
      case GameApiV2.Format.PGN => pgnContentType
      case GameApiV2.Format.JSON =>
        config match
          case _: GameApiV2.OneConfig => JSON
          case _ => ndJson.contentType

  private[controllers] def preloadUsers(game: lila.core.game.Game): Funit =
    env.user.lightUserApi.preloadMany(game.userIds)
  private[controllers] def preloadUsers(users: lila.core.user.GameUsers): Unit =
    env.user.lightUserApi.preloadUsers(users.all.collect:
      case Some(lila.core.user.WithPerf(u, _)) => u)
