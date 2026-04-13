import { initializeApp } from "https://www.gstatic.com/firebasejs/12.12.0/firebase-app.js";
import {
  GoogleAuthProvider,
  createUserWithEmailAndPassword,
  getAuth,
  onAuthStateChanged,
  signInWithEmailAndPassword,
  signInWithPopup,
  signOut as firebaseSignOut
} from "https://www.gstatic.com/firebasejs/12.12.0/firebase-auth.js";

const firebaseConfig = {
  apiKey: "AIzaSyCCK2wl9HQBXpDoWYmN9N1RWVD81tLxmGw",
  authDomain: "makarchess-f1e62.firebaseapp.com",
  projectId: "makarchess-f1e62",
  storageBucket: "makarchess-f1e62.firebasestorage.app",
  messagingSenderId: "1006828888909",
  appId: "1:1006828888909:web:26ff6344340239b6a6cd04",
  measurementId: "G-WP5KNDW0CT"
};

const app = initializeApp(firebaseConfig);
const auth = getAuth(app);
const googleProvider = new GoogleAuthProvider();

googleProvider.setCustomParameters({ prompt: "select_account" });

function normalizeAuthError(error) {
  switch (error?.code) {
    case "auth/invalid-email":
      return "Invalid email address.";
    case "auth/missing-password":
      return "Password is required.";
    case "auth/weak-password":
      return "Password should be at least 6 characters.";
    case "auth/email-already-in-use":
      return "An account with that email already exists.";
    case "auth/user-not-found":
    case "auth/wrong-password":
    case "auth/invalid-credential":
      return "Incorrect email or password.";
    case "auth/popup-closed-by-user":
      return "Google sign-in was cancelled.";
    default:
      return error?.message || "Authentication failed.";
  }
}

export function observeAuthState(callback) {
  return onAuthStateChanged(auth, callback);
}

export async function signUpWithEmail(email, password) {
  try {
    return await createUserWithEmailAndPassword(auth, email, password);
  } catch (error) {
    throw new Error(normalizeAuthError(error));
  }
}

export async function signInWithEmail(email, password) {
  try {
    return await signInWithEmailAndPassword(auth, email, password);
  } catch (error) {
    throw new Error(normalizeAuthError(error));
  }
}

export async function signInWithGoogle() {
  try {
    return await signInWithPopup(auth, googleProvider);
  } catch (error) {
    throw new Error(normalizeAuthError(error));
  }
}

export async function signOutCurrentUser() {
  try {
    await firebaseSignOut(auth);
  } catch (error) {
    throw new Error(normalizeAuthError(error));
  }
}

export async function getCurrentIdToken() {
  const user = auth.currentUser;
  if (!user) {
    return null;
  }
  return user.getIdToken();
}

export function getCurrentUser() {
  return auth.currentUser;
}
