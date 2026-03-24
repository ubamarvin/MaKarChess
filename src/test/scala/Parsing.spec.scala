import munit.FunSuite

class ParsingSpec extends FunSuite:
  test("parseMove parses e2e4") {
    val result = parseMove("e2e4")
    assertEquals(result, Right(Move(Position('e', 2), Position('e', 4))))
  }

  test("Move.fromString delegates to parseMove") {
    val result = Move.fromString("a2a4")
    assertEquals(result, Right(Move(Position('a', 2), Position('a', 4))))
  }

  test("parseMove rejects invalid format length") {
    val result = parseMove("e2e")
    assertEquals(result, Left(GameError.InvalidMoveFormat))
  }

  test("parseMove rejects invalid source position") {
    val result = parseMove("z2e4")
    assertEquals(result, Left(GameError.InvalidPosition))
  }

  test("parseMove rejects invalid target position") {
    val result = parseMove("e2e9")
    assertEquals(result, Left(GameError.InvalidPosition))
  }
