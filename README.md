
# Soil_fertility_prediction_app link

https://drive.google.com/file/d/1LKBbzqC0enUVNFBZQ0cXgju1lFa5WwnR/view?usp=sharing


# presentation video 

https://drive.google.com/drive/folders/1dcfx3-Lp_Sytu_vzCgTeBPGDrzYMkEIR?usp=sharing


# report of this project 

https://www.overleaf.com/4883321188zmkxgvfbndck#572e19




# Soil Fertility Prediction Project

This project aims to predict soil fertility based on various chemical and physical properties of the soil. It includes a machine learning model trained to classify soil fertility, a Flask API to serve predictions, and an Android application to interact with the API and display results.

## Overview

The core of the project is a neural network model (`soil_fertility_model.h5`) trained on a dataset (`dataset1.csv`) containing soil nutrient information. A Flask application (`app.py`) exposes this model through an API endpoint for predictions. An Android application (`SoilfertilityApp`) provides a user interface to input soil parameters and receive fertility predictions from the API.

## Features

* **Machine Learning Model:** A trained TensorFlow/Keras model for soil fertility classification.
* **Prediction API:** A Flask-based backend that loads the trained model and provides predictions via a REST API.
* **Android Application:** A user-friendly mobile application (written in Kotlin using Jetpack Compose) to:
    * Input soil nutrient values.
    * Send data to the prediction API.
    * Display the predicted fertility class and associated probabilities.
* **Data Preprocessing:** Includes a StandardScaler (`scaler.joblib`) and LabelEncoder (`label_encoder.joblib`) used during model training and for new predictions.
* **Model Training Notebook:** A Jupyter Notebook (`ds-project2.ipynb`) detailing the data loading, preprocessing, model building, training, and evaluation steps.

## Project Structure

The project is organized as follows:

* **`/` (Root Directory):**
    * `app.py`: The Flask application that serves the soil fertility prediction API.
    * `ds-project2.ipynb`: Jupyter Notebook containing the machine learning model development process.
    * `dataset1.csv`: The dataset used for training and evaluating the model.
    * `soil_fertility_model.h5`: The trained Keras model file.
    * `scaler.joblib`: The saved scikit-learn StandardScaler.
    * `label_encoder.joblib`: The saved scikit-learn LabelEncoder.
    * `requirements.txt`: Python dependencies for the backend. [cite: 10]
* **`/SoilfertilityApp/`:** Contains the Android application project.
    * `app/src/main/java/com/brijesh1715/soilfertilityapp/MainActivity.kt`: The main activity for the Android application, handling UI and API interaction.
    * `app/build.gradle.kts`: Gradle build script for the Android app, defining dependencies and build configurations.
    * `app/src/main/AndroidManifest.xml`: Android application manifest file.
    * Other standard Android project files and directories (`.idea/`, `gradle/`, `res/`, etc.).

## Dataset

The project utilizes `dataset1.csv`, which contains the following soil parameters:

* N (Nitrogen) (kg/ha)
* P (Phosphorus) (kg/ha)
* K (Potassium) (kg/ha)
* pH (Soil pH)
* EC (Electrical Conductivity) (dS/m)
* OC (Organic Carbon) (%)
* S (Sulphur) (ppm)
* Zn (Zinc) (ppm)
* Fe (Iron) (ppm)
* Cu (Copper) (ppm)
* Mn (Manganese) (ppm)
* B (Boron) (ppm)
* Output (Fertility Class: 0, 1, or 2)

## Model

### Training

The machine learning model is a neural network developed using TensorFlow and Keras. The complete process of data loading, preprocessing (including scaling and label encoding), model architecture definition, training, and evaluation is documented in the `ds-project2.ipynb` Jupyter Notebook.

Key steps in the notebook include:
* Loading the dataset.
* Splitting data into training and testing sets.
* Scaling numerical features using `StandardScaler`.
* Encoding the target variable 'Output' using `LabelEncoder`.
* Building a Sequential model with Dense layers and Dropout for regularization.
* Compiling the model with Adam optimizer and sparse categorical crossentropy loss.
* Training the model with EarlyStopping and ReduceLROnPlateau callbacks.
* Evaluating the model's performance.
* Saving the trained model, scaler, and label encoder.

### Prediction API (`app.py`)

The `app.py` script creates a Flask web server to serve predictions.
* It loads the pre-trained `soil_fertility_model.h5`, `scaler.joblib`, and `label_encoder.joblib`.
* It defines an endpoint `/predict_soil` that accepts POST requests with a JSON payload.
* The payload should contain a 'features' key, which can be a list of 12 soil nutrient values in the expected order (`N, P, K, pH, EC, OC, S, Zn, Fe, Cu, Mn, B`) or a dictionary of feature-value pairs.
* The API preprocesses the input features using the loaded scaler.
* It then uses the model to predict the fertility class and probabilities.
* The fertility classes are mapped to human-readable labels:
    * 0: "Class 0 (e.g., Low Fertility)"
    * 1: "Class 1 (e.g., Medium Fertility)"
    * 2: "Class 2 (e.g., High Fertility)"
* The API returns a JSON response containing the encoded prediction, human-readable prediction, and class probabilities.

## Android Application (`SoilfertilityApp`)

The Android application provides a mobile interface for users to get soil fertility predictions.

### Purpose

To allow users to easily input soil nutrient values and receive an instant fertility prediction by communicating with the backend API.

### Key Features (from `MainActivity.kt`)

* **User Input:** Provides `OutlinedTextField`s for each of the 12 expected soil nutrient values (N, P, K, pH, EC, OC, S, Zn, Fe, Cu, Mn, B). Hints are provided for each input field (e.g., "Nitrogen (N) (kg/ha)").
* **Input Validation:** Basic input validation is performed to ensure that entered values are numeric.
* **API Interaction:**
    * On button click ("Get Fertility Prediction"), the app constructs a JSON payload with the input features.
    * It makes an HTTP POST request to the `API_URL` (defined as `https://brijesh1715-soil-fertility-predictor.hf.space/predict_soil`).
    * Handles API responses, including success and error cases.
* **Result Display:**
    * A separate screen (`ResultScreen`) displays the human-readable prediction and the probabilities for each fertility class.
    * A "Predict Again" button allows the user to return to the input screen.
* **UI:** Built with Jetpack Compose, featuring a `Scaffold` to manage screen transitions between the input and result views. It uses `MaterialTheme` for styling.
* **Asynchronous Operations:** Uses Kotlin Coroutines (`CoroutineScope`, `Dispatchers.IO`, `withContext(Dispatchers.Main)`) for network operations to avoid blocking the UI thread.

## Setup and Installation

### Backend (Python/Flask API)

1.  **Prerequisites:**
    * Python 3.x
    * pip (Python package installer)
2.  **Clone the repository (if applicable).**
3.  **Navigate to the root directory of the project.**
4.  **Install dependencies:**
    ```bash
    pip install -r requirements.txt
    ```
    The `requirements.txt` file includes: [cite: 10]
    * flask
    * numpy
    * pandas
    * tensorflow
    * joblib
    * scikit-learn
5.  **Ensure the model files (`soil_fertility_model.h5`, `scaler.joblib`, `label_encoder.joblib`) are in the same directory as `app.py`.**

### Android Application (SoilfertilityApp)

1.  **Prerequisites:**
    * Android Studio (latest version recommended).
    * Android SDK.
2.  **Open the project:**
    * Open Android Studio.
    * Select "Open an Existing Project" and navigate to the `SoilfertilityApp` directory.
3.  **Build the project:**
    * Android Studio should automatically sync and build the project using Gradle. Dependencies are defined in `SoilfertilityApp/app/build.gradle.kts` and `SoilfertilityApp/build.gradle.kts`.
    * Key dependencies include AndroidX libraries, Kotlin, and Jetpack Compose.

## Usage

### Running the Backend API

1.  **Navigate to the root directory where `app.py` is located.**
2.  **Run the Flask application:**
    ```bash
    python app.py
    ```
3.  The API will typically start on `http://0.0.0.0:7860` (or as configured by Hugging Face Spaces if deployed there).

### Running the Android App

1.  **Ensure the backend API is running and accessible from the device/emulator where the Android app will run.**
    * The `API_URL` in `MainActivity.kt` is currently set to `https://brijesh1715-soil-fertility-predictor.hf.space/predict_soil`. If running the backend locally, you might need to change this URL (e.g., to `http://10.0.2.2:7860` for Android Emulator accessing local machine's localhost).
2.  **Connect an Android device or start an Android Emulator.**
3.  **In Android Studio, select the device/emulator and click the "Run" button.**
4.  The app will install and launch. Enter the soil nutrient values and tap "Get Fertility Prediction".

## Dependencies

### Python (Backend - `requirements.txt` [cite: 10])

* `flask`
* `numpy`
* `pandas`
* `tensorflow`
* `joblib`
* `scikit-learn`

### Android (Gradle - `app/build.gradle.kts`)

* `androidx.core:core-ktx`
* `androidx.lifecycle:lifecycle-runtime-ktx`
* `androidx.activity:activity-compose`
* `androidx.compose:compose-bom`
* `androidx.compose.ui:ui`
* `androidx.compose.ui:ui-graphics`
* `androidx.compose.ui:ui-tooling-preview`
* `androidx.compose.material3:material3`
* Testing libraries (`junit`, `androidx.test.ext:junit`, `androidx.compose.ui:ui-test-junit4`, `androidx.test.espresso:espresso-core`)

Project-level Gradle plugins are defined in `settings.gradle.kts` and `build.gradle.kts` (top-level).



## License

 MIT
