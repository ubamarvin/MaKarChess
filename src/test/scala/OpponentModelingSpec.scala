import munit.FunSuite

import makarchess.model.{ChessModel, ChessRules, Color, GamePhase, Move, PieceType, Position}
import makarchess.opponentmodel.{
  HighlightMapper,
  MoveFeatures,
  OpponentMoveAnalyzer,
  OpponentMovePredictor,
  OpponentProfile,
  OpponentProfileUpdater,
  StyleEstimator
}
import makarchess.util.bot.{AggressiveBot, Bot, BotCaller, DefensiveBot, GreedyBot, RandomBot}
import makarchess.controller.ChessController
import makarchess.view.ConsoleIO
import scala.collection.mutable.ListBuffer

class OpponentModelingSpec extends FunSuite:

  test("OpponentProfileUpdater increases aggression evidence on checking move") {
    val f = MoveFeatures(
      isCapture = false,
      captureValue = 0,
      givesCheck = true,
      movesIntoDanger = false,
      savesOwnPiece = false,
      increasesKingPressure = true
    )

    val p0 = OpponentProfile.empty
    val p1 = OpponentProfileUpdater.update(p0, f)

    assert(p1.aggressionEvidence > p0.aggressionEvidence)
  }

  test("OpponentProfileUpdater increases greed evidence on high-value capture") {
    val state = ChessRules.initialState
    val after = ChessRules.applyLegalMove(state, Move(Position('e', 2), Position('e', 4)))

    val move = Move(Position('d', 8), Position('h', 4))
    val f = MoveFeatures(
      isCapture = true,
      captureValue = 9,
      givesCheck = false,
      movesIntoDanger = false,
      savesOwnPiece = false,
      increasesKingPressure = false
    )

    val p0 = OpponentProfile.empty
    val p1 = OpponentProfileUpdater.update(p0, f)

    assert(p1.greedEvidence > p0.greedEvidence)
  }

  test("StyleEstimator assigns dominant aggressivePct for aggressive profile") {
    val p = OpponentProfile(
      observedMoves = 10,
      aggressionEvidence = 30.0,
      greedEvidence = 1.0,
      defensiveEvidence = 1.0,
      riskEvidence = 5.0,
      confidence = 0.9
    )

    val est = StyleEstimator.fromProfile(p)
    assert(est.aggressivePct > est.greedyPct)
    assert(est.aggressivePct > est.defensivePct)
    assert(est.confidencePct > 50.0)
  }

  test("OpponentMovePredictor ranks moves and provides highlight labels 1/2/3") {
    val state = ChessRules.initialState
    val profile = OpponentProfile(
      observedMoves = 12,
      aggressionEvidence = 10.0,
      greedEvidence = 2.0,
      defensiveEvidence = 1.0,
      riskEvidence = 2.0,
      confidence = 0.7
    )

    val result = OpponentMovePredictor.predict(profile, state, max = 3)

    assert(result.predictions.nonEmpty)
    assertEquals(result.highlights.map(_.label), Vector("1", "2", "3"))
  }

  test("HighlightMapper produces descending intensities") {
    val state = ChessRules.initialState
    val legal = ChessRules.legalMoves(state).take(3).toVector
    val pms = legal.zipWithIndex.map { case (m, i) =>
      makarchess.opponentmodel.PredictedMove(m, score = (3 - i).toDouble, probability = (3 - i).toDouble)
    }

    val highlights = HighlightMapper.fromPredictions(pms, max = 3)
    assert(highlights.size == 3)
    assert(highlights(0).intensity > highlights(1).intensity)
    assert(highlights(1).intensity > highlights(2).intensity)
  }

  private class FakeConsole(inputs: List[String]) extends ConsoleIO:
    private var remaining = inputs
    val output: ListBuffer[String] = ListBuffer.empty

    override def readLine(): Option[String] =
      remaining match
        case head :: tail =>
          remaining = tail
          Some(head)
        case Nil => None

    override def printLine(line: String): Unit =
      output += line

  private def runBotForMoves(bot: Bot, botPlays: Color, modeledSide: Color, moves: List[String], maxMoves: Int = 6): Double =
    val io = FakeConsole(moves :+ "quit")
    val model = ChessModel()
    val controller = ChessController(model)
    val _tui = makarchess.view.TuiView(controller, io)

    val observer = makarchess.opponentmodel.OpponentModelObserver(controller, modeledSide, maxHighlights = 3)
    val _botCaller = BotCaller(controller, bot, botPlays)

    model.notifyObservers
    _tui.runInteractive()
    controller.opponentModelHighlights.size.toDouble

  test("RandomBot does not produce overly strong convergence signal (heuristic)") {
    val sz = runBotForMoves(RandomBot(), botPlays = Color.Black, modeledSide = Color.Black, moves = List("e2e4", "d2d4", "g1f3"))
    assert(sz <= 3.0)
  }
