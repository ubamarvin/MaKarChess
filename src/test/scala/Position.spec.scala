import munit.FunSuite
import makarchess.model.Position

class PositionSpec extends FunSuite:

  test("Position.from returns a valid position for e2") {
    val result = Position.from('e', 2)
    assertEquals(result, Right(Position('e', 2)))
  }

  test("Position.from rejects an invalid file") {
    val result = Position.from('z', 2)
    assert(result.isLeft)
  }

  test("Position.from rejects an invalid rank") {
    val result = Position.from('e', 9)
    assert(result.isLeft)
  }

  test("Position.from accepts uppercase file and normalizes it") {
    val result = Position.from('E', 2)
    assertEquals(result, Right(Position('e', 2)))
  }
