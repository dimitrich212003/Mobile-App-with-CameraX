package com.example.cameraproj

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.cameraproj.databinding.ActivityGalleryBinding
import java.io.File

class GalleryActivity : AppCompatActivity() {

    private val galleryBinding : ActivityGalleryBinding by lazy {
        ActivityGalleryBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(galleryBinding.root)


        // Получаем путь к галерее
        val directory = File(externalMediaDirs[0].absolutePath)
        // Переносим фото и видео в массив (оставляем только файлы, исключая папки)
        val files = directory.listFiles()?.filter { it.isFile }?.toMutableList() ?: mutableListOf()



        // Создаем экземпляр кастомного адаптера, передавая в него перевернутый массив
        // Передаем лямбду для обработки нажатий

        lateinit var adapter: GalleryAdapter

        adapter = GalleryAdapter(files.reversed().toTypedArray(),
            { file ->
                handleFileClick(file)
            },
            { file ->

                // Удаляем файл
                files.remove(file)
                // Обновляем адаптер
                adapter.notifyDataSetChanged()

                // При необходимости удаляем файл из хранилища
                if (file.exists()) {
                    file.delete()
                }
            }
        )
        // Устанавливаем адаптер для представления
        // Адаптер отвечает за связывание данных (списка файлов) с элементами интерфейса (ячейками RecyclerView)
        galleryBinding.gallery.adapter = adapter
        // отображаем элементы списка в линейном порядке (вертикально или горизонтально), по дефолту - вертикально
        galleryBinding.gallery.layoutManager = LinearLayoutManager(this)
    }

    private fun handleFileClick(file: File) {
        if (file.extension in listOf("mp4", "avi", "mkv")) {
            // Если это видео, запускаем VideoPlayerActivity
            val intent = Intent(this, VideoPlayerActivity::class.java).apply {
                putExtra("video_path", file.absolutePath)
            }
            startActivity(intent)
        }
    }
}