package makarchess.persistence

import makarchess.model.ChessState
import makarchess.serialization.GameStateJsonCodec

final class GameStateJsonService(fileIO: FileIO, codec: GameStateJsonCodec, path: String = "gamestate.json"):
  def save(state: ChessState): Either[String, Unit] =
    for
      json <- codec.encode(state)
      _ <- fileIO.write(path, json)
    yield ()

  def load(): Either[String, ChessState] =
    for
      json <- fileIO.read(path)
      state <- codec.decode(json)
    yield state
