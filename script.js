import { initializeApp } from "https://www.gstatic.com/firebasejs/10.8.0/firebase-app.js";
import { getFirestore, collection, addDoc, onSnapshot, updateDoc, deleteDoc, doc, getDocs, query, where } from "https://www.gstatic.com/firebasejs/10.8.0/firebase-firestore.js";
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
let allBookings = [];
let myBookings = [];

// Hotel Setup with Updated Prices
let rooms = [];
for (let f = 1; f <= 10; f++) { for(let r = 1; r <= 10; r++) rooms.push({roomNo: f*100+r, type: "Standard", price: 75}); }
for (let f = 11; f <= 20; f++) { for(let r = 1; r <= 10; r++) rooms.push({roomNo: f*100+r, type: "Deluxe", price: 125}); }
for (let f = 21; f <= 25; f++) { for(let r = 1; r <= 10; r++) rooms.push({roomNo: f*100+r, type: "Suite", price: 250}); }
const holidays = ["01-01", "02-14", "05-01", "12-25", "12-31"]; 

// --- AUTH OBSERVER ---
onAuthStateChanged(auth, (user) => {
    if (user) {
        currentUser = user;
        document.getElementById('login-screen').style.display = 'none';
        document.getElementById('app-content').style.display = 'block';
        document.getElementById('guestName').value = user.displayName;
        document.getElementById('email').value = user.email;
        document.getElementById('status-indicator').innerText = `🟢 Connected as ${user.displayName}`;
        listenToAllBookings(); // Start syncing database
    } else {
        currentUser = null;
        document.getElementById('login-screen').style.display = 'block';
        document.getElementById('app-content').style.display = 'none';
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

// --- DATABASE SYNC & RENDER ---
function listenToAllBookings() {
    onSnapshot(collection(db, "bookings"), (snapshot) => {
        allBookings = [];
        myBookings = [];
        snapshot.forEach((doc) => {
            let data = doc.data();
            data.dbId = doc.id;
            allBookings.push(data);
            if (currentUser && data.userId === currentUser.uid) {
                myBookings.push(data);
            }
        });
        updateFloorDropdown();
        renderMyReservations();
    });
}

// --- CORE LOGIC (Overlap & Pricing) ---
function getDatesInRange(startStr, endStr) {
    let dates = [];
    let curr = new Date(startStr);
    let end = new Date(endStr);
    if (curr.getTime() === end.getTime()) return [curr.getTime()];
    while (curr < end) { dates.push(curr.getTime()); curr.setDate(curr.getDate() + 1); }
    return dates;
}

function isRoomAvailable(roomNo, checkIn, checkOut, excludeDbId = null) {
    let requestedDates = getDatesInRange(checkIn, checkOut);
    for (let b of allBookings) {
        if (b.roomNo === roomNo && b.dbId !== excludeDbId) { // Ignore the booking we are currently rescheduling
            let bookedDates = getDatesInRange(b.checkIn, b.checkOut);
            if (requestedDates.some(rd => bookedDates.includes(rd))) return false; 
        }
    }
    return true;
}

function calculateAdjustedPrice(basePrice, date) {
    let d = new Date(date);
    let mmdd = ("0" + (d.getMonth() + 1)).slice(-2) + "-" + ("0" + d.getDate()).slice(-2);
    if (holidays.includes(mmdd)) return basePrice * 1.21; 
    let day = d.getDay();
    if (day === 5 || day === 6 || day === 0) return basePrice * 1.13; 
    return basePrice * 0.90; 
}

function calculateTotalFare(roomPrice, inDateStr, outDateStr) {
    let inDate = new Date(inDateStr);
    let outDate = new Date(outDateStr);
    let total = 0;
    let current = new Date(inDate);
    if (inDate.getTime() === outDate.getTime()) {
        return calculateAdjustedPrice(roomPrice, current);
    }
    while(current < outDate) {
        total += calculateAdjustedPrice(roomPrice, current);
        current.setDate(current.getDate() + 1);
    }
    return total;
}

// --- UI UPDATES ---
const checkInEl = document.getElementById('checkIn');
const checkOutEl = document.getElementById('checkOut');
const todayStr = new Date().toISOString().split('T')[0];
checkInEl.min = todayStr;
checkOutEl.min = todayStr;

checkInEl.addEventListener('change', () => {
    checkOutEl.min = checkInEl.value; 
    if(checkOutEl.value < checkInEl.value) checkOutEl.value = checkInEl.value;
    updateFloorDropdown();
});
checkOutEl.addEventListener('change', updateFloorDropdown);
document.getElementById('roomType').addEventListener('change', updateFloorDropdown);

function updateFloorDropdown() {
    const cIn = checkInEl.value;
    const cOut = checkOutEl.value;
    const type = document.getElementById('roomType').value;
    const floorSelect = document.getElementById('desiredFloor');
    floorSelect.innerHTML = ''; 
    if (!cIn || !cOut) { floorSelect.innerHTML = '<option value="">Select dates first...</option>'; return; }

    let availableRooms = rooms.filter(r => r.type === type && isRoomAvailable(r.roomNo, cIn, cOut));
    if (availableRooms.length === 0) { floorSelect.innerHTML = '<option value="">No floors available</option>'; return; }

    let floors = [...new Set(availableRooms.map(r => Math.floor(r.roomNo / 100)))].sort((a,b)=>a-b);
    floors.forEach(f => {
        let opt = document.createElement('option'); opt.value = f; opt.text = `Floor ${f}`; floorSelect.add(opt);
    });
}

function triggerDownload(filename, text) {
    let element = document.createElement('a');
    element.setAttribute('href', 'data:text/plain;charset=utf-8,' + encodeURIComponent(text));
    element.setAttribute('download', filename);
    element.click();
}

// --- BOOKING LOGIC ---
document.getElementById('bookBtn').onclick = async function() {
    const btn = this;
    const cIn = checkInEl.value;
    const cOut = checkOutEl.value;
    const type = document.getElementById('roomType').value;
    const desiredFloor = parseInt(document.getElementById('desiredFloor').value);

    if (!cIn || !cOut || isNaN(desiredFloor)) return alert("Please fill all fields.");

    btn.disabled = true;
    btn.innerText = "Processing...";
    
    try {
        let available = rooms.filter(r => r.type === type && isRoomAvailable(r.roomNo, cIn, cOut));
        let floorRooms = available.filter(r => Math.floor(r.roomNo / 100) === desiredFloor);
        if(floorRooms.length === 0) throw new Error("Room no longer available.");
        
        let selectedRoom = floorRooms[Math.floor(Math.random() * floorRooms.length)];
        let totalFare = calculateTotalFare(selectedRoom.price, cIn, cOut);
        
        const refNo = Math.random().toString(36).substr(2, 8).toUpperCase();
        
        await addDoc(collection(db, "bookings"), {
            userId: currentUser.uid,
            ref: refNo,
            name: currentUser.displayName,
            email: currentUser.email,
            roomNo: selectedRoom.roomNo,
            type: selectedRoom.type,
            checkIn: cIn,
            checkOut: cOut,
            totalFare: totalFare,
            timestamp: new Date()
        });

        const receipt = `HOTEL CA RECEIPT\nRef: ${refNo}\nGuest: ${currentUser.displayName}\nRoom: ${selectedRoom.roomNo}\nDates: ${cIn} to ${cOut}\nTotal: $${totalFare.toFixed(2)}`;
        document.getElementById('bookingOutput').innerHTML = `<div class="receipt">${receipt}</div>`;
        triggerDownload(`Receipt_${refNo}.txt`, receipt);
    } catch (e) {
        alert(e.message);
    } finally {
        btn.disabled = false;
        btn.innerText = "Confirm Booking & Download Receipt";
    }
};

// --- MANAGE RESERVATIONS (MY DASHBOARD) ---
function renderMyReservations() {
    const list = document.getElementById('myReservationsList');
    if (myBookings.length === 0) {
        list.innerHTML = '<p>You have no active reservations.</p>';
        return;
    }
    
    let html = '';
    myBookings.forEach(b => {
        html += `
            <div class="booking-card">
                <p><strong>Ref: ${b.ref}</strong> | Room ${b.roomNo} (${b.type})</p>
                <p>Check-In: ${b.checkIn} | Check-Out: ${b.checkOut}</p>
                <p>Total Fare: $${b.totalFare.toFixed(2)}</p>
                
                <button class="action-btn" onclick="toggleReschedule('${b.dbId}')">Reschedule</button>
                <button class="danger-btn" onclick="cancelBooking('${b.dbId}', '${b.ref}')">Cancel</button>
                
                <div id="reschedule-${b.dbId}" class="reschedule-panel">
                    <p style="font-size: 13px; color: #e74c3c;"><em>A 5% fee ($${(b.totalFare * 0.05).toFixed(2)}) applies to rescheduling.</em></p>
                    <label>New Check-In:</label><input type="date" id="newIn-${b.dbId}" min="${todayStr}">
                    <label>New Check-Out:</label><input type="date" id="newOut-${b.dbId}" min="${todayStr}">
                    <button onclick="confirmReschedule('${b.dbId}', ${b.roomNo}, ${b.totalFare})" style="margin-top: 10px;">Confirm New Dates</button>
                </div>
            </div>
        `;
    });
    list.innerHTML = html;
}

window.toggleReschedule = function(dbId) {
    const panel = document.getElementById(`reschedule-${dbId}`);
    panel.style.display = panel.style.display === 'block' ? 'none' : 'block';
};

window.cancelBooking = async function(dbId, ref) {
    if(confirm(`Are you sure you want to cancel booking ${ref}?`)) {
        await deleteDoc(doc(db, "bookings", dbId));
        alert("Booking cancelled successfully.");
    }
};

window.confirmReschedule = async function(dbId, roomNo, oldTotalFare) {
    let newIn = document.getElementById(`newIn-${dbId}`).value;
    let newOut = document.getElementById(`newOut-${dbId}`).value;
    
    if(!newIn || !newOut || newOut < newIn) return alert("Invalid new dates.");
    
    if(!isRoomAvailable(roomNo, newIn, newOut, dbId)) {
        return alert("Sorry, your current room is booked by someone else during those new dates.");
    }

    // Math: New Date Price + 5% Penalty of Old Total
    let roomPrice = rooms.find(r => r.roomNo === roomNo).price;
    let newBaseFare = calculateTotalFare(roomPrice, newIn, newOut);
    let penaltyFee = oldTotalFare * 0.05;
    let finalNewFare = newBaseFare + penaltyFee;

    try {
        await updateDoc(doc(db, "bookings", dbId), {
            checkIn: newIn,
            checkOut: newOut,
            totalFare: finalNewFare
        });
        alert(`Rescheduled successfully! New Total (inc. penalty): $${finalNewFare.toFixed(2)}`);
    } catch(e) {
        alert("Error rescheduling: " + e.message);
    }
};

// --- SEARCH OTHER RESERVATIONS (Ref + Email) ---
document.getElementById('searchBtn').onclick = async function() {
    let ref = document.getElementById('searchRef').value.trim().toUpperCase();
    let email = document.getElementById('searchEmail').value.trim();
    let output = document.getElementById('manageOutput');
    
    if(!ref || !email) return alert("Please provide both Reference and Email.");
    
    const q = query(collection(db, "bookings"), where("ref", "==", ref), where("email", "==", email));
    const querySnapshot = await getDocs(q);
    
    if (querySnapshot.empty) {
        output.innerHTML = "<p style='color:red;'>No matching reservation found. Check Ref and Email.</p>";
    } else {
        querySnapshot.forEach((docSnap) => {
            let b = docSnap.data();
            output.innerHTML = `
                <div class="booking-card">
                    <h3>Found: ${b.ref}</h3>
                    <p><strong>Guest:</strong> ${b.name} | <strong>Room:</strong> ${b.roomNo} (${b.type})</p>
                    <p><strong>Dates:</strong> ${b.checkIn} to ${b.checkOut}</p>
                    <p><strong>Total Fare:</strong> $${b.totalFare.toFixed(2)}</p>
                </div>
            `;
        });
    }
};
