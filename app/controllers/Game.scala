package controllers

import chess.json.Json.given
import play.api.mvc.*

import java.time.format.DateTimeFormatter

import lila.api.GameApiV2.*
import lila.app.{ *, given }
import lila.core.id.GameAnyId
import lila.game.PgnDump.requestPgnFlags

final class Game(env: Env, apiC: => Api) extends LilaController(env):

  def bookmark(gameId: GameId) = AuthOrScopedBody(_.Web.Mobile, _.Preference.Write) { _ ?=> me ?=>
    env.bookmark.api
      .toggle(env.round.gameProxy.updateIfPresent)(gameId, me, getBoolOpt("v"))
      .inject(NoContent)
  }

  def delete(gameId: GameId) = Auth { _ ?=> me ?=>
    Found(env.game.gameRepo.game(gameId)): game =>
      if game.pgnImport.flatMap(_.user).exists(me.is(_)) then
        for
          _ <- env.bookmark.api.removeByGameId(game.id)
          _ <- env.game.gameRepo.remove(game.id)
          _ <- env.analyse.repo.remove(game.id)
          _ <- env.game.cached.clearNbImportedByCache(me)
        yield Redirect(routes.User.show(me.username))
      else Redirect(routes.Round.watcher(game.id, game.naturalOrientation))
  }

  def exportOne(id: GameAnyId) = OpenOrScoped(): ctx ?=>
    if getBool("divisionOnly") then JsonOptionOk(env.game.divider.fetchAndDivide(id.gameId))
    else
      Found(env.round.proxyRepo.gameIfPresentOrFetch(id.gameId)): game =>
        given Option[Me] = ctx.isFullAuth.so(ctx.me)
        val config = OneConfig(
          format = Format.byRequest,
          imported = getBool("imported"),
          flags =
            requestPgnFlags(extended = ctx.isAuth || game.isStrongOrRecent, delayMoves = delayMovesFromReq)
        )
        for
          content <- env.api.gameApiV2.exportOne(game, config)
          filename <- env.api.gameApiV2.filename(game, config.format)
        yield Ok(content)
          .asAttachment(filename)
          .withHeaders(headersForApiOrApp*)
          .as(config.contentType)
          .publicCache(Option.when(ctx.isAnon)(if game.playable then 10.seconds else 1.day))
          .varyAccept

  def exportByUser(username: UserStr) = AuthOrScoped()(handleExport(username))
  def apiExportByUser(username: UserStr) = OpenOrScoped()(handleExport(username))

  private def handleExport(username: UserStr)(using ctx: Context) =
    NoCrawlers:
      meOrFetch(username).flatMap:
        _.filter(u => u.enabled.yes || ctx.is(u) || isGrantedOpt(_.GamesModView)).so: user =>
          val format = Format.byRequest
          gamePlayersFiltersFromReq.sequence.flatMap:
            case Left(err) => JsonBadRequest(err)
            case Right(players) =>
              env.security.ipTrust
                .throttle(MaxPerSecond:
                  if ctx.is(UserId.explorer) then env.web.settings.apiExplorerGamesPerSecond.get()
                  else if ctx.is(user) then 60
                  else if ctx.isOAuth then 30 // bonus for oauth logged in only (not for CSRF)
                  else 25)
                .flatMap: perSecond =>
                  val finished = getBoolOpt("finished") | true
                  val config = ByUserConfig(
                    user = user,
                    format = format,
                    players = players,
                    since = getTimestamp("since"),
                    until = getTimestamp("until"),
                    max = getIntAs[Max]("max").map(_.atLeast(1)),
                    rated = getBoolOpt("rated"),
                    perfKey = get("perfType").orZero.split(",").flatMap(PerfKey(_)).toSet,
                    color = getColor(),
                    analysed = getBoolOpt("analysed"),
                    flags = requestPgnFlags(extended = false, delayMoves = delayMovesFromReq),
                    sort =
                      if get("sort").has("dateAsc") then GameSort.DateAsc
                      else GameSort.DateDesc,
                    perSecond = perSecond,
                    ongoing = getBool("ongoing") || !finished,
                    finished = finished
                  )
                  if ctx.is(UserId.explorer) then
                    Ok.chunked(env.api.gameApiV2.exportByUser(config))
                      .noProxyBuffer
                      .as(config.contentType)
                  else
                    apiC
                      .GlobalConcurrencyLimitPerIpAndUserOption(user.some)(
                        env.api.gameApiV2.exportByUser(config)
                      ): source =>
                        Ok.chunked(source)
                          .asAttachmentStream:
                            s"lichess_${user.username}_${fileDate}.${format.toString.toLowerCase}"
                          .as(config.contentType)

  private def fileDate = DateTimeFormatter.ofPattern("yyyy-MM-dd").print(nowInstant)

  def apiExportByUserImportedGames() = AuthOrScoped() { ctx ?=> me ?=>
    val annotated = getBool("annotated")
    val config = ImportedConfig(
      user = me.userId,
      annotated = annotated,
      flags = requestPgnFlags(extended = annotated, delayMoves = delayMovesFromReq)
        .copy(literate = getBoolOpt("literate") | annotated)
    )
    apiC.GlobalConcurrencyLimitPerIpAndUserOption(me.some)(
      env.api.gameApiV2.exportUserImportedGames(config)
    ): source =>
      Ok.chunked(source).as(PGN).asAttachmentStream(s"lichess_${me.username}_$fileDate.imported.pgn")
  }

  def apiExportByUserBookmarks() = Scoped() { ctx ?=> me ?=>
    val config = BookmarkConfig(
      user = me.userId,
      format = Format.byRequest,
      since = getTimestamp("since"),
      until = getTimestamp("until"),
      max = getIntAs[Max]("max").map(_.atLeast(1)),
      flags = requestPgnFlags(extended = false, delayMoves = delayMovesFromReq),
      sort = if get("sort").has("dateAsc") then GameSort.DateAsc else GameSort.DateDesc,
      perSecond = MaxPerSecond(30)
    )
    apiC.GlobalConcurrencyLimitPerIpAndUserOption(me.some)(
      env.api.gameApiV2.exportUserBookmarks(config)
    ): source =>
      Ok.chunked(source)
        .asAttachmentStream:
          s"lichess_${me.username}_$fileDate.bookmarks.${config.format.toString.toLowerCase}"
        .as(config.contentType)
  }

  def exportByIds = AnonOrScopedBody(parse.tolerantText)(): ctx ?=>
    val (max, perSec) = if ctx.me.exists(_.isVerifiedOrChallengeAdmin) then (600, 100) else (300, 30)
    val config = ByIdsConfig(
      ids = GameId.from(ctx.body.body.split(',').view.take(max).toSeq),
      format = Format.byRequest,
      flags = requestPgnFlags(extended = false, delayMoves = delayMovesFromReq),
      perSecond = MaxPerSecond(perSec),
      playerFile = get("players")
    )
    limit.GlobalConcurrencyLimitPerIP
      .download(req.ipAddress)(env.api.gameApiV2.exportByIds(config)): source =>
        Ok.chunked(source).as(config.contentType).noProxyBuffer

  def gamePlayersFiltersFromReq(using
      me: Option[Me],
      req: Req
  ): Either[String, Fu[GamePlayersConfig]] =
    for
      vs <- Right(getUserStr("vs"))
      players = List(me.map(_.userId), vs.map(_.id)).flatten
      valid = (p: Option[UserStr]) => p.forall(p => players.exists(_.is(p)))
      wonBy <- Right(getUserStr("wonBy")).filterOrElse(valid, "Invalid wonBy")
      lostBy <- Right(getUserStr("lostBy")).filterOrElse(valid, "Invalid lostBy")
    yield vs.so(meOrFetch).map(GamePlayersConfig(_, wonBy.map(_.id), lostBy.map(_.id)))

  private[controllers] def delayMovesFromReq(using Req)(using me: Option[Me]) =
    val trusted = get("key").exists(env.web.settings.noDelaySecret.get().value.contains) ||
      me.exists(_.is(UserId.t3))
    !trusted

  private[controllers] def preloadUsers(game: lila.core.game.Game): Funit =
    env.user.lightUserApi.preloadMany(game.userIds)
  private[controllers] def preloadUsers(users: lila.core.user.GameUsers): Unit =
    env.user.lightUserApi.preloadUsers(users.all.collect:
      case Some(lila.core.user.WithPerf(u, _)) => u)
