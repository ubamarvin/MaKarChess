import * as THREE from "three";

const FILES = ["a", "b", "c", "d", "e", "f", "g", "h"];
const squareMap = new Map();
const highlightedSquares = new Set();
let boardGroup = null;

const lightMaterial = new THREE.MeshStandardMaterial({ color: 0xe7d7b7, roughness: 0.9 });
const darkMaterial = new THREE.MeshStandardMaterial({ color: 0x72563c, roughness: 0.9 });
const highlightMaterial = new THREE.MeshStandardMaterial({ color: 0x4d7cff, emissive: 0x2746a3, roughness: 0.7 });

export function fileToIndex(file) {
  return FILES.indexOf(String(file).toLowerCase());
}

export function rankToIndex(rank) {
  return Number(rank) - 1;
}

export function squareToWorld(file, rank) {
  const x = fileToIndex(file) - 3.5;
  const z = rankToIndex(rank) - 3.5;
  return { x, y: 0, z };
}

function squareKey(file, rank) {
  return `${String(file).toLowerCase()}${rank}`;
}

function updateSquareMaterial(mesh, isHighlighted) {
  if (isHighlighted) {
    mesh.material = highlightMaterial;
    return;
  }

  mesh.material = mesh.userData.isLight ? lightMaterial : darkMaterial;
}

export function createBoard(scene) {
  if (boardGroup) {
    scene.remove(boardGroup);
  }

  boardGroup = new THREE.Group();
  boardGroup.name = "board";
  squareMap.clear();
  highlightedSquares.clear();

  const base = new THREE.Mesh(
    new THREE.BoxGeometry(9.2, 0.45, 9.2),
    new THREE.MeshStandardMaterial({ color: 0x2b3240, roughness: 0.95 })
  );
  base.position.y = -0.3;
  base.receiveShadow = true;
  boardGroup.add(base);

  const squareGeometry = new THREE.BoxGeometry(1, 0.2, 1);

  for (let rank = 1; rank <= 8; rank += 1) {
    for (let fileIndex = 0; fileIndex < FILES.length; fileIndex += 1) {
      const file = FILES[fileIndex];
      const { x, z } = squareToWorld(file, rank);
      const isLight = (fileIndex + rank) % 2 === 0;
      const square = new THREE.Mesh(squareGeometry, isLight ? lightMaterial : darkMaterial);
      square.receiveShadow = true;
      square.position.set(x, 0, z);
      square.userData = {
        type: "square",
        file,
        rank,
        coordinate: `${file}${rank}`,
        isLight
      };
      squareMap.set(squareKey(file, rank), square);
      boardGroup.add(square);
    }
  }

  scene.add(boardGroup);
  return boardGroup;
}

export function getSquareMeshAt(file, rank) {
  return squareMap.get(squareKey(file, rank)) || null;
}

export function clearHighlights() {
  for (const key of highlightedSquares) {
    const square = squareMap.get(key);
    if (square) {
      updateSquareMaterial(square, false);
    }
  }
  highlightedSquares.clear();
}

export function highlightSquare(file, rank) {
  const key = squareKey(file, rank);
  const square = squareMap.get(key);
  if (!square) {
    return;
  }

  updateSquareMaterial(square, true);
  highlightedSquares.add(key);
}

export function getBoardGroup() {
  return boardGroup;
}
