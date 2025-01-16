package com.example.cameraproj

import android.media.MediaMetadataRetriever
import android.os.Build
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.cameraproj.databinding.ListItemImageBinding
import java.io.File

// Адаптер автоматизирует процесс создания ImageView, загрузки в него изображения и управление прокруткой списка и запуском просмотра видео
//Адаптер отвечает за связывание данных (списка файлов) с элементами интерфейса (ячейками RecyclerView)
class GalleryAdapter(private val fileArray: Array<File>, private val onFileClick: (File) -> Unit, private val onDeleteClick: (File) -> Unit) : RecyclerView.Adapter<GalleryAdapter.ViewHolder>() {
    // Этот класс хранит ссылки на элементы интерфейса, чтобы не приходилось каждый раз искать их при прокрутке списка
    // получаем доступ к элементам макета list_item_image.xml
    class ViewHolder(private val binding: ListItemImageBinding) : RecyclerView.ViewHolder(binding.root) {
        @RequiresApi(Build.VERSION_CODES.M)
        fun bind(file: File, onFileClick: (File) -> Unit, onDeleteClick: (File) -> Unit) {
            // определяем, является ли файл видео
            val isVideo = file.extension in listOf("mp4", "avi", "mkv")

            // Используем Glide для изображений и значок для видео
            if (isVideo) {
                // Используем MediaMetadataRetriever для получения превью видео
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(file.absolutePath)

                val bitmap = retriever.getFrameAtTime(1000000, MediaMetadataRetriever.OPTION_CLOSEST) // Кадр на 1 секунде
                retriever.release()

                Glide.with(binding.root)
                    .load(bitmap)
                    .into(binding.localImg)

                // Добавляем иконку воспроизведения поверх превью
                binding.playIcon.visible()


                // Устанавливаем обработчик нажатия
                binding.root.setOnClickListener {
                    onFileClick(file)
                }
            } else {
                // Связываем файлы изображений с элементом интерфейса (ImageView)
                Glide.with(binding.root)
                    .load(file)
                    .into(binding.localImg)

                binding.playIcon.gone()
                binding.root.setOnClickListener(null)
            }

            binding.deleteButton.setOnClickListener {
                onDeleteClick(file) // Вызываем обработчик из активности
            }
        }
    }

    // Создаем новый экземпляр класса ViewHolder
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // layoutInflater использ. для преобразования XML макета в объекты View
        val layoutInflater = LayoutInflater.from(parent.context)
        // Возвращаем новый ViewHolder, передавая ему "надутый макет" list_item_image.xml
        return ViewHolder(ListItemImageBinding.inflate(layoutInflater, parent, false))
    }

    // Возвращаем кол-во изображений
    override fun getItemCount(): Int {
        return fileArray.size
    }

    // отображаем изображение для определенного ImageView, связывая их.
    @RequiresApi(Build.VERSION_CODES.M)
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(fileArray[position], onFileClick, onDeleteClick)
    }
}