package lila.practice
package ui

import lila.ui.*
import lila.core.i18n.I18nKey
import lila.core.study.data.StudyName

class PracticeFragments(helpers: Helpers):
  import helpers.trans.practice as trp

  val headersMap: Map[String, I18nKey] = Map(
    "Checkmates" -> trp.secHeadCheckmates,
    "Basic Tactics" -> trp.secHeadBasicTactics,
    "Intermediate Tactics" -> trp.secHeadIntermediateTactics,
    "Pawn Endgames" -> trp.secHeadPawnEndgames,
    "Rook Endgames" -> trp.secHeadRookEndgames
  )

  val studiesMap: Map[StudyName, I18nKey] = Map(
    StudyName("Piece Checkmates I") -> trp.stNamPieceCheckmatesI,
    StudyName("Checkmate Patterns I") -> trp.stNamCheckmatePatternsI,
    StudyName("Checkmate Patterns II") -> trp.stNamCheckmatePatternsII,
    StudyName("Checkmate Patterns III") -> trp.stNamCheckmatePatternsIII,
    StudyName("Checkmate Patterns IV") -> trp.stNamCheckmatePatternsIV,
    StudyName("Piece Checkmates II") -> trp.stNamPieceCheckmatesII,
    StudyName("Knight &amp; Bishop Mate") -> trp.stNamKnightAndBishopMate,
    StudyName("The Pin") -> trp.stNamThePin,
    StudyName("The Skewer") -> trp.stNamTheSkewer,
    StudyName("The Fork") -> trp.stNamTheFork,
    StudyName("Discovered Attacks") -> trp.stNamDiscoveredAttacks,
    StudyName("Double Check") -> trp.stNamDoubleCheck,
    StudyName("Overloaded Pieces") -> trp.stNamOverloadedPieces,
    StudyName("Zwischenzug") -> trp.stNamZwischenzug,
    StudyName("X-Ray") -> trp.stNamXRay,
    StudyName("Zugzwang") -> trp.stNamZugzwang,
    StudyName("Interference") -> trp.stNamInterference,
    StudyName("Greek Gift") -> trp.stNamGreekGift,
    StudyName("Deflection") -> trp.stNamDeflection,
    StudyName("Attraction") -> trp.stNamAttraction,
    StudyName("Underpromotion") -> trp.stNamUnderpromotion,
    StudyName("Desperado") -> trp.stNamDesperado,
    StudyName("Counter Check") -> trp.stNamCounterCheck,
    StudyName("Undermining") -> trp.stNamUndermining,
    StudyName("Clearance") -> trp.stNamClearance,
    StudyName("Key Squares") -> trp.stNamKeySquares,
    StudyName("Opposition") -> trp.stNamOpposition,
    StudyName("7th-Rank Rook Pawn") -> trp.stNam7thRankRookPawn,
    StudyName("Basic Rook Endgames") -> trp.stNamBasicRookEndgames,
    StudyName("Intermediate Rook Endings") -> trp.stNamIntermediateRookEndings,
    StudyName("Practical Rook Endings") -> trp.stNamPracticalRookEndings
  )

  val studiesDescMap: Map[String, I18nKey] = Map(
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

  private def getFragment[N](name: N, map: Map[N, I18nKey]): I18nKey =
    map.getOrElse(name, I18nKey(name.toString))

  def sectionHeader(s: PracticeSection): I18nKey = getFragment(s.name, headersMap)
  def studyName(name: StudyName): I18nKey = getFragment(name, studiesMap)
  def studiesDesc(name: String): I18nKey = getFragment(name, studiesDescMap)
