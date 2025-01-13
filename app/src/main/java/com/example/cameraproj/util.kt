package com.example.cameraproj

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.view.View
import android.widget.Toast
import com.google.android.material.dialog.MaterialAlertDialogBuilder

//  открытие настроек и инструкция для разрешения доступа к камере
fun appSettingOpen(context: Context) {
    // Показываем уведомление
    Toast.makeText(
        context,
        "Перейдите в настройки и разрешите приложению доступ к камере",
        Toast.LENGTH_LONG
    ).show()

    // Открываем настройки для текущего приложения с помощью Intent
    val settingIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
    settingIntent.data = Uri.parse("package:${context.packageName}")
    context.startActivity(settingIntent)
}

// отображает окно с предупреждением о необходимости предоставить разрешения
// для корректной работы приложения
fun warningPermissionDialog(context: Context, listener : DialogInterface.OnClickListener) {
    MaterialAlertDialogBuilder(context)
        .setMessage("Для корректной работы разрешите приложению доступ к камере (перейти в настройки)")
        .setCancelable(false)
        .setPositiveButton("Ok", listener)
        .create()
        .show()
}

// Используем при старте записи видео, чтобы динамически управлять видимостью элементов интерфейса

// Элемент будет отображаться на экране
fun View.visible() {
    visibility = View.VISIBLE
}

// Элемент будет скрыт, и он не займет места в макете
fun View.gone() {
    visibility = View.GONE
}