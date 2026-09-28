# Solución: Uso e Importación de `Icons.Default.Search` y Errores Composable

Este documento explica cómo importar y utilizar el icono `Icons.Default.Search` dentro de un componente `SearchBar` en Jetpack Compose (Material 3), así como la resolución de errores de contexto en funciones `@Composable`.

---

## 1. Importaciones en Kotlin

Para poder utilizar `Icons.Default.Search` en tus composables, debes incluir los siguientes `import` en la parte superior de tu archivo Kotlin (por ejemplo, `MainActivity.kt`):

```kotlin
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
```

> **Nota:** `Icons.Default.Search` es un alias de `Icons.Filled.Search`.

---

## 2. Código de Ejemplo (`SearchBar`)

A continuación se muestra la implementación del componente `SearchBar` utilizando `TextField` de Material 3:

```kotlin
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

@Composable
fun SearchBar(
    modifier: Modifier = Modifier
) {
    TextField(
        value = "",
        onValueChange = {},
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null
            )
        },
        colors = TextFieldDefaults.colors(
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            focusedContainerColor = MaterialTheme.colorScheme.surface
        ),
        placeholder = {
            Text(stringResource(R.string.placeholder_search))
        },
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
    )
}
```

---

## 3. Configuración en Gradle (Opcional)

El icono `Search` ya está incluido dentro del conjunto básico de Material Icons (`material3`). 

Si en el futuro deseas utilizar **iconos extendidos** adicionales, debes agregar la biblioteca en el archivo **`app/build.gradle.kts`** (del módulo `:app`, no en el `build.gradle.kts` raíz del proyecto):

```kotlin
// app/build.gradle.kts
dependencies {
    implementation("androidx.compose.material:material-icons-extended")
}
```

O si usas el catálogo de versiones (`gradle/libs.versions.toml`):

```toml
# gradle/libs.versions.toml
[libraries]
androidx-compose-material-icons-extended = { module = "androidx.compose.material:material-icons-extended" }
```

```kotlin
// app/build.gradle.kts
dependencies {
    implementation(libs.androidx.compose.material.icons.extended)
}
```

---

## 4. Solución al error: `@Composable invocations can only happen from the context of a @Composable function`

### ¿Qué significa el error?
En Jetpack Compose, las funciones marcadas con la anotación `@Composable` **solo se pueden llamar desde el contexto de otra función o bloque `@Composable`**.

### Causa del problema
El error ocurrió al usar una función de tema no resuelta o inexistente (por ejemplo, `MySootheTheme` copiada de un codelab en lugar de `CodelaBasicLayoutTheme`):

```kotlin
@Preview(showBackground = true, backgroundColor = 0xFFF5F0EE)
@Composable
fun FavoriteCollectionCardPreview() {
    MySootheTheme { // <-- MySootheTheme no existe en el proyecto
        FavoriteCollectionCard( // <-- ERROR: Invocación Composable desde un contexto no Composable
            text = R.string.ordutegia,
            drawable = R.drawable.ordutegia,
            modifier = Modifier.padding(8.dp)
        )
    }
}
```

Al no estar definida la función `MySootheTheme`, el compilador de Kotlin no puede saber que el bloque de llaves `{ ... }` debe tratarse como una lambda `@Composable () -> Unit`. Por tanto, lo interpreta como una lambda normal (`() -> Unit`), haciendo que la llamada a `FavoriteCollectionCard` dentro del bloque pierda el contexto Composable y falle.

### Solución
Utilizar la función de tema real generada para tu proyecto (`CodelaBasicLayoutTheme`):

```kotlin
@Preview(showBackground = true, backgroundColor = 0xFFF5F0EE)
@Composable
fun FavoriteCollectionCardPreview() {
    CodelaBasicLayoutTheme { // <-- Tema correcto con ámbito @Composable () -> Unit
        FavoriteCollectionCard(
            text = R.string.ordutegia,
            drawable = R.drawable.ordutegia,
            modifier = Modifier.padding(8.dp)
        )
    }
}
```
