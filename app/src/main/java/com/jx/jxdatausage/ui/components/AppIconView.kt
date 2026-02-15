package com.jx.jxdatausage.ui.components

import android.widget.ImageView
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun AppIconView(
    packageName: String?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val packageManager = context.packageManager
    AndroidView(
        modifier = modifier.size(40.dp),
        factory = { imageContext ->
            ImageView(imageContext).apply {
                scaleType = ImageView.ScaleType.CENTER_CROP
            }
        },
        update = { imageView ->
            val drawable = packageName?.let {
                runCatching { packageManager.getApplicationIcon(it) }.getOrNull()
            } ?: context.getDrawable(android.R.drawable.sym_def_app_icon)
            imageView.setImageDrawable(drawable)
        }
    )
}
