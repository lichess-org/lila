package lila.fide

import com.softwaremill.macwire.*
import play.api.libs.ws.StandaloneWSClient

import lila.core.config.CollName
import lila.core.fide.*
import lila.memo.{ CacheApi, PicfitApi, PicfitUrl }
import scalalib.paginator.Paginator

@Module
final class Env(
    db: lila.db.Db,
    cacheApi: CacheApi,
    picfitApi: PicfitApi,
    picfitUrl: PicfitUrl,
    ws: StandaloneWSClient
)(using
    Executor,
    akka.stream.Materializer
)(using mode: play.api.Mode, scheduler: Scheduler):

  val repo =
    FideRepo(
      playerColl = db(CollName("fide_player")),
      ratingColl = db(CollName("fide_player_rating")),
      federationColl = db(CollName("fide_federation")),
      followerColl = db(CollName("fide_player_follower"))
    )

  lazy val json = wire[FideJson]

  lazy val playerApi = wire[FidePlayerApi]

  lazy val federationApi = wire[FederationApi]

  lazy val paginator = wire[FidePaginator]

  def federationsOf: Federation.FedsOf = playerApi.federationsOf
  def federationNamesOf: Federation.NamesOf = playerApi.federationNamesOf
  def tokenize: Tokenize = FidePlayer.tokenize
  def guessPlayer: GuessPlayer = playerApi.guessPlayer.apply
  def getPlayer: GetPlayer = playerApi.get
  def getPlayerFollowers: GetPlayerFollowers = repo.follower.followers
  def photosJson: PhotosJson.Get = ids => playerApi.photos(ids).map(json.photosJson)

  def search(q: Option[String], page: Int = 1, order: FidePlayerOrder)(using
      me: Option[Me]
  ): Fu[Either[FidePlayer.WithFollow, Paginator[FidePlayer.WithFollow]]] =
    val query = q.so(_.trim)
    chess.FideId
      .from(query.toIntOption)
      .so(playerApi.fetch)
      .flatMap:
        case Some(player) =>
          me.so(repo.follower.isFollowing(_, player.id))
            .map(FidePlayer.WithFollow(player, _))
            .map(Left(_))
        case None => paginator.ordered(page, query, order).map(Right(_))

  private lazy val fideSync = wire[FidePlayerSync]

  if mode.isProd then
    scheduler.scheduleWithFixedDelay(1.hour, 1.hour): () =>
      if nowDateTime.getHour == 4
      then fideSync()

  lila.common.Cli.handle:
    case "fide" :: "player" :: "sync" :: Nil =>
      fideSync()
      fuccess("Updating the player database in the background.")
    case "fide" :: "player" :: "rip" :: fideId :: year :: Nil =>
      chess.FideId
        .from(fideId.toIntOption)
        .so(repo.player.setDeceasedYear(_, year.toIntOption))
        .inject("done")
