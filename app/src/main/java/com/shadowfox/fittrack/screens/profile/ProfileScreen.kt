package com.shadowfox.fittrack.screens.profile

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.shadowfox.fittrack.data.Achievement
import com.yalantis.ucrop.UCrop
import java.io.File

@Composable
fun ProfileScreen(
    navController: NavController,
    application: Application
) {
    val viewModel: ProfileViewModel = viewModel(
        factory = ProfileViewModel.Factory(application)
    )

    val badges by viewModel.earnedBadges.collectAsState()
    val profile by viewModel.userProfile.collectAsState()
    val steps by viewModel.totalSteps.collectAsState()
    val weight by viewModel.currentWeight.collectAsState()
    val water by viewModel.waterIntake.collectAsState()
    val bmi by viewModel.userBMI.collectAsState()
    val bmiCategory by viewModel.bmiCategory.collectAsState()
    val bmr by viewModel.userBMR.collectAsState()

    var isEditing by remember { mutableStateOf(false) }
    var showImageOptions by remember { mutableStateOf(false) }

    var editName by remember { mutableStateOf("") }
    var editAge by remember { mutableStateOf("") }
    var editHeight by remember { mutableStateOf("") }
    var editGoalWeight by remember { mutableStateOf("") }
    var editProfilePic by remember { mutableStateOf("") }

    val context = LocalContext.current

    // Holds the URI for the camera to save the temp image to
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }

    // --- 1. uCrop Launcher (The Cropper) ---
    val uCropLauncher = rememberLauncherForActivityResult(
        contract = object : ActivityResultContract<List<Uri>, Uri?>() {
            override fun createIntent(context: Context, input: List<Uri>): android.content.Intent {
                val sourceUri = input[0]
                val destinationUri = Uri.fromFile(File(context.cacheDir, "cropped_profile_${System.currentTimeMillis()}.jpg"))

                val options = UCrop.Options()
                options.setCircleDimmedLayer(true) // Key for circular profile pics
                options.setShowCropGrid(false)
                options.setToolbarTitle("Crop Profile Photo")
                options.setToolbarColor(android.graphics.Color.parseColor("#FFFFFF"))
                options.setStatusBarColor(android.graphics.Color.parseColor("#F0F0F0"))
                options.setToolbarWidgetColor(android.graphics.Color.BLACK)

                return UCrop.of(sourceUri, destinationUri)
                    .withAspectRatio(1f, 1f)
                    .withOptions(options)
                    .getIntent(context)
            }

            override fun parseResult(resultCode: Int, intent: android.content.Intent?): Uri? {
                return if (resultCode == android.app.Activity.RESULT_OK && intent != null) {
                    UCrop.getOutput(intent)
                } else {
                    null
                }
            }
        },
        onResult = { uri ->
            if (uri != null) {
                editProfilePic = uri.toString()
                if (!isEditing) {
                    viewModel.saveUserProfile(profile.name, profile.age, profile.height, profile.startWeight, profile.goalWeight, editProfilePic)
                }
            }
        }
    )

    // --- 2. Camera Launcher ---
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
        onResult = { success ->
            if (success && tempCameraUri != null) {
                // Photo taken! Send to cropper immediately.
                uCropLauncher.launch(listOf(tempCameraUri!!))
            }
        }
    )

    // --- 3. Gallery Launcher ---
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                // Photo picked! Send to cropper.
                uCropLauncher.launch(listOf(uri))
            }
        }
    )

    // --- 4. Permission Launcher (For Camera) ---
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                // Create temp file and launch camera
                val tmpFile = File.createTempFile("tmp_image_file", ".jpg", context.cacheDir).apply {
                    createNewFile()
                    deleteOnExit()
                }
                // Use FileProvider to get safe URI
                val uri = FileProvider.getUriForFile(context, "${application.packageName}.provider", tmpFile)
                tempCameraUri = uri
                cameraLauncher.launch(uri)
            } else {
                Toast.makeText(context, "Camera permission required to take photo", Toast.LENGTH_SHORT).show()
            }
        }
    )

    // Sync UI
    LaunchedEffect(profile) {
        if (!isEditing) {
            editName = profile.name
            editAge = profile.age
            editHeight = profile.height
            editGoalWeight = profile.goalWeight
            editProfilePic = profile.profilePicUri
        }
    }

    LaunchedEffect(Unit) { viewModel.refreshProfileData() }

    // --- OPTION DIALOGS ---
    if (showImageOptions) {
        AlertDialog(
            onDismissRequest = { showImageOptions = false },
            title = { Text(if (editProfilePic.isNotEmpty()) "Edit Profile Picture" else "Add Profile Picture") },
            text = {
                Column {
                    // Option: Camera
                    ListItem(
                        headlineContent = { Text("Take a picture") },
                        leadingContent = { Icon(Icons.Default.CameraAlt, null) },
                        modifier = Modifier.clickable {
                            showImageOptions = false
                            val permission = Manifest.permission.CAMERA
                            if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
                                val tmpFile = File.createTempFile("tmp_image_file", ".jpg", context.cacheDir).apply {
                                    createNewFile()
                                    deleteOnExit()
                                }
                                val uri = FileProvider.getUriForFile(context, "${application.packageName}.provider", tmpFile)
                                tempCameraUri = uri
                                cameraLauncher.launch(uri)
                            } else {
                                permissionLauncher.launch(permission)
                            }
                        }
                    )

                    // Option: Gallery (Change or Add)
                    ListItem(
                        headlineContent = { Text(if (editProfilePic.isNotEmpty()) "Choose from Gallery" else "Add from Gallery") },
                        leadingContent = { Icon(Icons.Default.Image, null) },
                        modifier = Modifier.clickable {
                            showImageOptions = false
                            galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        }
                    )

                    // Options ONLY if pic exists
                    if (editProfilePic.isNotEmpty()) {
                        // Edit Current
                        ListItem(
                            headlineContent = { Text("Edit current photo") },
                            leadingContent = { Icon(Icons.Default.Edit, null) },
                            modifier = Modifier.clickable {
                                showImageOptions = false
                                try {
                                    // Re-crop the existing image URI
                                    uCropLauncher.launch(listOf(Uri.parse(editProfilePic)))
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Cannot edit original file", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                        // Remove
                        ListItem(
                            headlineContent = { Text("Remove photo", color = MaterialTheme.colorScheme.error) },
                            leadingContent = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) },
                            modifier = Modifier.clickable {
                                showImageOptions = false
                                editProfilePic = ""
                                if (!isEditing) {
                                    viewModel.saveUserProfile(profile.name, profile.age, profile.height, profile.startWeight, profile.goalWeight, "")
                                }
                            }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showImageOptions = false }) { Text("Cancel") }
            }
        )
    }

    // --- MAIN UI ---
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        // Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .clickable { showImageOptions = true },
                contentAlignment = Alignment.Center
            ) {
                if (editProfilePic.isNotEmpty()) {
                    AsyncImage(
                        model = editProfilePic,
                        contentDescription = "Profile Picture",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(Icons.Default.Person, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
                }
                // Small overlay icon to indicate editability
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomEnd) {
                    Icon(Icons.Default.Edit, null, modifier = Modifier.size(20.dp).background(Color.White, CircleShape).padding(2.dp), tint = Color.Black)
                }
            }

            Spacer(Modifier.width(16.dp))

            if (isEditing) {
                Column(Modifier.weight(1f)) {
                    OutlinedTextField(value = editName, onValueChange = { editName = it }, label = { Text("Name") })
                }
            } else {
                Column(Modifier.weight(1f)) {
                    Text(text = profile.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text(text = "Goal: ${profile.goalWeight}kg", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                }
            }

            IconButton(onClick = {
                if (isEditing) {
                    viewModel.saveUserProfile(editName, editAge, editHeight, profile.startWeight, editGoalWeight, editProfilePic)
                }
                isEditing = !isEditing
            }) {
                Icon(if (isEditing) Icons.Default.Save else Icons.Default.Edit, contentDescription = "Edit")
            }
        }

        Spacer(Modifier.height(24.dp))

        // Stats
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            StatCard(title = "Steps", value = "$steps", modifier = Modifier.weight(1f))
            Spacer(Modifier.width(8.dp))
            StatCard(title = "Water", value = "${water}ml", modifier = Modifier.weight(1f))
            Spacer(Modifier.width(8.dp))
            StatCard(title = "Weight", value = "${weight}kg", modifier = Modifier.weight(1f))
        }

        // Edit Fields
        if (isEditing) {
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                OutlinedTextField(value = editAge, onValueChange = { editAge = it }, label = { Text("Age") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                Spacer(Modifier.width(8.dp))
                OutlinedTextField(value = editHeight, onValueChange = { editHeight = it }, label = { Text("Height (cm)") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            }
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = editGoalWeight, onValueChange = { editGoalWeight = it }, label = { Text("Goal Weight (kg)") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
        } else if (bmi > 0) {
            Spacer(Modifier.height(16.dp))
            HealthInsightsCard(bmi, bmiCategory, bmr)
        }

        Spacer(Modifier.height(32.dp))

        // Badges
        Text("Earned Badges", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))

        AnimatedVisibility(visible = badges.isNotEmpty(), enter = slideInVertically() + fadeIn()) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(badges) { badge -> BadgeItem(badge) }
            }
        }

        if (badges.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().height(100.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                Text("Start tracking to unlock badges!", color = Color.Gray)
            }
        }
    }
}

// Helpers (HealthInsightsCard, StatCard, BadgeItem) stay the same as before
@Composable
fun HealthInsightsCard(bmi: Float, category: String, bmr: Int) {
    val color = when(category) {
        "Normal Weight" -> Color(0xFF4CAF50)
        "Overweight" -> Color(0xFFFF9800)
        "Obese" -> Color(0xFFF44336)
        else -> Color(0xFF2196F3)
    }
    val progress by animateFloatAsState(
        targetValue = (bmi / 40f).coerceIn(0f, 1f),
        animationSpec = tween(1000)
    )
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(4.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Favorite, null, tint = color)
                Spacer(Modifier.width(8.dp))
                Text("Health Insights", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("BMI", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                    Text("$bmi", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text(category, style = MaterialTheme.typography.bodySmall, color = color)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Daily Burn (BMR)", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                    Row(verticalAlignment = Alignment.Bottom) {
                        Icon(Icons.Default.LocalFireDepartment, null, modifier = Modifier.size(20.dp), tint = Color(0xFFFF5722))
                        Text("$bmr kcal", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                color = color,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
        }
    }
}

@Composable
fun StatCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = title, style = MaterialTheme.typography.labelSmall)
            Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun BadgeItem(badge: Achievement) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier.size(70.dp).clip(CircleShape).background(Color(0xFFFFD700).copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
            Icon(badge.icon, contentDescription = badge.title, tint = Color(0xFFFFA000))
        }
        Spacer(Modifier.height(4.dp))
        Text(text = badge.title, style = MaterialTheme.typography.labelSmall, maxLines = 1)
    }
}