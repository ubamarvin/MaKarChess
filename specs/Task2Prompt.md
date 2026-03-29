You are a senior Scala 3 software engineer working on a chess project.

Task:
Refactor and extend the existing chess codebase so that it consistently follows a functional programming style as required by the course material.

Context:
The project is a chess implementation in Scala. The implementation must follow functional style principles and should not use low-level bitboard arithmetic. Even though ScalaChess uses abstractions such as Board, Bitboard, History, Status, Position, Move, Color, Role, and Piece, this project must implement the domain in a readable way without bit operations. The functional style requirements come first. Focus especially on the model/domain layer. :contentReference[oaicite:1]{index=1}

Goals:
1. Refactor the model/domain layer into a functional style.
2. Remove imperative and Java-like patterns.
3. Use immutable data structures and pure functions wherever possible.
4. Replace unsafe error/null handling with Scala idioms such as Option and Try.
5. Introduce functional composition patterns such as currying, closures, partially applied functions, function chaining, and for-comprehensions where they improve clarity.
6. Keep the code readable and suitable for teaching purposes.

Hard Requirements:
- Use Scala 3.
- Prefer case classes, enums, and immutable standard collections.
- In the model/domain layer, do not use `var`.
- Do not use `null` or `Null`.
- Replace nullable values with `Option`.
- Avoid `try-catch`; use `Try` where failure must be represented.
- Prefer expressions over statements.
- Prefer recursion and/or collection transformations (`map`, `flatMap`, `fold`, `filter`) over imperative loops.
- Avoid getters/setters in Java style.
- Keep classes short and let constructor parameters define state directly.
- Keep state immutable. Changes to game state must return a new updated state.
- Avoid global mutable state.
- Do not implement the board with bit operations.
- Use the Two-Track pattern for operations that can succeed or fail.
- Use for-comprehensions to unpack monads such as Option and Try where appropriate.

Architecture Guidance:
- The domain should model typical chess abstractions such as:
  - Color
  - Role / PieceType
  - Piece
  - Position
  - Move
  - Board
  - GameState
  - Status / game status
  - History
- The board should be implemented in a readable functional way, for example with standard collections such as:
  - Vector[Vector[Option[Piece]]]
  - Map[Position, Piece]
  Choose the representation that leads to the clearest immutable implementation.
- All domain operations should return new values instead of mutating existing ones.

Implementation Guidance:
- Convert Java-like methods with mutation into pure functions.
- Replace repetitive color-specific logic with curried functions when appropriate.
  Example idea:
  - instead of separate `checkWhite...` and `checkBlack...`, create generic functions like:
    `checkSomething(color: Color)(...)`
- Use partially applied functions when they help derive specialized functions from generic ones.
- Use closures when they improve reuse and avoid duplication.
- Use function chaining where it improves readability.
- Use `foldLeft`, `map`, `flatMap`, `exists`, `forall`, `collect`, etc. instead of manual counters and loops whenever appropriate.
- Represent invalid operations explicitly, e.g.:
  - `Option[GameState]`
  - `Either[ChessError, GameState]`
  - `Try[GameState]`
  Prefer `Either` or `Try` when you need an error reason, `Option` when only presence/absence matters.
- Apply the Two-Track pattern to move validation and move execution:
  - valid path continues with updated state
  - invalid path stays on the failure path and does not “jump back” implicitly

Concrete Refactoring Tasks:
1. Identify and remove:
   - mutable fields
   - null checks
   - imperative loops where a functional alternative is clearer
   - Java-style getters/setters
2. Refactor the board and game state to immutable case classes.
3. Refactor move execution so that:
   - input is explicit
   - output is a new state
   - failure is represented explicitly
4. Refactor validation logic into composable pure functions.
5. Introduce Option/Try/for-comprehension where missing data or failure is possible.
6. Introduce at least a few meaningful examples of:
   - currying
   - closures
   - partially applied functions
7. Keep the implementation easy to understand for students.

Deliverables:
- Refactored Scala source code.
- Short comments explaining important functional design decisions.
- If necessary, add or update tests to verify the new immutable behavior.
- Ensure the code compiles.

Output Format:
1. First, briefly summarize the functional issues you found in the current codebase.
2. Then propose the target design.
3. Then implement the refactoring step by step.
4. Show the final code changes.
5. Highlight where Option, Try, currying, closures, partially applied functions, recursion, collection operations, and two-track handling were introduced.

Quality Criteria:
- Readable over clever
- Functional over imperative
- Immutable over mutable
- Explicit error handling over hidden failure
- Reusable generic functions over duplicated color- or piece-specific code