package lila.relay

import reactivemongo.api.bson.*

import lila.core.net.Continent
import lila.relay.BSONHandlers.given

class RelaySpotlightTest extends munit.FunSuite:

  def spotlight(continent: Option[Continent], enabled: Boolean = true) =
    RelayTour.Spotlight(enabled, lila.core.i18n.defaultLanguage, none, continent)

  val handler = summon[BSONDocumentHandler[RelayTour.Spotlight]]

  test("no continent is visible from everywhere"):
    assert(spotlight(none).visibleFrom(Continent.EU.some))
    assert(spotlight(none).visibleFrom(none))

  test("continent is only visible from that continent"):
    assert(spotlight(Continent.EU.some).visibleFrom(Continent.EU.some))
    assert(!spotlight(Continent.EU.some).visibleFrom(Continent.AS.some))
    assert(!spotlight(Continent.EU.some).visibleFrom(none))

  test("a spotlight with only a continent is not empty"):
    assert(spotlight(none, enabled = false).isEmpty)
    assert(!spotlight(Continent.EU.some, enabled = false).isEmpty)

  test("continent BSON round trip"):
    val s = spotlight(Continent.EU.some)
    val doc = handler.writeTry(s).get
    assertEquals(doc.getAsOpt[String]("continent"), "EU".some)
    assertEquals(handler.readTry(doc).toOption, s.some)

  test("no continent is not written"):
    assert(!handler.writeTry(spotlight(none)).get.contains("continent"))

  test("a document without continent reads as none"):
    val doc = handler.writeTry(spotlight(Continent.EU.some)).get -- "continent"
    assertEquals(handler.readTry(doc).toOption.map(_.continent), Some(none))

  test("an invalid stored continent fails to read"):
    val doc = (handler.writeTry(spotlight(none)).get -- "continent") ++ BSONDocument("continent" -> "XX")
    assert(handler.readTry(doc).isFailure)
