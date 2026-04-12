package makarchess.model

case class PgnGame(
    tags: Map[String, String],
    moves: List[String],
    result: Option[String]
)
