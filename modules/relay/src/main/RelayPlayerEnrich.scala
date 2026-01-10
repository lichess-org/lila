package lila.relay

import chess.format.pgn.{ Tag, Tags }
import chess.{ FideId, PlayerName, PlayerTitle, IntRating }

import lila.core.socket.Sri
import lila.core.fide.{ PlayerToken, diacritics }
import lila.study.{ Chapter, ChapterRepo, StudyApi }

// used to change names and ratings of broadcast players
private case class RelayPlayerLine(
    name: Option[PlayerName],
    rating: Option[IntRating],
    title: Option[PlayerTitle],
    fideId: Option[FideId] = none
)

private object RelayPlayerLine:

  object tokenize:
    private val nonLetterRegex = """[^a-zA-Z0-9\s]+""".r
    private val splitRegex = """\W""".r
    private val titleRegex = """(?i)(dr|prof)\.""".r
    private val chessTitleRegex = s"""^(${chess.PlayerTitle.acronyms.mkString("|")} )""".r
    def apply(str: String): PlayerToken =
      val trimmed = str.trim.replaceAllIn(chessTitleRegex, "").trim
      splitRegex
        .split:
          java.text.Normalizer
            .normalize(trimmed, java.text.Normalizer.Form.NFD)
            .replace(",", " ")
            .replaceAllIn(titleRegex, "")
            .replaceAllIn(nonLetterRegex, "")
            .toLowerCase
        .toList
        .map(_.trim)
        .filter(_.nonEmpty)
        .distinct
        .sorted
        .mkString(" ")

  case class Ambiguous(name: PlayerName, players: List[RelayPlayerLine])

private case class RelayPlayersTextarea(text: String):

  def sortedText = text.linesIterator.toList.sorted.mkString("\n")

  lazy val parse: RelayPlayerLines = RelayPlayerLines:
    val lines = text.linesIterator
    lines.nonEmpty.so:
      text.linesIterator.take(1000).toList.flatMap(parse).toMap

  // Original name / Optional FideID / Optional title / Optional rating / Optional replacement name
  private def parse(line: String): Option[(PlayerName, RelayPlayerLine)] =
    val arr = line.split('/').map(_.trim)
    arr
      .lift(0)
      .map: fromName =>
        PlayerName(fromName) -> RelayPlayerLine(
          name = PlayerName.from(arr.lift(4).filter(_.nonEmpty)),
          rating = IntRating.from(arr.lift(3).flatMap(_.toIntOption)),
          title = arr.lift(2).flatMap(PlayerTitle.get),
          fideId = arr.lift(1).flatMap(_.toIntOption).map(FideId(_))
        )

private case class RelayPlayerLines(players: Map[PlayerName, RelayPlayerLine]):

  import RelayPlayerLine.tokenize

  def diff(prev: Option[RelayPlayerLines]): Option[RelayPlayerLines] =
    val prevPlayers = prev.so(_.players)
    val newPlayers =
      players.view
        .filter: (name, player) =>
          prevPlayers.get(name).forall(_ != player)
    newPlayers.nonEmpty.option(RelayPlayerLines(newPlayers.toMap))

  // With tokenized player names
  private lazy val tokenizedPlayers: Map[PlayerToken, RelayPlayerLine] =
    umlautifyPlayers(players).mapKeys(name => tokenize(name.value))

  // duplicated PlayerName with it's umlautified version
  private def umlautifyPlayers(players: Map[PlayerName, RelayPlayerLine]): Map[PlayerName, RelayPlayerLine] =
    players.foldLeft(players):
      case (map, (name, player)) =>
        map + (name.map(diacritics.remove) -> player)

  // With player names combinations.
  // For example, if the tokenized player name is "A B C D", the combinations will be:
  // A B, A C, A D, B C, B D, C D, A B C, A B D, A C D, B C D
  private lazy val combinationPlayers: Map[PlayerToken, List[RelayPlayerLine]] =
    val combinations = for
      (fullToken, player) <- tokenizedPlayers.toList
      words = fullToken.split(' ').filter(_.sizeIs > 1).toList
      size <- 2 to words.length.atMost(4)
      combination <- words.combinations(size)
    yield combination.mkString(" ") -> player
    combinations
      .foldLeft(Map.empty[PlayerToken, List[RelayPlayerLine]]):
        case (acc, (token, player)) =>
          acc + (token -> (player :: acc.getOrElse(token, Nil)))
      .view
      .mapValues(_.distinct)
      .toMap

  def update(games: RelayGames): (RelayGames, List[RelayPlayerLine.Ambiguous]) =
    games.foldLeft(Vector.empty -> Nil):
      case ((games, ambiguous), game) =>
        val (tags, ambi) = update(game.tags)
        (games :+ game.copy(tags = tags)) -> (ambi ::: ambiguous)

  def update(tags: Tags): (Tags, List[RelayPlayerLine.Ambiguous]) =
    Color.all.foldLeft(tags -> Nil):
      case ((tags, ambiguous), color) =>
        val name = tags.names(color)
        val matching = name.fold(Matching.NotFound)(findMatching)
        val newTags = tags ++ Tags:
          matching.match
            case Matching.Found(rp) =>
              List(
                rp.fideId.map(id => Tag(_.fideIds(color), id.toString)),
                rp.name.map(name => Tag(_.names(color), name)),
                rp.rating.map(rating => Tag(_.elos(color), rating.toString)),
                rp.title.map(title => Tag(_.titles(color), title.value))
              ).flatten
            case _ => Nil
        val newAmbiguous = matching match
          case Matching.Ambiguous(players) =>
            name.fold(ambiguous): name =>
              RelayPlayerLine.Ambiguous(name, players) :: ambiguous
          case _ => ambiguous
        (newTags, newAmbiguous)

  enum Matching:
    case Found(player: RelayPlayerLine)
    case NotFound
    case Ambiguous(players: List[RelayPlayerLine])

  private def findMatching(name: PlayerName): Matching =
    players
      .get(name)
      .map(Matching.Found.apply)
      .orElse:
        val token = tokenize(name.value)
        tokenizedPlayers
          .get(token)
          .map(Matching.Found.apply)
          .orElse:
            combinationPlayers
              .get(token)
              .map:
                case single :: Nil => Matching.Found(single)
                case multi => Matching.Ambiguous(multi)
      .getOrElse(Matching.NotFound)

private final class RelayPlayerEnrich(
    irc: lila.core.irc.IrcApi,
    roundRepo: RelayRoundRepo,
    fidePlayerApi: RelayFidePlayerApi,
    studyApi: StudyApi,
    chapterRepo: ChapterRepo
)(using Executor, akka.stream.Materializer):

  private val once = scalalib.cache.OnceEvery.hashCode[List[RelayPlayerLine.Ambiguous]](1.hour)

  def enrichAndReportAmbiguous(rt: RelayRound.WithTour)(games: RelayGames): RelayGames =
    rt.tour.players.fold(games): txt =>
      val (updated, ambiguous) = txt.parse.update(games)
      if ambiguous.nonEmpty && rt.tour.official && once(ambiguous) then
        def show(p: RelayPlayerLine): String = p.fideId.map(_.toString) | p.name.fold("?")(_.value)
        val players = ambiguous.map: a =>
          (a.name.value, a.players.map(show))
        irc.broadcastAmbiguousPlayers(rt.round.id, rt.fullName, players)
      updated

  /* When the players replacement text of a tournament is updated,
   * we go through all rounds of the tournament and immediately apply
   * the player replacements to all games.
   * Then we enrich all affected games based on the potentially new FIDE ID
   * of each player. */
  def onPlayerTextareaUpdate(tour: RelayTour, prev: RelayTour): Funit =
    tour.players.so:
      _.parse
        .diff(prev.players.map(_.parse))
        .so: newPlayers =>
          val enrichFromFideId = fidePlayerApi.enrichTags(tour)
          for
            studyIds <- roundRepo.studyIdsOf(tour.id)
            _ <- chapterRepo
              .byStudiesSource(studyIds)
              .mapAsync(1): chapter =>
                val (newTags, _) = newPlayers.update(chapter.tags)
                (newTags != chapter.tags).so:
                  enrichFromFideId(newTags)
                    .flatMap: enriched =>
                      val forcedReplacements = newTags.map(_.filterNot(enriched.value.contains))
                      val finalTags = enriched ++ forcedReplacements
                      val newName = Chapter.nameFromPlayerTags(finalTags)
                      studyApi.setTagsAndRename(
                        studyId = chapter.studyId,
                        chapterId = chapter.id,
                        tags = finalTags,
                        newName = newName.filter(_ != chapter.name)
                      )(lila.study.Who(chapter.ownerId, Sri("")))
              .run()
          yield ()
