import munit.FunSuite

class PositionSpec extends FunSuite {

  test("createPosition returns a valid position for e2") {
    val result = createPosition('e', 2)

    assertEquals(result, Right(Position('e', 2)))
  }

  test("createPosition rejects an invalid file") {
    val result = createPosition('z', 2)

    assertEquals(result, Left(GameError.InvalidPosition))
  }

  test("createPosition rejects an invalid rank") {
    val result = createPosition('e', 9)

    assertEquals(result, Left(GameError.InvalidPosition))
  }

  test("Position.from accepts uppercase file and normalizes it") {
    val result = Position.from('E', 2)

    assertEquals(result, Right(Position('e', 2)))
  }
}