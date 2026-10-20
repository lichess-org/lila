package lila.security

import lila.core.net.Continent

class LocationTest extends munit.FunSuite:

  test("unknown location is suspicious, even with a known continent"):
    assert(Location.isSuspicious(Location.unknown))
    assert(Location.isSuspicious(Location.unknown.copy(continent = Continent.EU.some)))

  test("known country is not suspicious"):
    assert(!Location.isSuspicious(Location("France", "FR".some, none, none, Continent.EU.some)))
