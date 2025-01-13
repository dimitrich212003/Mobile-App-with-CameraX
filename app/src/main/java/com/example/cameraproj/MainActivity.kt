package com.example.cameraproj

import android.content.ContentValues
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.OrientationEventListener
import android.view.Surface
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCapture.OutputFileOptions
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.cameraproj.databinding.ActivityMainBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.abs


class MainActivity : AppCompatActivity() {


    //Инициализируем ViewBinding с помощью Lazy (инициализируем mainBinding впервые, когда необходимо
    // его использовать для экономии ресурсов)
    private val mainBinding : ActivityMainBinding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

    // Уникальный идентификатор разрешения для доступа к камере и галлерее устройства
    private val multiplePermissionId = 14
    private val multiplePermissionNameList = if (Build.VERSION.SDK_INT > 33) {
        arrayListOf(
            android.Manifest.permission.CAMERA,
            android.Manifest.permission.RECORD_AUDIO,
            android.Manifest.permission.READ_EXTERNAL_STORAGE
        )
    } else {
        arrayListOf(
            android.Manifest.permission.CAMERA,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
            android.Manifest.permission.RECORD_AUDIO,
            android.Manifest.permission.READ_EXTERNAL_STORAGE

        )
    }

    // Используем при создании или запуске видео
    private lateinit var videoCapture: VideoCapture<Recorder>
    // Объект для записи видео
    private var recording: Recording? = null

    private var isPhoto = true

    // Используем при создании или запуске камеры (imageCapture.takePicture() - cделать фото)
    private lateinit var imageCapture: ImageCapture
    // Управление жизненным циклом камеры, помогает привязать камеру к интерфейсу приложения
    private lateinit var cameraProvider: ProcessCameraProvider
    // Управление фичами камеры (зум, фокусировка, вспышка и др.)
    private lateinit var camera: Camera
    // Позволяет выбрать переднюю или заднюю камеру
    private lateinit var cameraSelector: CameraSelector
    // указывает, какая камера выбрана в данный момент
    private var lensFacing = CameraSelector.LENS_FACING_BACK
    // Для отслеживания изменений ориентации устройства
    private var orientationEventListener : OrientationEventListener? = null




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(mainBinding.root)

        if (checkMultiplePermission()) {
            startCamera()
        }

        mainBinding.captureIB.setOnClickListener {
            if (isPhoto) {
                takePhoto()
            } else {
                captureVideo()
            }
        }

        mainBinding.flipCameraIB.setOnClickListener {
            lensFacing = if (lensFacing == CameraSelector.LENS_FACING_FRONT) {
                CameraSelector.LENS_FACING_BACK
            }
            else {
                CameraSelector.LENS_FACING_FRONT
            }
            bindCameraUserCases()
        }

        mainBinding.flashToggleIB.setOnClickListener {
            setFlashIcon(camera)
        }

        mainBinding.changeCameraToVideoIB.setOnClickListener {
            isPhoto = !isPhoto
            if (isPhoto) {
                mainBinding.changeCameraToVideoIB.setImageResource(R.drawable.video)
                mainBinding.captureIB.setImageResource(R.drawable.takephoto)
            } else {
                mainBinding.changeCameraToVideoIB.setImageResource(R.drawable.takephoto)
                mainBinding.captureIB.setImageResource(R.drawable.record)
            }

        }

        mainBinding.galleryIB.setOnClickListener {
            val intent = Intent(this, GalleryActivity::class.java)
            startActivity(intent)
        }

    }

    // Запись видео
    private fun captureVideo(){

        // отключение кнопки захвата видео, пока идет запись
        mainBinding.captureIB.isEnabled = false

        // скрытие лишних элементов интерфейса
        mainBinding.flashToggleIB.gone()
        mainBinding.flipCameraIB.gone()
        mainBinding.changeCameraToVideoIB.gone()
        mainBinding.galleryIB.gone()

        // Если запись идет она останавливается и сбрасывается
        // (Используем одну и ту же кнопку для начала и остановки записи)
        if (recording != null){
            recording?.stop()
            recording = null
            return
        }
        // Создание имени файла
        val fileName = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            .format(System.currentTimeMillis()) + ".mp4"

        // Подготовка методанных (ключ-значение) для сохранения видео
        val contentValues = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME,fileName)
            put(MediaStore.Video.Media.MIME_TYPE,"video/mp4")
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_MOVIES)
            }
        }

        // Настраиваем параметры сохранения видео
        val mediaStoreOutputOptions = MediaStoreOutputOptions
            // Взаимодействие с MediaStore и указываем, что видео должно быть сохранено в галерее
            .Builder(contentResolver, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
            // Передаем метаданные файла
            .setContentValues(contentValues)
            .build()

        recording = videoCapture.output
            // Подготавливаем запись с использованием настроенных параметров сохранения видео
            .prepareRecording(this, mediaStoreOutputOptions)

            // Включаем запись аудио, если разрешение предоставлено
            .apply {
                if (ActivityCompat.checkSelfPermission(
                        this@MainActivity,
                        android.Manifest.permission.RECORD_AUDIO
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    withAudioEnabled()
                }
            }
            // Запускаем запись в главном потоке
            .start(ContextCompat.getMainExecutor(this)){recordEvent->
                when(recordEvent){
                    // Событие срабатывает при начале записи
                    is VideoRecordEvent.Start -> {
                        // Меняем кнопку на стоп запись
                        mainBinding.captureIB.setImageResource(R.drawable.stop)
                        // Снова активируем ее
                        mainBinding.captureIB.isEnabled = true
                    }
                    // Событие срабатывает при завершении записи
                    is VideoRecordEvent.Finalize -> {
                        // Если нету ошибок, показываем уведомление, с URI сохраненного видео
                        if (!recordEvent.hasError()){
                            val message = "Видеозапись сохранена успешно: " + "${recordEvent.outputResults.outputUri}"
                            Toast.makeText(
                                this@MainActivity,
                                message,
                                Toast.LENGTH_LONG
                            ).show()

                            //Копируем файл в папку приложения для работы галереи
                            val savedURI = recordEvent.outputResults.outputUri
                            if (savedURI != null) {
                                try {
                                    val appFolder = externalMediaDirs[0]
                                    if (!appFolder.exists()) {
                                        appFolder.mkdir()
                                    }

                                    // Файл в папке приложения
                                    val destinationFile = File(appFolder, fileName)

                                    // Копируем файл
                                    contentResolver.openInputStream(savedURI)?.use { inputStream ->
                                        destinationFile.outputStream().use { outputStream ->
                                            inputStream.copyTo(outputStream)
                                        }
                                    }
                                }
                                catch (e: Exception) {
                                    Log.e("TAG", "Ошибка при копировании файла: ${e.message}")
                                }
                            }
                        }
                        // При ошибке запись останавливается и событие логируется
                        else{

                            recording?.close()
                            recording = null
                            Log.d("error", recordEvent.error.toString())
                        }
                        // Возвращаем иконки в исходное состояние
                        mainBinding.captureIB.setImageResource(R.drawable.record)
                        mainBinding.captureIB.isEnabled = true

                        mainBinding.flashToggleIB.visible()
                        mainBinding.flipCameraIB.visible()
                        mainBinding.changeCameraToVideoIB.visible()
                        mainBinding.galleryIB.visible()
                    }
                }
            }

    }


    // Создание и сохранение фотографии в определенную папку на телефоне
    private fun takePhoto() {

        // Создание папки для хранения фотографий
        val imageFolder = File(
            Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES
            ), "Images"
        )

        // Если папка не существует, создаем её
        if (!imageFolder.exists()) {
            imageFolder.mkdir()
        }

        // Создаем имя файла, засчет временной метки - делаем его уникальным
         val fileName = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            .format(System.currentTimeMillis()) + ".jpg"



        // Добавим метаданные изображения, чтобы сохранить его в системной галерее через MediaStore
        // Решаем баг с захватом изображения на Android 13
        // ContentValues - храним пары ключ-значение
        val contentValues = ContentValues().apply {
            // Имя файла
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            // Тип файла(mime-type)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            // Если Android 10 и выше
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                // Задаем относительный путь, куда сохраняем файл, обходим ограничения Scoped Storage
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Images")
            }
        }

        val outputOption =
            // Если Android 10 и выше
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                OutputFileOptions.Builder(
                    // Объект для взаимодействия с системной базой данных медиафайлов
                    contentResolver,
                    // Объект, указывающий на галерею для изображений. Сохраняем файлы в системной галлерее
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    // Метаданные
                    contentValues
                ).build()
            } else {
                // Создаем файл для изображения внутри директории
                val imageFile = File(imageFolder, fileName)
                OutputFileOptions.Builder(imageFile).build()
            }

        // захват экрана и сохранение фото в галлерею
        imageCapture.takePicture(
            // Указываем, куда и как сохранять фото
            outputOption,
            // Ассинхронная обработка метода сохранения фотографий и асинхронная обработка ошибок будет выполняться в основном потоке
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                // Обрабатываем метод сохранения фотографии
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    val message = "Фото готово ${outputFileResults.savedUri}"
                    Toast.makeText(
                        this@MainActivity,
                        message,
                        Toast.LENGTH_LONG
                    ).show()

                    //Копируем файл в папку приложения для работы галереи
                    val savedURI = outputFileResults.savedUri
                    if (savedURI != null) {
                        try {
                            val appFolder = externalMediaDirs[0]
                            if (!appFolder.exists()) {
                                appFolder.mkdir()
                            }

                            // Файл в папке приложения
                            val destinationFile = File(appFolder, fileName)

                            // Копируем файл
                            contentResolver.openInputStream(savedURI)?.use { inputStream ->
                                destinationFile.outputStream().use { outputStream ->
                                    inputStream.copyTo(outputStream)
                                }
                            }
                        }
                        catch (e: Exception) {
                            Log.e("TAG", "Ошибка при копировании файла: ${e.message}")
                        }
                    }
                }

                // Обрабатываем ошибки при сохранении фотографии
                override fun onError(exception: ImageCaptureException) {
                    Toast.makeText(
                        this@MainActivity,
                        exception.message.toString(),
                        Toast.LENGTH_LONG
                    ).show()
                }

            }
        )
    }

    private fun setFlashIcon(camera: Camera) {
        if (camera.cameraInfo.hasFlashUnit()) {
            if (camera.cameraInfo.torchState.value == 0) {
                camera.cameraControl.enableTorch(true)
                mainBinding.flashToggleIB.setImageResource(R.drawable.flashoff)
            }
            else {
                camera.cameraControl.enableTorch(false)
                mainBinding.flashToggleIB.setImageResource(R.drawable.flashon)
            }
        } else {
            Toast.makeText(
                this,
                "На вашем устройстве вспышка не поддерживается",
                Toast.LENGTH_LONG
            ).show()
            mainBinding.flashToggleIB.isEnabled = false
        }
    }

    // Проверка, получены ли все разрешения и запрос недостающих
    private fun checkMultiplePermission(): Boolean {
        val listPermissionNeeded = arrayListOf<String>()
        for (permission in multiplePermissionNameList) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    permission
                ) != PackageManager.PERMISSION_GRANTED)
            {
                listPermissionNeeded.add(permission)
            }
        }
        if (listPermissionNeeded.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                listPermissionNeeded.toTypedArray(),
                multiplePermissionId
            )
            return false
        }
        return true
    }

    // Дополнительно запрашиваем разрешения для приложения
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == multiplePermissionId) {

            if (grantResults.isNotEmpty()) {
                var isGrant = true

                // Проверяем все полученные разрешения, если хотя бы одно отконено, то переходим
                // к обработке отказов
                for(element in grantResults) {
                    if (element == PackageManager.PERMISSION_DENIED) {
                        isGrant = false
                    }
                }
                if (isGrant) {
                    startCamera()
                    // обработка отказов
                } else {
                    var someDenied = false
                    for (permission in permissions) {
                        // Решаем, надо ли показывать объяснение, зачем нужно разрешение пользователю
                        // (если не разу не запрашивали разрешение, вернет false
                        // если запрашивали, но пользователь отказал, вернет true
                        // если стоит галка (Don`t ask again), вернет false)
                        if (!ActivityCompat.shouldShowRequestPermissionRationale(
                                this,
                                permission
                            )) {
                            if (ActivityCompat.checkSelfPermission(
                                    this,
                                    permission
                                ) == PackageManager.PERMISSION_DENIED) {
                                someDenied = true
                            }
                        }
                    }
                    // Если остались отказы с опцией don`t ask again, то открываем настройки принудительно
                    if (someDenied) {
                        appSettingOpen(this)
                    }
                    // Если остались отказы но не с опцией don`t ask again, то показываем предупреждение
                    // с просьбой повторно получить разрешения
                    else {
                        warningPermissionDialog(this) {
                                _:DialogInterface, which: Int ->
                            when(which) {
                                DialogInterface.BUTTON_POSITIVE ->
                                    checkMultiplePermission()
                            }
                        }
                    }
                }
            }
        }
    }

    // запускаем процесс подключения камеры
    private fun startCamera() {
        // получаем экземпляр основного класса, управляющего камерой
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            // инициализируем провайдера камеры для привязки к интерфейсу приложения
            cameraProvider = cameraProviderFuture.get()
            // вызываем метод для привязки конкретных случаев (предварительный просмотр, захват изображения)
            bindCameraUserCases()
        }, ContextCompat.getMainExecutor(this)) // Слушатель должен выполняться в основном потоке
    }

    // Рассчитываем соотношение сторон экрана (4/3 - классическое, 16/9 - широкоформатное)
    private fun aspectRatio(width: Int, heigth: Int): Int {
        // Делим большую сторону на меньшую
        val previewRatio = maxOf(width, heigth).toDouble() / minOf(width, heigth)
        // Сравниваем результат с соотношениями 4/3 и 16/9, чтобы определить, какое из них ближе
        return if (abs(previewRatio - 4.0 / 3.0) <= abs(previewRatio - 16.0 / 9.0)) {
            AspectRatio.RATIO_4_3
        } else {
            AspectRatio.RATIO_16_9
        }
    }

    // Настраиваем и привязываем use case-ы, такие как предварительный просмотр(Preview), захват изображения(ImageCapture)
    private fun bindCameraUserCases() {
        // Определяем соотношение сторон
        val screenAspectRatio = aspectRatio(
            mainBinding.previewView.width,
            mainBinding.previewView.height
        )
        // Получаем текущую ориентацию экрана
        val rotation = mainBinding.previewView.display.rotation;

        // Выбираем разрешение экрана с учетом соотношения сторон и fallback-правил
        val resolutionSelector = ResolutionSelector.Builder()
            .setAspectRatioStrategy(
                AspectRatioStrategy(
                    screenAspectRatio,
                    AspectRatioStrategy.FALLBACK_RULE_AUTO // Выбор подходящего разрешения
                    // в случае, если камера устройства не поддерживает разрешение 4/3 или 16/9
                )
            )
            .build()


        // настраиваем выбор камеры и присваиваем заднюю камеру по дефолту
        cameraSelector = CameraSelector.Builder()
            .requireLensFacing(lensFacing)
            .build()

        // Настраиваем ориентацию фотографии в зависимости от ориентации устройства
        orientationEventListener = object : OrientationEventListener(this) {
            // Вызывается каждый раз при повороте устройства
            override fun onOrientationChanged(orientation: Int) {
                val myRotation = when (orientation) {
                    // повернули по часовой стрелке (верх телефона смотрит вправо)
                    in 45..134 -> Surface.ROTATION_270
                    // повернули по часовой стрелке (верх телефона смотрит вниз)
                    in 135..224 -> Surface.ROTATION_180
                    // повернули по часовой стрелке (верх телефона смотрит влево)
                    in 225..314 -> Surface.ROTATION_90
                    // обычное положение (верх телефона смотрит вверх)
                    else -> Surface.ROTATION_0
                }
                imageCapture.targetRotation = myRotation
                videoCapture.targetRotation = myRotation

            }
        }
        orientationEventListener?.enable()

        // Устанавливаем разрешение экрана и ориентацию а также привязиваем
        // surfaceProvider для отображения изображения с камеры на экране устройства
        val preview = Preview.Builder()
            .setResolutionSelector(resolutionSelector)
            .setTargetRotation(rotation)
            .build()
            .also {
                it.surfaceProvider = mainBinding.previewView.surfaceProvider
            }

        // Настраиваем качество записи видео
        val recorder = Recorder.Builder()
            .setQualitySelector(
                QualitySelector.from(
                    Quality.HIGHEST,
                    FallbackStrategy.higherQualityOrLowerThan(Quality.SD)
                )
            )
            .build()

        videoCapture = VideoCapture.withOutput(recorder).apply {
            targetRotation = rotation
        }

        // Настраиваем случай использования Захвата изображения (Фото + сохранение в галлерею)
        imageCapture = ImageCapture.Builder()
            // Указываем качество изображения
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
            // Указываем разрешение экрана
            .setResolutionSelector(resolutionSelector)
            // Указываем ориентацию устройства
            .setTargetRotation(rotation)
            .build()

        try {
            // Отвязываем предыдущие случаи использования
            cameraProvider.unbindAll()
            // Привязываем use case-ы к жизненному циклу активности
            camera = cameraProvider.bindToLifecycle(
                // привязываем возможность выбора камеры, предпросмотр перед фото/видео и захват экрана
                this, cameraSelector, preview, imageCapture, videoCapture
            )
        } catch (e: Exception) {
            Log.e("TAG", "Ошибка привязки Use Case: ${e.message}", e)
        }
    }

    override fun onResume() {
        super.onResume()
        orientationEventListener?.enable()
    }

    override fun onPause() {
        super.onPause()
        orientationEventListener?.disable()
    }

}