package makarchess.persistence

import makarchess.model.PgnGame
import makarchess.parser.PgnParser

final class PgnFileService(fileIO: FileIO, pgnParser: PgnParser):
  def load(path: String): Either[String, PgnGame] =
    for
      raw <- fileIO.read(path)
      game <- pgnParser.parse(raw)
    yield game
