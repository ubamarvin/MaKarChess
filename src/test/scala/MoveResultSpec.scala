import munit.FunSuite

import makarchess.model.MoveAttemptError
import makarchess.util.MoveResult

class MoveResultSpec extends FunSuite:

  test("MoveResult supports for-comprehension chaining") {
    val r =
      for
        a <- MoveResult.pure(2)
        b <- MoveResult.pure(3)
      yield a + b

    assertEquals(r, MoveResult.Ok(5))
  }

  test("MoveResult short-circuits on first Err") {
    val r =
      for
        _ <- MoveResult.fail(MoveAttemptError.InvalidInput)
        b <- MoveResult.pure(3)
      yield b

    assertEquals(r, MoveResult.Err(MoveAttemptError.InvalidInput))
  }

  test("Monad laws (basic examples): left identity, right identity, associativity") {
    def f(x: Int): MoveResult[Int] = MoveResult.pure(x + 1)
    def g(x: Int): MoveResult[Int] = MoveResult.pure(x * 2)

    val a = 7

    val leftIdentity = MoveResult.pure(a).flatMap(f)
    val leftIdentityRhs = f(a)
    assertEquals(leftIdentity, leftIdentityRhs)

    val m: MoveResult[Int] = MoveResult.pure(a)
    val rightIdentity = m.flatMap(MoveResult.pure)
    assertEquals(rightIdentity, m)

    val assoc1 = m.flatMap(f).flatMap(g)
    val assoc2 = m.flatMap(x => f(x).flatMap(g))
    assertEquals(assoc1, assoc2)
  }
