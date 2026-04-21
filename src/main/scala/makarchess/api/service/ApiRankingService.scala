package makarchess.api.service

import makarchess.api.dto.*

final class ApiRankingService:
  import ApiError.*

  private final case class RankingRecord(
      player: String,
      rating: Int = 1200,
      wins: Int = 0,
      draws: Int = 0,
      losses: Int = 0
  ):
    def games: Int = wins + draws + losses

  private var table: Map[String, RankingRecord] = Map.empty

  def addPlayer(request: RankingPlayerRequest): Either[ApiError, RankingEntryResponse] =
    val player = normalizePlayer(request.player)
    if player.isEmpty then Left(BadRequest("Player name is required."))
    else
      this.synchronized {
        val record = table.getOrElse(player, RankingRecord(player))
        table = table.updated(player, record)
        Right(toResponse(record))
      }

  def leaderboard(): RankingResponse =
    this.synchronized {
      RankingResponse(table.values.toList.sortBy(record => (-record.rating, record.player)).map(toResponse))
    }

  def recordResult(request: RankingResultRequest): Either[ApiError, RankingResultResponse] =
    val whitePlayer = normalizePlayer(request.whitePlayer)
    val blackPlayer = normalizePlayer(request.blackPlayer)
    parseResult(request.result) match
      case Left(error) => Left(error)
      case Right((whiteScore, blackScore)) =>
        if whitePlayer.isEmpty || blackPlayer.isEmpty then Left(BadRequest("Both player names are required."))
        else if whitePlayer == blackPlayer then Left(BadRequest("Players must be different."))
        else
          this.synchronized {
            val white = table.getOrElse(whitePlayer, RankingRecord(whitePlayer))
            val black = table.getOrElse(blackPlayer, RankingRecord(blackPlayer))
            val updatedWhite = updateRecord(white, black.rating, whiteScore)
            val updatedBlack = updateRecord(black, white.rating, blackScore)
            table = table.updated(whitePlayer, updatedWhite).updated(blackPlayer, updatedBlack)
            Right(RankingResultResponse(toResponse(updatedWhite), toResponse(updatedBlack)))
          }

  private def parseResult(result: String): Either[ApiError, (Double, Double)] =
    result.trim.toLowerCase match
      case "white" | "white_win" | "1-0" => Right(1.0 -> 0.0)
      case "black" | "black_win" | "0-1" => Right(0.0 -> 1.0)
      case "draw" | "remis" | "1/2-1/2" => Right(0.5 -> 0.5)
      case other =>
        Left(BadRequest(s"Invalid result value: $other. Use 'white', 'black' or 'draw'."))

  private def updateRecord(record: RankingRecord, opponentRating: Int, score: Double): RankingRecord =
    val expected = 1.0 / (1.0 + math.pow(10.0, (opponentRating - record.rating) / 400.0))
    val nextRating = math.round(record.rating + 32 * (score - expected)).toInt
    score match
      case 1.0 => record.copy(rating = nextRating, wins = record.wins + 1)
      case 0.5 => record.copy(rating = nextRating, draws = record.draws + 1)
      case _   => record.copy(rating = nextRating, losses = record.losses + 1)

  private def normalizePlayer(player: String): String =
    Option(player).map(_.trim).getOrElse("")

  private def toResponse(record: RankingRecord): RankingEntryResponse =
    RankingEntryResponse(
      player = record.player,
      rating = record.rating,
      wins = record.wins,
      draws = record.draws,
      losses = record.losses,
      games = record.games
    )
