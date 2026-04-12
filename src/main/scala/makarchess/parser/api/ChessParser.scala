package makarchess.parser.api

trait ChessParser[A]:
  def parse(input: String): Either[String, A]
