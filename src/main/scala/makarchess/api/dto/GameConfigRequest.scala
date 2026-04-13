package makarchess.api.dto

final case class GameConfigRequest(
    botType: Option[String] = None,
    botPlays: Option[String] = None,
    modeledSide: Option[String] = None
)
