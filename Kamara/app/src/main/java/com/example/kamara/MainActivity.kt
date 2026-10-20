package com.example.kamara

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.view.PreviewView
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import com.example.kamara.ui.theme.KamaraTheme
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.android.gms.location.LocationServices
import java.text.SimpleDateFormat
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            KamaraTheme {
                ComposableCameraScreen()
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ComposableCameraScreen() {
    // Solicitamos tanto el permiso de cámara como los permisos de ubicación GPS
    val permissionsState = rememberMultiplePermissionsState(
        listOf(
            android.Manifest.permission.CAMERA,
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )

    LaunchedEffect(Unit) {
        if (!permissionsState.allPermissionsGranted) {
            permissionsState.launchMultiplePermissionRequest()
        }
    }

    // Comprobamos si el permiso de la cámara está concedido
    val cameraGranted = permissionsState.permissions.find {
        it.permission == android.Manifest.permission.CAMERA
    }?.status?.isGranted ?: false

    if (cameraGranted) {
        CameraPreviewContent()
    } else {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Se requiere permiso de cámara para continuar.")
        }
    }
}

@SuppressLint("MissingPermission")
@Composable
fun CameraPreviewContent()  {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Estado para cambiar entre cámara trasera y frontal
    var cameraSelector by remember { mutableStateOf(CameraSelector.DEFAULT_BACK_CAMERA) }

    // Estado para guardar la Uri de la última foto tomada
    var lastPhotoUri by remember { mutableStateOf<Uri?>(null) }

    // Estado para evitar que abra la web cientos de veces seguidas por cada fotograma
    var lastScannedUrl by remember { mutableStateOf("") }

    val imageCapture = remember { ImageCapture.Builder().build() }
    val previewView = remember { PreviewView(context) }

    // Reconfigura la cámara si cambia el selector (trasera/frontal)
    LaunchedEffect(cameraSelector) {
        val cameraProviderFuture = androidx.camera.lifecycle.ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            //vista previa
            val preview = androidx.camera.core.Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

// Configuración del analizador QR
            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also { analysis ->
                    analysis.setAnalyzer(
                        ContextCompat.getMainExecutor(context),
                        QrCodeAnalyzer { qrTexto ->
                            // 1. Comprobamos si el texto leído es una URL válida (empieza por http:// o https://)
                            if ((qrTexto.startsWith("http://") || qrTexto.startsWith("https://")) && qrTexto != lastScannedUrl) {
                                lastScannedUrl = qrTexto // Guardamos la URL para no reabrirla inmediatamente

                                try {
                                    // 2. Creamos la intención de abrir el navegador
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(qrTexto))
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "No se pudo abrir la URL: $qrTexto", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    )
                }

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageCapture,
                    //se añade el analizador QR
                    imageAnalysis
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(context))
    }



    Box(modifier = Modifier.fillMaxSize()) {
        // 1. Vista previa de la cámara
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )

        // 2. Miniatura de la última foto (Abajo a la izquierda)
        lastPhotoUri?.let { uri ->
            IconButton(
                onClick = {
                    // Abre la imagen a pantalla completa en la Galería
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, "image/*")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(intent)
                },
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(32.dp)
                    .size(56.dp)
                    .clip(CircleShape)
                    .border(2.dp, Color.White, CircleShape)
            ) {
                AsyncImage(
                    model = uri,
                    contentDescription = "Última foto tomada",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // 3. Botón obturador (Centro abajo, redondo y sin texto)
        Button(
            onClick = {
                val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
                try {
                    fusedLocationClient.lastLocation
                        .addOnSuccessListener { location: Location? ->
                            takePhotoAndSaveLocal(context, imageCapture, location) { newUri ->
                                lastPhotoUri = newUri
                            }
                        }
                        .addOnFailureListener {
                            takePhotoAndSaveLocal(context, imageCapture, null) { newUri ->
                                lastPhotoUri = newUri
                            }
                        }
                } catch (e: Exception) {
                    takePhotoAndSaveLocal(context, imageCapture, null) { newUri ->
                        lastPhotoUri = newUri
                    }
                }
            },
            shape = CircleShape,
            contentPadding = PaddingValues(0.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(32.dp)
                .size(70.dp)
        ) {
            // Sin texto
        }

        // 4. Botón para conmutar cámara (Abajo a la derecha)
        IconButton(
            onClick = {
                cameraSelector = if (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
                    CameraSelector.DEFAULT_FRONT_CAMERA
                } else {
                    CameraSelector.DEFAULT_BACK_CAMERA
                }
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(32.dp)
                .size(56.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = "Invertir cámara",
                tint = Color.White,
                modifier = Modifier.size(36.dp)
            )
        }
    }
}

private fun takePhotoAndSaveLocal(
    context: Context,
    imageCapture: ImageCapture,
    location: Location?,
    onPhotoCaptured: (Uri?) -> Unit
) {
    // 1. Crear los metadatos de la imagen con la posición GPS recibida
    val photoMetadata = ImageCapture.Metadata().apply {
        this.location = location
    }

    val name = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
        .format(System.currentTimeMillis())

    val contentValues = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, "IMG_$name.jpg")
        put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
        put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/MiAppCompose")
    }

    val outputOptions = ImageCapture.OutputFileOptions.Builder(
        context.contentResolver,
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        contentValues
    )
        .setMetadata(photoMetadata)
        .build()

    imageCapture.takePicture(
        outputOptions,
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                val savedUri = outputFileResults.savedUri
                onPhotoCaptured(savedUri)
                Toast.makeText(
                    context,
                    "Foto guardada localmente",
                    Toast.LENGTH_SHORT
                ).show()
            }

            override fun onError(exception: ImageCaptureException) {
                Toast.makeText(
                    context,
                    "Error al guardar la foto: ${exception.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    )
}