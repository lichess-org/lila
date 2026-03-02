package lila.puzzle

import scalalib.actor.AsyncActorSequencers
import scalalib.paginator.Paginator

import lila.core.i18n.I18nKey
import lila.db.dsl.{ *, given }
import lila.db.paginator.Adapter

final class PuzzleApi(
    colls: PuzzleColls,
    trustApi: PuzzleTrustApi,
    countApi: PuzzleCountApi,
    openingApi: PuzzleOpeningApi
)(using Executor, Scheduler):

  import BsonHandlers.given

  object puzzle:

    def find(id: PuzzleId): Fu[Option[Puzzle]] =
      colls.puzzle(_.byId[Puzzle](id))

    def of(user: User, page: Int): Fu[Paginator[Puzzle]] =
      colls.puzzle: coll =>
        Paginator(
          adapter = new Adapter[Puzzle](
            collection = coll,
            selector = $doc("users" -> user.id),
            projection = none,
            sort = $sort.desc("glicko.r")
          ),
          page,
          MaxPerPage(30)
        )

    def setIssue(id: PuzzleId, issue: String): Fu[Boolean] =
      colls.puzzle(_.updateField($id(id), Puzzle.BSONFields.issue, issue).map(_.n > 0))

    val reportDedup = scalalib.cache.OnceEvery[PuzzleId](7.days)

  private[puzzle] object round:

    def find(user: User, puzzleId: PuzzleId): Fu[Option[PuzzleRound]] =
      colls.round(_.byId[PuzzleRound](PuzzleRound.Id(user.id, puzzleId).toString))

    private[PuzzleApi] def exists(user: User, puzzleId: PuzzleId): Fu[Boolean] =
      colls.round(_.exists($id(PuzzleRound.Id(user.id, puzzleId).toString)))

    def upsert(r: PuzzleRound, angle: PuzzleAngle): Funit =
      val roundDoc = roundHandler.write(r) ++
        $doc(
          PuzzleRound.BSONFields.user -> r.id.userId,
          PuzzleRound.BSONFields.angle -> angle.some.filter(_ != PuzzleAngle.mix)
        )
      colls.round(_.update.one($id(r.id), roundDoc, upsert = true)).void

    def themes(id: PuzzleRound.Id): Fu[Option[List[PuzzleRound.Theme]]] =
      colls.round:
        _.primitiveOne[List[PuzzleRound.Theme]]($id(id), PuzzleRound.BSONFields.themes)

  object vote:

    private val sequencer = AsyncActorSequencers[PuzzleId](
      maxSize = Max(32),
      expiration = 1.minute,
      timeout = 3.seconds,
      name = "puzzle.vote",
      monitor = lila.log.asyncActorMonitor.highCardinality
    )

    def update(id: PuzzleId, user: User, vote: Boolean): Funit =
      lila.common.Uptime
        .startedSinceSeconds(30) // puzzle voting is expensive and often times out on startup
        .so:
          sequencer(id):
            round
              .find(user, id)
              .flatMapz: prevRound =>
                trustApi.vote(user, prevRound, vote).flatMapz { weight =>
                  val voteValue = (if vote then 1 else -1) * weight
                  lila.mon.puzzle.vote.count(vote, prevRound.win.yes).increment()
                  updatePuzzle(id, voteValue, prevRound.vote)
                    .zip(colls.round {
                      _.updateField($id(prevRound.id), PuzzleRound.BSONFields.vote, voteValue)
                    })
                    .void
                }
          .monSuccess(_.puzzle.vote.future)
            .recoverDefault

    private def updatePuzzle(puzzleId: PuzzleId, newVote: Int, prevVote: Option[Int]): Funit =
      colls.puzzle: coll =>
        import Puzzle.BSONFields as F
        coll
          .one[Bdoc](
            $id(puzzleId),
            $doc(F.voteUp -> true, F.voteDown -> true, F.day -> true, F.id -> false)
          )
          .flatMapz: doc =>
            val prevUp = ~doc.int(F.voteUp)
            val prevDown = ~doc.int(F.voteDown)
            val up = (prevUp + ~newVote.some.filter(0 <) - ~prevVote.filter(0 <)).atLeast(newVote)
            val down = (prevDown - ~newVote.some.filter(0 >) + ~prevVote.filter(0 >)).atLeast(-newVote)
            coll.update
              .one(
                $id(puzzleId),
                $set(
                  F.voteUp -> up.atLeast(0),
                  F.voteDown -> down.atLeast(0),
                  F.vote -> ((up - down).toFloat / (up + down)).atLeast(0).atMost(1)
                ) ++ {
                  newVote <= -100 &&
                  doc.getAsOpt[Instant](F.day).exists(_.isAfter(nowInstant.minusDays(1)))
                }.so($unset(F.day))
              )
              .void

  def angles: Fu[PuzzleAngle.All] = for
    themes <- theme.categorizedWithCount
    openings <- openingApi.collection
  yield PuzzleAngle.All(themes, openings)

  object theme:

    private[PuzzleApi] def categorizedWithCount: Fu[List[(I18nKey, List[PuzzleTheme.WithCount])]] =
      countApi.countsByTheme.map: counts =>
        PuzzleTheme.categorized.map: (cat, puzzles) =>
          cat -> puzzles.map: pt =>
            PuzzleTheme.WithCount(pt, counts.getOrElse(pt.key, 0))

    private def updateRoundThemes(puzzle: PuzzleId, themes: List[PuzzleRound.Theme], weight: Option[Int]) =
      import PuzzleRound.BSONFields as F
      $set(F.themes -> themes, F.puzzle -> puzzle, F.weight -> weight)

    def vote(id: PuzzleId, themeStr: String, vote: Option[Boolean])(using
        me: Me
    ): FuRaise[PuzzleTheme.VoteError, Unit] =
      import PuzzleRound.BSONFields as F
      import PuzzleTheme.VoteError.*
      PuzzleTheme
        .findDynamic(themeStr)
        .orElse:
          me.is(UserId.lichess).so(PuzzleTheme.findVisible(themeStr))
        .raiseIfNone(Fail(s"Unknown theme $themeStr"))
        .flatMap: theme =>
          if me.is(UserId.lichess) then lichessVote(id, theme.key, vote)
          else
            for
              puzRound <- round.find(me, id)
              puzRound <- puzRound.raiseIfNone(Fail(s"Puzzle $id not yet played"))
              newThemes <- PuzzleRound.themeVote(puzRound.themes)(theme.key, vote).raiseIfNone(Unchanged)
              update <-
                if newThemes.isEmpty || !PuzzleRound.themesLookSane(newThemes)
                then fuccess($unset(F.themes, F.puzzle).some)
                if vote.isEmpty then fuccess(updateRoundThemes(id, newThemes, none).some)
                else trustApi.theme(me).map2(t => updateRoundThemes(id, newThemes, t.some))
              _ <- update.so(up => colls.round(_.update.one($id(puzRound.id), up)).void)
              _ <- update.isDefined.so:
                colls.puzzle(_.updateField($id(puzRound.id.puzzleId), Puzzle.BSONFields.dirty, true)).void
            yield lila.mon.puzzle.vote.theme(theme.key.value, vote, puzRound.win.yes).increment()

    private def lichessVote(
        puzzleId: PuzzleId,
        theme: PuzzleTheme.Key,
        vote: Option[Boolean]
    ): FuRaise[PuzzleTheme.VoteError, Unit] =
      val roundId = PuzzleRound.Id(UserId.lichess, puzzleId)
      for
        prev <- round.themes(roundId)
        prev <- prev.raiseIfNone(PuzzleTheme.VoteError.Fail(s"Puzzle $puzzleId not yet tagged by lichess"))
        newThemes <- PuzzleRound.themeVote(prev)(theme, vote).raiseIfNone(PuzzleTheme.VoteError.Unchanged)
        _ <- colls.round(_.update.one($id(roundId), updateRoundThemes(puzzleId, newThemes, none)))
        _ <- colls.puzzle(_.updateField($id(puzzleId), Puzzle.BSONFields.dirty, true))
      yield ()

  object casual:

    private val store = scalalib.cache.ExpireSetMemo[String](30.minutes)

    private def key(user: User, id: PuzzleId) = s"${user.id}:${id}"

    def setCasualIfNotYetPlayed(user: User, puzzle: Puzzle): Funit =
      round.exists(user, puzzle.id).not.mapz(store.put(key(user, puzzle.id)))

    def apply(user: User, id: PuzzleId) = store.get(key(user, id))
