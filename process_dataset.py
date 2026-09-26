import pandas as pd
import firebase_admin
from firebase_admin import credentials, firestore
from sklearn.cluster import DBSCAN
import numpy as np

# ROAD-SOS DATASET PROCESSOR
# INSTRUCTIONS:
# 1. Place the "Indian Road Accident Dataset 2022-2025" (e.g. dataset.csv) in this directory.
# 2. Download your Firebase service account key JSON file and save it as "serviceAccountKey.json".
# 3. Run this script: `pip install pandas scikit-learn firebase-admin numpy` then `python process_dataset.py`

DATASET_PATH = 'dataset.csv'
FIREBASE_KEY_PATH = 'serviceAccountKey.json'

def calculate_risk_level(score):
    if score >= 0.8:
        return "VERY HIGH"
    elif score >= 0.6:
        return "HIGH"
    elif score >= 0.3:
        return "MODERATE"
    return "LOW"

def main():
    print("RoadSOS: Starting Dataset Processing...")
    
    # 1. Initialize Firebase
    try:
        cred = credentials.Certificate(FIREBASE_KEY_PATH)
        firebase_admin.initialize_app(cred)
        db = firestore.client()
        print("Firebase initialized successfully.")
    except Exception as e:
        print(f"Error initializing Firebase. Ensure {FIREBASE_KEY_PATH} is present.")
        print(str(e))
        return

    # 2. Load Dataset
    try:
        # Load the 20,000 records from 2022-2025
        df = pd.read_csv(DATASET_PATH)
        print(f"Dataset loaded: {len(df)} records found.")
    except Exception as e:
        print(f"Error loading dataset. Ensure {DATASET_PATH} is present.")
        print(str(e))
        return

    # 3. Preprocess and Clean
    df = df.dropna(subset=['latitude', 'longitude'])
    
    # 4. Clustering (Grouping nearby accidents into Risk Zones)
    # Using DBSCAN to find dense clusters of accidents. 
    # eps=0.005 is roughly 500 meters. min_samples=3 means at least 3 accidents make a zone.
    coords = df[['latitude', 'longitude']].values
    dbscan = DBSCAN(eps=0.005, min_samples=3, metric='euclidean').fit(coords)
    df['cluster'] = dbscan.labels_
    
    # Exclude noise (-1)
    clusters = df[df['cluster'] != -1]
    
    print(f"Found {len(clusters['cluster'].unique())} accident-prone zones.")

    # 5. Calculate Zone Statistics and Risk Score
    risk_zones = []
    
    for cluster_id, group in clusters.groupby('cluster'):
        center_lat = group['latitude'].mean()
        center_lng = group['longitude'].mean()
        accident_count = len(group)
        
        # Determine common cause
        common_cause = group['cause'].mode()[0] if 'cause' in group.columns else "Unknown"
        city = group['city'].mode()[0] if 'city' in group.columns else "Unknown"
        state = group['state'].mode()[0] if 'state' in group.columns else "Unknown"
        
        # Calculate derived risk score based on existing risk_score + severity + density
        # Assuming the dataset has a 'risk_score' field as per requirements
        avg_dataset_risk = group['risk_score'].mean() if 'risk_score' in group.columns else 0.5
        
        # Normalize accident count to a 0-1 multiplier (assuming max ~ 50 in a cluster)
        density_factor = min(accident_count / 50.0, 1.0)
        
        # Final calculated risk score
        final_risk_score = min((avg_dataset_risk * 0.6) + (density_factor * 0.4), 1.0)
        
        risk_level = calculate_risk_level(final_risk_score)
        
        zone_data = {
            "latitude": float(center_lat),
            "longitude": float(center_lng),
            "historical_accidents": int(accident_count),
            "common_cause": str(common_cause),
            "city": str(city),
            "state": str(state),
            "risk_score": float(final_risk_score),
            "risk_level": risk_level,
            "data_period": "2022-2025"
        }
        risk_zones.append(zone_data)

    # 6. Upload to Firestore
    print("Uploading processed zones to Firestore...")
    batch = db.batch()
    collection_ref = db.collection('risk_zones')
    
    for i, zone in enumerate(risk_zones):
        doc_ref = collection_ref.document(f"zone_{i}")
        batch.set(doc_ref, zone)
        
        # Firestore batches are limited to 500 writes
        if (i + 1) % 400 == 0:
            batch.commit()
            batch = db.batch()
            
    batch.commit()
    print("Upload complete! Risk zones are now live in Firestore.")

if __name__ == "__main__":
    main()
