from flask import Flask, request, jsonify
import numpy as np
import pandas as pd
import tensorflow as tf
import joblib

app = Flask(__name__)

# Load the trained model, scaler, and label encoder
try:
    model = tf.keras.models.load_model("soil_fertility_model.h5")
    scaler = joblib.load("scaler.joblib")
    label_encoder = joblib.load("label_encoder.joblib")
    print("Model, scaler, and label encoder loaded successfully.")
except Exception as e:
    print(f"Error loading artifacts: {e}")
    model, scaler, label_encoder = None, None, None

# Define the order of features expected by the model/scaler
# This MUST match the order of columns in numerical_cols used during training.
EXPECTED_COLUMNS = ['N', 'P', 'K', 'pH', 'EC', 'OC', 'S', 'Zn', 'Fe', 'Cu', 'Mn', 'B']

@app.route('/predict_soil', methods=['POST'])
def predict():
    if not model or not scaler or not label_encoder:
        return jsonify({"error": "Model or preprocessors not loaded"}), 500

    try:
        data = request.get_json(force=True)
        print(f"Received data: {data}")

        # Ensure input is a list or dictionary
        if not isinstance(data.get('features'), list) and not isinstance(data.get('features'), dict):
            return jsonify({"error": "Input 'features' should be a list of values or a dictionary of feature-value pairs."}), 400

        input_features_dict = {}
        if isinstance(data.get('features'), list):
            if len(data['features']) != len(EXPECTED_COLUMNS):
                return jsonify({"error": f"Expected {len(EXPECTED_COLUMNS)} features, got {len(data['features'])}"}), 400
            # Assume the list is in the correct order
            input_features_dict = dict(zip(EXPECTED_COLUMNS, data['features']))
        elif isinstance(data.get('features'), dict):
            input_features_dict = data.get('features')


        # Create a DataFrame in the correct order for scaling
        # Ensure all expected columns are present, fill missing with NaN or raise error
        ordered_input_values = []
        for col in EXPECTED_COLUMNS:
            if col not in input_features_dict:
                 return jsonify({"error": f"Missing feature: {col}"}), 400
            ordered_input_values.append(input_features_dict[col])
        
        input_df = pd.DataFrame([ordered_input_values], columns=EXPECTED_COLUMNS)
        
        # Scale the features
        scaled_features = scaler.transform(input_df)
        
        # Make prediction
        prediction_proba = model.predict(scaled_features)
        predicted_class_encoded = np.argmax(prediction_proba, axis=1)[0]
        
        # (Optional) Get original label if you had string labels
        # For 0,1,2 output, this step might just confirm the class
        # predicted_label_original = label_encoder.inverse_transform([predicted_class_encoded])[0]
        
        # For this model, predicted_class_encoded is what we need (0, 1, or 2)
        # If you want to map these to human-readable labels (e.g., "Low", "Medium", "High")
        # you can add a mapping here or in the frontend.
        fertility_map = {0: "Class 0 (e.g., Low Fertility)", 1: "Class 1 (e.g., Medium Fertility)", 2: "Class 2 (e.g., High Fertility)"}
        human_readable_prediction = fertility_map.get(int(predicted_class_encoded), "Unknown Class")


        return jsonify({
            'encoded_prediction': int(predicted_class_encoded),
            'human_readable_prediction': human_readable_prediction,
            # 'original_label_prediction': str(predicted_label_original) # if applicable
            'probabilities': prediction_proba.tolist()[0] # Send probabilities for each class
        })

    except Exception as e:
        print(f"Error during prediction: {e}")
        return jsonify({"error": str(e)}), 500

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=7860) # Or let HF manage this