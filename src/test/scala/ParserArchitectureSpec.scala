import munit.FunSuite

import makarchess.model.{ChessRules, PgnReplay, Position, Piece, Color, PieceType}
import makarchess.parser.ParserModule
import makarchess.parser.api.ParserBackend
import makarchess.serialization.UpickleGameStateJsonCodec

class ParserArchitectureSpec extends FunSuite:
  private val fenInput = "r1bqkbnr/pppp1ppp/2n5/4p3/2B1P3/5N2/PPPP1PPP/RNBQK2R b KQkq - 2 3"
  private val pgnInput =
    """[Event "Scholar's Mate"]
      |[Result "1-0"]
      |
      |1. e4 e5
      |2. Bc4 Nc6
      |3. Qh5 Nf6
      |4. Qxf7# 1-0
      |""".stripMargin
  private val londonGameOne =
    """[Event "London m5"]
      |[Site "London"]
      |[Date "1862.??.??"]
      |[Round "?"]
      |[White "Mackenzie, George Henry"]
      |[Black "Paulsen, Louis"]
      |[Result "1-0"]
      |[WhiteElo ""]
      |[BlackElo ""]
      |[ECO "C51"]
      |
      |1.e4 e5 2.Nf3 Nc6 3.Bc4 Bc5 4.b4 Bxb4 5.c3 Bc5 6.O-O d6 7.d4 exd4 8.cxd4 Bb6
      |9.Nc3 Na5 10.Bd3 Ne7 11.e5 dxe5 12.dxe5 O-O 13.Qc2 h6 14.Ba3 c5 15.Rad1 Bd7
      |16.e6 fxe6 17.Bh7+ Kh8 18.Ne5 Nd5 19.Nxd5 exd5 20.Rxd5 Bf5 21.Rxd8 Bxc2 22.Rxf8+ Rxf8
      |23.Bxc2  1-0
      |""".stripMargin
  private val londonGameTwo =
    """[Event "London m5"]
      |[Site "London"]
      |[Date "1862.??.??"]
      |[Round "?"]
      |[White "Paulsen, Louis"]
      |[Black "Mackenzie, George Henry"]
      |[Result "1-0"]
      |[WhiteElo ""]
      |[BlackElo ""]
      |[ECO "C51"]
      |
      |1.e4 e5 2.Nf3 Nc6 3.Bc4 Bc5 4.b4 Bxb4 5.c3 Bc5 6.O-O d6 7.d4 exd4 8.cxd4 Bb6
      |9.Bb2 Nf6 10.d5 Na5 11.Bd3 O-O 12.Nc3 Bg4 13.Ne2 Bxf3 14.gxf3 c5 15.Qd2 c4
      |16.Bc2 Rc8 17.Bc3 Rc5 18.Kh1 Ne8 19.Rg1 f6 20.Nf4 Nc7 21.Nh5 Rf7 22.Qh6 Qf8
      |23.Ba4 Kh8 24.Bxf6  1-0
      |""".stripMargin

  test("all FEN parsers produce the same parsed result") {
    val results = ParserBackend.values.toList.map(backend => ParserModule.fenParser(backend).parse(fenInput))
    assert(results.forall(_.isRight))
    assertEquals(results.distinct.size, 1)
  }

  test("all FEN parsers support roundtrip render") {
    ParserBackend.values.foreach { backend =>
      val parser = ParserModule.fenParser(backend)
      val fen = parser.parse(fenInput).toOption.get
      assertEquals(parser.parse(parser.render(fen)), Right(fen))
    }
  }

  test("all PGN parsers produce the same parsed result") {
    val results = ParserBackend.values.toList.map(backend => ParserModule.pgnParser(backend).parse(pgnInput))
    assert(results.forall(_.isRight))
    assertEquals(results.distinct.size, 1)
  }

  test("PGN replay reaches the expected final position and supports navigation") {
    val parser = ParserModule.pgnParser(ParserBackend.Fast)
    val game = parser.parse(pgnInput).toOption.get
    val cursor = PgnReplay.buildCursor(ChessRules.initialState, game.moves).toOption.get

    assertEquals(cursor.states.length, game.moves.length + 1)
    assertEquals(cursor.currentState.toOption.get, ChessRules.evaluateState(ChessRules.initialState))

    val atEnd = cursor.jumpToEnd()
    val finalState = atEnd.currentState.toOption.get
    assertEquals(finalState.phase, makarchess.model.GamePhase.Checkmate(Color.White))
    assertEquals(finalState.board.get(Position('f', 7)), Some(Piece(Color.White, PieceType.Queen)))

    val steppedBack = atEnd.stepBackward().toOption.get
    assertEquals(steppedBack.index, game.moves.length - 1)
    assertEquals(steppedBack.stepForward().toOption.get.index, game.moves.length)
    assertEquals(cursor.jumpTo(2).toOption.get.index, 2)
  }

  test("PGN parsers render and reparse consistently") {
    ParserBackend.values.foreach { backend =>
      val parser = ParserModule.pgnParser(backend)
      val game = parser.parse(pgnInput).toOption.get
      assertEquals(parser.parse(parser.render(game)), Right(game))
    }
  }

  test("PGN parsers accept compact move-number notation from provided examples") {
    val inputs = List(londonGameOne, londonGameTwo)

    inputs.foreach { input =>
      val results = ParserBackend.values.toList.map(backend => ParserModule.pgnParser(backend).parse(input))
      assert(results.forall(_.isRight))
      assertEquals(results.distinct.size, 1)
    }
  }

  test("provided PGN examples replay successfully") {
    val parser = ParserModule.pgnParser(ParserBackend.Fast)
    val games = List(londonGameOne, londonGameTwo)

    games.foreach { input =>
      val game = parser.parse(input).toOption.get
      val replay = PgnReplay.toFinalState(ChessRules.initialState, game.moves)
      assertEquals(replay.isRight, true)
    }
  }

  test("ChessState JSON codec roundtrips") {
    val codec = UpickleGameStateJsonCodec()
    val state = ChessRules.applyLegalMove(ChessRules.initialState, makarchess.model.Move(Position('e', 2), Position('e', 4)))

    val json = codec.encode(state).toOption.get
    val decoded = codec.decode(json)
    assertEquals(decoded, Right(state))
  }
