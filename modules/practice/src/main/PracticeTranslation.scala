package lila.practice
package ui

import scala.collection.immutable

import lila.ui.*
import lila.core.i18n.I18nKey

class practiceFragments(helpers: Helpers):
  import helpers.trans.practice as trp

  val headersMap: immutable.Map[String, I18nKey] = Map(
    "Checkmates" -> trp.secHeadCheckmates,
    "Basic Tactics" -> trp.secHeadBasicTactics,
    "Intermediate Tactics" -> trp.secHeadIntermediateTactics,
    "Pawn Endgames" -> trp.secHeadPawnEndgames,
    "Rook Endgames" -> trp.secHeadRookEndgames
  )

  val studiesMap: immutable.Map[String, I18nKey] = Map(
    "Piece Checkmates I" -> trp.stNamPieceCheckmatesI,
    "Checkmate Patterns I" -> trp.stNamCheckmatePatternsI,
    "Checkmate Patterns II" -> trp.stNamCheckmatePatternsII,
    "Checkmate Patterns III" -> trp.stNamCheckmatePatternsIII,
    "Checkmate Patterns IV" -> trp.stNamCheckmatePatternsIV,
    "Piece Checkmates II" -> trp.stNamPieceCheckmatesII,
    "Knight &amp; Bishop Mate" -> trp.stNamKnightAndBishopMate,
    "The Pin" -> trp.stNamThePin,
    "The Skewer" -> trp.stNamTheSkewer,
    "The Fork" -> trp.stNamTheFork,
    "Discovered Attacks" -> trp.stNamDiscoveredAttacks,
    "Double Check" -> trp.stNamDoubleCheck,
    "Overloaded Pieces" -> trp.stNamOverloadedPieces,
    "Zwischenzug" -> trp.stNamZwischenzug,
    "X-Ray" -> trp.stNamXRay,
    "Zugzwang" -> trp.stNamZugzwang,
    "Interference" -> trp.stNamInterference,
    "Greek Gift" -> trp.stNamGreekGift,
    "Deflection" -> trp.stNamDeflection,
    "Attraction" -> trp.stNamAttraction,
    "Underpromotion" -> trp.stNamUnderpromotion,
    "Desperado" -> trp.stNamDesperado,
    "Counter Check" -> trp.stNamCounterCheck,
    "Undermining" -> trp.stNamUndermining,
    "Clearance" -> trp.stNamClearance,
    "Key Squares" -> trp.stNamKeySquares,
    "Opposition" -> trp.stNamOpposition,
    "7th-Rank Rook Pawn" -> trp.stNam7thRankRookPawn,
    "Basic Rook Endgames" -> trp.stNamBasicRookEndgames,
    "Intermediate Rook Endings" -> trp.stNamIntermediateRookEndings,
    "Practical Rook Endings" -> trp.stNamPracticalRookEndings
  )

  val studiesDescMap: immutable.Map[String, I18nKey] = Map(
    "Basic checkmates" -> trp.stDesBasicCheckmates,
    "Recognize the patterns" -> trp.stDesRecognizeThePatterns,
    "Challenging checkmates" -> trp.stDesChallengingCheckmates,
    "Interactive lesson" -> trp.stDesInteractiveLesson,
    "Pin it to win it" -> trp.stDesPinItToWinIt,
    "Yum - skewers!" -> trp.stDesYumSkewers,
    "Use the fork, Luke" -> trp.stDesUseTheForkLuke,
    "Including discovered checks" -> trp.stDesIncludingDiscoveredChecks,
    "A very powerful tactic" -> trp.stDesAVeryPowerfulTactic,
    "They have too much work" -> trp.stDesTheyHaveTooMuchWork,
    "In-between moves" -> trp.stDesInBetweenMoves,
    "Attacking through an enemy piece" -> trp.stDesAttackingThroughAnEnemyPiece,
    "Being forced to move" -> trp.stDesBeingForcedToMove,
    "Interpose a piece to great effect" -> trp.stDesInterposeAPieceToGreatEffect,
    "Study the greek gift sacrifice" -> trp.stDesStudyTheGreekGiftSacrifice,
    "Distracting a defender" -> trp.stDesDistractingADefender,
    "Lure a piece to a bad square" -> trp.stDesLureAPieceToABadSquare,
    "Promote - but not to a queen" -> trp.stDesPromoteButNotToAQueen,
    "A piece is lost, but it can still help" -> trp.stDesAPieceIsLostButItCanStillHelp,
    "Respond to a check with a check" -> trp.stDesRespondToACheckWithACheck,
    "Remove the defending piece" -> trp.stDesRemoveTheDefendingPiece,
    "Get out of the way" -> trp.stDesGetOutOfTheWay,
    "Reach a key square" -> trp.stDesReachAKeySquare,
    "Take the opposition" -> trp.stDesTakeTheOpposition,
    "Versus a Queen" -> trp.stDesVersusAQueen,
    "And Passive Rook vs Rook" -> trp.stDesAndPassiveRookVsRook,
    "Lucena and Philidor" -> trp.stDesLucenaAndPhilidor,
    "Broaden your knowledge" -> trp.stDesBroadenYourKnowledge,
    "Rook endings with several pawns" -> trp.stDesRookEndingsWithSeveralPawns
  )

  private def getFragment(name: String, map: Map[String, I18nKey]): I18nKey =
    map.getOrElse(name, I18nKey(name))

  def sectionHeader(name: String): I18nKey = getFragment(name, headersMap)
  def studyName(name: String): I18nKey = getFragment(name, studiesMap)
  def studiesDesc(name: String): I18nKey = getFragment(name, studiesDescMap)
