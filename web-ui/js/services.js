import {
  analyzeFen,
  chooseBotMove,
  getBoard,
  getBotTypes,
  getCurrentAnalysis,
  getRanking,
  getStatus,
  health,
  newGame,
  recordRankingResult,
  resetGame,
  ApiClientError
} from "./api.js";

const outputs = {
  game: document.getElementById("game-service-output"),
  bot: document.getElementById("bot-service-output"),
  analysis: document.getElementById("analysis-service-output"),
  ranking: document.getElementById("ranking-service-output")
};

const controls = {
  botType: document.getElementById("service-bot-type"),
  analysisFen: document.getElementById("service-analysis-fen"),
  rankingWhitePlayer: document.getElementById("ranking-white-player"),
  rankingBlackPlayer: document.getElementById("ranking-black-player"),
  rankingResult: document.getElementById("ranking-result")
};

function renderOutput(target, payload) {
  outputs[target].textContent = JSON.stringify(payload, null, 2);
}

function renderError(target, error) {
  const message = error instanceof ApiClientError ? error.message : "Unexpected frontend error.";
  outputs[target].textContent = JSON.stringify({ error: message }, null, 2);
}

async function run(target, request) {
  outputs[target].textContent = "Loading...";

  try {
    renderOutput(target, await request());
  } catch (error) {
    renderError(target, error);
  }
}

async function handleAction(action) {
  switch (action) {
    case "health":
      await run("game", health);
      break;
    case "game-status":
      await run("game", getStatus);
      break;
    case "game-board":
      await run("game", getBoard);
      break;
    case "game-new":
      await run("game", () => newGame());
      break;
    case "game-reset":
      await run("game", resetGame);
      break;
    case "bot-types":
      await run("bot", getBotTypes);
      break;
    case "bot-move":
      await run("bot", () => chooseBotMove(controls.botType.value));
      break;
    case "analysis-current":
      await run("analysis", getCurrentAnalysis);
      break;
    case "analysis-fen":
      await run("analysis", () => analyzeFen(controls.analysisFen.value.trim() || null));
      break;
    case "ranking-result":
      await run("ranking", () =>
        recordRankingResult(
          controls.rankingWhitePlayer.value.trim(),
          controls.rankingBlackPlayer.value.trim(),
          controls.rankingResult.value
        )
      );
      break;
    case "ranking-list":
      await run("ranking", getRanking);
      break;
    default:
      break;
  }
}

document.querySelectorAll("[data-service-action]").forEach((button) => {
  button.addEventListener("click", () => {
    handleAction(button.dataset.serviceAction);
  });
});

Promise.allSettled([health(), getBotTypes(), getCurrentAnalysis(), getRanking()]).then(
  ([healthResult, botResult, analysisResult, rankingResult]) => {
    renderOutput("game", resultPayload(healthResult));
    renderOutput("bot", resultPayload(botResult));
    renderOutput("analysis", resultPayload(analysisResult));
    renderOutput("ranking", resultPayload(rankingResult));
  }
);

function resultPayload(result) {
  if (result.status === "fulfilled") {
    return result.value;
  }

  const message = result.reason instanceof ApiClientError
    ? result.reason.message
    : "Backend not reachable.";
  return { error: message };
}
