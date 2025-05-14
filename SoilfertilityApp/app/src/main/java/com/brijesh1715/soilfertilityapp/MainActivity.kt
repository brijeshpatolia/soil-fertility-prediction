package com.brijesh1715.soilfertilityapp

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.brijesh1715.soilfertilityapp.ui.theme.SoilfertilityAppTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

// Your API URL - Make sure this is correct
private const val API_URL = "https://brijesh1715-soil-fertility-predictor.hf.space/predict_soil"

// Define the order and hints for features
private val EXPECTED_COLUMNS = listOf("N", "P", "K", "pH", "EC", "OC", "S", "Zn", "Fe", "Cu", "Mn", "B")
private val FEATURE_HINTS = mapOf(
    "N" to "Nitrogen (N) (kg/ha)",
    "P" to "Phosphorus (P) (kg/ha)",
    "K" to "Potassium (K) (kg/ha)",
    "pH" to "Soil pH",
    "EC" to "EC (dS/m)",
    "OC" to "Organic Carbon (%)",
    "S" to "Sulphur (S) (ppm)",
    "Zn" to "Zinc (Zn) (ppm)",
    "Fe" to "Iron (Fe) (ppm)",
    "Cu" to "Copper (Cu) (ppm)",
    "Mn" to "Manganese (Mn) (ppm)",
    "B" to "Boron (B) (ppm)"
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SoilfertilityAppTheme {
                // A surface container using the 'background' color from the theme
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    SoilFertilityApp() // Changed to a new root composable
                }
            }
        }
    }
}

@Composable
fun SoilFertilityApp() {
    var currentScreen by remember { mutableStateOf(Screen.Input) }
    var humanPredictionResult by remember { mutableStateOf<String?>(null) }
    var probabilitiesResult by remember { mutableStateOf<String?>(null) }

    Scaffold { innerPadding ->
        when (currentScreen) {
            Screen.Input -> SoilInputScreen(
                modifier = Modifier.padding(innerPadding),
                onPredictionSuccess = { humanPrediction, probabilities ->
                    humanPredictionResult = humanPrediction
                    probabilitiesResult = probabilities
                    currentScreen = Screen.Result
                }
            )
            Screen.Result -> ResultScreen(
                modifier = Modifier.padding(innerPadding),
                humanPrediction = humanPredictionResult,
                probabilities = probabilitiesResult,
                onGoBack = {
                    currentScreen = Screen.Input
                    // Optionally clear previous results when going back
                    humanPredictionResult = null
                    probabilitiesResult = null
                }
            )
        }
    }
}

enum class Screen {
    Input,
    Result
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SoilInputScreen(
    modifier: Modifier = Modifier,
    onPredictionSuccess: (humanPrediction: String, probabilities: String) -> Unit
) {
    val inputStates = EXPECTED_COLUMNS.associateWith { remember { mutableStateOf("") } }
    var isLoading by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            "Soil Fertility Predictor",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        Text(
            "Enter Soil Nutrient Values:",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        EXPECTED_COLUMNS.forEach { featureName ->
            OutlinedTextField(
                value = inputStates[featureName]?.value ?: "",
                onValueChange = { newValue ->
                    val filteredValue = newValue.filterIndexed { index, char ->
                        char.isDigit() || (char == '.' && newValue.count { it == '.' } <= 1 && (index > 0 || newValue.length == 1 || (index == 0 && newValue.length > 1 && newValue[1].isDigit())))
                    }
                    inputStates[featureName]?.value = if (filteredValue.startsWith(".")) "0$filteredValue" else filteredValue
                },
                label = { Text(FEATURE_HINTS[featureName] ?: featureName) },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = if (featureName == EXPECTED_COLUMNS.last()) ImeAction.Done else ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onDone = { focusManager.clearFocus() }
                ),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                Log.d("PredictButton", "Button Clicked!")
                focusManager.clearFocus()
                val featureValues = mutableListOf<Double>()
                var validInput = true
                errorText = null

                for (featureName in EXPECTED_COLUMNS) {
                    val valueString = inputStates[featureName]?.value ?: ""
                    Log.d("PredictButton", "Validating feature: $featureName, Value: '$valueString'")
                    if (valueString.isEmpty() || valueString == "." || valueString.endsWith(".")) {
                        Log.w("PredictButton", "Validation failed for $featureName: Empty or invalid format")
                        Toast.makeText(context, "Please enter a valid number for ${FEATURE_HINTS[featureName] ?: featureName}", Toast.LENGTH_LONG).show()
                        validInput = false
                        break
                    }
                    val valueDouble = valueString.toDoubleOrNull()
                    if (valueDouble != null) {
                        featureValues.add(valueDouble)
                    } else {
                        Log.w("PredictButton", "Validation failed for $featureName: Not a valid double - '$valueString'")
                        Toast.makeText(context, "Invalid number for ${FEATURE_HINTS[featureName] ?: featureName}", Toast.LENGTH_LONG).show()
                        validInput = false
                        break
                    }
                }

                Log.d("PredictButton", "Input validation complete. validInput: $validInput")

                if (validInput) {
                    isLoading = true
                    val jsonPayload = JSONObject()
                    jsonPayload.put("features", JSONArray(featureValues))
                    Log.d("PredictButton", "JSON Payload created: ${jsonPayload.toString()}")

                    coroutineScope.launch(Dispatchers.IO) {
                        Log.d("PredictButton", "Coroutine launched for API call.")
                        try {
                            val url = URL(API_URL)
                            val connection = url.openConnection() as HttpURLConnection
                            connection.requestMethod = "POST"
                            connection.setRequestProperty("Content-Type", "application/json; utf-8")
                            connection.setRequestProperty("Accept", "application/json")
                            connection.connectTimeout = 15000
                            connection.readTimeout = 15000
                            connection.doOutput = true

                            OutputStreamWriter(connection.outputStream, StandardCharsets.UTF_8).use { writer ->
                                writer.write(jsonPayload.toString())
                                writer.flush()
                            }
                            Log.d("PredictButton", "Data sent to API.")

                            val responseCode = connection.responseCode
                            Log.d("PredictButton", "API Response Code: $responseCode")

                            if (responseCode == HttpURLConnection.HTTP_OK) {
                                BufferedReader(InputStreamReader(connection.inputStream, StandardCharsets.UTF_8)).use { reader ->
                                    val response = reader.readText()
                                    Log.d("PredictButton", "API Response: $response")
                                    val jsonResponse = JSONObject(response)
                                    val humanPrediction = jsonResponse.getString("human_readable_prediction")
                                    val probabilitiesArray = jsonResponse.getJSONArray("probabilities")
                                    val probs = DoubleArray(probabilitiesArray.length()) { probabilitiesArray.getDouble(it) }
                                    val probabilitiesString = "Probabilities: \nClass 0: ${"%.2f".format(probs.getOrElse(0) { 0.0 })}\nClass 1: ${"%.2f".format(probs.getOrElse(1) { 0.0 })}\nClass 2: ${"%.2f".format(probs.getOrElse(2) { 0.0 })}"

                                    withContext(Dispatchers.Main) {
                                        onPredictionSuccess(humanPrediction, probabilitiesString)
                                        isLoading = false
                                        errorText = null
                                    }
                                }
                            } else {
                                val errorResponseText = try { BufferedReader(InputStreamReader(connection.errorStream, StandardCharsets.UTF_8)).use { it.readText() } } catch (e: Exception) { "Error (Code $responseCode) - No error details." }
                                Log.e("API_CALL", "Error Response ($responseCode): $errorResponseText")
                                withContext(Dispatchers.Main) {
                                    errorText = "API Error: $errorResponseText"
                                    isLoading = false
                                }
                            }
                            connection.disconnect()
                        } catch (e: Exception) {
                            Log.e("PredictButton", "Exception in coroutine", e)
                            e.printStackTrace()
                            withContext(Dispatchers.Main) {
                                errorText = "Network Error: ${e.message ?: "Unknown network error"}"
                                isLoading = false
                            }
                        }
                    }
                } else {
                    Log.d("PredictButton", "Skipping API call due to invalid input.")
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .height(50.dp),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 3.dp)
            } else {
                Text("Get Fertility Prediction")
            }
        }

        if (errorText != null && !isLoading) {
            Text(
                text = errorText!!,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun ResultScreen(
    modifier: Modifier = Modifier,
    humanPrediction: String?,
    probabilities: String?,
    onGoBack: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "Prediction Result",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = humanPrediction ?: "No result yet",
                    style = MaterialTheme.typography.titleLarge, // Made it larger
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = probabilities ?: "",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onGoBack,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Filled.ArrowBack, contentDescription = "Go Back", modifier = Modifier.size(ButtonDefaults.IconSize))
            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
            Text("Predict Again")
        }
    }
}

@Preview(showBackground = true, name = "Input Screen Preview")
@Composable
fun SoilInputScreenPreview() {
    SoilfertilityAppTheme {
        SoilInputScreen(onPredictionSuccess = { _, _ -> })
    }
}

@Preview(showBackground = true, name = "Result Screen Preview")
@Composable
fun ResultScreenPreview() {
    SoilfertilityAppTheme {
        ResultScreen(
            humanPrediction = "Class 1 (e.g., Medium Fertility)",
            probabilities = "Probabilities: \nClass 0: 0.10\nClass 1: 0.80\nClass 2: 0.10",
            onGoBack = {}
        )
    }
}
