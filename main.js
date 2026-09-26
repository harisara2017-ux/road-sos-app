import L from 'leaflet';
import 'leaflet-routing-machine';
import { getRiskZonesNearRoute } from './firebaseService.js';

// Setup Map
const map = L.map('map', {
  zoomControl: false
}).setView([13.0827, 80.2707], 12); // Default to Chennai

// Add Tile Layer (OpenStreetMap)
L.tileLayer('https://{s}.basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}{r}.png', {
  attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors &copy; <a href="https://carto.com/attributions">CARTO</a>',
  subdomains: 'abcd',
  maxZoom: 20
}).addTo(map);

// Define custom icons
const createRiskIcon = (color) => {
  return L.divIcon({
    className: 'custom-div-icon',
    html: `<div style="background-color:${color}; width:24px; height:24px; border-radius:50%; border:3px solid white; box-shadow:0 0 4px rgba(0,0,0,0.5);"></div>`,
    iconSize: [24, 24],
    iconAnchor: [12, 12]
  });
};

const RISK_COLORS = {
  'LOW': '#10b981',
  'MODERATE': '#eab308',
  'HIGH': '#f97316',
  'VERY HIGH': '#ef4444'
};

// State
let currentRouteControl = null;
let riskMarkers = [];
let routePolylineCoords = [];
let currentRiskZones = [];
let userLocationMarker = null;

const sourceInput = document.getElementById('source-input');
const destInput = document.getElementById('dest-input');
const generateBtn = document.getElementById('btn-generate-route');

const popup = document.getElementById('warning-popup');
const btnSaferRoute = document.getElementById('btn-safer-route');
const btnContinue = document.getElementById('btn-continue');
const warnLevel = document.getElementById('warn-level');
const warnCount = document.getElementById('warn-count');
const warnScore = document.getElementById('warn-score');

// Geocoding helper (using Nominatim for demo/free usage)
async function geocode(query) {
  try {
    const res = await fetch(`https://nominatim.openstreetmap.org/search?format=json&q=${encodeURIComponent(query)}`);
    const data = await res.json();
    if (data && data.length > 0) {
      return L.latLng(data[0].lat, data[0].lon);
    }
  } catch (e) {
    console.error('Geocoding error:', e);
  }
  return null;
}

// Generate Route
generateBtn.addEventListener('click', async () => {
  const srcText = sourceInput.value;
  const dstText = destInput.value;
  if (!srcText || !dstText) {
    alert("Please enter both Source and Destination.");
    return;
  }

  generateBtn.innerText = 'Calculating...';
  
  const srcLatLng = await geocode(srcText);
  const dstLatLng = await geocode(dstText);

  if (!srcLatLng || !dstLatLng) {
    alert("Could not find locations. Try being more specific (e.g., 'Chennai', 'Tambaram').");
    generateBtn.innerText = 'Generate Route';
    return;
  }

  calculateRoute(srcLatLng, dstLatLng);
});

function calculateRoute(src, dst) {
  if (currentRouteControl) {
    map.removeControl(currentRouteControl);
  }
  clearRiskMarkers();

  currentRouteControl = L.Routing.control({
    waypoints: [src, dst],
    routeWhileDragging: false,
    addWaypoints: false,
    fitSelectedRoutes: true,
    show: false,
    lineOptions: {
      styles: [{ color: '#3b82f6', opacity: 0.8, weight: 6 }]
    }
  }).addTo(map);

  currentRouteControl.on('routesfound', async function(e) {
    const routes = e.routes;
    const summary = routes[0].summary;
    generateBtn.innerText = `Generate Route`;
    
    // Extract route coordinates
    routePolylineCoords = routes[0].coordinates;
    
    // In a real app, send bounding box or polyline to backend to get intersecting risk zones
    // We will query our Firebase service
    currentRiskZones = await getRiskZonesNearRoute(routePolylineCoords);
    
    displayRiskZones(currentRiskZones);
    analyzeRouteRisk(currentRiskZones);
    
    // Simulate navigation/movement along route to trigger warnings
    simulateNavigation(routePolylineCoords);
  });
}

function clearRiskMarkers() {
  riskMarkers.forEach(m => map.removeLayer(m));
  riskMarkers = [];
}

function displayRiskZones(zones) {
  zones.forEach(zone => {
    const color = RISK_COLORS[zone.risk_level] || RISK_COLORS['LOW'];
    const marker = L.marker([zone.latitude, zone.longitude], {
      icon: createRiskIcon(color)
    }).addTo(map);
    
    marker.bindPopup(`
      <div style="font-family:Inter,sans-serif;">
        <strong style="color:${color};font-size:14px;">${zone.risk_level} RISK ZONE</strong><br/>
        Historical Accidents: ${zone.historical_accidents}<br/>
        Risk Score: ${zone.risk_score.toFixed(2)}<br/>
        Common Cause: ${zone.common_cause}<br/>
        <small>${zone.city}, ${zone.state}</small>
      </div>
    `);
    
    // Add heatmap circle as well
    const circle = L.circle([zone.latitude, zone.longitude], {
      color: color,
      fillColor: color,
      fillOpacity: 0.2,
      radius: 400 // 400m radius
    }).addTo(map);
    
    riskMarkers.push(marker);
    riskMarkers.push(circle);
  });
}

function analyzeRouteRisk(zones) {
  // Determine overall route risk based on zones
  const hasVeryHigh = zones.some(z => z.risk_level === 'VERY HIGH');
  const hasHigh = zones.some(z => z.risk_level === 'HIGH');
  
  if (hasVeryHigh || hasHigh) {
    // In a real app, this logic would evaluate multiple alternative routes (routes[1], routes[2])
    // and suggest the safest one. Here we prepare the UI state.
    btnSaferRoute.style.display = 'block';
  } else {
    btnSaferRoute.style.display = 'none';
  }
}

// SIMULATE GPS LOCATION DURING NAVIGATION
let simInterval;
let warnedZones = new Set();

function simulateNavigation(coords) {
  if (simInterval) clearInterval(simInterval);
  if (userLocationMarker) map.removeLayer(userLocationMarker);
  
  warnedZones.clear();
  let currentIndex = 0;
  
  userLocationMarker = L.circleMarker(coords[0], {
    color: 'white',
    fillColor: '#2563eb',
    fillOpacity: 1,
    radius: 8,
    weight: 3
  }).addTo(map);

  simInterval = setInterval(() => {
    if (currentIndex >= coords.length) {
      clearInterval(simInterval);
      return;
    }
    
    const currLoc = coords[currentIndex];
    userLocationMarker.setLatLng(currLoc);
    
    // Check distance to upcoming risk zones
    checkUpcomingRisks(currLoc);
    
    currentIndex += Math.max(1, Math.floor(coords.length / 50)); // jump to speed up simulation
  }, 1000);
}

function checkUpcomingRisks(currentLoc) {
  const WARNING_DISTANCE_METERS = 800; // Trigger warning at 800m
  
  currentRiskZones.forEach(zone => {
    if (warnedZones.has(zone.id)) return;
    
    const zoneLoc = L.latLng(zone.latitude, zone.longitude);
    const distance = currentLoc.distanceTo(zoneLoc);
    
    if (distance <= WARNING_DISTANCE_METERS && (zone.risk_level === 'HIGH' || zone.risk_level === 'VERY HIGH')) {
      triggerWarning(zone, distance);
      warnedZones.add(zone.id);
    }
  });
}

function triggerWarning(zone, distance) {
  warnLevel.innerText = zone.risk_level;
  warnLevel.style.color = RISK_COLORS[zone.risk_level];
  warnCount.innerText = zone.historical_accidents;
  warnScore.innerText = zone.risk_score.toFixed(2);
  
  const h3 = popup.querySelector('h3');
  h3.style.color = RISK_COLORS[zone.risk_level];
  
  popup.classList.remove('hidden');
}

btnContinue.addEventListener('click', () => {
  popup.classList.add('hidden');
});

btnSaferRoute.addEventListener('click', () => {
  popup.classList.add('hidden');
  alert("Routing to Safer Alternative... (Calculating new Leaflet Route)");
  // In a real app, you would switch the leaflet-routing-machine to the alternative route index
});

// Setup map interaction to close popup when clicking outside
map.on('click', () => {
  popup.classList.add('hidden');
});
