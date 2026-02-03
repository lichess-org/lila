package lila.relay

import lila.core.i18n.*

class RelayI18nTest extends munit.FunSuite:

  given Translator = TranslatorStub
  given play.api.i18n.Lang = defaultLang

  test("Tour name"):
    // https://lichess.org/broadcast/turkish-cadet--youth-championships-2026--u13-open/round-9/iV4feSab
    val name = RelayTour.Name("Turkish Cadet & Youth Championships 2026 | U13 Open")
    assertEquals(RelayI18n(name), name.value)
