# MoveResult als selbstgebaute Monad in MaKarChess

## Wo wird die Monade definiert?

Die selbstgebaute Monade heißt `MoveResult[A]` und ist hier definiert:

- `src/main/scala/makarchess/util/MoveResult.scala`

Dort wird ein generischer Datentyp `MoveResult[A]` implementiert, der entweder

- einen erfolgreichen Wert (`Ok(value)`) oder
- einen Fehler (`Err(error)`)

enthält.

Die für eine Monade typischen Operationen sind dort vorhanden:

- `pure` (Wert in den Kontext heben)
- `map` (Wert transformieren, wenn ok)
- `flatMap` (abhängige Schritte verketten, mit Short-Circuit bei Fehler)

Damit kann man mehrere Schritte, die jeweils fehlschlagen können, sauber aneinanderreihen.

## Wo wird die Monade verwendet?

`MoveResult` ist in der Move-Pipeline an den Stellen eingesetzt, an denen früher typischerweise `Either`/`Option` verwendet würde. Konkret:

- **Move Parsing (UCI String -> Move)**
  - `src/main/scala/makarchess/model/ChessRules.scala`
  - Funktion: `ChessRules.parseUci(input: String): MoveResult[Move]`

- **Move anwenden / validieren im Modell**
  - `src/main/scala/makarchess/model/ChessModel.scala`
  - Funktion: `tryMove(uci: String): MoveResult[ChessState]` (bzw. die Methode, die den Zug versucht)
  - Hier werden Parsing, Legality-Checks und State-Update über ein `for`-Comprehension / `flatMap` miteinander verkettet.

- **Controller (Eingabe verarbeiten)**
  - `src/main/scala/makarchess/controller/ChessController.scala`
  - Funktion: `handleMoveInput(input: String): MoveResult[Unit]`
  - Die Rückgabe ist `MoveResult`, sodass View-Schichten (TUI/GUI) einen Erfolg oder Fehler einheitlich behandeln können.

- **TUI (Fehler/Ok ausgeben)**
  - `src/main/scala/makarchess/view/TuiView.scala`
  - Die TUI matched auf `MoveResult.Ok` / `MoveResult.Err` und gibt entsprechend Feedback.

- **GUI (Fehler/Ok ausgeben)**
  - `src/main/scala/makarchess/view/GuiView.scala`
  - Beim Absenden eines Zuges (Textfeld/Button oder Click-to-move) wird `controller.handleMoveInput(...)` aufgerufen und anschließend auf `MoveResult.Ok` / `MoveResult.Err` gematcht.

- **Tests**
  - `src/test/scala/MoveResultSpec.scala` testet die grundlegenden Eigenschaften (u.a. Verkettung/Short-Circuit).
  - Diverse Specs wurden angepasst, um `MoveResult` statt `Either` zu erwarten.

## Allgemeiner Vorteil einer Monade

Eine Monade ist (informell) ein Muster, um **Berechnungen mit Kontext** zu strukturieren. Der “Kontext” kann z.B. sein:

- Fehlerbehandlung (`Either`, `Try`, hier: `MoveResult`)
- Optionalität (`Option`)
- Nebenwirkungen/Sequenzierung (`IO`)
- Mehrere Ergebnisse/Non-Determinismus (`List`)

### Vorteil im Code

- **Komponierbarkeit**: Viele kleine Schritte lassen sich zu einer größeren Berechnung zusammensetzen.
- **Weniger Boilerplate**: Statt nach jedem Schritt manuell `if error then return error` zu schreiben, übernimmt das `flatMap` (bzw. `for`-Comprehension) das Short-Circuiting.
- **Klare, lineare Lesbarkeit**: Die Logik steht “von oben nach unten” wie ein Ablaufplan, obwohl intern Fehlerfälle korrekt behandelt werden.
- **Einheitliche Fehlerstrategie**: Alle Move-Schritte (parsen, prüfen, anwenden) liefern denselben Ergebnis-Typ zurück.

### Vorteil konkret in MaKarChess

Beim Schachzug sind viele Schritte möglich, die fehlschlagen können (Format falsch, ungültige Koordinate, illegale Bewegung, Spiel vorbei, …). `MoveResult` erlaubt:

- diese Schritte **nacheinander** zu schreiben,
- beim ersten Fehler **automatisch abzubrechen**,
- und am Ende in TUI/GUI **einheitlich** Erfolg vs. Fehler zu behandeln.
