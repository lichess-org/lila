package lila.fide

import chess.{ FideId, FideTC, PlayerTitle }
import chess.rating.Elo

class FidePlayerSyncTest extends munit.FunSuite:

  import FidePlayerSync.{ parseInactiveId, parseLine }

  // real lines from the august 2026 blitz_rating_list.txt
  val blitzInactiveMan =
    "703303         Leko, Peter                                                  HUN M   GM                           2738  0   10 1979  i   "
  val blitzInactiveWoman =
    "700070         Polgar, Judit                                                HUN F   GM                           2736  0   10 1976  wi  "
  val blitzActiveMan =
    "537001345      A Arbhin Vanniarajan                                         IND M                                1431  0   40 2018      "
  val blitzActiveWoman =
    "48701955       Aadya Gowda                                                  IND F   WCM  WCM                     1807  0   40 2013  w   "

  // real line from the august 2026 players_list.txt, holding the three ratings
  // This player is inactive in blitz, but active in standard and rapid.
  val combinedLeko =
    "703303         Leko, Peter                                                  HUN M   GM                           2676  0   10 2700  0   10 2738  0   10 1979      "

  test("parseInactiveId picks up the i flag"):
    assertEquals(parseInactiveId(blitzInactiveMan), Some(703303))

  test("parseInactiveId picks up the wi flag"):
    assertEquals(parseInactiveId(blitzInactiveWoman), Some(700070))

  test("parseInactiveId ignores active players"):
    assertEquals(parseInactiveId(blitzActiveMan), None)
    assertEquals(parseInactiveId(blitzActiveWoman), None)

  test("parseInactiveId ignores the header and short lines"):
    assertEquals(parseInactiveId("ID Number      Name"), None)
    assertEquals(parseInactiveId(""), None)

  test("parseLine reads the combined list"):
    val player = parseLine(Map.empty)(combinedLeko).get
    assertEquals(player.id, FideId(703303))
    assertEquals(player.name.value, "Leko, Peter")
    assertEquals(player.title, Some(PlayerTitle.GM))
    assertEquals(player.year, Some(1979))
    assertEquals(player.standard, Some(Elo(2676)))
    assertEquals(player.rapid, Some(Elo(2700)))
    assertEquals(player.blitz, Some(Elo(2738)))

  test("parseLine takes inactivity from the rating lists, not from the combined line"):
    assertEquals(parseLine(Map.empty)(combinedLeko).get.inactive, Set.empty[FideTC])
    val player = parseLine(Map(FideTC.blitz -> Set(1, 703303, 999999)))(combinedLeko).get
    assertEquals(player.inactive, Set(FideTC.blitz))
    assert(player.isInactiveForTc(FideTC.blitz))
    assert(!player.isInactiveForTc(FideTC.standard))
    assert(!player.isInactiveForTc(FideTC.rapid))

  test("parseLine flags every time control the player is inactive in"):
    val all = FideTC.values.view.map(_ -> Set(703303)).toMap
    assertEquals(parseLine(all)(combinedLeko).get.inactive, FideTC.values.toSet)

  val leko = parseLine(Map.empty)(combinedLeko).get

  test("isInactive needs every rated time control to be flagged"):
    assert(!leko.copy(inactive = Set.empty).isInactive)
    assert(!leko.copy(inactive = Set(FideTC.blitz)).isInactive)
    assert(!leko.copy(inactive = Set(FideTC.blitz, FideTC.rapid)).isInactive)
    assert(leko.copy(inactive = FideTC.values.toSet).isInactive)

  test("isInactive covers players rated in a single time control"):
    val standardOnly = leko.copy(rapid = None, blitz = None)
    assert(standardOnly.copy(inactive = Set(FideTC.standard)).isInactive)
    assert(!standardOnly.copy(inactive = Set(FideTC.blitz)).isInactive)

  test("isInactive needs a flag, not just an absence of ratings"):
    val unrated = leko.copy(standard = None, rapid = None, blitz = None)
    assert(!unrated.copy(inactive = Set.empty).isInactive)
    assert(unrated.copy(inactive = Set(FideTC.rapid)).isInactive)
