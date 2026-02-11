# Cubiks PRO

An Android app that photographs CUBIKS psychometric test questions and solves them using Claude's vision API.

## Features

- **Camera Capture**: Take photos of CUBIKS questions with a guided viewfinder
- **AI-Powered Solving**: Uses the Anthropic Claude API (vision) to analyze and solve questions
- **Supported Question Types**:
  - Spatial reasoning (cube folding/unfolding, rotations)
  - Numerical reasoning (data interpretation, sequences)
  - Verbal reasoning (logical deductions)
  - Abstract reasoning (pattern sequences)
  - Logiks (logical grid puzzles)
- **Step-by-Step Explanations**: Shows the reasoning process, not just the answer
- **Confidence Levels**: Indicates how confident the model is in its answer

## Setup

### Prerequisites

- Android Studio Hedgehog (2023.1.1) or later
- Android SDK 34
- Kotlin 1.9+
- An Anthropic API key ([get one here](https://console.anthropic.com))

### Configuration

1. Clone the repository
2. Set your API key using **one** of these methods:

   **Option A** - In `local.properties`:
   ```properties
   ANTHROPIC_API_KEY=sk-ant-your-key-here
   ```

   **Option B** - In the app's Settings screen at runtime

3. Open in Android Studio and run on a device with a camera

## Architecture

```
app/src/main/java/com/cubikspro/
├── MainActivity.kt              # Entry point
├── api/
│   └── AnthropicApiClient.kt   # Claude API integration with vision
├── camera/
│   └── CameraCapture.kt        # CameraX photo capture
└── ui/
    ├── theme/Theme.kt           # Material 3 theming
    ├── viewmodel/SolverViewModel.kt  # State management
    └── screens/
        ├── Navigation.kt        # Compose navigation
        ├── CameraScreen.kt      # Camera viewfinder UI
        ├── ResultScreen.kt      # Solution display
        └── SettingsScreen.kt    # API key configuration
```

- **Jetpack Compose** for UI
- **CameraX** for camera capture
- **OkHttp** for API networking
- **DataStore** for persisting the API key
- **MVVM** architecture with `StateFlow`

## How It Works

1. User captures a photo of a CUBIKS question
2. The image is encoded to base64 and sent to the Claude API
3. Claude analyzes the image using its vision capabilities
4. The response is parsed into structured fields (answer, analysis, explanation)
5. Results are displayed with the answer, confidence level, and step-by-step explanation
