package com.example.kamara

import android.content.ContentValues
import android.content.Context
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.kamara.ui.theme.KamaraTheme
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import java.text.SimpleDateFormat
import java.util.Locale

// ==============================================================================
// 1. ACTIVIDAD PRINCIPAL (Punto de entrada de la aplicación Android)
// ==============================================================================
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // setContent inicializa la interfaz gráfica construida con Jetpack Compose
        setContent {
            KamaraTheme {
                // Inicia la pantalla principal de la cámara
                ComposableCameraScreen()
            }
        }
    }
}

// 2. PANTALLA PRINCIPAL CON GESTIÓN DE PERMISOS DE CÁMARA
/*
 @OptIn(ExperimentalPermissionsApi::class): Es un permiso para el compilador de Kotlin.
 Permite usar la API de permisos de Accompanist (aún marcada como experimental por Google).
*/
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ComposableCameraScreen() {
    // Guarda el estado del permiso de la cámara (si ha sido concedido o no)
    val cameraPermissionState = rememberPermissionState(android.Manifest.permission.CAMERA)

    // LaunchedEffect se ejecuta automáticamente al iniciar este componente
    LaunchedEffect(Unit) {
        // Si el usuario aún no ha concedido el permiso, se lo solicitamos interactiva mente
        if (!cameraPermissionState.status.isGranted) {
            cameraPermissionState.launchPermissionRequest()
        }
    }

    // Comprobamos si el permiso ha sido concedido
    if (cameraPermissionState.status.isGranted) {
        // Si hay permiso: mostramos el visor de la cámara y el botón
        CameraPreviewContent()
    } else {
        // Si el usuario rechazó el permiso: mostramos un mensaje explicativo
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Se requiere permiso de cámara para continuar.")
        }
    }
}

// 3. COMPONENTE DE VISTA PREVIA Y BOTÓN DE CAPTURA DE CÁMARA

@Composable
fun CameraPreviewContent() {
    // Obtenemos el contexto actual de Android y el ciclo de vida de la pantalla
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Caso de uso 1: ImageCapture (para tomar fotos en alta resolución)
    val imageCapture = remember { ImageCapture.Builder().build() }
    
    // Vista nativa de Android (PreviewView) que renderiza la imagen en tiempo real
    val previewView = remember { PreviewView(context) }

    // Configuramos y vinculamos CameraX cuando se crea la pantalla
    LaunchedEffect(Unit) {
        val cameraProviderFuture = androidx.camera.lifecycle.ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            // Obtenemos el proveedor de la cámara
            val cameraProvider = cameraProviderFuture.get()

            // Caso de uso 2: Preview (conecta el sensor de la cámara con la vista PreviewView)
            val preview = androidx.camera.core.Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            // Seleccionamos la cámara trasera por defecto (DEFAULT_FRONT_CAMERA para la frontal)
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                // Desvinculamos cualquier uso previo antes de volver a vincular
                cameraProvider.unbindAll()

                // Vinculamos la cámara al ciclo de vida de la Activity (se apaga/enciende automáticamente)
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageCapture
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(context))
    }

    // Diseño de la pantalla: Vista previa de cámara + Botón superpuesto en la parte inferior
    Box(modifier = Modifier.fillMaxSize()) {
        // Integramos la vista nativa de Android (PreviewView) dentro de Compose
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )

        // Botón para tomar la fotografía
        Button(
            onClick = { takePhotoAndSaveLocal(context, imageCapture) },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(32.dp)
        ) {
            Text("Tomar y Guardar Foto")
        }
    }
}

// ==============================================================================
// 4. FUNCIÓN PARA DISPARAR Y GUARDAR LA FOTO EN LA GALERÍA (Media Provider)
// ==============================================================================
private fun takePhotoAndSaveLocal(
    context: Context,
    imageCapture: ImageCapture
) {
    // Generamos un nombre único basado en la fecha y hora actual (ej. IMG_20261014_120000.jpg)
    val name = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
        .format(System.currentTimeMillis())

    // Definimos los metadatos para guardar el archivo en la galería pública mediante MediaStore
    val contentValues = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, "IMG_$name.jpg")
        put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
        // Carpeta pública donde se guardará la imagen: Pictures/MiAppCompose
        put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/MiAppCompose")
    }

    // Opciones de salida para configurar dónde guardar la foto resultante
    val outputOptions = ImageCapture.OutputFileOptions.Builder(
        context.contentResolver,
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        contentValues
    ).build()

    // Disparar la captura de foto
    imageCapture.takePicture(
        outputOptions,
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageSavedCallback {
            // Callback si la foto se guardó correctamente
            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                val savedUri = outputFileResults.savedUri
                Toast.makeText(
                    context,
                    "Foto guardada localmente en: $savedUri",
                    Toast.LENGTH_LONG
                ).show()
            }

            // Callback si ocurrió un error durante la captura o guardado
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