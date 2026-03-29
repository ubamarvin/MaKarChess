import munit.FunSuite

import makarchess.model.{CastlingRights, Color}

class ChessTypesSpec extends FunSuite:

  test("CastlingRights.without clears the correct side") {
    val r = CastlingRights(true, true, true, true)

    val w = r.without(Color.White)
    assertEquals(w, CastlingRights(false, false, true, true))

    val b = r.without(Color.Black)
    assertEquals(b, CastlingRights(true, true, false, false))
  }

