import lidraughts.base.PimpedFuture
import lidraughts.game.PdnDump
import lidraughts.importer.ImportData
import org.specs2.matcher.ValidationMatchers
import org.specs2.mutable.Specification

import scala.concurrent.Future

class PdnIOPerfTest extends Specification with ValidationMatchers {

  @inline implicit def toPimpedFuture[A](f: Future[A]) = new PimpedFuture(f)

  args(skipAll = true)

  val longPdn = """
[Event "Casual Rapid game"]
[Site "https://lidraughts.org/001NahOY"]
[Date "2020.04.07"]
[Round "-"]
[White "Anonymous"]
[Black "lidraughts AI level 3"]
[Result "2-0"]
[UTCDate "2020.04.07"]
[UTCTime "00:54:55"]
[WhiteElo "?"]
[BlackElo "?"]
[GameType "20"]
[TimeControl "600+0"]
[Opening "?"]
[Termination "Normal"]
[Annotator "lidraughts.org"]

1. 31-26 {[%clock w0:10:00 B0:10:00]} 18-22 {[%clock W0:10:00 b0:10:00]} 2. 34-30 {[%clock w0:09:57 B0:09:58]} 20-25 {[%clock W0:09:57 b0:09:58]} 3. 30-24 {[%clock w0:09:55 B0:09:58]} 19x30 {[%clock W0:09:55 b0:09:58]} 4. 35x24 {[%clock w0:09:54 B0:09:56]} 14-20 {[%clock W0:09:54 b0:09:56]} 5. 24-19 {[%clock w0:09:51 B0:09:56]} 13x24 {[%clock W0:09:51 b0:09:56]} 6. 33-29 {[%clock w0:09:49 B0:09:55]} 24x33 {[%clock W0:09:49 b0:09:55]} 7. 38x29 {[%clock w0:09:47 B0:09:53]} 20-24 {[%clock W0:09:47 b0:09:53]} 8. 29x20 {[%clock w0:09:46 B0:09:49]} 15x24 {[%clock W0:09:46 b0:09:49]} 9. 39-33 {[%clock w0:09:46 B0:09:47]} 22-27 {[%clock W0:09:46 b0:09:47]} 10. 32x21 {[%clock w0:09:37 B0:09:47]} 16x27 {[%clock W0:09:37 b0:09:47]} 11. 37-31 {[%clock w0:09:35 B0:09:43]} 17-22 {[%clock W0:09:35 b0:09:43]} 12. 33-28 {[%clock w0:09:30 B0:09:43]} 22x33 {[%clock W0:09:30 b0:09:43]} 13. 31x22 {[%clock w0:09:29 B0:09:39]} 10-14 {[%clock W0:09:29 b0:09:39]} 14. 40-35 {[%clock w0:09:25 B0:09:34]} 14-19 {[%clock W0:09:25 b0:09:34]} 15. 43-38 {[%clock w0:09:09 B0:09:31]} 24-29 {[%clock W0:09:09 b0:09:31]} 16. 38-32 {[%clock w0:09:08 B0:09:27]} 8-13 {[%clock W0:09:08 b0:09:27]} 17. 32-28 {[%clock w0:09:06 B0:09:24]} 12-17 {[%clock W0:09:06 b0:09:24]} 18. 28x39 {[%clock w0:09:04 B0:09:24]} 17x28 {[%clock W0:09:04 b0:09:24]} 19. 44-40 {[%clock w0:09:02 B0:09:21]} 19-23 {[%clock W0:09:02 b0:09:21]} 20. 39-34 {[%clock w0:09:02 B0:09:19]} 13-18 {[%clock W0:09:02 b0:09:19]} 21. 35-30 {[%clock w0:09:01 B0:09:16]} 29-33 {[%clock W0:09:01 b0:09:16]} 22. 30-24 {[%clock w0:08:58 B0:09:12]} 25-30 {[%clock W0:08:58 b0:09:12]} 23. 24x35 {[%clock w0:08:56 B0:09:09]} 9-13 {[%clock W0:08:56 b0:09:09]} 24. 34-30 {[%clock w0:08:55 B0:09:08]} 7-12 {[%clock W0:08:55 b0:09:08]} 25. 30-25 {[%clock w0:08:54 B0:09:05]} 2-8 {[%clock W0:08:54 b0:09:05]} 26. 25-20 {[%clock w0:08:54 B0:09:02]} 5-10 {[%clock W0:08:54 b0:09:02]} 27. 20-15 {[%clock w0:08:54 B0:08:59]} 10-14 {[%clock W0:08:54 b0:08:59]} 28. 35-30 {[%clock w0:08:54 B0:08:56]} 14-19 {[%clock W0:08:54 b0:08:56]} 29. 30-25 {[%clock w0:08:53 B0:08:54]} 1-7 {[%clock W0:08:53 b0:08:54]} 30. 40-35 {[%clock w0:08:41 B0:08:52]} 19-24 {[%clock W0:08:41 b0:08:52]} 31. 25-20 {[%clock w0:08:39 B0:08:51]} 33-39 {[%clock W0:08:39 b0:08:51]} 32. 20x29 {[%clock w0:08:38 B0:08:50]} 23x34 {[%clock W0:08:38 b0:08:50]} 33. 45-40 {[%clock w0:08:32 B0:08:49]} 34x45 {[%clock W0:08:32 b0:08:49]} 34. 35-30 {[%clock w0:08:31 B0:08:47]} 11-16 {[%clock W0:08:31 b0:08:47]} 35. 30-25 {[%clock w0:08:31 B0:08:45]} 28-32 {[%clock W0:08:31 b0:08:45]} 36. 25-20 {[%clock w0:08:30 B0:08:42]} 4-9 {[%clock W0:08:30 b0:08:42]} 37. 20-14 {[%clock w0:08:23 B0:08:42]} 9x20 {[%clock W0:08:23 b0:08:42]} 38. 15x24 {[%clock w0:08:20 B0:08:41]} 7-11 {[%clock W0:08:20 b0:08:41]} 39. 24-20 {[%clock w0:08:20 B0:08:39]} 18-23 {[%clock W0:08:20 b0:08:39]} 40. 20-15 {[%clock w0:08:19 B0:08:38]} 12-18 {[%clock W0:08:19 b0:08:38]} 41. 15-10 {[%clock w0:08:07 B0:08:36]} 16-21 {[%clock W0:08:07 b0:08:36]} 42. 26x17 {[%clock w0:08:01 B0:08:36]} 11x22 {[%clock W0:08:01 b0:08:36]} 43. 42-37 {[%clock w0:08:00 B0:08:35]} 32-38 {[%clock W0:08:00 b0:08:35]} 44. 37-32 {[%clock w0:07:57 B0:08:34]} 38x27 {[%clock W0:07:57 b0:08:34]} 45. 10-5 {[%clock w0:07:44 B0:08:32]} 23-29 {[%clock W0:07:44 b0:08:32]} 46. 5-37 {[%clock w0:07:42 B0:08:30]} 6-11 {[%clock W0:07:42 b0:08:30]} 47. 37-42 {[%clock w0:07:41 B0:08:28]} 27-32 {[%clock W0:07:41 b0:08:28]} 48. 42x24 {[%clock w0:07:39 B0:08:27]} 11-16 {[%clock W0:07:39 b0:08:27]} 49. 24-35 {[%clock w0:07:38 B0:08:26]} 32-38 {[%clock W0:07:38 b0:08:26]} 50. 35-44 {[%clock w0:07:37 B0:08:24]} 39-43 {[%clock W0:07:37 b0:08:24]} 51. 48x39 {[%clock w0:07:31 B0:08:23]} 22-27 {[%clock W0:07:31 b0:08:23]} 52. 39-33 {[%clock w0:07:29 B0:08:23]} 38x29 {[%clock W0:07:29 b0:08:23]} 53. 44-39 {[%clock w0:07:27 B0:08:22]} 18-23 {[%clock W0:07:27 b0:08:22]} 54. 39-48 {[%clock w0:07:26 B0:08:21]} 16-21 {[%clock W0:07:26 b0:08:21]} 55. 48-43 {[%clock w0:07:12 B0:08:19]} 29-33 {[%clock W0:07:12 b0:08:19]} 56. 43-48 {[%clock w0:07:09 B0:08:19]} 27-32 {[%clock W0:07:09 b0:08:19]} 57. 48-37 {[%clock w0:07:07 B0:08:18]} 23-28 {[%clock W0:07:07 b0:08:18]} 58. 36-31 {[%clock w0:07:04 B0:08:17]} 33-38 {[%clock W0:07:04 b0:08:17]} 59. 37-48 {[%clock w0:06:56 B0:08:16]} 28-33 {[%clock W0:06:56 b0:08:16]} 60. 31-27 {[%clock w0:06:54 B0:08:15]} 13-18 {[%clock W0:06:54 b0:08:15]} 61. 27x16 {[%clock w0:06:53 B0:08:14]} 18-23 {[%clock W0:06:53 b0:08:14]} 62. 16-11 {[%clock w0:06:51 B0:08:13]} 8-12 {[%clock W0:06:51 b0:08:13]} 63. 48-43 {[%clock w0:06:33 B0:08:11]} 23-29 {[%clock W0:06:33 b0:08:11]} 64. 11-6 {[%clock w0:06:30 B0:08:10]} 32-37 {[%clock W0:06:30 b0:08:10]} 65. 43x8 {[%clock w0:06:25 B0:06:24]} 3x12 {[%clock W0:06:25 b0:06:24]} 66. 41x32 {[%clock w0:08:10 B0:06:18]} 29-34 {[%clock W0:08:10 b0:06:18]} 67. 32-27 {[%clock w0:08:08 B0:06:17]} 33-38 {[%clock W0:08:08 b0:06:17]} 68. 27-22 {[%clock w0:08:08 B0:06:16]} 12-18 {[%clock W0:08:08 b0:06:16]} 69. 22x13 {[%clock w0:08:06 B0:06:14]} 34-39 {[%clock W0:08:06 b0:06:14]} 70. 13-8 {[%clock w0:08:05 B0:06:12]} 39-43 {[%clock W0:08:05 b0:06:12]} 71. 8-3 {[%clock w0:08:05 B0:06:11]} 43-48 {[%clock W0:08:05 b0:06:11]} 72. 3-25 {[%clock w0:08:04 B0:06:07]} 48-37 {[%clock W0:08:04 b0:06:07]} 73. 6-1 {[%clock w0:08:03 B0:06:02]} 37-32 {[%clock W0:08:03 b0:06:02]} 74. 1-6 {[%clock w0:08:02 B0:06:00]} 32-21 {[%clock W0:08:02 b0:06:00]} 75. 6-39 {[%clock w0:08:01 B0:05:46]} 21-16 {[%clock W0:08:01 b0:05:46]} 76. 39-48 {[%clock w0:08:00 B0:05:45]} 16-27 {[%clock W0:08:00 b0:05:45]} 77. 25-3 {[%clock w0:07:59 B0:05:35]} 27-13 {[%clock W0:07:59 b0:05:35]} 78. 3-21 {[%clock w0:07:59 B0:05:33]} 13-2 {[%clock W0:07:59 b0:05:33]} 79. 21x43 {[%clock w0:07:58 B0:05:32]} 2-13 {[%clock W0:07:58 b0:05:32]} 80. 47-42 {[%clock w0:07:57 B0:05:23]} 13-24 {[%clock W0:07:57 b0:05:23]} 81. 43-38 {[%clock w0:07:56 B0:05:21]} 24-15 {[%clock W0:07:56 b0:05:21]} 82. 49-43 {[%clock w0:07:55 B0:05:12]} 15-4 {[%clock W0:07:55 b0:05:12]} 83. 42-37 {[%clock w0:07:54 B0:05:10]} 4-15 {[%clock W0:07:54 b0:05:10]} 84. 48-42 {[%clock w0:07:53 B0:05:05]} 15-4 {[%clock W0:07:53 b0:05:05]} 85. 37-32 {[%clock w0:07:53 B0:05:04]} 4-15 {[%clock W0:07:53 b0:05:04]} 86. 32-28 {[%clock w0:07:52 B0:05:02]} 15-4 {[%clock W0:07:52 b0:05:02]} 87. 28-23 {[%clock w0:07:51 B0:05:01]} 4-13 {[%clock W0:07:51 b0:05:01]} 88. 43-39 {[%clock w0:07:50 B0:04:57]} 13-8 {[%clock W0:07:50 b0:04:57]} 89. 39-33 {[%clock w0:07:49 B0:04:56]} 8-26 {[%clock W0:07:49 b0:04:56]} 90. 42-48 {[%clock w0:07:49 B0:04:53]} 26-8 {[%clock W0:07:49 b0:04:53]} 91. 33-29 {[%clock w0:07:47 B0:04:52]} 8-13 {[%clock W0:07:47 b0:04:52]} 92. 38-47 {[%clock w0:07:47 B0:04:47]} 13-35 {[%clock W0:07:47 b0:04:47]} 93. 47-36 {[%clock w0:07:46 B0:04:46]} 35-40 {[%clock W0:07:46 b0:04:46]} 94. 48-42 {[%clock w0:07:45 B0:04:44]} 40-35 {[%clock W0:07:45 b0:04:44]} 95. 42-47 {[%clock w0:07:44 B0:04:43]} 35-40 {[%clock W0:07:44 b0:04:43]} 96. 36-18 {[%clock w0:07:43 B0:04:40]} 40-35 {[%clock W0:07:43 b0:04:40]} 97. 46-41 {[%clock w0:07:42 B0:04:34]} 35-40 {[%clock W0:07:42 b0:04:34]} 98. 41-36 {[%clock w0:07:41 B0:04:32]} 40-35 {[%clock W0:07:41 b0:04:32]} 99. 36-31 {[%clock w0:07:40 B0:04:31]} 35-49 {[%clock W0:07:40 b0:04:31]} 100. 31-26 {[%clock w0:07:39 B0:04:31]} 49-32 {[%clock W0:07:39 b0:04:31]} 101. 47-42 {[%clock w0:07:38 B0:04:26]} 32x14 {[%clock W0:07:38 b0:04:26]} 102. 18-12 {[%clock w0:07:37 B0:04:17]} 14-46 {[%clock W0:07:37 b0:04:17]} 103. 42-47 {[%clock w0:07:36 B0:04:16]} 46-19 {[%clock W0:07:36 b0:04:16]} 104. 26-21 {[%clock w0:07:35 B0:04:14]} 19-28 {[%clock W0:07:35 b0:04:14]} 105. 21-16 {[%clock w0:07:34 B0:04:13]} 28-39 {[%clock W0:07:34 b0:04:13]} 106. 29-23 {[%clock w0:07:33 B0:04:10]} 39-6 {[%clock W0:07:33 b0:04:10]} 107. 23-18 {[%clock w0:07:33 B0:04:09]} 6-28 {[%clock W0:07:33 b0:04:09]} 108. 18-13 {[%clock w0:07:32 B0:04:07]} 28-10 {[%clock W0:07:32 b0:04:07]} 109. 13-8 {[%clock w0:07:31 B0:04:06]} 10-14 {[%clock W0:07:31 b0:04:06]} 110. 8-3 {[%clock w0:07:30 B0:04:04]} 14-46 {[%clock W0:07:30 b0:04:04]} 111. 3-9 {[%clock w0:07:30 B0:03:58]} 46-28 {[%clock W0:07:30 b0:03:58]} 112. 9-4 {[%clock w0:07:28 B0:03:57]} 28-6 {[%clock W0:07:28 b0:03:57]} 113. 4-10 {[%clock w0:07:27 B0:03:55]} 6-39 {[%clock W0:07:27 b0:03:55]} 114. 10-5 {[%clock w0:07:26 B0:03:54]} 39-22 {[%clock W0:07:26 b0:03:54]} 115. 12-23 {[%clock w0:07:26 B0:03:53]} 22-17 {[%clock W0:07:26 b0:03:53]} 116. 47-41 {[%clock w0:07:25 B0:03:51]} 17-22 {[%clock W0:07:25 b0:03:51]} 117. 41-46 {[%clock w0:07:24 B0:03:51]} 22-39 {[%clock W0:07:24 b0:03:51]} 118. 23-1 {[%clock w0:07:23 B0:03:48]} 39-22 {[%clock W0:07:23 b0:03:48]} 119. 1-6 {[%clock w0:07:22 B0:03:47]} 22-31 {[%clock W0:07:22 b0:03:47]} 120. 16-11 {[%clock w0:07:21 B0:03:46]} 31-18 {[%clock W0:07:21 b0:03:46]} 121. 6-1 {[%clock w0:07:20 B0:03:41]} 18-31 {[%clock W0:07:20 b0:03:41]} 122. 11-7 {[%clock w0:07:19 B0:03:39]} 31-42 {[%clock W0:07:19 b0:03:39]} 123. 7-2 {[%clock w0:07:18 B0:03:38]} 42-33 {[%clock W0:07:18 b0:03:38]} 124. 5-23 {[%clock w0:07:18 B0:01:46]} 33-38 {[%clock W0:07:18 b0:01:46]} 125. 1-18 {[%clock w0:07:17 B0:01:45]} 38-49 {[%clock W0:07:17 b0:01:45]} 126. 2-24 {[%clock w0:07:16 B0:01:44]} 49-21 {[%clock W0:07:16 b0:01:44]} 127. 23-19 {[%clock w0:07:15 B0:01:37]} 21-16 {[%clock W0:07:15 b0:01:37]} 128. 18-12 {[%clock w0:07:15 B0:01:36]} 16-27 {[%clock W0:07:15 b0:01:36]} 129. 46-41 {[%clock w0:07:14 B0:01:23]} 27-22 {[%clock W0:07:14 b0:01:23]} 130. 41-47 {[%clock w0:07:13 B0:01:22]} 22-39 {[%clock W0:07:13 b0:01:22]} 131. 47-29 {[%clock w0:07:12 B0:01:21]} 39-6 {[%clock W0:07:12 b0:01:21]} 132. 19-2 {[%clock w0:07:11 B0:01:15]} 6-28 {[%clock W0:07:11 b0:01:15]} 133. 2-7 {[%clock w0:07:09 B0:01:13]} 28-44 {[%clock W0:07:09 b0:01:13]} 134. 50x39 {[%clock w0:07:09 B0:01:11]} 45-50 {[%clock W0:07:09 b0:01:11]} 135. 39-34 {[%clock w0:07:08 B0:01:10]} 50-6 {[%clock W0:07:08 b0:01:10]} 136. 34-30 {[%clock w0:07:08 B0:01:04]} 6-50 {[%clock W0:07:08 b0:01:04]} 137. 30-25 {[%clock w0:07:06 B0:01:03]} 50-28 {[%clock W0:07:06 b0:01:03]} 138. 25-20 {[%clock w0:07:05 B0:01:03]} 28-32 {[%clock W0:07:05 b0:01:03]} 139. 20-15 {[%clock w0:07:04 B0:01:02]} 32-49 {[%clock W0:07:04 b0:01:02]} 140. 15-10 {[%clock w0:07:03 B0:01:01]} 49-27 {[%clock W0:07:03 b0:01:01]} 141. 10-5 {[%clock w0:07:02 B0:01:01]} 27-49 {[%clock W0:07:02 b0:01:01]} 142. 12-3 {[%clock w0:07:01 B0:00:55]} 49-27 {[%clock W0:07:01 b0:00:55]} 143. 7-1 {[%clock w0:07:00 B0:00:54]} 27-16 {[%clock W0:07:00 b0:00:54]} 144. 1-6 {[%clock w0:07:00 B0:00:53]} 16-27 {[%clock W0:07:00 b0:00:53]} 145. 6-11 {[%clock w0:06:59 B0:00:52]} 27-49 {[%clock W0:06:59 b0:00:52]} 146. 11-16 {[%clock w0:06:58 B0:00:51]} 49-44 {[%clock W0:06:58 b0:00:51]} 147. 29-45 {[%clock w0:06:57 B0:00:50]} 44-6 {[%clock W0:06:57 b0:00:50]} 148. 45-50 {[%clock w0:06:56 B0:00:49]} 6-1 {[%clock W0:06:56 b0:00:49]} 149. 24-35 {[%clock w0:06:55 B0:00:48]} 1-29 {[%clock W0:06:55 b0:00:48]} 150. 3-8 {[%clock w0:06:54 B0:00:43]} 29-1 {[%clock W0:06:54 b0:00:43]} 151. 8-2 {[%clock w0:06:54 B0:00:42]} {Black resigns.} 2-0
"""
  val importData = ImportData(longPdn, None)
  val pdnFlags = PdnDump.WithFlags(
    evals = false,
    opening = false
  )
  val pdnDump = new PdnDump(
    netBaseUrl = "https://lidraughts.org",
    getLightUser = _ => Future.successful(None)
  )

  "import export PDN" should {
    "once" in {
      val nb = 40
      val iterations = 10
      def pdnImport = importData.preprocess(None).toOption map { _.game.sloppy } get
      def pdnExport(g: lidraughts.game.Game) = pdnDump(g, None, pdnFlags)
      val game = pdnImport

      def runIn(its: Int) { for (i ← 1 to its) pdnImport }
      def runOut(its: Int) { for (i ← 1 to its) pdnExport(game).void }

      println("warming up")
      if (nb * iterations > 1) {
        runIn(nb * 4)
        runOut(nb * 4)
      }
      println("running tests")
      val durationsIn = for (i ← 1 to iterations) yield {
        val start = System.currentTimeMillis
        runIn(nb)
        val duration = System.currentTimeMillis - start
        println(s"import $nb games in $duration ms")
        duration
      }
      val durationsOut = for (i ← 1 to iterations) yield {
        val start = System.currentTimeMillis
        runOut(nb)
        val duration = System.currentTimeMillis - start
        println(s"export $nb games in $duration ms")
        duration
      }
      val nbGames = iterations * nb
      val moveNanosIn = (1000000 * durationsIn.sum) / nbGames
      val moveNanosOut = (1000000 * durationsOut.sum) / nbGames
      println(s"Average import = $moveNanosIn nanoseconds per game")
      println(s"                 ${1000000000 / moveNanosIn} games per second")
      println(s"Average export = $moveNanosOut nanoseconds per game")
      println(s"                 ${1000000000 / moveNanosOut} games per second")
      true === true
    }
  }

}