package makarchess.serialization

import makarchess.model.ChessState

trait GameStateJsonCodec:
  def encode(state: ChessState): Either[String, String]
  def decode(json: String): Either[String, ChessState]
