package makarchess.persistence

import makarchess.model.Fen
import makarchess.parser.FenParser

final class FenFileService(fileIO: FileIO, fenParser: FenParser):
  def load(path: String): Either[String, Fen] =
    for
      raw <- fileIO.read(path)
      fen <- fenParser.parse(raw)
    yield fen
