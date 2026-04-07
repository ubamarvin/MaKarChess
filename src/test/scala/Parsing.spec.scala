import munit.FunSuite
import makarchess.model.{ChessRules, Move, MoveAttemptError, Position}
import makarchess.util.MoveResult

class ParsingSpec extends FunSuite:

  test("parseUci parses e2e4") {
    val result = ChessRules.parseUci("e2e4")
    assertEquals(result, MoveResult.Ok(Move(Position('e', 2), Position('e', 4))))
  }

  test("parseUci rejects invalid format length") {
    val result = ChessRules.parseUci("e2e")
    assertEquals(result, MoveResult.Err(MoveAttemptError.InvalidInput))
  }

  test("parseUci rejects invalid source position") {
    val result = ChessRules.parseUci("z2e4")
    assertEquals(result, MoveResult.Err(MoveAttemptError.InvalidInput))
  }

  test("parseUci rejects invalid target position") {
    val result = ChessRules.parseUci("e2e9")
    assertEquals(result, MoveResult.Err(MoveAttemptError.InvalidInput))
  }
