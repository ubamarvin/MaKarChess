import * as THREE from "three";
import { squareToWorld } from "./board.js";

let piecesGroup = null;

const whiteMaterial = new THREE.MeshStandardMaterial({ color: 0xf4f1e8, roughness: 0.55, metalness: 0.15 });
const blackMaterial = new THREE.MeshStandardMaterial({ color: 0x202632, roughness: 0.45, metalness: 0.2 });

function materialFor(color) {
  return color === "White" ? whiteMaterial : blackMaterial;
}

function createPawn(material) {
  return new THREE.Mesh(new THREE.CylinderGeometry(0.22, 0.28, 0.65, 24), material);
}

function createRook(material) {
  return new THREE.Mesh(new THREE.BoxGeometry(0.56, 0.7, 0.56), material);
}

function createKnight(material) {
  return new THREE.Mesh(new THREE.ConeGeometry(0.3, 0.8, 4), material);
}

function createBishop(material) {
  const group = new THREE.Group();
  const body = new THREE.Mesh(new THREE.CylinderGeometry(0.2, 0.28, 0.72, 20), material);
  const top = new THREE.Mesh(new THREE.ConeGeometry(0.2, 0.34, 18), material);
  top.position.y = 0.48;
  group.add(body, top);
  return group;
}

function createQueen(material) {
  const group = new THREE.Group();
  const body = new THREE.Mesh(new THREE.CylinderGeometry(0.23, 0.31, 0.88, 20), material);
  const crown = new THREE.Mesh(new THREE.ConeGeometry(0.24, 0.35, 18), material);
  crown.position.y = 0.6;
  group.add(body, crown);
  return group;
}

function createKing(material) {
  const group = new THREE.Group();
  const body = new THREE.Mesh(new THREE.CylinderGeometry(0.24, 0.32, 1.0, 20), material);
  const top = new THREE.Mesh(new THREE.BoxGeometry(0.12, 0.32, 0.12), material);
  top.position.y = 0.67;
  const cross = new THREE.Mesh(new THREE.BoxGeometry(0.3, 0.08, 0.08), material);
  cross.position.y = 0.67;
  group.add(body, top, cross);
  return group;
}

function createPieceMesh(piece) {
  const material = materialFor(piece.color);

  switch (piece.kind) {
    case "Pawn":
      return createPawn(material);
    case "Rook":
      return createRook(material);
    case "Knight":
      return createKnight(material);
    case "Bishop":
      return createBishop(material);
    case "Queen":
      return createQueen(material);
    case "King":
      return createKing(material);
    default:
      return createPawn(material);
  }
}

export function clearPieces(scene) {
  if (piecesGroup) {
    scene.remove(piecesGroup);
  }

  piecesGroup = new THREE.Group();
  piecesGroup.name = "pieces";
  scene.add(piecesGroup);
}

export function renderPieces(scene, boardState) {
  if (!piecesGroup) {
    clearPieces(scene);
  }

  piecesGroup.clear();

  for (const occupiedSquare of boardState.board || []) {
    const { position, piece } = occupiedSquare;
    const mesh = createPieceMesh(piece);
    const { x, z } = squareToWorld(position.file, position.rank);
    mesh.position.set(x, 0.52, z);
    mesh.castShadow = true;
    mesh.traverse((child) => {
      child.castShadow = true;
      child.userData = {
        type: "piece",
        color: piece.color,
        kind: piece.kind,
        position: `${position.file}${position.rank}`,
        file: position.file,
        rank: position.rank
      };
    });
    piecesGroup.add(mesh);
  }
}

export function getPiecesGroup() {
  return piecesGroup;
}
