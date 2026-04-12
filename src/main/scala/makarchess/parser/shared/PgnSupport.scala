package makarchess.parser.shared

import makarchess.model.PgnGame

object PgnSupport:
  private val preferredTagOrder = List("Event", "Site", "Date", "Round", "White", "Black", "Result", "WhiteElo", "BlackElo", "ECO")

  def buildGame(tagLines: Seq[(String, String)], rawMoveText: String): Either[String, PgnGame] =
    val tags = tagLines.foldLeft(Map.empty[String, String]) { case (acc, (key, value)) => acc + (key -> value) }
    val tokens = tokenizeMoves(rawMoveText)
    val (moveTokens, resultToken) =
      tokens.lastOption match
        case Some(value) if isResult(value) => (tokens.dropRight(1), Some(value))
        case _                              => (tokens, tags.get("Result"))
    Right(PgnGame(tags, moveTokens, resultToken))

  def renderPgn(game: PgnGame): String =
    val tags =
      if game.tags.isEmpty then ""
      else
        val preferred = preferredTagOrder.flatMap(key => game.tags.get(key).map(value => key -> value))
        val remaining = game.tags.toList.filterNot { case (k, _) => preferredTagOrder.contains(k) }.sortBy(_._1)
        (preferred ++ remaining).map { case (k, v) => s"[$k \"$v\"]" }.mkString("\n") + "\n\n"

    val moveText =
      game.moves.grouped(2).zipWithIndex.map {
        case (white :: black :: Nil, idx) => s"${idx + 1}.$white $black"
        case (white :: Nil, idx)          => s"${idx + 1}.$white"
        case _                            => ""
      }.filter(_.nonEmpty).mkString("\n")

    val resultSuffix = game.result match
      case Some(result) if moveText.nonEmpty => s" $result"
      case Some(result)                      => result
      case None                              => ""

    tags + moveText + resultSuffix

  def tokenizeMoves(raw: String): List[String] =
    val withoutComments = raw.replaceAll("\\{[^}]*\\}", " ")
    val withoutVariations = withoutComments.replaceAll("\\([^)]*\\)", " ")
    withoutVariations
      .split("\\s+")
      .toList
      .map(_.trim)
      .filter(_.nonEmpty)
      .filterNot(token => token.matches("""\$\d+"""))
      .flatMap(normalizeToken)
      .filter(_.nonEmpty)

  def isResult(token: String): Boolean =
    normalizeResultToken(token) match
      case "1-0" | "0-1" | "1/2-1/2" | "*" => true
      case _                                     => false

  private def stripAnnotations(token: String): String =
    token.replaceAll("[!?]+$", "")

  private def normalizeToken(token: String): List[String] =
    val stripped = stripAnnotations(token)

    if stripped.matches("""\d+\.(\.\.)?""") then Nil
    else if stripped.matches("""\d+\.(\.\.)?.+""") then
      val compactMove = stripped.replaceFirst("""^\d+\.(\.\.)?""", "")
      val normalized = normalizeResultToken(compactMove)
      if normalized.nonEmpty then List(normalized) else Nil
    else
      val normalized = normalizeResultToken(stripped)
      if normalized.nonEmpty then List(normalized) else Nil

  private def normalizeResultToken(token: String): String =
    token match
      case value if value.matches("""(1-0|0-1|1/2-1/2|\*)[.]+""") => value.replaceAll("""[.]+$""", "")
      case value                                                      => value
