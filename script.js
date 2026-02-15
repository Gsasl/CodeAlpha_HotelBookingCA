import { initializeApp } from "https://www.gstatic.com/firebasejs/10.8.0/firebase-app.js";
import { getFirestore, collection, addDoc, onSnapshot, query, where } from "https://www.gstatic.com/firebasejs/10.8.0/firebase-firestore.js";
import { getAuth, signInWithPopup, GoogleAuthProvider, signOut, onAuthStateChanged } from "https://www.gstatic.com/firebasejs/10.8.0/firebase-auth.js";

const firebaseConfig = {
    apiKey: "AIzaSyCfKIYABJTbLnp7FWp_LKMf6m-rtc2fslw",
    authDomain: "hotel-ca.firebaseapp.com",
    projectId: "hotel-ca",
    storageBucket: "hotel-ca.firebasestorage.app",
    messagingSenderId: "540055595631",
    appId: "1:540055595631:web:27bbf0936ee4ff89a6fb9a"
};

const app = initializeApp(firebaseConfig);
const db = getFirestore(app);
const auth = getAuth(app);
const provider = new GoogleAuthProvider();
let currentUser = null;

// --- AUTH OBSERVER ---
onAuthStateChanged(auth, (user) => {
    const loginScreen = document.getElementById('login-screen');
    const appContent = document.getElementById('app-content');
    if (user) {
        currentUser = user;
        loginScreen.style.display = 'none';
        appContent.style.display = 'block';
        document.getElementById('guestName').value = user.displayName;
        document.getElementById('email').value = user.email;
    } else {
        currentUser = null;
        loginScreen.style.display = 'block';
        appContent.style.display = 'none';
    }
});

document.getElementById('googleLoginBtn').onclick = () => signInWithPopup(auth, provider);
document.getElementById('googleLogoutBtn').onclick = () => signOut(auth);

// --- TAB LOGIC ---
window.showTab = (tabId) => {
    document.querySelectorAll('.section').forEach(s => s.classList.remove('active'));
    document.querySelectorAll('.tab').forEach(t => t.classList.remove('active'));
    document.getElementById(tabId).classList.add('active');
    document.getElementById('tab-' + tabId).classList.add('active');
};
document.getElementById('tab-book').onclick = () => showTab('book');
document.getElementById('tab-manage').onclick = () => showTab('manage');
document.getElementById('tab-policies').onclick = () => showTab('policies');

// --- RECEIPT DOWNLOAD ---
function triggerDownload(filename, text) {
    let element = document.createElement('a');
    element.setAttribute('href', 'data:text/plain;charset=utf-8,' + encodeURIComponent(text));
    element.setAttribute('download', filename);
    element.click();
}

document.getElementById('bookBtn').onclick = async () => {
    const refNo = Math.random().toString(36).substr(2, 8).toUpperCase();
    const receipt = `HOTEL CA RECEIPT\nRef: ${refNo}\nGuest: ${currentUser.displayName}`;
    
    await addDoc(collection(db, "bookings"), {
        userId: currentUser.uid,
        ref: refNo,
        email: currentUser.email,
        checkIn: document.getElementById('checkIn').value
    });
    
    document.getElementById('bookingOutput').innerHTML = `<div class="receipt">${receipt}</div>`;
    triggerDownload(`Receipt_${refNo}.txt`, receipt);
};
