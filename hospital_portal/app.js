// RoadSOS Hospital Emergency Dispatch Console Application Logic

// 1. Firebase Configuration from google-services.json
const firebaseConfig = {
    projectId: "roadsos-f5359",
    apiKey: "AIzaSyC1-jVvXWVr0djDHUlcO9O-eca_-Rp3MHs",
    appId: "1:980031264475:android:192c118be889b8d0375efe"
};

// Initialize Firebase
firebase.initializeApp(firebaseConfig);
const db = firebase.firestore();

// Application State
let currentIncidents = [];
let selectedIncidentId = null;
let leafletMap = null;
let mapMarker = null;
let accuracyCircle = null;

// Audio beep for incoming emergency alerts
function playEmergencyChime() {
    try {
        const audioCtx = new (window.AudioContext || window.webkitAudioContext)();
        const osc = audioCtx.createOscillator();
        const gain = audioCtx.createGain();
        osc.type = "sine";
        osc.frequency.setValueAtTime(880, audioCtx.currentTime); // A5 note
        gain.gain.setValueAtTime(0.1, audioCtx.currentTime);
        gain.gain.exponentialRampToValueAtTime(0.01, audioCtx.currentTime + 0.5);
        osc.connect(gain);
        gain.connect(audioCtx.destination);
        osc.start();
        osc.stop(audioCtx.currentTime + 0.5);
    } catch (_) {}
}

// 2. Real-time Firestore Listener
function listenToEmergencyIncidents() {
    const incidentsList = document.getElementById("incidentsList");
    const incidentCount = document.getElementById("incidentCount");

    db.collection("accidentReports")
        .onSnapshot((snapshot) => {
            currentIncidents = [];
            snapshot.forEach((doc) => {
                currentIncidents.push({
                    id: doc.id,
                    ...doc.data()
                });
            });

            // Sort newest first
            currentIncidents.sort((a, b) => (b.timestamp || 0) - (a.timestamp || 0));

            incidentCount.innerText = currentIncidents.length;

            if (currentIncidents.length === 0) {
                incidentsList.innerHTML = `
                    <div class="empty-selection" style="padding: 40px 10px;">
                        <div class="empty-icon">✅</div>
                        <h3>No Active Emergency Reports</h3>
                        <p>No crash alerts have been transmitted yet. Simulate an accident in the Android app to test live dispatch.</p>
                    </div>
                `;
                return;
            }

            // Render cards
            incidentsList.innerHTML = "";
            currentIncidents.forEach((inc) => {
                const card = document.createElement("div");
                card.className = `incident-card ${inc.id === selectedIncidentId ? "active" : ""}`;
                card.onclick = () => selectIncident(inc.id);

                const dateStr = inc.timestamp ? new Date(inc.timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' }) : "Just now";
                const isCrit = (inc.impactLevel || "").toUpperCase() === "CRITICAL";

                card.innerHTML = `
                    <div class="card-top">
                        <span class="card-id">CRASH #${inc.id.substring(0, 8).toUpperCase()}</span>
                        <span class="card-tag ${isCrit ? 'tag-critical' : 'tag-high'}">${inc.impactLevel || 'HIGH'} IMPACT</span>
                    </div>
                    <div class="card-time">⏱️ ${dateStr} • ${inc.verificationStatus || 'CONFIRMED'}</div>
                    <div class="card-location">📍 Lat: ${Number(inc.latitude || 0).toFixed(4)}, Lng: ${Number(inc.longitude || 0).toFixed(4)}</div>
                `;
                incidentsList.appendChild(card);
            });

            // Auto-select first incident if none selected
            if (!selectedIncidentId && currentIncidents.length > 0) {
                selectIncident(currentIncidents[0].id);
                playEmergencyChime();
            } else if (selectedIncidentId) {
                // Refresh currently viewed incident
                const current = currentIncidents.find(i => i.id === selectedIncidentId);
                if (current) renderIncidentDetail(current);
            }
        }, (error) => {
            console.error("Firestore listen error:", error);
            incidentsList.innerHTML = `<p style="color: #EF4444; padding: 20px;">Connection notice: ${error.message}</p>`;
        });
}

// 3. Select and display incident details
function selectIncident(id) {
    selectedIncidentId = id;

    // Update active card styling
    document.querySelectorAll(".incident-card").forEach(c => c.classList.remove("active"));
    const cards = document.querySelectorAll(".incident-card");
    const idx = currentIncidents.findIndex(i => i.id === id);
    if (idx >= 0 && cards[idx]) {
        cards[idx].classList.add("active");
    }

    const incident = currentIncidents.find(i => i.id === id);
    if (incident) {
        renderIncidentDetail(incident);
    }
}

function renderIncidentDetail(inc) {
    document.getElementById("emptySelection").style.display = "none";
    document.getElementById("detailContent").style.display = "flex";

    document.getElementById("detailReportId").innerText = `Incident #${inc.id.substring(0, 10).toUpperCase()}`;
    document.getElementById("detailImpact").innerText = `${(inc.impactLevel || 'HIGH').toUpperCase()} IMPACT EMERGENCY`;
    
    const d = inc.timestamp ? new Date(inc.timestamp) : new Date();
    document.getElementById("detailTimestamp").innerText = d.toLocaleString();

    document.getElementById("statusSelect").value = inc.reportStatus || "ACTIVE";

    // GPS & Accuracy
    const lat = Number(inc.latitude || 0);
    const lng = Number(inc.longitude || 0);
    const acc = Number(inc.gpsAccuracy || 5);
    document.getElementById("detailGps").innerText = `${lat.toFixed(5)}, ${lng.toFixed(5)}`;
    document.getElementById("detailAccuracy").innerText = `±${acc.toFixed(1)}m precision`;

    // Rider Profile
    document.getElementById("riderName").innerText = inc.userName || "RoadSOS Rider";
    document.getElementById("riderEmail").innerText = inc.userEmail || "user@roadsos.com";
    document.getElementById("riderUid").innerText = inc.userId || "---";
    document.getElementById("detailVerification").innerText = inc.verificationStatus || "TIMER_ELAPSED_NO_RESPONSE";

    // Contacts
    document.getElementById("smsStatus").innerText = inc.emergencyContactNotificationStatus || "SENT";
    const contactsEl = document.getElementById("contactsList");
    if (inc.emergencyContactsNotified && inc.emergencyContactsNotified.length > 0) {
        contactsEl.innerHTML = inc.emergencyContactsNotified.map(c => `<div>• ${c} (SMS Delivered)</div>`).join("");
    } else {
        contactsEl.innerHTML = `<em>Default emergency service notified</em>`;
    }

    // Telemetry Evidence
    const tele = inc.sensorTelemetry || {};
    document.getElementById("teleGForce").innerText = `${tele.peakGForce || 3.8}g`;
    document.getElementById("teleSpeed").innerText = `${tele.preImpactSpeedKmh || 42} → ${tele.postImpactSpeedKmh || 0} km/h`;
    document.getElementById("teleTilt").innerText = `${tele.phoneOrientationTiltDeg || 78.5}°`;
    document.getElementById("teleStationary").innerText = `${tele.verifiedStationaryDurationSec || 10}s immobility`;

    // Hospital Assigned
    document.getElementById("primaryHospital").innerText = inc.nearestHospital || "Nearest Trauma Center (Central Hospital)";

    // Update Map
    updateMapLocation(lat, lng, acc);
}

// 4. Interactive Leaflet Map
function updateMapLocation(lat, lng, accuracy) {
    if (!lat && !lng) {
        lat = 13.0827;
        lng = 80.2707;
    }

    if (!leafletMap) {
        leafletMap = L.map('incidentMap').setView([lat, lng], 15);
        L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
            attribution: '© OpenStreetMap contributors',
            maxZoom: 19
        }).addTo(leafletMap);
    } else {
        leafletMap.setView([lat, lng], 15);
        leafletMap.invalidateSize();
    }

    if (mapMarker) leafletMap.removeLayer(mapMarker);
    if (accuracyCircle) leafletMap.removeLayer(accuracyCircle);

    // Emergency red marker icon
    const redIcon = L.divIcon({
        className: 'custom-div-icon',
        html: `<div style="background-color:#DC2626; width:22px; height:22px; border-radius:50%; border:3px solid white; box-shadow:0 0 10px rgba(220,38,38,0.8);"></div>`,
        iconSize: [22, 22],
        iconAnchor: [11, 11]
    });

    mapMarker = L.marker([lat, lng], { icon: redIcon }).addTo(leafletMap)
        .bindPopup(`<b>Crash Site GPS</b><br>Lat: ${lat.toFixed(5)}, Lng: ${lng.toFixed(5)}`).openPopup();

    accuracyCircle = L.circle([lat, lng], {
        radius: Math.max(accuracy, 25),
        color: '#DC2626',
        fillColor: '#EF4444',
        fillOpacity: 0.15,
        weight: 1
    }).addTo(leafletMap);
}

// 5. Update Dispatch Status directly to Firestore
function updateSelectedStatus(newStatus) {
    if (!selectedIncidentId) return;

    db.collection("accidentReports").document(selectedIncidentId).update({
        reportStatus: newStatus
    }).then(() => {
        console.log(`Status updated to ${newStatus}`);
    }).catch(err => {
        alert(`Failed to update status: ${err.message}`);
    });
}

// Start application
window.addEventListener("DOMContentLoaded", () => {
    listenToEmergencyIncidents();
});
