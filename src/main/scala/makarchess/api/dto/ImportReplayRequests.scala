package makarchess.api.dto

final case class FenRequest(fen: String)

final case class PgnRequest(pgn: String)

final case class ReplayStatusResponse(
    active: Boolean,
    index: Option[Int],
    length: Option[Int]
)
