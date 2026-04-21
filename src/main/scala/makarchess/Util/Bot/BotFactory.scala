package makarchess.util.bot

object BotFactory:

  val availableNames: List[String] =
    List("random", "greedy", "defensive", "aggressive")

  def fromName(name: String): Either[String, Bot] =
    name.trim.toLowerCase match
      case "random"     => Right(RandomBot())
      case "greedy"     => Right(GreedyBot())
      case "defensive"  => Right(DefensiveBot())
      case "aggressive" => Right(AggressiveBot())
      case other         => Left(s"Unknown bot type: ${other}")
