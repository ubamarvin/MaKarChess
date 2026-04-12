import munit.FunSuite

import java.nio.file.Files

import makarchess.model.ChessRules
import makarchess.persistence.{GameStateJsonService, LocalFileIO}
import makarchess.serialization.UpickleGameStateJsonCodec

class GameStateJsonServiceSpec extends FunSuite:
  test("GameStateJsonService saves and loads gamestate json") {
    val tempFile = Files.createTempFile("makarchess-gamestate", ".json")
    val service = GameStateJsonService(LocalFileIO(), UpickleGameStateJsonCodec(), tempFile.toString)
    val state = ChessRules.initialState

    assertEquals(service.save(state), Right(()))
    assertEquals(service.load(), Right(state))
  }
