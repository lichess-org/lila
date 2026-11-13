package lila.study

import chess.format.UciPath
import chess.format.pgn.{ PgnStr, Tag, Tags }
import chess.variant.Standard
import chess.{ ByColor, Centis, Color, Outcome }
import play.api.libs.json.*
import reactivemongo.api.bson.*

import java.time.Instant

import lila.core.fide.Federation
import lila.core.pref.Pref
import lila.core.socket.Sri
import lila.core.study.Visibility
import lila.core.ublog.QualityFilter
import lila.db.dsl.{ *, given }
import lila.tree.Clock

import BSONHandlers.given
import ChapterPreview.bson.given
import ChapterPreview.json.given

class RelayClockTest extends munit.FunSuite:

  given Executor = scala.concurrent.ExecutionContext.global
  given Federation.Guess = _ => None

  private val studyId = StudyId("study123")
  private val chapterId = StudyChapterId("chapter1")
  private val ownerId = UserId("alice")
  private val at = Instant.ofEpochMilli(1_700_000_000_123L)
  private val root = StudyPgnImport
    .result(PgnStr("1. e4 {[%clk 00:01:00]} e5 {[%clk 00:00:40]} 2. Nf3 {[%clk 00:00:55]}"), Nil)
    .toOption
    .get
    .root
  private val tags = Tags(List(Tag(_.White, "Alice"), Tag(_.Black, "Bob"), Tag(_.Result, "*")))
  private val chapter = Chapter(
    id = chapterId,
    studyId = studyId,
    name = StudyChapterName("Game"),
    setup = Chapter.Setup(None, Standard, Color.White),
    root = root,
    tags = tags,
    order = 1,
    ownerId = ownerId,
    relay = Some(Chapter.relayInit.copy(path = root.mainlinePath, lastMoveAt = Some(at), clockAt = Some(at))),
    createdAt = Instant.now
  )

  test("clock age is signed and rounds each instant to epoch milliseconds"):
    val now = Instant.ofEpochSecond(-1, 999_500_000)
    val future = Instant.ofEpochSecond(0, 500_000)
    assertEquals(Chapter.clockAge(future, now), BigInt(-1))
    assertEquals(Chapter.clockAge(now, future), BigInt(1))
    assertEquals(Chapter.clockAge(at, at.plusMillis(1234)), BigInt(1234))

  test("clock age remains a number beyond the range of epoch milliseconds and Long subtraction"):
    assert(Chapter.clockAge(Instant.MIN, Instant.MAX) > BigInt(Long.MaxValue))
    assert(Chapter.clockAge(Instant.MAX, Instant.MIN) < BigInt(Long.MinValue))
    val bsonMinimum = Instant.ofEpochMilli(Long.MinValue)
    assert(Chapter.clockAge(bsonMinimum, Instant.EPOCH) > BigInt(Long.MaxValue))
    assert(ChapterRepo.validateClockAt(Some(bsonMinimum)).isSuccess)
    val extremePreview = Json.toJsObject(preview(Some(Instant.MAX)))
    assert((extremePreview \ "clockAge").as[BigDecimal] < BigDecimal(Long.MinValue))
    val extremeSocket = StudySocket.clockPayload(
      Position.Ref(chapterId, UciPath.root),
      None,
      None,
      Some(Some(Instant.MIN))
    )
    assert((extremeSocket \ "clockAge").as[BigDecimal] > BigDecimal(Long.MaxValue))

  test("relay BSON supports legacy documents and persists the independent clock anchor"):
    val handler = summon[BSONDocumentHandler[Chapter.Relay]]
    val legacy = Chapter.relayInit.copy(lastMoveAt = Some(at))
    val legacyDoc = handler.writeTry(legacy).get
    assert(!legacyDoc.contains("clockAt"))
    assertEquals(handler.readTry(legacyDoc).get, legacy)
    val relay = legacy.copy(clockAt = Some(at.plusMillis(4321)))
    val written = handler.writeTry(relay).get
    assertEquals(written.getAsOpt[Instant]("lastMoveAt"), Some(at))
    assertEquals(written.getAsOpt[Instant]("clockAt"), relay.clockAt)
    assertEquals(handler.readTry(written).get, relay)
    assertEquals(handler.readTry(legacyDoc ++ bdoc("clockAt" -> BSONNull)).get.clockAt, None)
    val earliest = legacy.copy(clockAt = Some(Instant.ofEpochMilli(Long.MinValue)))
    assertEquals(handler.readTry(handler.writeTry(earliest).get).get, earliest)

  test("clock update sets or unsets the anchor together with node and denormalized clocks"):
    val clock = Clock(Centis(3210))
    val clocks: Chapter.BothClocks = ByColor(Some(Centis(1234)), None)
    val nodeField = s"${UciPath.root.toDbField}.${Node.BsonFields.clock}"

    def update(anchor: Option[Option[Instant]]) =
      ChapterRepo.clockUpdate(UciPath.root, clock, Some(clocks), anchor).get

    def sets(doc: BSONDocument) = doc.getAsOpt[BSONDocument]("$set").get

    val unchanged = update(None)
    assertEquals(sets(unchanged).getAsOpt[Clock](nodeField), Some(clock))
    assertEquals(sets(unchanged).getAsOpt[Chapter.BothClocks]("denorm.clocks"), Some(clocks))
    assert(!sets(unchanged).contains("relay.clockAt"))
    assert(!unchanged.contains("$unset"))

    val assigned = update(Some(Some(at)))
    assertEquals(sets(assigned).getAsOpt[Instant]("relay.clockAt"), Some(at))
    assertEquals(sets(assigned).getAsOpt[Clock](nodeField), Some(clock))
    assertEquals(sets(assigned).getAsOpt[Chapter.BothClocks]("denorm.clocks"), Some(clocks))
    assert(!assigned.contains("$unset"))

    val cleared = update(Some(None))
    assert(!sets(cleared).contains("relay.clockAt"))
    assert(cleared.getAsOpt[BSONDocument]("$unset").exists(_.contains("relay.clockAt")))
    assertEquals(sets(cleared).getAsOpt[Clock](nodeField), Some(clock))
    assertEquals(sets(cleared).getAsOpt[Chapter.BothClocks]("denorm.clocks"), Some(clocks))
    assert(ChapterRepo.clockUpdate(UciPath.root, clock, None, Some(Some(Instant.MAX))).isFailure)

  test("an anchor cannot create an incomplete relay field on an ordinary chapter"):
    val repo = new ChapterRepo(null)(using summon[Executor], null)
    repo
      .setClockAndDenorm(chapter.copy(relay = None), UciPath.root, Clock(Centis(100)), None, Some(Some(at)))
      .failed
      .map(error => assert(error.getMessage.contains("non-relay chapter")))

  test("all relay write paths reject anchors that cannot be persisted as BSON dates"):
    val repo = new ChapterRepo(null)(using summon[Executor], null)
    val invalidRelay = Chapter.relayInit.copy(clockAt = Some(Instant.MAX))
    val invalidChapter = chapter.copy(relay = Some(invalidRelay))
    val writes = List(
      repo.setRelay(chapterId, invalidRelay),
      repo.insert(invalidChapter),
      repo.update(invalidChapter),
      repo.addSubTree(invalidChapter, root.mainline.head, UciPath.root, Some(invalidRelay)),
      repo.setClockAndDenorm(chapter, UciPath.root, Clock(Centis(100)), None, Some(Some(Instant.MAX)))
    )
    scala.concurrent.Future
      .traverse(writes)(_.failed)
      .map: errors =>
        assertEquals(errors.size, writes.size)
        errors.foreach(error => assert(error.getMessage.contains("outside the BSON date range")))

  test("denormalization uses the mainline parent after the relay path is cleared"):
    val ended = chapter.copy(relay = chapter.relay.map(_.copy(path = UciPath.root))).updateDenorm
    assertEquals(ended.denorm.get.clocks, ByColor[Option[Centis]](Some(Centis(5500)), Some(Centis(4000))))
    assertEquals(ended.relay.flatMap(_.lastMoveAt), Some(at))
    val live = chapter.updateDenorm
    assertEquals(live.denorm.get.clocks, ended.denorm.get.clocks)

    val first = UciPath.root + root.mainline.head.id
    val withRootClock = root.setClockAt(Some(Clock(Centis(9000))), UciPath.root).get
    val oneMove = chapter.copy(root = withRootClock, relay = chapter.relay.map(_.copy(path = first)))
    assertEquals(
      oneMove.updateDenorm.denorm.get.clocks,
      ByColor[Option[Centis]](Some(Centis(6000)), Some(Centis(9000)))
    )

  private def preview(anchor: Option[Instant]): ChapterPreview = ChapterPreview(
    id = chapterId,
    name = StudyChapterName("Game"),
    players = None,
    orientation = Color.White,
    fen = chess.format.Fen.initial,
    lastMove = None,
    lastMoveAt = Some(Instant.now.minusSeconds(30)),
    check = None,
    points = Some(None),
    clockAt = anchor
  )

  test("preview JSON keeps thinkTime and omits the clock age when finished or absent"):
    val active = preview(Some(Instant.now.plusSeconds(60)))
    val json = Json.toJsObject(active)
    assert((json \ "clockAge").as[BigDecimal] < 0)
    assert((json \ "thinkTime").as[Int] >= 30)
    assert(!Json.toJsObject(active.copy(clockAt = None)).value.contains("clockAge"))
    for result <- List("1-0", "1/2-1/2", "0-0", "1/2-0") do
      val finished = active.copy(points = Some(Outcome.pointsFromResult(result)))
      assert(finished.finished, result)
      val finishedJson = Json.toJsObject(finished)
      assert(!finishedJson.value.contains("clockAge"), result)
      assert(!finishedJson.value.contains("thinkTime"), result)

  test("cached previews refresh their clock ages for every response"):
    val active = preview(Some(at))
    val finished = active.copy(points = Some(Outcome.pointsFromResult("0-0")))
    val cached = List(ChapterPreview.json.cached(active), ChapterPreview.json.cached(finished))
    val first = ChapterPreview.json.withCurrentClockAges(cached, at.plusMillis(100)).as[JsArray]
    val second = ChapterPreview.json.withCurrentClockAges(cached, at.plusMillis(250)).as[JsArray]
    assertEquals((first(0) \ "clockAge").as[BigDecimal], BigDecimal(100))
    assertEquals((second(0) \ "clockAge").as[BigDecimal], BigDecimal(250))
    assertEquals((first(0) \ "thinkTime").toOption, (second(0) \ "thinkTime").toOption)
    assert((second(1) \ "clockAge").toOption.isEmpty)

  test("preview created from a chapter preserves its independent clock anchor"):
    val api = new ChapterPreviewApi(null, _ => scala.concurrent.Future.successful(Map.empty), null)
    val withNewAnchor = chapter.copy(relay = chapter.relay.map(_.copy(clockAt = Some(at.plusMillis(1234)))))
    val fromChapter = api.fromChapter(withNewAnchor.updateDenorm)
    assertEquals(fromChapter.lastMoveAt, Some(at))
    assertEquals(fromChapter.clockAt, Some(at.plusMillis(1234)))
    assertEquals(fromChapter.players.flatMap(_.white.clock), Some(Centis(5500)))
    assertEquals(fromChapter.players.flatMap(_.black.clock), Some(Centis(4000)))

  test("preview projection and reader preserve clockAt independently of lastMoveAt"):
    assertEquals(ChapterPreview.bson.projection.getAsOpt[String]("clockAt"), Some("$relay.clockAt"))
    val reader = summon[BSONDocumentReader[ChapterPreview]]
    val legacy = bdoc("_id" -> chapterId, "name" -> StudyChapterName("Game"), "lastMoveAt" -> at)
    assertEquals(reader.readTry(legacy).get.clockAt, None)
    val updated = legacy ++ bdoc("clockAt" -> at.plusMillis(2000), "tags" -> tags)
    val read = reader.readTry(updated).get
    assertEquals(read.lastMoveAt, Some(at))
    assertEquals(read.clockAt, Some(at.plusMillis(2000)))
    assert(!read.finished)
    val finished = reader.readTry(updated ++ bdoc("tags" -> Tags(List(Tag(_.Result, "0-0"))))).get
    assert(finished.finished)
    assert(!Json.toJsObject(finished).value.contains("clockAge"))

  test("addNode payload carries raw clocks and an optional relay clock age"):
    val node = root.mainline.head
    val pos = Position.Ref(chapterId, UciPath.root)
    val who = Who(ownerId, Sri("test"))
    val relay = Chapter.relayInit.copy(path = pos.path + node.id, clockAt = Some(Instant.now.plusSeconds(60)))
    val payload = StudySocket.addNodePayload(pos, node, false, Some(relay), who)
    assertEquals((payload \ "relayPath").as[String], "!")
    assert((payload \ "clockAge").as[BigDecimal] < 0)
    assertEquals((payload \ "n" \ "clock").as[Int], 6000)
    assert(!payload.value.contains("w"))
    val legacy = StudySocket.addNodePayload(pos, node, false, Some(relay.copy(clockAt = None)), who)
    assert(!legacy.value.contains("clockAge"))
    val regular = StudySocket.addNodePayload(pos, node, false, None, who)
    assert(!regular.value.contains("clockAge"))
    assert(regular.value.contains("w"))

  test("clock payload distinguishes omitted, assigned, and cleared anchors without relayClocks"):
    val pos = Position.Ref(chapterId, root.mainlinePath)
    def payload(anchor: Option[Option[Instant]]) =
      StudySocket.clockPayload(pos, Some(Centis(3210)), None, anchor)
    assert(!payload(None).value.contains("clockAge"))
    val assigned = payload(Some(Some(Instant.now.plusSeconds(60))))
    assert((assigned \ "clockAge").as[BigDecimal] < 0)
    assert(!assigned.value.contains("relayClocks"))
    val cleared = payload(Some(None))
    assertEquals(cleared.value.get("clockAge"), Some(JsNull))
    assert(!cleared.value.contains("relayClocks"))
    val referenceOnly = StudySocket.clockPayload(pos, None, None, Some(Some(at)))
    assertEquals(referenceOnly.value.get("c"), Some(JsNull))
    assert(referenceOnly.value.contains("clockAge"))
    val clocks: Chapter.BothClocks = ByColor(Some(Centis(1234)), None)
    val withClocks = StudySocket.clockPayload(pos, Some(Centis(3210)), Some(clocks), None)
    assertEquals((withClocks \ "relayClocks").as[JsArray], Json.arr(1234, JsNull))

  private val pref: Pref = new Pref:
    val id = ownerId
    val coords = 0
    val keyboardMove = 0
    val voice = None
    val rookCastle = 0
    val animation = 0
    val destination = false
    val moveEvent = 0
    val highlight = false
    val is3d = false
    val resizeHandle = 0
    val uiRoundness = 0
    val theme = ""
    val pieceSet = ""
    val usingAltSocket = None
    val blogFilter = QualityFilter.all
    val bg = 0
    def hasKeyboardMove = false
    def hasVoice = false
    def hasSpeech = false
    def hideRatingsInGame = false
    def showRatings = true
    def animationMillis = 0
    def animationMillisForSpeedPuzzles = 0
    def pieceNotationIsLetter = false
    def bgOpacity = 0

  test("full study JSON includes the current chapter age without previews, but not after a result"):
    given Option[Me] = None
    given Pref = pref
    val study = Study(
      id = studyId,
      name = StudyName("Broadcast"),
      members = StudyMembers.empty,
      position = Position.Ref(chapterId, UciPath.root),
      ownerId = ownerId,
      visibility = Visibility.public,
      settings = Settings.init,
      from = Study.From.Relay(None),
      likes = Study.Likes(0),
      createdAt = Instant.now,
      updatedAt = Instant.now
    )
    val view = new JsonView(null, null)
    val live = chapter.copy(relay = chapter.relay.map(_.copy(clockAt = Some(Instant.now.plusSeconds(60)))))
    val legacy = live.copy(relay = live.relay.map(_.copy(clockAt = None)))
    val finished = live.copy(tags = tags + Tag(_.Result, "0-0"))
    val extreme = live.copy(relay = live.relay.map(_.copy(clockAt = Some(Instant.MAX))))
    for
      liveJson <- view.full(study, live, None, withMembers = false)
      legacyJson <- view.full(study, legacy, None, withMembers = false)
      finishedJson <- view.full(study, finished, None, withMembers = false)
      extremeJson <- view.full(study, extreme, None, withMembers = false)
    yield
      assert(!liveJson.value.contains("chapters"))
      assert((liveJson \ "chapter" \ "clockAge").as[BigDecimal] < 0)
      assert((legacyJson \ "chapter" \ "clockAge").toOption.isEmpty)
      assert((finishedJson \ "chapter" \ "clockAge").toOption.isEmpty)
      assert((extremeJson \ "chapter" \ "clockAge").as[BigDecimal] < BigDecimal(Long.MinValue))
